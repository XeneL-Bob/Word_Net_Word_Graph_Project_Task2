package wordnetcomp;

import Util.Corpus;
import Util.Pair;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Task 2: Count ordered bigrams and query kth-tier bigram frequency.
 * note: direction matters (u->v != v->u).
 */
public class CoOccurrence {

    private final Map<Pair, Integer> bigramCount = new HashMap<>();
    private final Map<Integer, List<Pair>> bucket = new HashMap<>();
    private List<Integer> freqsDesc;

    // ---------- Build ordered bigram counts ----------
    public void buildBigrams(Corpus corpus) {
        for (List<String> sent : corpus.sentences) {
            if (sent.size() < 2) continue;
            for (int i = 0; i + 1 < sent.size(); i++) {
                Pair p = new Pair(sent.get(i), sent.get(i + 1));
                bigramCount.merge(p, 1, Integer::sum);
            }
        }
    }

    // ---------- Write CSV: from,to,count (no header), order not required ----------
    public void writeBigramCSV(Path outCsv) throws IOException {
        Files.createDirectories(outCsv.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
            for (Map.Entry<Pair, Integer> e : bigramCount.entrySet()) {
                bw.write(e.getKey().from + "," + e.getKey().to + "," + e.getValue());
                bw.newLine();
            }
        }
    }

    // ---------- Build frequency tiers for rank query ----------
    public void buildRankBuckets() {
        bucket.clear();
        for (Map.Entry<Pair, Integer> e : bigramCount.entrySet()) {
            bucket.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (List<Pair> list : bucket.values()) Collections.sort(list); // (from asc, to asc)
        freqsDesc = new ArrayList<>(bucket.keySet());
        freqsDesc.sort(Comparator.reverseOrder());
    }

    // ---------- kth-tier query ----------
    public void getBigramRank(int k) {
        if (freqsDesc == null || freqsDesc.isEmpty() || k < 1 || k > freqsDesc.size()) {
            System.out.printf("Bigram Rank %d: 0 pair(s) with 0 occurrence(s).\n", k);
            System.out.println("Pairs include: []");
            return;
        }
        int f = freqsDesc.get(k - 1);
        List<Pair> pairs = bucket.get(f);
        System.out.printf("Bigram Rank %d: %d pair(s) with %d occurrence(s).\n", k, pairs.size(), f);
        System.out.println("Pairs include: " + pairs.toString());
    }

    public Map<Pair, Integer> getBigramCountMap() {
        return Collections.unmodifiableMap(bigramCount);
    }

    // ---------- Standalone entry (optional) ----------
    public static void main(String[] args) throws Exception {
        Path book   = Paths.get("Resources", "book.txt");
        Path outDir = Paths.get("OutputFiles");
        Corpus corpus = Corpus.load(book);

        CoOccurrence co = new CoOccurrence();
        co.buildBigrams(corpus);
        co.writeBigramCSV(outDir.resolve("bigram_frequency.csv"));
        co.buildRankBuckets();
        co.getBigramRank(1);
    }
}
