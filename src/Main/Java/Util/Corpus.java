package wordnet.util;
import java.io.*;
 import java.nio.file.*;
 import java.util.*;
public final class Corpus {
  public final List<List<String>> sentences;
  public final Set<String> vocab; // TreeSet for deterministic order
  private Corpus(List<List<String>> s, Set<String> v){ sentences=s; vocab=v; }
  public static Corpus load(Path p) throws IOException {
    List<List<String>> sents = new ArrayList<>();
    Set<String> vocab = new TreeSet<>();
    try (BufferedReader br = Files.newBufferedReader(p)) {
      for (String line; (line = br.readLine()) != null;) {
        String[] toks = Tokenizer.cleanToTokens(line);
        if (toks.length == 0) continue;
        List<String> sent = Arrays.asList(toks);
        sents.add(sent);
        vocab.addAll(sent);
      }
    }
    return new Corpus(Collections.unmodifiableList(sents), Collections.unmodifiableSet(vocab));
  }
}
