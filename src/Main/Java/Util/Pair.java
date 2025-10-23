package Util;

import java.util.Objects;

/**
 * Simple immutable (from,to) pair for ordered bigrams/edges.
 * Comparable by from asc, then to asc to guarantee deterministic ordering.
 */
public final class Pair implements Comparable<Pair> {
    public final String from;
    public final String to;

    public Pair(String from, String to) {
        this.from = from;
        this.to   = to;
    }

    @Override
    public int compareTo(Pair o) {
        int c = this.from.compareTo(o.from);
        return (c != 0) ? c : this.to.compareTo(o.to);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) return false;
        Pair p = (Pair) o;
        return from.equals(p.from) && to.equals(p.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        // Task 2 printing requirement: pair shown as two words separated by a whitespace
        return from + " " + to;
    }
}
