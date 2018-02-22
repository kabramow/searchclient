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
         */
        public ArrayList<ArrayList<Boolean>> walls = new ArrayList<>();
	public ArrayList<ArrayList<Character>> goals = new ArrayList<>();

	public SearchClient(BufferedReader serverMessages) throws Exception {
		// Read lines specifying colors
		String line = serverMessages.readLine();

		if (line.matches("^[a-z]+:\\s*[0-9A-Z](\\s*,\\s*[0-9A-Z])*\\s*$")) {
			System.err.println("Error, client does not support colors.");
			System.exit(1);
		}

		int row = 0;
		boolean agentFound = false;

		// find maxRow and maxCol
		int maxRow = 70;
		int maxCol = 70;


		this.initialState = new Node(null, maxRow, maxCol);

		while (!line.equals("")) {
			walls.add(new ArrayList<Boolean>());
			goals.add(new ArrayList<Character>());
			this.initialState.boxes.add(new ArrayList<>());
			for (int col = 0; col < line.length(); col++) {
				char chr = line.charAt(col);

				if (chr == '+') { // Wall.
					/* Changed from this.intialstate.walls because walls is no longer an attribute of initalstate
                                         * Changed walls to nested arraylist 
                                         * --> Because they're arraylists, instead of a 2d array, we just add new elements.
                                         */
					walls.get(row).add(true); 
					goals.get(row).add('\u0000'); // Use null character value, necessary because of ArrayList change.
					this.initialState.boxes.get(row).add('\u0000'); // Use null character value, necessary because of ArrayList change.
				}
				else {
					walls.get(row).add(false);

					if ('0' <= chr && chr <= '9') { // Agent.
						if (agentFound) {
							System.err.println("Error, not a single agent level");
							System.exit(1);
						}
						agentFound = true;
						this.initialState.agentRow = row;
						this.initialState.agentCol = col;
						goals.get(row).add('\u0000');
						this.initialState.boxes.get(row).add('\u0000');
					} else if ('A' <= chr && chr <= 'Z') { // Box.
						this.initialState.boxes.get(row).add(chr);
						goals.get(row).add('\u0000');
					} else if ('a' <= chr && chr <= 'z') { // Goal.
						//changed this.initialState.goals to this.goals because goals is no longer an attribute of node
						goals.get(row).add(chr);
						this.initialState.boxes.get(row).add('\u0000');
					} else if (chr == ' ') {
						// Free space.
						goals.get(row).add('\u0000');
						this.initialState.boxes.get(row).add('\u0000');
					} else {
						System.err.println("Error, read invalid level character: " + (int) chr);
						System.exit(1);
					}
				}
			}
			line = serverMessages.readLine();
			row++;
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
                    strategy = new StrategyBestFirst(new AStar(client.initialState));
                    break;
                case "-wastar":
                    // You're welcome to test WA* out with different values, but for the report you must at least indicate benchmarks for W = 5.
                    strategy = new StrategyBestFirst(new WeightedAStar(client.initialState, 5));
                    break;
                case "-greedy":
                    strategy = new StrategyBestFirst(new Greedy(client.initialState));
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
