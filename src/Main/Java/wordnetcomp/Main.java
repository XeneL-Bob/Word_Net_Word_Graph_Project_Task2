package wordnetcomp;

import Util.Corpus;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Main driver for the Word Net / Word Graph project (Tasks 1–4).
 *
 * What it does:
 *  1) Loads and tokenizes the book once.
 *  2) Task 1: builds word frequencies and writes CSVs.
 *  3) Task 2: builds bigram co-occurrences and writes CSV.
 *  4) Task 3: builds a word graph, prints a shortest path + k-hop neighbors.
 *  5) Task 4: generates a simple sentence from a seed word.
 *
 * Default inputs assume:
 *   - Text at: Resources/book.txt (relative to working dir)
 *   - Outputs go to: OutputFiles/
 *   - Example “Dune-ish” words for the graph demo: paul -> arrakis
 *
 * CLI overrides (all optional, --key=value):
 *   --book=Resources/book.txt
 *   --out=OutputFiles
 *   --wordRanks=1,2
 *   --bigramRanks=1
 *   --src=paul
 *   --dst=arrakis
 *   --hops=1
 *   --seed=paul
 *   --len=6
 *
 * Examples:
 *   java ... wordnetcomp.Main
 *   java ... wordnetcomp.Main --src=jessica --dst=atreides --hops=2 --seed=desert --len=8
 */
public final class Main {

    public static void main(String[] args) {
        // parseArgs is very simple: it only handles --key=value
        Map<String, String> opts = parseArgs(args);

        // NOTE: these defaults match the assignment folders you’ve set up
        Path book   = Paths.get(opts.getOrDefault("book", "Resources/book.txt")).normalize();
        Path outDir = Paths.get(opts.getOrDefault("out",  "OutputFiles")).normalize();

        // For Task 1 and Task 2 demo prints (rank buckets)
        List<Integer> wordRanks   = parseRankList(opts.getOrDefault("wordRanks", "1,2"));
        List<Integer> bigramRanks = parseRankList(opts.getOrDefault("bigramRanks", "1"));

        // Task 3 graph demo inputs (lowercased to match tokenizer)
        String src  = opts.getOrDefault("src",  "paul").toLowerCase(Locale.ROOT);
        String dst  = opts.getOrDefault("dst",  "arrakis").toLowerCase(Locale.ROOT);
        int hops    = parseInt(opts.getOrDefault("hops", "1"), 1);

        // Task 4 generator inputs
        String seed = opts.getOrDefault("seed", "paul").toLowerCase(Locale.ROOT);
        int genLen  = parseInt(opts.getOrDefault("len",  "6"), 6);

        try {
            // Make sure the output folder exists before we try to write files
            Files.createDirectories(outDir);

            System.out.println("[INFO] Loading corpus from: " + book.toAbsolutePath());
            Corpus corpus = Corpus.load(book); // <-- Only load once and reuse below

            // ---------------- Task 1: Word Frequency ----------------
            System.out.println("[INFO] Task 1: Building word frequencies…");
            WordFrequency wf = new WordFrequency();
            wf.buildCounts(corpus);
            wf.writeWordFrequencyCSV(outDir.resolve("word_frequency.csv"));
            wf.writeSortedWordsCSV(outDir.resolve("sorted_words.csv"));
            wf.buildRankBuckets(); // prepares “rank -> words” view

            // Demo: print a couple of top ranks (configurable)
            for (int r : wordRanks) {
                wf.getTopKWords(r); // method name in class prints “Rank r …”
            }

            // ---------------- Task 2: Bigram Co-occurrence ----------------
            System.out.println("[INFO] Task 2: Building bigram co-occurrences…");
            CoOccurrence co = new CoOccurrence();
            co.buildBigrams(corpus);
            co.writeBigramCSV(outDir.resolve("bigram_frequency.csv"));
            co.buildRankBuckets();

            // Demo: print top bigram rank(s)
            for (int r : bigramRanks) {
                co.getBigramRank(r);
            }

            // ---------------- Task 3: Word Graph (connections) ----------------
            System.out.println("[INFO] Task 3: Building word graph…");
            WordConnection wc = new WordConnection();
            wc.buildGraphFromCorpus(corpus);

            // Demo: shortest path + k-hop neighbors using (src, dst, hops)
            wc.getShortestPath(src, dst);
            wc.getWordsAtHops(src, hops);

            // ---------------- Task 4: Simple Generator ----------------
            System.out.println("[INFO] Task 4: Generating a sentence…");
            Generator gen = new Generator();
            gen.prepare(corpus);
            gen.generateSentence(seed, genLen);

            System.out.println("[OK] All tasks finished. CSVs are in: " + outDir.toAbsolutePath());

        } catch (NoSuchFileException nsfe) {
            // Very common cause: running from the wrong working directory or missing book.txt
            System.err.println("[ERROR] Could not find input text file: " + book.toAbsolutePath());
            System.err.println("        Ensure Resources/book.txt exists relative to your run directory.");
            System.exit(1);
        } catch (Exception e) {
            // Generic catch so the process reports the stacktrace (useful during dev)
            System.err.println("[ERROR] Unhandled exception:");
            e.printStackTrace();
            System.exit(2);
        }
    }

    // ---- helpers below are intentionally simple (junior-friendly) ----

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        if (args == null) return map;
        for (String a : args) {
            // only accept --key=value
            if (a != null && a.startsWith("--")) {
                int ix = a.indexOf('=');
                if (ix > 2 && ix < a.length() - 1) {
                    String k = a.substring(2, ix).trim();
                    String v = a.substring(ix + 1).trim();
                    if (!k.isEmpty() && !v.isEmpty()) {
                        map.put(k, v);
                    }
                }
            }
        }
        return map;
    }

    private static List<Integer> parseRankList(String csv) {
        List<Integer> out = new ArrayList<>();
        if (csv == null || csv.isEmpty()) return out;
        for (String part : csv.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                try {
                    out.add(Integer.parseInt(p));
                } catch (NumberFormatException ignored) {
                    // if bad input, just skip it (keeps the runner forgiving)
                }
            }
        }
        return out.isEmpty() ? List.of(1) : out;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}