package com.example.crypto;

/**
 * Calculates password entropy and practical strength level.
 */
public final class PasswordStrengthCalculator {

    public enum StrengthLevel {
        VERY_WEAK,
        WEAK,
        FAIR,
        STRONG,
        VERY_STRONG
    }

    public static class StrengthResult {
        public final StrengthLevel level;
        public final int score; // 0 to 4
        public final double entropyBits;
        public final String feedback;

        public StrengthResult(StrengthLevel level, int score, double entropyBits, String feedback) {
            this.level = level;
            this.score = score;
            this.entropyBits = entropyBits;
            this.feedback = feedback;
        }
    }

    private PasswordStrengthCalculator() {
    }

    public static StrengthResult evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return new StrengthResult(StrengthLevel.VERY_WEAK, 0, 0, "Password is empty");
        }

        int length = password.length();
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigits = false;
        boolean hasSymbols = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigits = true;
            else hasSymbols = true;
        }

        int poolSize = 0;
        if (hasLower) poolSize += 26;
        if (hasUpper) poolSize += 26;
        if (hasDigits) poolSize += 10;
        if (hasSymbols) poolSize += 33;

        double entropy = length * (Math.log(poolSize > 0 ? poolSize : 1) / Math.log(2));

        int score = 0;
        if (length >= 8) score++;
        if (length >= 12) score++;
        if (hasLower && hasUpper) score++;
        if (hasDigits && hasSymbols) score++;

        // Deductions for simple patterns
        if (password.toLowerCase().contains("1234") ||
            password.toLowerCase().contains("password") ||
            password.toLowerCase().contains("qwerty") ||
            password.toLowerCase().contains("admin")) {
            score = Math.max(0, score - 2);
        }

        StrengthLevel level;
        String feedback;

        if (score <= 0 || length < 6) {
            level = StrengthLevel.VERY_WEAK;
            feedback = "Very weak. Increase length and mix symbols.";
        } else if (score == 1 || length < 8) {
            level = StrengthLevel.WEAK;
            feedback = "Weak. Needs more variety and length.";
        } else if (score == 2) {
            level = StrengthLevel.FAIR;
            feedback = "Fair. Consider reaching 14+ characters.";
        } else if (score == 3) {
            level = StrengthLevel.STRONG;
            feedback = "Strong. Good resistance to brute force.";
        } else {
            level = StrengthLevel.VERY_STRONG;
            feedback = "Very strong. Cryptographically excellent!";
        }

        return new StrengthResult(level, score, entropy, feedback);
    }
}
