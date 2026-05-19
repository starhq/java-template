package com.github.starhq.template.common.util;

import java.util.Set;
import java.util.regex.Pattern;

public class PasswordStrengthChecker {

    public enum StrengthLevel {
        EASY(0, 3),
        MEDIUM(4, 6),
        STRONG(7, 9),
        VERY_STRONG(10, 12),
        EXTREMELY_STRONG(13, Integer.MAX_VALUE);

        private final int minScore;
        private final int maxScore;

        StrengthLevel(int minScore, int maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        public static StrengthLevel fromScore(int score) {
            for (StrengthLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return EASY;
        }
    }

    private enum CharacterType {
        DIGIT(Character::isDigit),
        LOWERCASE(Character::isLowerCase),
        UPPERCASE(Character::isUpperCase),
        SPECIAL(c -> !Character.isLetterOrDigit(c));

        private final CharPredicate predicate;

        CharacterType(CharPredicate predicate) {
            this.predicate = predicate;
        }

        boolean matches(char c) {
            return predicate.test(c);
        }

        @FunctionalInterface
        interface CharPredicate {
            boolean test(char c);
        }
    }

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "123456", "password", "123456789", "12345678", "12345", "111111",
            "1234567", "sunshine", "qwerty", "iloveyou", "princess", "admin",
            "welcome", "666666", "abc123", "football", "123123", "monkey",
            "654321", "!@#$%^&*", "charlie", "aa123456", "donald", "password1",
            "qwerty123", "147258369", "987654321", "1qaz2wsx", "asdfghjkl",
            "1q2w3e4r", "1234abcd", "3.1415926");

    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{2,}");

    /**
     * Calculate password strength score.
     *
     * @param password the password to check
     * @return strength score (0-15+)
     */
    public static int calculateScore(String password) {
        if (password == null || password.isBlank()) {
            return 0;
        }

        password = password.trim();
        int length = password.length();

        // Early exit for very weak passwords
        if (length <= 3 || isAllSameCharacter(password)) {
            return 0;
        }

        int score = 0;
        PasswordMetrics metrics = new PasswordMetrics(password);

        // Length-based scoring
        score += getLengthScore(length);

        // Character diversity scoring
        score += getCharacterDiversityScore(metrics);

        // Combination bonuses
        score += getCombinationBonus(length, metrics);

        // Penalties
        score -= getPenalties(password, length, metrics);

        return Math.max(score, 0);
    }

    /**
     * Get password strength level.
     *
     * @param password the password to check
     * @return the strength level
     */
    public static StrengthLevel getStrengthLevel(String password) {
        int score = calculateScore(password);
        return StrengthLevel.fromScore(score);
    }

    private static int getLengthScore(int length) {
        if (length >= 12)
            return 3;
        if (length >= 8)
            return 2;
        if (length >= 6)
            return 1;
        return 0;
    }

    private static int getCharacterDiversityScore(PasswordMetrics metrics) {
        int score = 0;
        if (metrics.hasDigits)
            score++;
        if (metrics.hasLowercase)
            score++;
        if (metrics.hasUppercase)
            score++;
        if (metrics.hasSpecial)
            score++;
        return score;
    }

    private static int getCombinationBonus(int length, PasswordMetrics metrics) {
        int bonus = 0;
        int typeCount = metrics.getTypeCount();

        if (length > 4 && typeCount >= 2)
            bonus++;
        if (length > 6 && typeCount >= 3)
            bonus++;
        if (length > 8 && typeCount >= 4)
            bonus += 2;

        return bonus;
    }

    private static int getPenalties(String password, int length, PasswordMetrics metrics) {
        int penalty = 0;

        // Single character type penalty
        if (metrics.getTypeCount() == 1) {
            penalty += 2;
        }

        // Short password penalty
        if (length < 6) {
            penalty += (6 - length);
        }

        // Repeated pattern penalty
        if (hasRepeatedPattern(password)) {
            penalty += 2;
        }

        // Common password penalty
        if (isCommonPassword(password)) {
            penalty += 3;
        }

        // Sequential characters penalty
        if (hasSequentialChars(password)) {
            penalty++;
        }

        return penalty;
    }

    private static boolean isAllSameCharacter(String password) {
        return password.chars().distinct().count() == 1;
    }

    private static boolean hasRepeatedPattern(String password) {
        // Check for repeated sequences (e.g., "abcabcabc")
        int len = password.length();
        if (len >= 6 && len % 3 == 0) {
            int partLen = len / 3;
            String part1 = password.substring(0, partLen);
            String part2 = password.substring(partLen, partLen * 2);
            String part3 = password.substring(partLen * 2);
            if (part1.equals(part2) && part2.equals(part3)) {
                return true;
            }
        }

        // Check for repeated characters (e.g., "aaa", "111")
        return REPEATED_PATTERN.matcher(password).find();
    }

    private static boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return COMMON_PASSWORDS.stream()
                .anyMatch(common -> lowerPassword.contains(common) || common.contains(lowerPassword));
    }

    private static boolean hasSequentialChars(String password) {
        int sequentialCount = 0;
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (c2 == c1 + 1 && c3 == c2 + 1) {
                sequentialCount++;
            }
        }
        return sequentialCount >= 2;
    }

    /**
     * Internal class to hold password metrics.
     */
    private static class PasswordMetrics {
        final boolean hasDigits;
        final boolean hasLowercase;
        final boolean hasUppercase;
        final boolean hasSpecial;

        PasswordMetrics(String password) {
            boolean digits = false, lowercase = false, uppercase = false, special = false;

            for (char c : password.toCharArray()) {
                if (!digits && CharacterType.DIGIT.matches(c)) {
                    digits = true;
                } else if (!lowercase && CharacterType.LOWERCASE.matches(c)) {
                    lowercase = true;
                } else if (!uppercase && CharacterType.UPPERCASE.matches(c)) {
                    uppercase = true;
                } else if (!special && CharacterType.SPECIAL.matches(c)) {
                    special = true;
                }

                // Early exit if all types found
                if (digits && lowercase && uppercase && special) {
                    break;
                }
            }

            this.hasDigits = digits;
            this.hasLowercase = lowercase;
            this.hasUppercase = uppercase;
            this.hasSpecial = special;
        }

        int getTypeCount() {
            int count = 0;
            if (hasDigits)
                count++;
            if (hasLowercase)
                count++;
            if (hasUppercase)
                count++;
            if (hasSpecial)
                count++;
            return count;
        }
    }
}
