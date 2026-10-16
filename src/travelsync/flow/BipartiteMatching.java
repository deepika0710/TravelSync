package travelsync.flow;

import java.util.Arrays;

public class BipartiteMatching {

    // eligible[i][j] = true if traveler i can stay at hotel j (one traveler per hotel)
    // returns for each traveler the hotel number, or -1 if nobody is free for them
    public static int[] match(boolean[][] eligible, int numHotels) {
        int travelers = eligible.length;
        int[] hotelTaken = new int[numHotels];
        Arrays.fill(hotelTaken, -1);

        for (int t = 0; t < travelers; t++) {
            boolean[] seen = new boolean[numHotels];
            tryAssign(t, eligible, seen, hotelTaken);
        }

        int[] result = new int[travelers];
        Arrays.fill(result, -1);
        for (int h = 0; h < numHotels; h++) {
            if (hotelTaken[h] != -1) result[hotelTaken[h]] = h;
        }
        return result;
    }

    // tries to give traveler t a hotel, asking others to move if needed
    private static boolean tryAssign(int t, boolean[][] eligible, boolean[] seen, int[] hotelTaken) {
        for (int h = 0; h < hotelTaken.length; h++) {
            if (eligible[t][h] && !seen[h]) {
                seen[h] = true;
                if (hotelTaken[h] == -1 || tryAssign(hotelTaken[h], eligible, seen, hotelTaken)) {
                    hotelTaken[h] = t;
                    return true;
                }
            }
        }
        return false;
    }

    // same idea, but each hotel has several rooms: rooms[j] = number of rooms in hotel j
    public static int[] matchWithRooms(boolean[][] eligible, int[] rooms) {
        int slots = 0;
        for (int r : rooms) slots += r;
        int[] slotHotel = new int[slots];
        int k = 0;
        for (int h = 0; h < rooms.length; h++) {
            for (int r = 0; r < rooms[h]; r++) slotHotel[k++] = h;
        }

        boolean[][] eligibleSlots = new boolean[eligible.length][slots];
        for (int t = 0; t < eligible.length; t++) {
            for (int s = 0; s < slots; s++) {
                eligibleSlots[t][s] = eligible[t][slotHotel[s]];
            }
        }

        int[] slotResult = match(eligibleSlots, slots);
        int[] result = new int[eligible.length];
        for (int t = 0; t < result.length; t++) {
            result[t] = (slotResult[t] == -1) ? -1 : slotHotel[slotResult[t]];
        }
        return result;
    }
}