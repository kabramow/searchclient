package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;

import searchclient.Memory;
import searchclient.Strategy.*;
import searchclient.Heuristic.*;

public class SearchClient {
    public Node initialState;

    /* Refactored walls and goals to SearchClient, instead of Node so that 
     * they would not be regenerated for every node created <saves memory>
     *
     * We also changed walls and goals to be represented as 2d-arrays instead 
     * of the nested ArrayList structure we had in part 1 of our assignment. 
     * This increased efficiency and was per your comments on part 1. 
     */
    public boolean[][] walls;
    public char[][] goals;

    public int maxRow = 0;
    public int maxCol = 0;

    public SearchClient(BufferedReader serverMessages) throws Exception {
        // Read lines specifying colors
        String line = serverMessages.readLine();

        if (line.matches("^[a-z]+:\\s*[0-9A-Z](\\s*,\\s*[0-9A-Z])*\\s*$")) {
            System.err.println("Error, client does not support colors.");
            System.exit(1);
        }

        //Removes trailing spaces
        line = line.replaceAll("\\s+$", "");

        boolean agentFound = false;

        ArrayList<String> readLines = new ArrayList<>();
        readLines.add(line);

        while (!line.equals("")) {
            line = serverMessages.readLine();
            line = line.replaceAll("\\s+$", "");
            maxRow++;
            readLines.add(line);
            int lineLen = line.length();
            if(lineLen > maxCol){
                maxCol = lineLen;
            }
        }

        // Instantiate walls and goals representations.
        walls = new boolean[maxRow][maxCol];
        goals = new char[maxRow][maxCol];
        this.initialState = new Node(null, maxRow, maxCol);

        // Look through lines read again
        // So, here, we stored the already read buffer lines in a list so
        //  we could access/read them again. 
        for (int row = 0; row < readLines.size(); row++) {
            String currentLine = readLines.get(row);
            this.initialState.boxes.add(new ArrayList<>());
            for (int col = 0; col < currentLine.length(); col++) {
                char chr = currentLine.charAt(col);

                if (chr == '+') { // Wall.
                    /* Changed from this.intialstate.walls because walls is no longer an attribute of initalstate
                     * Changed walls to nested arraylist
                     * --> Because they're arraylists, instead of a 2d array, we just add new elements.
                     */
                    walls[row][col] = true;
                    this.initialState.boxes.get(row).add('\u0000'); // Use null character value, necessary because of ArrayList change.
                    // We kept this convention because it still worked, but I realize it's not necessary.
                }
                else {
                    if ('0' <= chr && chr <= '9') { // Agent.
                        if (agentFound) {
                            System.err.println("Error, not a single agent level");
                            System.exit(1);
                        }
                        agentFound = true;
                        this.initialState.agentRow = row;
                        this.initialState.agentCol = col;
                        goals[row][col] = '\u0000';
                        initialState.boxes.get(row).add('\u0000');
                    } else if ('A' <= chr && chr <= 'Z') { // Box.
                        this.initialState.boxes.get(row).add(chr);
                    } else if ('a' <= chr && chr <= 'z') { // Goal.
                        goals[row][col] = chr;
                        this.initialState.boxes.get(row).add('\u0000');
                    } else if (chr == ' ') {
                        // Free space.
                        this.initialState.boxes.get(row).add('\u0000');
                    } else {
                        System.err.println("Error, read invalid level character: " + (int) chr);
                        System.exit(1);
                    }
                }
            }
        }
    }

    public LinkedList<Node> Search(Strategy strategy) throws IOException {
        System.err.format("Search starting with strategy %s.\n", strategy.toString());
        strategy.addToFrontier(this.initialState);

        int iterations = 0;
        while (true) {
            if (iterations == 1000) {
                System.err.println(strategy.searchStatus());
                iterations = 0;
            }

            if (strategy.frontierIsEmpty()) {
                return null;
            }

            Node leafNode = strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState(goals)) {
                return leafNode.extractPlan();
            }

            strategy.addToExplored(leafNode);
            //getExpandedNodes now takes walls as an argument
            for (Node n : leafNode.getExpandedNodes(walls)) { // The list of expanded nodes is shuffled randomly; see Node.java.
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }
            iterations++;
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        // Use stderr to print to console
        System.err.println("SearchClient initializing. I am sending this using the error output stream.");

        // Read level and create the initial state of the problem
        SearchClient client = new SearchClient(serverMessages);

        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                    strategy = new StrategyBFS();
                    break;
                case "-dfs":
                    strategy = new StrategyDFS();
                    break;
                case "-astar":
                    strategy = new StrategyBestFirst(new AStar(client.initialState, client.goals, client.walls, client.maxRow, client.maxCol ));
                    break;
                case "-wastar":
                    // You're welcome to test WA* out with different values, but for the report you must at least indicate benchmarks for W = 5.
                    strategy = new StrategyBestFirst(new WeightedAStar(client.initialState, client.goals, client.walls, 5, client.maxRow, client.maxCol));
                    break;
                case "-greedy":
                    strategy = new StrategyBestFirst(new Greedy(client.initialState, client.goals, client.walls, client.maxRow, client.maxCol));
                    break;
                default:
                    strategy = new StrategyBFS();
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
            strategy = new StrategyBFS();
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }

        LinkedList<Node> solution;
        try {
            solution = client.Search(strategy);
        } catch (OutOfMemoryError ex) {
            System.err.println("Maximum memory usage exceeded.");
            solution = null;
        }

        if (solution == null) {
            System.err.println(strategy.searchStatus());
            System.err.println("Unable to solve level.");
            System.exit(0);
        } else {
            System.err.println("\nSummary for " + strategy.toString());
            System.err.println("Found solution of length " + solution.size());
            System.err.println(strategy.searchStatus());

            for (Node n : solution) {
                String act = n.action.toString();
                System.out.println(act);
                String response = serverMessages.readLine();
                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                    System.err.format("%s was attempted in \n%s\n", act, n.toString());
                    break;
                }
            }
        }
    }
}
