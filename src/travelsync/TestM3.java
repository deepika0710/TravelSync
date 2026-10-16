package travelsync;

import java.util.ArrayList;
import java.util.List;
import travelsync.dp.*;
import travelsync.model.*;

public class TestM3 {
    public static void main(String[] args) {
        // 1. Edit distance
        System.out.println("Levenshtein Prais->Paris: " + EditDistance.levenshtein("Prais", "Paris"));
        System.out.println("Damerau     Prais->Paris: " + EditDistance.damerau("Prais", "Paris"));

        List<String> names = new ArrayList<>();
        for (Destination d : SampleData.destinations()) names.add(d.getName());
        System.out.println("Did you mean (Kyto): " + EditDistance.suggest("Kyto", names));
        System.out.println("Did you mean (Santorni): " + EditDistance.suggest("Santorni", names));
        System.out.println("Did you mean (Pariss): " + EditDistance.suggest("Pariss", names));

        // 2. Bitmask DP: 0=Goa 1=Paris 2=Bali 3=Kyoto 4=Manali (travel hours)
        int[][] dist = {
            {0, 12, 8, 10, 6},
            {12, 0, 15, 14, 9},
            {8, 15, 0, 7, 11},
            {10, 14, 7, 0, 13},
            {6, 9, 11, 13, 0}
        };
        ItineraryPlanner.Result trip = ItineraryPlanner.bestRoute(dist, 0, 3);
        System.out.println("Best 3-stop trip from Goa: cost=" + trip.cost + ", route=" + trip.route);

        // 3. Matrix chain
        MatrixChain.Result mc = MatrixChain.solve(new int[] {10, 30, 5, 60});
        System.out.println("Matrix chain cost=" + mc.cost + ", order=" + mc.order);

        // 4. Optimal BST
        OptimalBST.Result t1 = OptimalBST.solve(new String[] {"A", "B", "C"}, new int[] {34, 8, 50});
        System.out.println("Optimal BST check: cost=" + t1.cost + ", tree=" + t1.tree);
        OptimalBST.Result t2 = OptimalBST.forDestinations(SampleData.destinations());
        System.out.println("Destination BST: cost=" + t2.cost + ", tree=" + t2.tree);
    }
}