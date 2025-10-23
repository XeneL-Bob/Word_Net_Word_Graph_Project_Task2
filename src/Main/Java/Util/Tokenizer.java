package Util;

/**
 * Tokenizer utility.
 * note: keep input sanitation in one place so
 * every task (freq, bigrams, graph) stays consistent/deterministic.
 */
public final class Tokenizer {
    private Tokenizer() {}

    /**
     * Convert a raw line to lower-case a–z tokens.
     * Non [a-z] chars -> space, then split on whitespace.
     * Time: O(len(line))
     */
    public static String[] cleanToTokens(String line) {
        if (line == null) return new String[0];
        String cleaned = line.toLowerCase().replaceAll("[^a-z]", " ").trim();
        if (cleaned.isEmpty()) return new String[0];
        return cleaned.split("\\s+"); // robust if there are multiple spaces
    }
}
