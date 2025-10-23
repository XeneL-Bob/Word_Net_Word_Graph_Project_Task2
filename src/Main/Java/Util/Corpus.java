package Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Corpus reader: loads book.txt once and shares tokens to all tasks.
 * note: reading once avoids inconsistent parsing and is faster overall.
 */
public final class Corpus {
    public final List<List<String>> sentences; // immutable
    public final Set<String> vocab;            // immutable, TreeSet for alpha determinism

    private Corpus(List<List<String>> s, Set<String> v) {
        this.sentences = Collections.unmodifiableList(s);
        this.vocab     = Collections.unmodifiableSet(v);
    }

    /**
     * Load all sentences from book.txt and build vocab (TreeSet for deterministic order).
     * Time: O(total characters), Space: O(N tokens).
     */
    public static Corpus load(Path pathToBook) throws IOException {
        List<List<String>> sents = new ArrayList<>();
        Set<String> vocab = new TreeSet<>();
        try (BufferedReader br = Files.newBufferedReader(pathToBook)) {
            for (String line; (line = br.readLine()) != null; ) {
                String[] toks = Tokenizer.cleanToTokens(line);
                if (toks.length == 0) continue;
                List<String> sent = Arrays.asList(toks);
                sents.add(sent);
                vocab.addAll(sent);
            }
        }
        return new Corpus(sents, vocab);
    }
}
