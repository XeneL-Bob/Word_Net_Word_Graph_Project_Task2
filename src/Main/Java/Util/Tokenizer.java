package wordnet.util;
public final class Tokenizer {
  private Tokenizer() {}
  public static String[] cleanToTokens(String line) {
    if (line == null) return new String[0];
    String cleaned = line.toLowerCase().replaceAll("[^a-z]", " ").trim();
    if (cleaned.isEmpty()) return new String[0];
    return cleaned.split("\\s+"); // single spaces to be expected depedendancy
  }
}