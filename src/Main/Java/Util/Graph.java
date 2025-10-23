package Util;

import java.util.*;

/**
 * Directed weighted word graph built from bigram counts.
 * Weight(u->v) = 100.0 / count(u,v). Higher co-occ = lower cost.
 * Junior note: adjacency lists are kept sorted by 'to' (alpha)
 * so any tie-breaking is deterministic.
 */
public final class Graph {

    public static final class Edge {
        public final String to;
        public final int count;
        public final double weight; // 100.0 / count

        public Edge(String to, int count) {
            this.to = to;
            this.count = count;
            this.weight = 100.0 / count;
        }
    }

    private final Map<String, List<Edge>> adj = new HashMap<>();

    public Graph() {}

    /**
     * Build adjacency from bigram count map.
     * Expects keys as Pair(from,to), values as counts.
     * Time: O(E log E) due to sorting adjacency by 'to'.
     */
    public static Graph fromBigramCounts(Map<Pair, Integer> bigramCount) {
        Graph g = new Graph();
        for (Map.Entry<Pair, Integer> e : bigramCount.entrySet()) {
            Pair p = e.getKey();
            int c  = e.getValue();
            g.adj.computeIfAbsent(p.from, k -> new ArrayList<>()).add(new Edge(p.to, c));
            // ensure node exists even if no outgoing edges
            g.adj.computeIfAbsent(p.to, k -> new ArrayList<>());
        }
        // sort neighbors by destination for deterministic traversals
        for (List<Edge> edges : g.adj.values()) {
            edges.sort(Comparator.comparing(ed -> ed.to));
        }
        return g;
    }

    public List<Edge> neighbors(String u) {
        return adj.getOrDefault(u, Collections.emptyList());
    }

    public Set<String> nodes() {
        return adj.keySet();
    }

    // ---------- Dijkstra with lexicographic tie-break ----------
    private static final class NodeDist implements Comparable<NodeDist> {
        final String node;
        final double dist;
        NodeDist(String node, double dist) { this.node = node; this.dist = dist; }
        @Override public int compareTo(NodeDist o) {
            int c = Double.compare(this.dist, o.dist);
            return (c != 0) ? c : this.node.compareTo(o.node);
        }
    }

    /**
     * Dijkstra from src to dst. Tie-breaks by node name in PQ comparator.
     * Returns (distance, path) or null if unreachable.
     * Time: O((V+E) log V)
     */
    public PathResult shortestPath(String src, String dst) {
        if (src.equals(dst)) {
            List<String> path = new ArrayList<>();
            path.add(src);
            return new PathResult(0.0, path);
        }
        Map<String, Double> dist = new HashMap<>();
        Map<String, String>  prev = new HashMap<>();
        for (String v : nodes()) dist.put(v, Double.POSITIVE_INFINITY);
        if (!dist.containsKey(src) || !dist.containsKey(dst)) return null;
        dist.put(src, 0.0);

        PriorityQueue<NodeDist> pq = new PriorityQueue<>();
        pq.add(new NodeDist(src, 0.0));

        while (!pq.isEmpty()) {
            NodeDist cur = pq.poll();
            if (cur.dist > dist.get(cur.node) + 1e-12) continue; // stale
            if (cur.node.equals(dst)) break; // we popped best dst

            for (Edge e : neighbors(cur.node)) {
                double nd = cur.dist + e.weight;
                if (nd + 1e-12 < dist.get(e.to)) {
                    dist.put(e.to, nd);
                    prev.put(e.to, cur.node);
                    pq.add(new NodeDist(e.to, nd));
                } else if (Math.abs(nd - dist.get(e.to)) <= 1e-12) {
                    // equal distance: tie-break lexicographically on next step
                    // since PQ comparator already uses node name,
                    // re-insert ensures deterministic pick if needed
                    pq.add(new NodeDist(e.to, nd));
                }
            }
        }

        if (!prev.containsKey(dst) && !src.equals(dst)) {
            // could be directly connected or not discovered
            if (Double.isInfinite(dist.get(dst))) return null;
        }

        // Reconstruct path if reachable
        if (Double.isInfinite(dist.get(dst))) return null;
        LinkedList<String> path = new LinkedList<>();
        String at = dst;
        path.addFirst(at);
        while (prev.containsKey(at)) {
            at = prev.get(at);
            path.addFirst(at);
        }
        return new PathResult(dist.get(dst), path);
    }

    /**
     * BFS exact hop distance from src.
     * Returns alphabetically sorted list of nodes at exactly 'hops' edges.
     * Time: O(V+E)
     */
    public List<String> nodesAtHops(String src, int hops) {
        if (!nodes().contains(src)) return Collections.emptyList();
        Map<String, Integer> d = new HashMap<>();
        Queue<String> q = new ArrayDeque<>();
        d.put(src, 0);
        q.add(src);

        while (!q.isEmpty()) {
            String u = q.poll();
            int du = d.get(u);
            if (du == hops) continue; // don't expand further from exact layer
            for (Edge e : neighbors(u)) {
                if (!d.containsKey(e.to)) {
                    d.put(e.to, du + 1);
                    q.add(e.to);
                }
            }
        }

        List<String> ans = new ArrayList<>();
        for (Map.Entry<String, Integer> en : d.entrySet()) {
            if (en.getValue() == hops) ans.add(en.getKey());
        }
        Collections.sort(ans); // deterministic output
        return ans;
    }

    // Small holder for (distance, path)
    public static final class PathResult {
        public final double distance;
        public final List<String> path;
        public PathResult(double distance, List<String> path) {
            this.distance = distance;
            this.path = path;
        }
    }
}
