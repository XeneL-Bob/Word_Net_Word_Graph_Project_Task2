package wordnetcomp;

import Util.Corpus;
import Util.Pair;
import java.util.*;

/**
 * Task 4: Deterministic auto-sentence generation.
 * Rule: from current word, pick outgoing neighbor with max bigram count; tie -> alphabetical 'to'.
 * Stop early if no outgoing edges.
 */
public class Generator {

    // For fast next-word choice: map u -> list of (v,count) sorted by (count desc, v asc)
    private final Map<String, List<Map.Entry<String,Integer>>> bestNext = new HashMap<>();

    /** Build bigram counts and pre-sort neighbors for O(1) next-choice. */
    public void prepare(Corpus corpus) {
        Map<Pair, Integer> bigramCount = new HashMap<>();
        for (List<String> sent : corpus.sentences) {
            for (int i = 0; i + 1 < sent.size(); i++) {
                Pair p = new Pair(sent.get(i), sent.get(i + 1));
                bigramCount.merge(p, 1, Integer::sum);
            }
        }
        Map<String, Map<String, Integer>> byFrom = new HashMap<>();
        for (Map.Entry<Pair, Integer> e : bigramCount.entrySet()) {
            byFrom.computeIfAbsent(e.getKey().from, k -> new HashMap<>())
                  .put(e.getKey().to, e.getValue());
        }
        // sort each neighbor list by (count desc, to asc)
        for (Map.Entry<String, Map<String,Integer>> e : byFrom.entrySet()) {
            List<Map.Entry<String,Integer>> lst = new ArrayList<>(e.getValue().entrySet());
            lst.sort((a,b) -> {
                int c = Integer.compare(b.getValue(), a.getValue());
                return (c != 0) ? c : a.getKey().compareTo(b.getKey());
            });
            bestNext.put(e.getKey(), lst);
        }
    }

    // ---------- Sentence generation ----------
    public void generateSentence(String sWord, int length) {
        if (length < 1) length = 1;
        List<String> sentence = new ArrayList<>();
        sentence.add(sWord);

        String cur = sWord;
        while (sentence.size() < length) {
            List<Map.Entry<String,Integer>> outs = bestNext.get(cur);
            if (outs == null || outs.isEmpty()) {
                System.out.printf("Incomplete sentence with %d words is: %s\n", sentence.size(), sentence);
                return;
            }
            String next = outs.get(0).getKey(); // best by our sorting
            sentence.add(next);
            cur = next;
        }
        System.out.printf("Complete sentence with %d words is: %s\n", sentence.size(), sentence);
    }

    // ---------- Standalone entry (optional) ----------
    public static void main(String[] args) throws Exception {
        Corpus corpus = Corpus.load(java.nio.file.Paths.get("Resources", "book.txt"));
        Generator g = new Generator();
        g.prepare(corpus);
        g.generateSentence("tim", 5);
    }
}
