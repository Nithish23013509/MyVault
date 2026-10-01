package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.FirebaseAuthManager
import com.example.crypto.AESEncryptionManager
import com.example.crypto.PasswordGenerator
import com.example.crypto.PasswordStrengthCalculator
import com.example.data.AppDatabase
import com.example.data.FirestoreVaultRepository
import com.example.data.VaultCollection
import com.example.data.VaultEntry
import com.example.data.VaultEntryDao
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.data.VaultRepository
import com.example.security.BiometricAuthManager
import com.example.security.SecurityPreferences
import com.example.util.NetworkMonitor
import com.example.util.SyncNotificationHelper
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab {
    HOME, ITEMS, FAVORITES, SETTINGS
}

data class ItemsFilter(
    val type: VaultItemType? = null,
    val collectionId: String? = null,
    val tag: String? = null,
    val favoritesOnly: Boolean = false,
    val pinnedOnly: Boolean = false,
    val sensitiveOnly: Boolean = false,
    val archived: Boolean = false
)

data class RecordTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val fieldCount: Int,
    val fields: List<Pair<String, String>>
)

sealed interface VaultEntriesUiState {
    data object Loading : VaultEntriesUiState
    data class Success(val entries: List<VaultEntry>) : VaultEntriesUiState
    data class Error(val message: String) : VaultEntriesUiState
}

data class SecurityAudit(
    val healthScore: Int = 100,
    val totalItems: Int = 0,
    val weakPasswordsCount: Int = 0,
    val reusedPasswordsCount: Int = 0,
    val expiringDocsCount: Int = 0,
    val recommendations: List<String> = emptyList()
)

data class PasswordGenConfig(
    val length: Int = 16,
    val useUpper: Boolean = true,
    val useLower: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true,
    val avoidAmbiguous: Boolean = true
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val vaultEntryDao: VaultEntryDao
    private val repository: VaultRepository
    private val encryptionManager: AESEncryptionManager = AESEncryptionManager.getInstance()
    private val securityPrefs: SecurityPreferences = SecurityPreferences(application)
    private val clipboardManager: ClipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isSetupCompleted = MutableStateFlow(securityPrefs.isSetupCompleted)
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _configuredPinLength = MutableStateFlow(securityPrefs.pinLength)
    val configuredPinLength: StateFlow<Int> = _configuredPinLength.asStateFlow()

    private val _isBiometricAvailable = MutableStateFlow(
        BiometricAuthManager.getInstance().isBiometricAvailable(application)
    )
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    private val _selectedCategory = MutableStateFlow<VaultItemType?>(null)
    val selectedCategory: StateFlow<VaultItemType?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _clipboardClearSeconds = MutableStateFlow<Int?>(null)
    val clipboardClearSeconds: StateFlow<Int?> = _clipboardClearSeconds.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var clipboardClearJob: Job? = null

    // Password Generator state
    private val _genConfig = MutableStateFlow(PasswordGenConfig())
    val genConfig: StateFlow<PasswordGenConfig> = _genConfig.asStateFlow()

    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    // Firebase Auth & Cloud Firestore
    private val authManager: FirebaseAuthManager = FirebaseAuthManager.getInstance(application)
    private val firestoreRepo: FirestoreVaultRepository = FirestoreVaultRepository.getInstance(application)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val isAuthLoading: StateFlow<Boolean> = authManager.isLoading
    val authError: StateFlow<String?> = authManager.authError

    private val _isLoginPassed = MutableStateFlow(authManager.currentUser.value != null)
    val isLoginPassed: StateFlow<Boolean> = _isLoginPassed.asStateFlow()

    fun continueLocalMode() {
        _isLoginPassed.value = true
    }

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow<String?>(null)
    val cloudSyncStatus: StateFlow<String?> = _cloudSyncStatus.asStateFlow()

    private val networkMonitor = NetworkMonitor(application)

    init {
        val database = AppDatabase.getDatabase(application)
        vaultEntryDao = database.vaultEntryDao()
        repository = VaultRepository(database.vaultDao())
        regeneratePassword()

        // Create notification channel for cloud sync alerts
        SyncNotificationHelper.createNotificationChannel(application)

        // Automatically connect anonymously to Cloud Firestore if auto-sync is enabled
        viewModelScope.launch {
            if (securityPrefs.autoCloudSyncEnabled && authManager.currentUser.value == null) {
                authManager.signInAnonymously()
            }
        }

        // Observe auth state changes to start/stop real-time cloud sync automatically
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null && _isUnlocked.value && securityPrefs.autoCloudSyncEnabled) {
                    triggerAutoSync()
                    startCloudListener()
                } else if (user == null && securityPrefs.autoCloudSyncEnabled) {
                    stopCloudListener()
                    authManager.signInAnonymously()
                }
            }
        }

        // Observe network state to auto-sync when connection is restored
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && _isUnlocked.value && securityPrefs.autoCloudSyncEnabled) {
                    triggerAutoSync()
                }
            }
        }
    }

    val vaultEntries: StateFlow<List<VaultEntry>> = vaultEntryDao.getAllEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val vaultEntriesUiState: StateFlow<VaultEntriesUiState> = vaultEntryDao.getAllEntries()
        .map<List<VaultEntry>, VaultEntriesUiState> { VaultEntriesUiState.Success(it) }
        .catch { emit(VaultEntriesUiState.Error(it.localizedMessage ?: "Failed loading encrypted vault entries")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VaultEntriesUiState.Loading
        )

    val items: StateFlow<List<VaultItem>> = repository.allItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _itemsFilter = MutableStateFlow(ItemsFilter())
    val itemsFilter: StateFlow<ItemsFilter> = _itemsFilter.asStateFlow()

    private val _collections = MutableStateFlow<List<VaultCollection>>(loadDefaultAndCustomCollections())
    val collections: StateFlow<List<VaultCollection>> = _collections.asStateFlow()

    val recentItems: StateFlow<List<VaultItem>> = items.mapStateFlow(viewModelScope) { all ->
        all.sortedByDescending { it.updatedAt }.take(6)
    }

    val favoriteItems: StateFlow<List<VaultItem>> = items.mapStateFlow(viewModelScope) { all ->
        all.filter { it.effectiveFavorite }
    }

    // Settings flows
    val appLockEnabled = MutableStateFlow(securityPrefs.appLockEnabled)
    val biometricEnabled = MutableStateFlow(securityPrefs.isBiometricEnabled)
    val autoLockTimeout = MutableStateFlow(securityPrefs.autoLockTimeoutSeconds)
    val clipboardTimeout = MutableStateFlow(securityPrefs.clipboardClearTimeoutSeconds)
    val sensitiveDataMasked = MutableStateFlow(securityPrefs.sensitiveDataMasked)
    val revealAuthRequired = MutableStateFlow(securityPrefs.revealAuthRequired)
    val autoCloudSyncEnabled = MutableStateFlow(securityPrefs.autoCloudSyncEnabled)
    val lastCloudSyncTime = MutableStateFlow(securityPrefs.lastCloudSyncTime)

    val recordTemplates = listOf(
        RecordTemplate(
            id = "identity",
            name = "Identity",
            icon = "🪪",
            fieldCount = 4,
            fields = listOf(
                "ID Number" to "number",
                "Full Legal Name" to "text",
                "Issue Date" to "text",
                "Expiry Date" to "text"
            )
        ),
        RecordTemplate(
            id = "vehicle",
            name = "Vehicle",
            icon = "🚗",
            fieldCount = 5,
            fields = listOf(
                "Registration Number" to "text",
                "Chassis Number" to "text",
                "Engine Number" to "text",
                "Insurance Policy" to "text",
                "Valid Till" to "text"
            )
        ),
        RecordTemplate(
            id = "college",
            name = "College",
            icon = "🎓",
            fieldCount = 4,
            fields = listOf(
                "Student ID / Roll No" to "number",
                "Degree / Department" to "text",
                "Semester" to "text",
                "Advisor / Campus" to "text"
            )
        )
    )

    val filteredItems: StateFlow<List<VaultItem>> = combine(
        items,
        _selectedCategory,
        _searchQuery,
        _itemsFilter
    ) { allItems, category, query, filter ->
        allItems.filter { item ->
            val matchesType = (filter.type == null && category == null) ||
                    (filter.type != null && item.type == filter.type) ||
                    (category != null && item.type == category)

            val matchesCollection = filter.collectionId == null ||
                    item.collectionId == filter.collectionId ||
                    item.categoryTag.equals(filter.collectionId, ignoreCase = true)

            val matchesFav = !filter.favoritesOnly || item.effectiveFavorite
            val matchesPin = !filter.pinnedOnly || item.pinned
            val matchesSensitive = !filter.sensitiveOnly || item.sensitive
            val matchesArchived = filter.archived == item.archived

            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.categoryTag.contains(query, ignoreCase = true) ||
                    item.effectiveTags.contains(query, ignoreCase = true) ||
                    item.effectiveValue.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true)

            matchesType && matchesCollection && matchesFav && matchesPin && matchesSensitive && matchesArchived && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val audit: StateFlow<SecurityAudit> = items.mapStateFlow(viewModelScope) { vaultItems ->
        calculateAudit(vaultItems)
    }

    private var activeSessionPin: String = "1234"
    private var backgroundTimestamp: Long = 0L

    fun getEffectiveSessionPin(): String {
        if (activeSessionPin.isNotBlank()) return activeSessionPin
        val savedPin = securityPrefs.getMasterPinDecrypted()
        if (!savedPin.isNullOrBlank()) {
            activeSessionPin = savedPin
            return savedPin
        }
        return "1234"
    }

    fun ensureCloudSyncActive() {
        if (!securityPrefs.autoCloudSyncEnabled) return
        viewModelScope.launch {
            if (authManager.currentUser.value == null) {
                authManager.signInAnonymously()
            }
            triggerAutoSync()
            startCloudListener()
        }
    }

    fun onAppBackgrounded() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (!_isUnlocked.value) return
        if (backgroundTimestamp != 0L) {
            val elapsedSeconds = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            val timeoutSeconds = securityPrefs.autoLockTimeoutSeconds
            if (timeoutSeconds == 0 || (timeoutSeconds > 0 && elapsedSeconds >= timeoutSeconds)) {
                lockVault()
                backgroundTimestamp = 0L
                return
            }
        }
        backgroundTimestamp = 0L
        ensureCloudSyncActive()
    }

    fun setupMasterPin(pin: String) {
        if (pin.length < 4) {
            _statusMessage.value = "PIN must be at least 4 digits"
            return
        }
        activeSessionPin = pin
        securityPrefs.setMasterPin(pin)
        _configuredPinLength.value = pin.length
        _isSetupCompleted.value = true
        _isUnlocked.value = true
        _statusMessage.value = "Master PIN configured. MyVault protection is ready!"
        ensureCloudSyncActive()
    }

    fun unlockWithPin(pin: String): Boolean {
        val verified = securityPrefs.verifyMasterPin(pin)
        if (verified) {
            activeSessionPin = pin
            _isUnlocked.value = true
            _statusMessage.value = null
            ensureCloudSyncActive()
        } else {
            _statusMessage.value = "Incorrect PIN. Please try again."
        }
        return verified
    }

    fun unlockWithBiometric() {
        activeSessionPin = securityPrefs.getMasterPinDecrypted() ?: "1234"
        _isUnlocked.value = true
        _statusMessage.value = null
        ensureCloudSyncActive()
    }

    fun quickDemoUnlock() {
        if (!securityPrefs.isSetupCompleted) {
            securityPrefs.setMasterPin("1234")
            _configuredPinLength.value = 4
            _isSetupCompleted.value = true
        }
        activeSessionPin = "1234"
        _isUnlocked.value = true
        _statusMessage.value = "Vault unlocked. Master PIN is 1234."
        ensureCloudSyncActive()
    }

    fun lockVault() {
        _isUnlocked.value = false
        _searchQuery.value = ""
        stopCloudListener()
        _statusMessage.value = "Vault locked."
    }

    fun selectCategory(type: VaultItemType?) {
        _selectedCategory.value = type
        _itemsFilter.value = _itemsFilter.value.copy(type = type)
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun updateItemsFilter(filter: ItemsFilter) {
        _itemsFilter.value = filter
    }

    fun clearItemsFilter() {
        _itemsFilter.value = ItemsFilter()
        _selectedCategory.value = null
    }

    fun createCollection(name: String, icon: String, colorHex: String, description: String = "") {
        val newCol = VaultCollection(
            id = name.lowercase().replace(" ", "_"),
            name = name.trim(),
            icon = icon.ifBlank { "📁" },
            colorHex = colorHex,
            description = description.trim()
        )
        val current = _collections.value.toMutableList()
        current.add(newCol)
        _collections.value = current
        saveCustomCollections(current)
        _statusMessage.value = "Collection \"$name\" created"
        if (securityPrefs.autoCloudSyncEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val user = authManager.currentUser.value ?: authManager.signInAnonymously().getOrNull()
                    if (user != null) {
                        firestoreRepo.saveCustomCollections(user.uid, securityPrefs.customCollectionsJson)
                    }
                } catch (e: Exception) {
                    Log.w("VaultViewModel", "Auto cloud sync collection notice: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun loadDefaultAndCustomCollections(): List<VaultCollection> {
        val defaults = mutableListOf(
            VaultCollection("personal", "Personal", "🏠", "#10B981", "Everyday accounts & documents"),
            VaultCollection("college", "College", "🎓", "#3B82F6", "Academic portal, emails & student credentials"),
            VaultCollection("work", "Work", "💼", "#F59E0B", "Professional logins & work records"),
            VaultCollection("projects", "Projects", "📁", "#8B5CF6", "Code repositories & server access")
        )
        try {
            val jsonStr = securityPrefs.customCollectionsJson
            if (jsonStr.isNotBlank()) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    defaults.add(
                        VaultCollection(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            icon = obj.optString("icon", "📁"),
                            colorHex = obj.optString("colorHex", "#10B981"),
                            description = obj.optString("description", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("VaultViewModel", "Error loading custom collections: ${e.localizedMessage}")
        }
        return defaults
    }

    private fun saveCustomCollections(all: List<VaultCollection>) {
        try {
            val defaultsIds = setOf("personal", "college", "work", "projects")
            val customOnly = all.filter { it.id !in defaultsIds }
            val array = JSONArray()
            for (col in customOnly) {
                val obj = JSONObject().apply {
                    put("id", col.id)
                    put("name", col.name)
                    put("icon", col.icon)
                    put("colorHex", col.colorHex)
                    put("description", col.description)
                }
                array.put(obj)
            }
            securityPrefs.customCollectionsJson = array.toString()
        } catch (e: Exception) {
            Log.e("VaultViewModel", "Error saving custom collections: ${e.localizedMessage}")
        }
    }

    fun verifyPinForReveal(pin: String): Boolean {
        return securityPrefs.verifyMasterPin(pin)
    }

    fun setAppLock(enabled: Boolean) {
        securityPrefs.appLockEnabled = enabled
        appLockEnabled.value = enabled
    }

    fun setBiometric(enabled: Boolean) {
        securityPrefs.isBiometricEnabled = enabled
        biometricEnabled.value = enabled
    }

    fun setAutoLockTimeout(seconds: Int) {
        securityPrefs.autoLockTimeoutSeconds = seconds
        autoLockTimeout.value = seconds
    }

    fun setClipboardTimeout(seconds: Int) {
        securityPrefs.clipboardClearTimeoutSeconds = seconds
        clipboardTimeout.value = seconds
    }

    fun setSensitiveDataMasked(masked: Boolean) {
        securityPrefs.sensitiveDataMasked = masked
        sensitiveDataMasked.value = masked
    }

    fun setRevealAuthRequired(required: Boolean) {
        securityPrefs.revealAuthRequired = required
        revealAuthRequired.value = required
    }

    fun togglePin(item: VaultItem) {
        viewModelScope.launch {
            val updated = item.copy(pinned = !item.pinned)
            saveItem(updated)
        }
    }

    fun toggleArchive(item: VaultItem) {
        viewModelScope.launch {
            val updated = item.copy(archived = !item.archived)
            saveItem(updated)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveItem(item: VaultItem) {
        viewModelScope.launch {
            try {
                val itemWithTimestamp = item.copy(updatedAt = System.currentTimeMillis())
                val savedId = repository.saveItem(itemWithTimestamp)
                val updatedItem = if (itemWithTimestamp.id == 0L) itemWithTimestamp.copy(id = savedId) else itemWithTimestamp
                if (securityPrefs.autoCloudSyncEnabled) {
                    launch(Dispatchers.IO) {
                        try {
                            val user = authManager.currentUser.value ?: authManager.signInAnonymously().getOrNull()
                            if (user != null) {
                                val cloudSalt = firestoreRepo.getOrCreateCloudSalt(user.uid)
                                val cloudKey = firestoreRepo.deriveCloudKey(getEffectiveSessionPin(), cloudSalt)
                                firestoreRepo.saveItem(user.uid, updatedItem, cloudKey)
                                securityPrefs.lastCloudSyncTime = System.currentTimeMillis()
                                lastCloudSyncTime.value = securityPrefs.lastCloudSyncTime
                                _cloudSyncStatus.value = "Auto-Synced • ${items.value.size} items in Cloud"
                            }
                        } catch (e: Exception) {
                            Log.w("VaultViewModel", "Cloud sync on save: ${e.localizedMessage}")
                        }
                    }
                }
                _statusMessage.value = "${item.type.displayName} saved securely"
            } catch (e: Exception) {
                _statusMessage.value = "Error saving item: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Encrypts sensitive raw content using AES-256-GCM and persists the new VaultEntry via VaultEntryDao.
     */
    fun saveNewEntry(
        title: String,
        rawContent: String,
        category: String,
        onSuccess: ((Long) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encrypted = encryptionManager.encrypt(rawContent)
                    ?: throw IllegalStateException("AES encryption produced null output")

                val entry = VaultEntry(
                    title = title.trim(),
                    encryptedContent = encrypted,
                    category = category.trim().lowercase(),
                    timestamp = System.currentTimeMillis()
                )
                val newId = vaultEntryDao.insertEntry(entry)
                _statusMessage.value = "Protected entry \"$title\" saved successfully"
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke(newId)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed saving entry: ${e.localizedMessage}"
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }
        }
    }

    /**
     * Persists an existing or pre-encrypted VaultEntry via VaultEntryDao.
     */
    fun saveVaultEntry(
        entry: VaultEntry,
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (entry.id == 0L) {
                    vaultEntryDao.insertEntry(entry)
                } else {
                    vaultEntryDao.updateEntry(entry)
                }
                _statusMessage.value = "Vault entry saved"
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error saving vault entry: ${e.localizedMessage}"
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }
        }
    }

    /**
     * Decrypts a vault entry's encrypted content using the Android KeyStore-backed AES engine.
     */
    suspend fun decryptEntryContent(entry: VaultEntry): String = withContext(Dispatchers.IO) {
        try {
            encryptionManager.decrypt(entry.encryptedContent)
        } catch (e: Exception) {
            "Decryption failed: ${e.localizedMessage}"
        }
    }

    /**
     * Deletes a VaultEntry from local storage.
     */
    fun deleteVaultEntry(entry: VaultEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                vaultEntryDao.deleteEntry(entry)
                _statusMessage.value = "Entry deleted"
            } catch (e: Exception) {
                _statusMessage.value = "Error deleting entry: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Deletes a VaultEntry by its ID.
     */
    fun deleteVaultEntryById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                vaultEntryDao.deleteById(id)
                _statusMessage.value = "Entry deleted"
            } catch (e: Exception) {
                _statusMessage.value = "Error deleting entry: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Loads a single VaultEntry by ID.
     */
    fun loadEntryById(id: Long, onResult: (VaultEntry?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = vaultEntryDao.getEntryById(id)
            withContext(Dispatchers.Main) {
                onResult(entry)
            }
        }
    }

    /**
     * Initiates Google Sign-In with Firebase Auth via Credential Manager.
     */
    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudSyncStatus.value = "Authenticating with Google..."
            val result = authManager.signInWithGoogle(activity)
            result.onSuccess { user ->
                _isLoginPassed.value = true
                _cloudSyncStatus.value = "Connected as ${user.displayName ?: user.email}"
                _statusMessage.value = "Google Sign-In successful. Firestore sync active."
                syncWithFirestore()
            }.onFailure { err ->
                val errorMsg = err.localizedMessage ?: "Sign-in failed"
                if (errorMsg.contains("No credentials", ignoreCase = true) || errorMsg.contains("No Google account", ignoreCase = true)) {
                    _cloudSyncStatus.value = "No Google account detected. You can use Email Sign-In below."
                    _statusMessage.value = "No Google account on device. Please sign in with Email & Password."
                } else if (errorMsg.contains("10:") || errorMsg.contains("Developer error", ignoreCase = true)) {
                    _cloudSyncStatus.value = "Google configuration notice: Add SHA-1 to Firebase Console"
                    _statusMessage.value = "Please ensure your app SHA-1 fingerprint is registered in Firebase Console."
                } else {
                    _cloudSyncStatus.value = "Google Sign-In: $errorMsg"
                    _statusMessage.value = "Google Sign-In: $errorMsg"
                }
            }
            _isCloudSyncing.value = false
        }
    }

    /**
     * Signs in with email and password.
     */
    fun signInWithEmail(email: String, pass: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudSyncStatus.value = "Signing in with email..."
            val result = authManager.signInWithEmail(email, pass)
            result.onSuccess { user ->
                _isLoginPassed.value = true
                _cloudSyncStatus.value = "Connected as ${user.email}"
                _statusMessage.value = "Signed in as ${user.email}"
                syncWithFirestore()
                onSuccess?.invoke()
            }.onFailure { err ->
                _cloudSyncStatus.value = "Sign-in notice: ${err.localizedMessage}"
                _statusMessage.value = "Sign-in notice: ${err.localizedMessage}"
            }
            _isCloudSyncing.value = false
        }
    }

    /**
     * Creates a new Firebase Auth account with email and password.
     */
    fun signUpWithEmail(email: String, pass: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudSyncStatus.value = "Creating Firebase account..."
            val result = authManager.signUpWithEmail(email, pass)
            result.onSuccess { user ->
                _isLoginPassed.value = true
                _cloudSyncStatus.value = "Account created: ${user.email}"
                _statusMessage.value = "Account created. Firestore sync active."
                syncWithFirestore()
                onSuccess?.invoke()
            }.onFailure { err ->
                _cloudSyncStatus.value = "Sign-up notice: ${err.localizedMessage}"
                _statusMessage.value = "Sign-up notice: ${err.localizedMessage}"
            }
            _isCloudSyncing.value = false
        }
    }

    fun setAutoCloudSync(enabled: Boolean) {
        securityPrefs.autoCloudSyncEnabled = enabled
        autoCloudSyncEnabled.value = enabled
        if (enabled) {
            triggerAutoSync(isUserInitiated = true)
            startCloudListener()
        } else {
            stopCloudListener()
            _cloudSyncStatus.value = "Automatic Cloud Sync Paused"
        }
    }

    private var cloudListener: ListenerRegistration? = null

    private fun startCloudListener() {
        cloudListener?.remove()
        val user = authManager.currentUser.value ?: return
        if (!securityPrefs.autoCloudSyncEnabled) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudSalt = firestoreRepo.getOrCreateCloudSalt(user.uid)
                val cloudKey = firestoreRepo.deriveCloudKey(getEffectiveSessionPin(), cloudSalt)
                cloudListener = firestoreRepo.listenToItems(
                    userId = user.uid,
                    cloudKey = cloudKey,
                    onItemsUpdated = { remoteItems ->
                        viewModelScope.launch(Dispatchers.IO) {
                            val localMap = items.value.associateBy { it.id }
                            for (remote in remoteItems) {
                                val local = localMap[remote.id]
                                if (local == null || remote.updatedAt > local.updatedAt) {
                                    repository.saveItem(remote)
                                }
                            }
                            securityPrefs.lastCloudSyncTime = System.currentTimeMillis()
                            lastCloudSyncTime.value = securityPrefs.lastCloudSyncTime
                            _cloudSyncStatus.value = "Auto-Synced • ${items.value.size} items protected in Cloud"
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w("VaultViewModel", "Error starting cloud sync listener: ${e.localizedMessage}")
            }
        }
    }

    private fun stopCloudListener() {
        cloudListener?.remove()
        cloudListener = null
    }

    /**
     * Performs a full Two-Way Zero-Knowledge Cloud Sync with Cloud Firestore.
     */
    fun triggerAutoSync(isUserInitiated: Boolean = false) {
        if (!securityPrefs.autoCloudSyncEnabled && !isUserInitiated) return
        if (!_isUnlocked.value) return

        viewModelScope.launch(Dispatchers.IO) {
            val user = authManager.currentUser.value ?: authManager.signInAnonymously().getOrNull()
            if (user == null) {
                _cloudSyncStatus.value = "Cloud Sync: Connecting..."
                return@launch
            }
            _isCloudSyncing.value = true
            _cloudSyncStatus.value = "Zero-Knowledge cloud syncing..."
            try {
                val cloudSalt = firestoreRepo.getOrCreateCloudSalt(user.uid)
                val cloudKey = firestoreRepo.deriveCloudKey(getEffectiveSessionPin(), cloudSalt)

                // 1. Fetch remote cloud items
                val remoteResult = firestoreRepo.fetchItems(user.uid, cloudKey)
                val remoteItems = remoteResult.getOrNull() ?: emptyList()
                val remoteMap = remoteItems.associateBy { it.id }

                // 2. Local items
                val localItems = items.value
                val localMap = localItems.associateBy { it.id }

                var uploaded = 0
                var pulled = 0

                // Upload local items that don't exist remotely or are newer locally
                for (local in localItems) {
                    val remote = remoteMap[local.id]
                    if (remote == null || local.updatedAt > remote.updatedAt) {
                        firestoreRepo.saveItem(user.uid, local, cloudKey)
                        uploaded++
                    }
                }

                // Pull and store remote items that don't exist locally or are newer remotely
                for (remote in remoteItems) {
                    val local = localMap[remote.id]
                    if (local == null || remote.updatedAt > local.updatedAt) {
                        repository.saveItem(remote)
                        pulled++
                    }
                }

                // Sync custom collections to and from cloud
                try {
                    val cloudCollectionsJson = firestoreRepo.fetchCustomCollections(user.uid)
                    if (!cloudCollectionsJson.isNullOrBlank() && securityPrefs.customCollectionsJson.isBlank()) {
                        securityPrefs.customCollectionsJson = cloudCollectionsJson
                        _collections.value = loadDefaultAndCustomCollections()
                    } else if (securityPrefs.customCollectionsJson.isNotBlank()) {
                        firestoreRepo.saveCustomCollections(user.uid, securityPrefs.customCollectionsJson)
                    }
                } catch (e: Exception) {
                    Log.w("VaultViewModel", "Collections sync notice: ${e.localizedMessage}")
                }

                securityPrefs.lastCloudSyncTime = System.currentTimeMillis()
                lastCloudSyncTime.value = securityPrefs.lastCloudSyncTime
                _cloudSyncStatus.value = "Auto-Synced • ${items.value.size} items protected in Cloud"
                if (isUserInitiated) {
                    _statusMessage.value = "Cloud Sync: $uploaded uploaded, $pulled updated"
                }
                if (uploaded > 0 || pulled > 0) {
                    SyncNotificationHelper.showSyncNotification(
                        getApplication<Application>(),
                        "MyVault Cloud Sync",
                        "$uploaded uploaded, $pulled updated • All items encrypted in Cloud"
                    )
                }
            } catch (e: Exception) {
                Log.w("VaultViewModel", "Auto-sync notice: ${e.localizedMessage}")
                _cloudSyncStatus.value = "Sync notice: ${e.localizedMessage}"
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    /**
     * Signs out of Firebase.
     */
    fun signOutFirebase() {
        stopCloudListener()
        authManager.signOut()
        _cloudSyncStatus.value = "Signed out of Cloud"
        _statusMessage.value = "Signed out of Firebase"
    }

    /**
     * Syncs encrypted items to Cloud Firestore with True Zero-Knowledge encryption.
     */
    fun syncWithFirestore() {
        triggerAutoSync(isUserInitiated = true)
    }

    /**
     * Restores encrypted items from Cloud Firestore with True Zero-Knowledge decryption.
     */
    fun restoreFromFirestore() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = authManager.currentUser.value ?: authManager.signInAnonymously().getOrNull()
            if (user == null) {
                _cloudSyncStatus.value = "Please sign in to restore from Cloud."
                return@launch
            }

            _isCloudSyncing.value = true
            _cloudSyncStatus.value = "Restoring Zero-Knowledge items from Cloud..."
            try {
                val cloudSalt = firestoreRepo.getOrCreateCloudSalt(user.uid)
                val cloudKey = firestoreRepo.deriveCloudKey(getEffectiveSessionPin(), cloudSalt)
                val itemsResult = firestoreRepo.fetchItems(user.uid, cloudKey)
                var restoredItems = 0
                itemsResult.onSuccess { cloudItems ->
                    for (item in cloudItems) {
                        repository.saveItem(item)
                        restoredItems++
                    }
                    securityPrefs.lastCloudSyncTime = System.currentTimeMillis()
                    lastCloudSyncTime.value = securityPrefs.lastCloudSyncTime
                    _cloudSyncStatus.value = "Restored $restoredItems items from Cloud"
                    _statusMessage.value = "Restored $restoredItems encrypted items from Cloud Firestore"
                }.onFailure { err ->
                    _cloudSyncStatus.value = "Restore notice: ${err.localizedMessage}"
                }
            } catch (e: Exception) {
                _cloudSyncStatus.value = "Restore notice: ${e.localizedMessage}"
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun deleteItem(item: VaultItem) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                if (securityPrefs.autoCloudSyncEnabled) {
                    launch(Dispatchers.IO) {
                        try {
                            val user = authManager.currentUser.value ?: authManager.signInAnonymously().getOrNull()
                            if (user != null) {
                                firestoreRepo.deleteItem(user.uid, item.id)
                                securityPrefs.lastCloudSyncTime = System.currentTimeMillis()
                                lastCloudSyncTime.value = securityPrefs.lastCloudSyncTime
                                _cloudSyncStatus.value = "Auto-Synced • ${items.value.size} items in Cloud"
                            }
                        } catch (e: Exception) {
                            Log.w("VaultViewModel", "Auto cloud delete notice: ${e.localizedMessage}")
                        }
                    }
                }
                _statusMessage.value = "Item removed"
            } catch (e: Exception) {
                _statusMessage.value = "Error removing item: ${e.localizedMessage}"
            }
        }
    }

    fun toggleFavorite(item: VaultItem) {
        viewModelScope.launch {
            val updated = item.copy(
                favorite = !item.favorite,
                isFavorite = !item.favorite,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveItem(updated)
            if (securityPrefs.autoCloudSyncEnabled) {
                launch(Dispatchers.IO) {
                    try {
                        val user = authManager.currentUser.value ?: authManager.signInAnonymously().getOrNull()
                        if (user != null) {
                            val cloudSalt = firestoreRepo.getOrCreateCloudSalt(user.uid)
                            val cloudKey = firestoreRepo.deriveCloudKey(getEffectiveSessionPin(), cloudSalt)
                            firestoreRepo.saveItem(user.uid, updated, cloudKey)
                            securityPrefs.lastCloudSyncTime = System.currentTimeMillis()
                            lastCloudSyncTime.value = securityPrefs.lastCloudSyncTime
                            _cloudSyncStatus.value = "Auto-Synced • ${items.value.size} items in Cloud"
                        }
                    } catch (e: Exception) {
                        Log.w("VaultViewModel", "Cloud sync on toggleFavorite: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    fun copyToClipboard(label: String, sensitiveText: String) {
        if (sensitiveText.isEmpty()) return
        val clip = ClipData.newPlainText(label, sensitiveText)
        clipboardManager.setPrimaryClip(clip)

        val timeoutSec = securityPrefs.clipboardClearTimeoutSeconds
        clipboardClearJob?.cancel()

        if (timeoutSec > 0) {
            _statusMessage.value = "$label copied! Auto-clearing in ${timeoutSec}s."
            clipboardClearJob = viewModelScope.launch {
                for (i in timeoutSec downTo 1) {
                    _clipboardClearSeconds.value = i
                    delay(1000)
                }
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                _clipboardClearSeconds.value = null
                _statusMessage.value = "Clipboard cleared for your privacy."
            }
        } else {
            _clipboardClearSeconds.value = null
            _statusMessage.value = "$label copied to clipboard."
        }
    }

    fun updateGenConfig(newConfig: PasswordGenConfig) {
        _genConfig.value = newConfig
        regeneratePassword()
    }

    fun regeneratePassword() {
        val config = _genConfig.value
        _generatedPassword.value = PasswordGenerator.generate(
            config.length,
            config.useUpper,
            config.useLower,
            config.useDigits,
            config.useSymbols,
            config.avoidAmbiguous
        )
    }

    fun exportEncryptedBackup(passphrase: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentItems = items.value
                val encryptedBackup = repository.exportEncryptedBackup(passphrase, currentItems)
                onResult(true, encryptedBackup)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Export failed")
            }
        }
    }

    fun importEncryptedBackup(backupStr: String, passphrase: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.importEncryptedBackup(backupStr, passphrase)
                onResult(true, "Successfully imported $count items into vault!")
            } catch (e: Exception) {
                onResult(false, "Decryption error: ${e.localizedMessage}")
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun calculateAudit(vaultItems: List<VaultItem>): SecurityAudit {
        if (vaultItems.isEmpty()) {
            return SecurityAudit(
                healthScore = 100,
                totalItems = 0,
                weakPasswordsCount = 0,
                reusedPasswordsCount = 0,
                expiringDocsCount = 0,
                recommendations = listOf("Vault is empty. Add your credentials, links, and documents.")
            )
        }

        val passwords = vaultItems.mapNotNull { if (it.password.isNotEmpty()) it.password else null }
        var weakCount = 0
        val frequencyMap = HashMap<String, Int>()

        for (pwd in passwords) {
            val eval = PasswordStrengthCalculator.evaluate(pwd)
            if (eval.score <= 1 || pwd.length < 8) {
                weakCount++
            }
            frequencyMap[pwd] = (frequencyMap[pwd] ?: 0) + 1
        }

        val reusedCount = frequencyMap.values.filter { it > 1 }.sum()

        // Expiring documents check
        var expiringDocs = 0
        val now = System.currentTimeMillis()
        val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (item in vaultItems) {
            if (item.type == VaultItemType.DOCUMENT_ID && item.documentExpiry.isNotBlank()) {
                try {
                    val date = dateFormat.parse(item.documentExpiry)
                    if (date != null && (date.time - now) < ninetyDaysMs) {
                        expiringDocs++
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        var score = 100
        score -= (weakCount * 12)
        score -= (reusedCount * 10)
        score -= (expiringDocs * 8)
        if (score < 10) score = 10
        if (score > 100) score = 100

        val recs = mutableListOf<String>()
        if (weakCount > 0) recs.add("$weakCount password(s) are weak. Use the built-in generator.")
        if (reusedCount > 0) recs.add("$reusedCount reused passwords detected. Use unique passwords per service.")
        if (expiringDocs > 0) recs.add("$expiringDocs document(s) expiring within 90 days.")
        if (recs.isEmpty()) recs.add("Protection Ready. All credentials secured and shielded by device protection.")

        return SecurityAudit(
            healthScore = score,
            totalItems = vaultItems.size,
            weakPasswordsCount = weakCount,
            reusedPasswordsCount = reusedCount,
            expiringDocsCount = expiringDocs,
            recommendations = recs
        )
    }

    private fun <T, R> StateFlow<T>.mapStateFlow(
        scope: kotlinx.coroutines.CoroutineScope,
        transform: (T) -> R
    ): StateFlow<R> {
        val initial = transform(this.value)
        val flow = MutableStateFlow(initial)
        scope.launch {
            this@mapStateFlow.collect {
                flow.value = transform(it)
            }
        }
        return flow.asStateFlow()
    }
}
