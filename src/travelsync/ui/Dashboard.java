package travelsync.ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import travelsync.dp.*;
import travelsync.flow.*;
import travelsync.model.*;
import travelsync.strings.*;
import travelsync.suffix.*;

public class Dashboard extends JFrame {

    private final List<Destination> destinations = SampleData.destinations();
    private final List<Hotel> hotels = SampleData.hotels();
    private final List<Booking> bookings = SampleData.bookings();
    private final TravelIndex travelIndex = new TravelIndex(destinations, hotels);

    public Dashboard() {
        setTitle("TravelSync Dashboard");
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("M1: Search", buildSearchTab());
        tabs.addTab("M2: Similar Places", buildSimilarTab());
        tabs.addTab("M3: Spell-check & Trip", buildDpTab());
        tabs.addTab("M4: Transport Flow", buildFlowTab());

        add(tabs);
    }

    // ---------- M1 tab ----------
    private JPanel buildSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField input = new JTextField();
        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setLineWrap(true);

        JButton searchBtn = new JButton("Search descriptions (KMP)");
        JButton bookingBtn = new JButton("Search booking code (Rabin-Karp)");

        searchBtn.addActionListener(e -> {
            String q = input.getText().trim();
            StringBuilder sb = new StringBuilder();
            for (Destination d : destinations) {
                if (!KMP.searchIgnoreCase(d.getDescription(), q).isEmpty()) {
                    sb.append("Found in: ").append(d.getName()).append("\n");
                }
            }
            output.setText(sb.length() == 0 ? "No matches." : sb.toString());
        });

        bookingBtn.addActionListener(e -> {
            String q = input.getText().trim();
            List<Booking> found = RabinKarp.findBookings(bookings, q);
            output.setText(found.isEmpty() ? "No bookings found." : found.toString());
        });

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(new JLabel("Enter a keyword or booking code:"), BorderLayout.NORTH);
        top.add(input, BorderLayout.CENTER);
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 5, 5));
        btnRow.add(searchBtn);
        btnRow.add(bookingBtn);
        top.add(btnRow, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);
        return panel;
    }

    // ---------- M2 tab ----------
    private JPanel buildSimilarTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField input = new JTextField();
        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setLineWrap(true);

        JButton indexBtn = new JButton("Search all descriptions (Suffix Array index)");
        JButton similarBtn = new JButton("Find most similar destinations");

        indexBtn.addActionListener(e -> {
            List<String> found = travelIndex.search(input.getText().trim());
            output.setText(found.isEmpty() ? "No matches." : String.join("\n", found));
        });

        similarBtn.addActionListener(e -> output.setText(CommonDescriptions.mostSimilarPair(destinations)));

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(new JLabel("Enter a word to search across all descriptions:"), BorderLayout.NORTH);
        top.add(input, BorderLayout.CENTER);
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 5, 5));
        btnRow.add(indexBtn);
        btnRow.add(similarBtn);
        top.add(btnRow, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);
        return panel;
    }
        // ---------- M3 tab ----------
    private JPanel buildDpTab() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Spell-check section
        JPanel spellPanel = new JPanel(new BorderLayout(5, 5));
        JTextField spellInput = new JTextField();
        JTextArea spellOutput = new JTextArea();
        spellOutput.setEditable(false);
        JButton spellBtn = new JButton("Did you mean...? (Damerau-Levenshtein)");
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Destination d : destinations) names.add(d.getName());
        spellBtn.addActionListener(e -> {
            String best = EditDistance.suggest(spellInput.getText().trim(), names);
            int dist = EditDistance.damerau(spellInput.getText().trim().toLowerCase(), best.toLowerCase());
            spellOutput.setText("Did you mean: " + best + " (distance " + dist + ")");
        });
        spellPanel.add(new JLabel("Misspelled destination name:"), BorderLayout.NORTH);
        spellPanel.add(spellInput, BorderLayout.CENTER);
        JPanel spellSouth = new JPanel(new BorderLayout());
        spellSouth.add(spellBtn, BorderLayout.NORTH);
        spellSouth.add(spellOutput, BorderLayout.CENTER);
        spellPanel.add(spellSouth, BorderLayout.SOUTH);

        // Itinerary section
        JPanel tripPanel = new JPanel(new BorderLayout(5, 5));
        JTextArea tripOutput = new JTextArea();
        tripOutput.setEditable(false);
        JButton tripBtn = new JButton("Best 3-stop trip from Goa (Bitmask DP)");
        tripBtn.addActionListener(e -> {
            int[][] dist = {
                {0, 12, 8, 10, 6},
                {12, 0, 15, 14, 9},
                {8, 15, 0, 7, 11},
                {10, 14, 7, 0, 13},
                {6, 9, 11, 13, 0}
            };
            String[] names5 = {"Goa", "Paris", "Bali", "Kyoto", "Manali"};
            ItineraryPlanner.Result r = ItineraryPlanner.bestRoute(dist, 0, 3);
            StringBuilder sb = new StringBuilder("Cost: " + r.cost + "\nRoute: ");
            for (int idx : r.route) sb.append(names5[idx]).append(" -> ");
            sb.setLength(sb.length() - 4);
            tripOutput.setText(sb.toString());
        });
        tripPanel.add(tripBtn, BorderLayout.NORTH);
        tripPanel.add(new JScrollPane(tripOutput), BorderLayout.CENTER);

        panel.add(spellPanel);
        panel.add(tripPanel);
        return panel;
    }

    // ---------- M4 tab ----------
    private JPanel buildFlowTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea output = new JTextArea();
        output.setEditable(false);

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

        JButton flowBtn = new JButton("Compute max flow (Ford-Fulkerson / Edmonds-Karp / Dinic)");
        JButton cutBtn = new JButton("Find bottleneck (Min-Cut)");
        JButton matchBtn = new JButton("Assign travelers to hotels (Bipartite Matching)");

        flowBtn.addActionListener(e -> output.setText(
                "Ford-Fulkerson: " + MaxFlow.fordFulkerson(net, 0, 5) + "\n" +
                "Edmonds-Karp:  " + MaxFlow.edmondsKarp(net, 0, 5) + "\n" +
                "Dinic:         " + MaxFlow.dinic(net, 0, 5)));

        cutBtn.addActionListener(e -> {
            MaxFlow.CutResult cut = MaxFlow.minCut(net, 0, 5);
            output.setText("Min-cut value: " + cut.value + "\nBottleneck links: " + cut.edgesToString());
        });

        matchBtn.addActionListener(e -> {
            boolean[][] eligible = {
                { true,  true,  false },
                { true,  false, false },
                { false, true,  true  },
                { false, false, true  }
            };
            int[] result = BipartiteMatching.match(eligible, 3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < result.length; i++) {
                sb.append("Traveler ").append(i).append(" -> ")
                  .append(result[i] == -1 ? "no hotel" : "Hotel " + result[i]).append("\n");
            }
            output.setText(sb.toString());
        });

        JPanel btnCol = new JPanel(new GridLayout(3, 1, 5, 5));
        btnCol.add(flowBtn);
        btnCol.add(cutBtn);
        btnCol.add(matchBtn);

        panel.add(btnCol, BorderLayout.NORTH);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}