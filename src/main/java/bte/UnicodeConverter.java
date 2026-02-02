package bte;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting regular characters to Unicode superscript and
 * subscript variants.
 * Note: Not all characters have Unicode super/subscript equivalents.
 */
public class UnicodeConverter {

    // Superscript mappings
    private static final Map<Character, Character> SUPERSCRIPT_MAP = new HashMap<>();

    // Subscript mappings
    private static final Map<Character, Character> SUBSCRIPT_MAP = new HashMap<>();

    static {
        // Superscript digits
        SUPERSCRIPT_MAP.put('0', '⁰');
        SUPERSCRIPT_MAP.put('1', '¹');
        SUPERSCRIPT_MAP.put('2', '²');
        SUPERSCRIPT_MAP.put('3', '³');
        SUPERSCRIPT_MAP.put('4', '⁴');
        SUPERSCRIPT_MAP.put('5', '⁵');
        SUPERSCRIPT_MAP.put('6', '⁶');
        SUPERSCRIPT_MAP.put('7', '⁷');
        SUPERSCRIPT_MAP.put('8', '⁸');
        SUPERSCRIPT_MAP.put('9', '⁹');

        // Superscript symbols
        SUPERSCRIPT_MAP.put('+', '⁺');
        SUPERSCRIPT_MAP.put('-', '⁻');
        SUPERSCRIPT_MAP.put('=', '⁼');
        SUPERSCRIPT_MAP.put('(', '⁽');
        SUPERSCRIPT_MAP.put(')', '⁾');

        // Superscript letters (limited set available)
        SUPERSCRIPT_MAP.put('n', 'ⁿ');
        SUPERSCRIPT_MAP.put('i', 'ⁱ');

        // Subscript digits
        SUBSCRIPT_MAP.put('0', '₀');
        SUBSCRIPT_MAP.put('1', '₁');
        SUBSCRIPT_MAP.put('2', '₂');
        SUBSCRIPT_MAP.put('3', '₃');
        SUBSCRIPT_MAP.put('4', '₄');
        SUBSCRIPT_MAP.put('5', '₅');
        SUBSCRIPT_MAP.put('6', '₆');
        SUBSCRIPT_MAP.put('7', '₇');
        SUBSCRIPT_MAP.put('8', '₈');
        SUBSCRIPT_MAP.put('9', '₉');

        // Subscript symbols
        SUBSCRIPT_MAP.put('+', '₊');
        SUBSCRIPT_MAP.put('-', '₋');
        SUBSCRIPT_MAP.put('=', '₌');
        SUBSCRIPT_MAP.put('(', '₍');
        SUBSCRIPT_MAP.put(')', '₎');

        // Subscript letters (very limited)
        SUBSCRIPT_MAP.put('a', 'ₐ');
        SUBSCRIPT_MAP.put('e', 'ₑ');
        SUBSCRIPT_MAP.put('o', 'ₒ');
        SUBSCRIPT_MAP.put('x', 'ₓ');
    }

    /**
     * Convert a character to its superscript equivalent if available.
     * 
     * @param c The character to convert
     * @return The superscript variant, or the original character if no mapping
     *         exists
     */
    public static char toSuperscript(char c) {
        return SUPERSCRIPT_MAP.getOrDefault(c, c);
    }

    /**
     * Convert a character to its subscript equivalent if available.
     * 
     * @param c The character to convert
     * @return The subscript variant, or the original character if no mapping exists
     */
    public static char toSubscript(char c) {
        return SUBSCRIPT_MAP.getOrDefault(c, c);
    }

    /**
     * Convert a string to superscript.
     * Characters without superscript equivalents remain unchanged.
     */
    public static String toSuperscript(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(toSuperscript(c));
        }
        return result.toString();
    }

    /**
     * Convert a string to subscript.
     * Characters without subscript equivalents remain unchanged.
     */
    public static String toSubscript(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(toSubscript(c));
        }
        return result.toString();
    }

    /**
     * Check if a character has a superscript equivalent.
     */
    public static boolean hasSuperscript(char c) {
        return SUPERSCRIPT_MAP.containsKey(c);
    }

    /**
     * Check if a character has a subscript equivalent.
     */
    public static boolean hasSubscript(char c) {
        return SUBSCRIPT_MAP.containsKey(c);
    }
}
