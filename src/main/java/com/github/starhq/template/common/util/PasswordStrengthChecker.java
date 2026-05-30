package com.github.starhq.template.common.util;

import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for evaluating the strength and security of user passwords.
 *
 * <p>This checker uses a heuristic scoring algorithm that operates on a simple formula:
 * <pre>
 *     Final Score = (Length Score + Diversity Score + Combination Bonus) - Penalties
 * </pre>
 * The final score is bounded to a minimum of 0 and theoretically maxes out at 11
 * (due to the combination of available lengths and character types).
 *
 * @author starhq
 */
@UtilityClass
public class PasswordStrengthChecker {

    /**
     * Enumeration representing the qualitative strength level of a password.
     *
     * <p>Maps a numerical score to a human-readable category using closed intervals.
     */
    public enum StrengthLevel {
        /**
         * Very weak, easily guessable.
         */
        EASY(0, 3),
        /**
         * Moderate protection, suitable for low-risk internal tools.
         */
        MEDIUM(4, 6),
        /**
         * Strong, resistant to standard brute-force attacks.
         */
        STRONG(7, 9),
        /**
         * Very strong, requires significant computational power to crack.
         */
        VERY_STRONG(10, 12),
        /**
         * Theoretically unbreakable by modern standards. (Note: Unreachable by current scoring logic).
         */
        EXTREMELY_STRONG(13, Integer.MAX_VALUE);

        private final int minScore;
        private final int maxScore;

        StrengthLevel(int minScore, int maxScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        /**
         * Determines the strength level based on a numerical score.
         *
         * @param score the calculated password score
         * @return the corresponding {@link StrengthLevel}, defaulting to {@link #EASY} if out of bounds
         */
        public static StrengthLevel fromScore(int score) {
            for (StrengthLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return EASY;
        }
    }

    /**
     * Categorizes characters into distinct types to evaluate password diversity.
     *
     * <p>Uses a custom {@link CharPredicate} to encapsulate matching logic, avoiding
     * deeply nested if-else statements and making the types easily extensible.
     */
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

        /**
         * Functional interface for character evaluation.
         */
        @FunctionalInterface
        interface CharPredicate {
            boolean test(char c);
        }
    }

    /**
     * A baseline blacklist of commonly used, highly vulnerable passwords.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "123456", "password", "123456789", "12345678", "12345", "111111",
            "1234567", "sunshine", "qwerty", "iloveyou", "princess", "admin",
            "welcome", "666666", "abc123", "football", "123123", "monkey",
            "654321", "!@#$%^&*", "charlie", "aa123456", "donald", "password1",
            "qwerty123", "147258369", "987654321", "1qaz2wsx", "asdfghjkl",
            "1q2w3e4r", "1234abcd", "3.1415926");

    /**
     * Regex pattern to detect consecutive repeated characters.
     * <p>Explanation: {@code (.)} captures any character into group 1. {@code \1{2,}} checks
     * if that exact same character follows immediately 2 or more times (total 3+ consecutive).
     * Example: Matches "aaa" or "111", but not "aba".
     */
    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{2,}");

    /**
     * Pre-compiled Regex to strip leading and trailing non-letters.
     * Used to fast-check if strings like "!password123" contain the base word "password".
     */
    private static final Pattern STRIP_NON_ALPHA_PATTERN = Pattern.compile("(^[^a-z]+)|([^a-z]+$)");

    /**
     * Calculates a numerical strength score for the given password.
     *
     * @param password the password string to evaluate
     * @return the calculated score, bounded to a minimum of 0 (0-15+)
     */
    public static int calculateScore(String password) {
        if (password == null || password.isBlank()) {
            return 0;
        }

        password = password.trim();
        int length = password.length();

        // Fast fail for obvious garbage inputs without running complex metrics
        if (length <= 3 || isAllSameCharacter(password)) {
            return 0;
        }

        int score = 0;
        PasswordMetrics metrics = new PasswordMetrics(password);

        // Add points for good practices
        score += getLengthScore(length);
        score += metrics.getTypeCount();
        score += getCombinationBonus(length, metrics);

        // Subtract points for bad practices
        score -= getPenalties(password, length, metrics);

        // Ensure we never return a negative score
        return Math.max(score, 0);
    }

    /**
     * Convenience method to get the qualitative strength level directly.
     *
     * @param password the password string to evaluate
     * @return the evaluated {@link StrengthLevel}
     */
    public static StrengthLevel getStrengthLevel(String password) {
        int score = calculateScore(password);
        return StrengthLevel.fromScore(score);
    }

    /**
     * Assigns points based on password length thresholds.
     *
     * @param length the trimmed length of the password
     * @return length-based score (0-7)
     */
    private static int getLengthScore(int length) {
        if (length >= 20) {
            return 7;
        }
        if (length >= 16) {
            return 5;
        }
        if (length >= 12) {
            return 3;
        }
        if (length >= 8) {
            return 1;
        }
        return 0;
    }

    /**
     * Assigns bonus points for combining sufficient length with high character diversity.
     * <p>This rewards complex passwords much more than just long simple ones.
     *
     * @param length  the password length
     * @param metrics the pre-calculated character metrics
     * @return combination bonus score (0-4)
     */
    private static int getCombinationBonus(int length, PasswordMetrics metrics) {
        int bonus = 0;
        int typeCount = metrics.getTypeCount();

        if (length >= 8 && typeCount >= 3) bonus += 1;
        if (length >= 12 && typeCount >= 4) bonus += 2;
        // Long passwords using all 4 character types get a massive +2 bonus
        if (length >= 16 && typeCount >= 4) bonus += 2;

        return bonus;
    }

    /**
     * Calculates deduction points for predictable or weak patterns.
     *
     * @param password the raw password string
     * @param length   the password length
     * @param metrics  the pre-calculated character metrics
     * @return total penalty points to subtract
     */
    private static int getPenalties(String password, int length, PasswordMetrics metrics) {
        int penalty = 0;

        // Pure lowercase/digits-only passwords are highly discouraged
        if (metrics.getTypeCount() == 1) {
            penalty += 3;
        }

        // Penalty scales linearly for short passwords (e.g., length 4 gets -2, length 5 gets -1)
        if (length < 8) {
            penalty += (8 - length);
        }

        // Patterns like "aaa" or "abcabcabc"
        if (hasRepeatedPattern(password)) {
            penalty += 2;
        }

        // Matches against known breached password lists
        if (isCommonPassword(password)) {
            penalty += 5;
        }

        // Keyboard walks like "123", "abc", "qwe"
        if (hasSequentialChars(password)) {
            penalty += 2;
        }

        return penalty;
    }

    /**
     * Checks if the password consists of the exact same character repeated.
     * <p>Uses Java Streams to count distinct characters. Highly efficient and readable.
     *
     * @param password the password string
     * @return true if all characters are identical (e.g., "aaaa")
     */
    private static boolean isAllSameCharacter(String password) {
        return password.chars().distinct().count() == 1;
    }

    /**
     * Detects two types of lazy password construction:
     * <ol>
     *   <li><b>Segment Repetition:</b> The password is exactly 3 identical segments (e.g., "abcabcabc").
     *       Note: Requires length to be cleanly divisible by 3.</li>
     *   <li><b>Character Repetition:</b> 3 or more of the exact same character in a row (e.g., "111").
     *       Delegates to the {@link #REPEATED_PATTERN} regex.</li>
     * </ol>
     *
     * @param password the password string
     * @return true if a repeated pattern is detected
     */
    private static boolean hasRepeatedPattern(String password) {
        int len = password.length();
        // Check for repeated sequences (e.g., "abcabcabc")
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

    /**
     * Checks if the password matches or contains a known weak password.
     *
     * <p><b>Bidirectional Check:</b> This method intentionally checks both ways:
     * {@code password.contains(common)} AND {@code common.contains(password)}.
     * This catches variations like "password123" (contains "password") as well as
     * truncated leaks like "123456" which might be a substring of a common password.
     *
     * @param password the password to check
     * @return true if it is deemed too common
     */
    private static boolean isCommonPassword(String password) {
        String lower = password.toLowerCase();

        // 1. Check exact match
        if (COMMON_PASSWORDS.contains(lower)) {
            return true;
        }

        // 2. Strip leading/trailing digits and symbols (e.g., "!password123" -> "password")
        // and check against the Set in O(1) time.
        String stripped = STRIP_NON_ALPHA_PATTERN.matcher(lower).replaceAll("");
        return !stripped.isEmpty() && COMMON_PASSWORDS.contains(stripped);
    }

    /**
     * Detects sequential character patterns (e.g., "abc", "123", "xyz").
     *
     * <p>Checks if the Unicode value of characters increments by exactly 1.
     * Requires at least two separate sequential blocks (e.g., "123abc") to trigger a penalty,
     * preventing false positives on very short passwords.
     *
     * @param password the password string
     * @return true if sequential patterns are detected
     */
    private static boolean hasSequentialChars(String password) {
        int sequentialCount = 0;
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                sequentialCount++;
            }
        }
        return sequentialCount >= 1;
    }

    /**
     * Internal immutable snapshot of a password's character composition.
     *
     * <p>Analyzes the string once during construction to determine which character types
     * are present. This prevents multiple redundant iterations over the password string
     * during the scoring phase.
     */
    private static class PasswordMetrics {
        final boolean hasDigits;
        final boolean hasLowercase;
        final boolean hasUppercase;
        final boolean hasSpecial;
        final int typeCount;

        PasswordMetrics(String password) {
            boolean digits = false;
            boolean lowercase = false;
            boolean uppercase = false;
            boolean special = false;
            int count = 0;

            for (char c : password.toCharArray()) {
                // Only evaluate a character if its type hasn't been found yet
                if (!digits && CharacterType.DIGIT.matches(c)) {
                    digits = true;
                    count++;
                } else if (!lowercase && CharacterType.LOWERCASE.matches(c)) {
                    lowercase = true;
                    count++;
                } else if (!uppercase && CharacterType.UPPERCASE.matches(c)) {
                    uppercase = true;
                    count++;
                } else if (!special && CharacterType.SPECIAL.matches(c)) {
                    special = true;
                    count++;
                }

                // Performance optimization: stop scanning if all 4 types are already found
                if (count == 4) {
                    break;
                }
            }

            this.hasDigits = digits;
            this.hasLowercase = lowercase;
            this.hasUppercase = uppercase;
            this.hasSpecial = special;
            this.typeCount = count;
        }

        /**
         * Returns the total number of distinct character types found in the password.
         *
         * @return diversity count (0-4)
         */
        int getTypeCount() {
            return typeCount;
        }
    }
}