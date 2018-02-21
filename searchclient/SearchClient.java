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
	//public static int MAX_ROW = 70;
	//public static int MAX_COL = 70;
	//public boolean[][] walls;// = new boolean[MAX_ROW][MAX_COL];
	public char[][] goals; // = new char[MAX_ROW][MAX_COL];
	public ArrayList<ArrayList<Boolean>> walls = new ArrayList<ArrayList<Boolean>>();
//	public ArrayList<ArrayList<Character>> goals = new ArrayList<>();

	public SearchClient(BufferedReader serverMessages) throws Exception {
		// Read lines specifying colors
		String line = serverMessages.readLine();

		if (line.matches("^[a-z]+:\\s*[0-9A-Z](\\s*,\\s*[0-9A-Z])*\\s*$")) {
			System.err.println("Error, client does not support colors.");
			System.exit(1);
		}

		int row = 0;
		boolean agentFound = false;

		//find maxRow and maxCol
		int maxRow = 70;
		int maxCol = 70;




		//walls = new boolean[maxRow][maxCol];
		goals = new char[maxRow][maxCol];

		this.initialState = new Node(null, maxRow, maxCol);

		System.err.println("HERE MADDIEHIHIHIHIHIHIHIHI");

		while (!line.equals("")) {
			System.err.println("HERE KARINAHIHIHIHIHIHIHIHI");
			walls.add(new ArrayList<Boolean>());
			for (int col = 0; col < line.length(); col++) {
				char chr = line.charAt(col);

				if (chr == '+') { // Wall.
					//changed from this.intialstate.walls because walls is no longer an attribute of initalstate
					//this.walls[row][col] = true;
					//changed walls to nested arraylist -access row and then add boolean to end of list
					//need to initialize array list for row
					/*if(walls.get(row) == null){
						System.err.println("here");
						walls.set(row,new ArrayList<Boolean>());
					}*/
					walls.get(row).add(true);

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
						} else if ('A' <= chr && chr <= 'Z') { // Box.
							this.initialState.boxes[row][col] = chr;
						} else if ('a' <= chr && chr <= 'z') { // Goal.
							//changed this.initialState.goals to this.goals because goals is no longer an attribute of node
							this.goals[row][col] = chr;
						} else if (chr == ' ') {
							// Free space.
						} else {
							System.err.println("Error, read invalid level character: " + (int) chr);
							System.exit(1);
						}
				}
			}
			System.err.println("HERE got through conditions");
			line = serverMessages.readLine();
			System.err.println("read another line");
			row++;

			System.err.println("new row");
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
