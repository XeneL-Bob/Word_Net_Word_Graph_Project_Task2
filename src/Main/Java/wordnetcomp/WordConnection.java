package wordnetcomp;

import Util.*;

import java.nio.file.*;
import java.util.*;

/**
 * Task 3: Build word graph from bigrams, answer shortest path and exact-hop queries.
 * Junior note: Dijkstra uses weight=100.0/count so high co-occurrence yields lower cost.
 */
public class WordConnection {

    private Graph graph; // built from bigram counts

    /** Build graph (can reuse CoOccurrence bigrams or rebuild here). */
    public void buildGraphFromCorpus(Corpus corpus) {
        // Reuse CoOccurrence logic to count bigrams quickly
        Map<Pair, Integer> bigramCount = new HashMap<>();
        for (List<String> sent : corpus.sentences) {
            for (int i = 0; i + 1 < sent.size(); i++) {
                Pair p = new Pair(sent.get(i), sent.get(i + 1));
                bigramCount.merge(p, 1, Integer::sum);
            }
        }
        this.graph = Graph.fromBigramCounts(bigramCount);
    }

    // ---------- 3.1 Shortest path ----------
    public void getShortestPath(String src, String dst) {
        if (graph == null) {
            System.out.printf("Shortest Path between '%s' and '%s' does not exist.\n", src, dst);
            return;
        }
        if (src.equals(dst)) {
            System.out.printf("Shortest Path between '%s' and '%s' has total cost %.6f, with %d hops.\n",
                    src, dst, 0.0, 0);
            System.out.println("Path: [" + src + "]");
            return;
        }
        Graph.PathResult res = graph.shortestPath(src, dst);
        if (res == null) {
            System.out.printf("Shortest Path between '%s' and '%s' does not exist.\n", src, dst);
        } else {
            System.out.printf("Shortest Path between '%s' and '%s' has total cost %.6f, with %d hops.\n",
                    src, dst, res.distance, res.path.size() - 1);
            System.out.println("Path: " + res.path.toString());
        }
    }

    // ---------- 3.2 Nodes at exact hop distance ----------
    public void getWordsAtHops(String src, int hops) {
        if (graph == null) {
            System.out.printf("Total number of nodes with %d hop(s) from '%s': %d\n", hops, src, 0);
            System.out.println("Words: []");
            return;
        }
        List<String> at = graph.nodesAtHops(src, hops);
        System.out.printf("Total number of nodes with %d hop(s) from '%s': %d\n", hops, src, at.size());
        // Spec says include the list; keep deterministic alpha order.
        System.out.println("Words: " + at.toString());
    }

    // ---------- Standalone entry (optional) ----------
    public static void main(String[] args) throws Exception {
        Path book = Paths.get("Resources", "book.txt");
        Corpus corpus = Corpus.load(book);
        WordConnection wc = new WordConnection();
        wc.buildGraphFromCorpus(corpus);

        // quick demo (adjust words to your book.txt)
        wc.getShortestPath("tim", "bread");
        wc.getWordsAtHops("tim", 1);
    }
}
