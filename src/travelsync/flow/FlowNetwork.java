package travelsync.flow;

import java.util.ArrayList;
import java.util.List;
import travelsync.model.TransportLink;

public class FlowNetwork {

    // one directed link inside the working ("residual") graph
    static class Edge {
        int to;
        int cap;  // spare capacity left
        int rev;  // position of the reverse edge in the other city's list

        Edge(int to, int cap, int rev) {
            this.to = to;
            this.cap = cap;
            this.rev = rev;
        }
    }

    private final int n;
    private final List<int[]> links = new ArrayList<>(); // {from, to, capacity}

    public FlowNetwork(int n) {
        this.n = n;
    }

    public int size() { return n; }

    public List<int[]> getLinks() { return links; }

    public void addLink(int from, int to, int capacity) {
        links.add(new int[] { from, to, capacity });
    }

    public void addLink(TransportLink link) {
        addLink(link.getFrom(), link.getTo(), link.getCapacity());
    }

    // builds a fresh working copy, so every algorithm starts from the original capacities
    List<List<Edge>> buildResidual() {
        List<List<Edge>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
        for (int[] l : links) {
            int u = l[0], v = l[1], c = l[2];
            if (u == v) continue;
            int ui = g.get(u).size();
            int vi = g.get(v).size();
            g.get(u).add(new Edge(v, c, vi));
            g.get(v).add(new Edge(u, 0, ui));
        }
        return g;
    }
}