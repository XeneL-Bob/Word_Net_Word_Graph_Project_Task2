package wordnetcomp;

import Util.Corpus;
import Util.Graph;
import Util.Pair;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Master driver for all tasks with simple CLI, validation and timing.
 *
 * Tasks:
 *   wf    – word frequency (csvs + top-k tiers)
 *   co    – bigram co-occurrence (csv + rank bucket demo)
 *   graph – word graph queries (shortest path, words at hops)
 *   gen   – simple text generator
 *   all   – run everything (default)
 *
 * Examples:
 *   java wordnetcomp.Main --task wf -k 3
 *   java wordnetcomp.Main --task graph --start paul --target arrakis --hops 2
 *   java wordnetcomp.Main -i Resources/book.txt -o OutputFiles --task all
 *   java wordnetcomp.Main --check
 */
public class Main {

    // ---------- configuration ----------
    static class Config {
        Path book    = Paths.get("Resources", "book.txt");
        Path outDir  = Paths.get("OutputFiles");
        String task  = "all";               // all | wf | co | graph | gen
        int kTier    = 3;                   // how many frequency tiers to print (wf & co)
        String start = "paul";              // start word (graph & gen)
        String target = "arrakis";          // target word (graph)
        int hops     = 1;                   // neighbor radius (graph)
        int genLen   = 6;                   // generated sentence length (gen)
        boolean checkSelf = false;          // run quick self-check after tasks
    }

    public static void main(String[] args) {
        Config cfg = parseArgs(args);

        // Validate IO
        try {
            ensureFileExists(cfg.book);
            Files.createDirectories(cfg.outDir);
        } catch (IOException e) {
            logError("FATAL: " + e.getMessage(), e);
            return;
        }

        // Load corpus (once)
        long loadStart = System.nanoTime();
        final Corpus corpus;
        try {
            corpus = Corpus.load(cfg.book);
        } catch (IOException | RuntimeException e) {
            logError("FATAL: failed to load corpus: " + e.getMessage(), e);
            return;
        }
        System.out.println("Load corpus finished in " + elapsedMs(loadStart) + " ms");

        boolean all = cfg.task.equals("all");
        if (all || cfg.task.equals("wf"))    run("Task 1 – Word Frequency", () -> runWordFrequency(corpus, cfg));
        if (all || cfg.task.equals("co"))    run("Task 2 – Co-occurrence",  () -> runCoOccurrence(corpus, cfg));
        if (all || cfg.task.equals("graph")) run("Task 3 – Word Graph",     () -> runWordGraph(corpus, cfg));
        if (all || cfg.task.equals("gen"))   run("Task 4 – Generator",      () -> runGenerator(corpus, cfg));

        if (cfg.checkSelf) run("Self-check", () -> selfCheck(corpus));
    }

    // ---------- Task runners ----------

    private static void runWordFrequency(Corpus corpus, Config cfg) {
        long t = System.nanoTime();
        WordFrequency wf = new WordFrequency();
        wf.buildCounts(corpus);
        wf.buildRankBuckets();
        System.out.println("Build word counts finished in " + elapsedMs(t) + " ms");

        try {
            wf.writeWordFrequencyCSV(cfg.outDir.resolve("word_frequency.csv"));
            wf.writeSortedWordsCSV(cfg.outDir.resolve("sorted_words.csv"));
        } catch (IOException ioe) {
            System.err.println("WARN: writing word-frequency CSVs failed: " + ioe.getMessage());
        }

        int k = Math.max(1, cfg.kTier);
        for (int tier = 1; tier <= k; tier++) wf.getTopKWords(tier);
    }

    private static void runCoOccurrence(Corpus corpus, Config cfg) {
        long t = System.nanoTime();
        CoOccurrence co = new CoOccurrence();
        co.buildBigrams(corpus);
        co.buildRankBuckets();
        System.out.println("Build bigrams finished in " + elapsedMs(t) + " ms");

        try {
            co.writeBigramCSV(cfg.outDir.resolve("bigram_frequency.csv"));
        } catch (IOException ioe) {
            System.err.println("WARN: writing bigram CSV failed: " + ioe.getMessage());
        }

        co.getBigramRank(Math.max(1, cfg.kTier));
    }

    private static void runWordGraph(Corpus corpus, Config cfg) {
        WordConnection wc = new WordConnection();
        wc.buildGraphFromCorpus(corpus);

        String a = cfg.start.toLowerCase(Locale.ROOT);
        String b = cfg.target.toLowerCase(Locale.ROOT);

        if (!corpusContains(corpus, a)) {
            System.err.println("WARN: start word '" + a + "' not found in corpus.");
            return;
        }
        if (!corpusContains(corpus, b)) {
            System.err.println("WARN: target word '" + b + "' not found in corpus.");
            return;
        }

        wc.getShortestPath(a, b);
        wc.getWordsAtHops(a, Math.max(0, cfg.hops));
    }

    private static void runGenerator(Corpus corpus, Config cfg) {
        Generator g = new Generator();
        g.prepare(corpus);
        g.generateSentence(cfg.start.toLowerCase(Locale.ROOT), Math.max(1, cfg.genLen));
    }

    // ---------- Self-check (optional) ----------

    private static void selfCheck(Corpus corpus) {
        int issues = 0;

        // Word presence
        issues += expect(corpusContains(corpus, "paul"),    "word 'paul' exists");
        issues += expect(corpusContains(corpus, "arrakis"), "word 'arrakis' exists");

        // Bigram presence
        CoOccurrence co = new CoOccurrence();
        co.buildBigrams(corpus);
        Map<Pair,Integer> bc = co.getBigramCountMap();

        issues += expect(bc.containsKey(new Pair("paul","atreides")), "bigram 'paul atreides' exists");
        issues += expect(bc.containsKey(new Pair("desert","power")),  "bigram 'desert power' exists");
        issues += expect(bc.containsKey(new Pair("gom","jabbar")),    "bigram 'gom jabbar' exists");

        // Graph shortest path sanity (should be 4 hops and ~400 cost in this corpus)
        Graph g = Graph.fromBigramCounts(bc);
        Graph.PathResult pr = g.shortestPath("paul", "arrakis");
        boolean okPath = pr != null && pr.path.size() - 1 == 4 && Math.abs(pr.distance - 400.0) < 1e-9;
        issues += expect(okPath, "shortest path paul→arrakis is 4 hops with cost 400.0");

        if (issues == 0) {
            System.out.println("Self-check OK ✅");
        } else {
            System.out.println("Self-check found " + issues + " issue(s) ❗ (see warnings above)");
        }
    }

    private static int expect(boolean condition, String label) {
        if (condition) {
            System.out.println("[OK]   " + label);
            return 0;
        } else {
            System.out.println("[WARN] " + label + " – not satisfied");
            return 1;
        }
    }

    // ---------- Utility ----------

    private static void run(String title, Runnable task) {
        System.out.println("\n================ " + title + " ================");
        long t = System.nanoTime();
        try {
            task.run();
        } catch (RuntimeException | Error ex) { // multi-catch
            logError("ERROR in " + title + ": " + ex.getMessage(), ex);
        } finally {
            System.out.println(title + " finished in " + elapsedMs(t) + " ms");
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static boolean corpusContains(Corpus corpus, String w) {
        for (List<String> s : corpus.sentences)
            for (String x : s)
                if (x.equals(w)) return true;
        return false;
    }

    private static void ensureFileExists(Path p) throws IOException {
        if (!Files.isRegularFile(p)) throw new IOException("Input file not found: " + p.toAbsolutePath());
    }

    private static void logError(String message, Throwable t) {
        System.err.println(message);
        System.err.println(t.getClass().getSimpleName() + ": " + t.getMessage());
        Throwable cause = t.getCause();
        while (cause != null) {
            System.err.println("Caused by: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            cause = cause.getCause();
        }
    }

    private static Config parseArgs(String[] args) {
        Config c = new Config();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i": case "--input":  c.book   = Paths.get(args[++i]); break;
                case "-o": case "--out":    c.outDir = Paths.get(args[++i]); break;
                case "-t": case "--task":   c.task   = args[++i].toLowerCase(Locale.ROOT); break;
                case "-k": case "--tier":   c.kTier  = parseInt(args[++i], c.kTier); break;
                case "--start":             c.start  = args[++i]; break;
                case "--target":            c.target = args[++i]; break;
                case "--hops":              c.hops   = parseInt(args[++i], c.hops); break;
                case "--len":               c.genLen = parseInt(args[++i], c.genLen); break;
                case "--check":             c.checkSelf = true; break;
                case "-h": case "--help":   printHelpAndExit();
                default:
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        printHelpAndExit();
                    }
            }
        }
        return c;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void printHelpAndExit() {
        System.out.println("""
            Usage: java wordnetcomp.Main [options]
              -i, --input <file>     Input text file (default Resources/book.txt)
              -o, --out <dir>        Output directory (default OutputFiles)
              -t, --task <name>      all | wf | co | graph | gen (default all)
              -k, --tier <n>         Print top <n> frequency tiers (default 3)
              --start <word>         Start word for graph & generator (default 'paul')
              --target <word>        Target word for graph path (default 'arrakis')
              --hops <n>             Hop distance for neighbors (default 1)
              --len <n>              Generated sentence length (default 6)
              --check                Run a quick self-check of counts/graph
              -h, --help             Show this help
            """);
        System.exit(0);
    }
}