package wordnetcomp;

import Util.Corpus;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Task 1: Count words, sort by (freq desc, word asc), and answer kth-tier query.
 * Junior note: we pre-bucket by frequency so getTopKWords(k) is O(size_of_tier).
 */
public class WordFrequency {

    private final Map<String, Integer> wordCount = new HashMap<>();
    private final Map<Integer, List<String>> freqBuckets = new HashMap<>();
    private List<Integer> distinctFreqsDesc;

    // ---------- Build counts from corpus ----------
    public void buildCounts(Corpus corpus) {
        for (List<String> sent : corpus.sentences) {
            for (String w : sent) wordCount.merge(w, 1, Integer::sum);
        }
    }

    // ---------- CSV writers (no header) ----------
    public void writeWordFrequencyCSV(Path outCsv) throws IOException {
        Files.createDirectories(outCsv.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
            for (Map.Entry<String, Integer> e : wordCount.entrySet()) {
                bw.write(e.getKey() + "," + e.getValue());
                bw.newLine();
            }
        }
    }

    public void writeSortedWordsCSV(Path outCsv) throws IOException {
        Files.createDirectories(outCsv.getParent());
        List<Map.Entry<String, Integer>> sorted = wordCount.entrySet().stream()
                .sorted((a, b) -> {
                    int c = Integer.compare(b.getValue(), a.getValue()); // freq desc
                    return (c != 0) ? c : a.getKey().compareTo(b.getKey()); // word asc
                })
                .collect(Collectors.toList());
        try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
            for (Map.Entry<String, Integer> e : sorted) {
                bw.write(e.getKey() + "," + e.getValue());
                bw.newLine();
            }
        }
    }

    // ---------- Precompute rank tiers ----------
    public void buildRankBuckets() {
        freqBuckets.clear();
        for (Map.Entry<String, Integer> e : wordCount.entrySet()) {
            freqBuckets.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        for (List<String> tier : freqBuckets.values()) Collections.sort(tier); // alpha inside tier
        distinctFreqsDesc = new ArrayList<>(freqBuckets.keySet());
        distinctFreqsDesc.sort(Comparator.reverseOrder());
    }

    // ---------- kth-tier query ----------
    public void getTopKWords(int k) {
        if (distinctFreqsDesc == null || distinctFreqsDesc.isEmpty() || k < 1 || k > distinctFreqsDesc.size()) {
            System.out.printf("Rank %d: 0 word(s) with 0 occurrence(s).\n", k);
            System.out.println("Word(s) include: []");
            return;
        }
        int freq = distinctFreqsDesc.get(k - 1);
        List<String> words = freqBuckets.get(freq);
        System.out.printf("Rank %d: %d word(s) with %d occurrence(s).\n", k, words.size(), freq);
        System.out.println("Word(s) include: " + words.toString());
    }

    // ---------- Standalone entry (optional) ----------
    public static void main(String[] args) throws Exception {
        Path book   = Paths.get("Resources", "book.txt");
        Path outDir = Paths.get("OutputFiles");
        Corpus corpus = Corpus.load(book);

        WordFrequency wf = new WordFrequency();
        wf.buildCounts(corpus);
        wf.writeWordFrequencyCSV(outDir.resolve("word_frequency.csv"));
        wf.writeSortedWordsCSV(outDir.resolve("sorted_words.csv"));
        wf.buildRankBuckets();
        wf.getTopKWords(1);
    }
}
