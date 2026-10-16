package travelsync;

import java.util.Arrays;
import travelsync.flow.*;

public class TestM4 {
    public static void main(String[] args) {
        // 0 = Source city, 5 = Destination city, 1-4 = connecting cities
        FlowNetwork net = new FlowNetwork(6);
        net.addLink(0, 1, 16);
        net.addLink(0, 2, 13);
        net.addLink(1, 3, 12);
        net.addLink(2, 1, 4);
        net.addLink(2, 4, 14);
        net.addLink(3, 2, 9);
        net.addLink(3, 5, 20);
        net.addLink(4, 3, 7);
        net.addLink(4, 5, 4);

        System.out.println("Ford-Fulkerson max flow: " + MaxFlow.fordFulkerson(net, 0, 5));
        System.out.println("Edmonds-Karp  max flow: " + MaxFlow.edmondsKarp(net, 0, 5));
        System.out.println("Dinic         max flow: " + MaxFlow.dinic(net, 0, 5));

        MaxFlow.CutResult cut = MaxFlow.minCut(net, 0, 5);
        System.out.println("Min-cut value: " + cut.value);
        System.out.println("Bottleneck links: " + cut.edgesToString());

        // Travelers 0-3, hotels 0-2
        boolean[][] eligible = {
            { true,  true,  false },  // traveler 0 can use hotel 0 or 1
            { true,  false, false },  // traveler 1 can use hotel 0
            { false, true,  true  },  // traveler 2 can use hotel 1 or 2
            { false, false, true  }   // traveler 3 can use hotel 2
        };
        System.out.println("One room per hotel:  " + Arrays.toString(BipartiteMatching.match(eligible, 3)));
        System.out.println("Rooms {1,2,1}:       " + Arrays.toString(BipartiteMatching.matchWithRooms(eligible, new int[] { 1, 2, 1 })));
    }
}