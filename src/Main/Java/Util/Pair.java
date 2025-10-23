package wordnet.util;
public final class Pair implements Comparable<Pair> {
  public final String from, to;
  public Pair(String f, String t) { this.from=f; this.to=t; }
  @Override public int compareTo(Pair o) {
    int c = this.from.compareTo(o.from);
    return (c!=0) ? c : this.to.compareTo(o.to);
  }
  @Override public boolean equals(Object o){ 
    if(!(o instanceof Pair)) return false;
    Pair p=(Pair)o; return from.equals(p.from) && to.equals(p.to);
  }
  @Override public int hashCode(){ return 31*from.hashCode()+to.hashCode(); }
  @Override public String toString(){ return from+" "+to; }
}
