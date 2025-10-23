package wordnetcomp;

import Util.Corpus;
import java.nio.file.*;

/**
 * Master driver that can run all tasks.
 * note: loads the corpus once, then each task uses the same data.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Path book   = Paths.get("Resources", "book.txt");
        Path outDir = Paths.get("OutputFiles");
        Corpus corpus = Corpus.load(book);

        // ---- Task 1 ----
        WordFrequency wf = new WordFrequency();
        wf.buildCounts(corpus);
        wf.writeWordFrequencyCSV(outDir.resolve("word_frequency.csv"));
        wf.writeSortedWordsCSV(outDir.resolve("sorted_words.csv"));
        wf.buildRankBuckets();
        wf.getTopKWords(1);   // demo
        wf.getTopKWords(2);   // demo

        // ---- Task 2 ----
        CoOccurrence co = new CoOccurrence();
        co.buildBigrams(corpus);
        co.writeBigramCSV(outDir.resolve("bigram_frequency.csv"));
        co.buildRankBuckets();
        co.getBigramRank(1);  // demo

        // ---- Task 3 ----
        WordConnection wc = new WordConnection();
        wc.buildGraphFromCorpus(corpus);
        // change examples below to words that exist in your book.txt
        wc.getShortestPath("paul", "arrakis");
        wc.getWordsAtHops("paul", 1);

        // ---- Task 4 ----
        Generator gen = new Generator();
        gen.prepare(corpus);
        gen.generateSentence("paul", 6);
    }
}
