package com.example.crypto;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-entropy cryptographic password generator in Java.
 */
public final class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String AMBIGUOUS = "0Ol1I|";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate(
            int length,
            boolean useUpper,
            boolean useLower,
            boolean useDigits,
            boolean useSymbols,
            boolean avoidAmbiguous) {

        if (length < 6) length = 6;
        if (length > 64) length = 64;

        StringBuilder charPool = new StringBuilder();
        List<Character> guaranteedChars = new ArrayList<>();

        String effectiveLower = avoidAmbiguous ? removeChars(LOWER, AMBIGUOUS) : LOWER;
        String effectiveUpper = avoidAmbiguous ? removeChars(UPPER, AMBIGUOUS) : UPPER;
        String effectiveDigits = avoidAmbiguous ? removeChars(DIGITS, AMBIGUOUS) : DIGITS;
        String effectiveSymbols = avoidAmbiguous ? removeChars(SYMBOLS, AMBIGUOUS) : SYMBOLS;

        if (useLower) {
            charPool.append(effectiveLower);
            guaranteedChars.add(randomChar(effectiveLower));
        }
        if (useUpper) {
            charPool.append(effectiveUpper);
            guaranteedChars.add(randomChar(effectiveUpper));
        }
        if (useDigits) {
            charPool.append(effectiveDigits);
            guaranteedChars.add(randomChar(effectiveDigits));
        }
        if (useSymbols) {
            charPool.append(effectiveSymbols);
            guaranteedChars.add(randomChar(effectiveSymbols));
        }

        if (charPool.length() == 0) {
            // Default fallback
            charPool.append(effectiveLower).append(effectiveDigits);
            guaranteedChars.add(randomChar(effectiveLower));
            guaranteedChars.add(randomChar(effectiveDigits));
        }

        String pool = charPool.toString();
        List<Character> resultList = new ArrayList<>(guaranteedChars);

        while (resultList.size() < length) {
            resultList.add(randomChar(pool));
        }

        // Shuffle guaranteed chars to prevent predictable positions
        Collections.shuffle(resultList, SECURE_RANDOM);

        StringBuilder sb = new StringBuilder(resultList.size());
        for (char c : resultList) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static char randomChar(String source) {
        int index = SECURE_RANDOM.nextInt(source.length());
        return source.charAt(index);
    }

    private static String removeChars(String source, String remove) {
        StringBuilder sb = new StringBuilder();
        for (char c : source.toCharArray()) {
            if (remove.indexOf(c) == -1) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
