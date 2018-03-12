package searchclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	HashMap<Character, ArrayList<int[]>> goalLocations;
	public Heuristic(Node initialState, ArrayList<ArrayList<Character>> goals) {
		// Here's a chance to pre-process the static parts of the level.
		//TODO make hashmap of goals
		/*HashMap<Character, ArrayList<int[]>>*/ goalLocations = new HashMap<>();
		for(int row = 0; row < goals.size(); row++){
			for(int col = 0; col < goals.get(row).size(); col++){
				char currentChar = goals.get(row).get(col);
				if(currentChar != '\u0000'){
					//set array with location of point
					int[] currentLocation = {row, col};
					ArrayList<int[]> locationsOfChar;
					//if there are already locations of char listed, then find them
					if(goalLocations.containsKey(currentChar)){
						 locationsOfChar = goalLocations.get(currentChar);
					}
					//if there are not locations of char found initialize a new array containing them
					else{
						locationsOfChar = new ArrayList<>();
					}
					//then add new location found to list of locations
					locationsOfChar.add(currentLocation);
					//and add updated locations to hashmap
					goalLocations.put(currentChar, locationsOfChar);
				}
			}
		}
		//TODO determine distance from one point to another for every point in the array
	}

	//Upper A is the box
	//lower a is the goal
	public int h(Node n) {
		//track goal node and closest row
		ArrayList<Object[]> debuggingMapper = new ArrayList<>();
		int returnSum = 0;
		ArrayList<ArrayList<Character>> boxes = n.boxes;
		for(int row = 0; row < boxes.size(); row++) {
			for (int col = 0; col < boxes.get(row).size(); col++) {

				char currentChar = Character.toLowerCase(boxes.get(row).get(col));
				//if current value is a box
				if(currentChar != '\u0000'){
					if(goalLocations.containsKey(currentChar)){
						//find char locations
						ArrayList<int[]> currentGoalLocations = goalLocations.get(currentChar);
						int closestDistance = 100000000;
						int closestX = -10;
						int closestY = -10;
						for(int[] location : currentGoalLocations){
							int goalRow = location[0];
							int goalCol = location[1];
							int x1Minusx2 = goalRow - row;
							int y1Minusy2 = goalCol - col;
							//find manhattan distance away
							int manhattanDistance = x1Minusx2 + y1Minusy2;
							if(manhattanDistance < closestDistance){
								closestDistance = manhattanDistance;
								closestX = goalRow;
								closestY = goalCol;
							}
						}
						returnSum += closestDistance;
						Object[] debugger = {currentChar, row, col, closestX, closestY, closestDistance};
						debuggingMapper.add(debugger);
					}
				}
			}
		}
		return returnSum;
		//make hashmap of goal nodes
	}

	public String debugMapperToString(Object[] mapper){
		return "For char " + mapper[0] + " in (" + mapper[1] + ", " + mapper[2]
				+ ") the closest goal is (" + mapper[3] + ", " + mapper[4] + ") with a distance of " + mapper[5];
	}

	///manhattan distance that favors closest
	///manhattan distance that favors farthest
	///manhattan distance that only claims one per object
	///manhattan favoring first
	//map of nodes? map of goal nodes?

	public abstract int f(Node n);

	@Override
	public int compare(Node n1, Node n2) {
		return this.f(n1) - this.f(n2);
	}

	public static class AStar extends Heuristic {
		public AStar(Node initialState, ArrayList<ArrayList<Character>> goals) {
			super(initialState, goals);
		}

		@Override
		public int f(Node n) {
			return n.g() + this.h(n);
		}

		@Override
		public String toString() {
			return "A* evaluation";
		}
	}

	public static class WeightedAStar extends Heuristic {
		private int W;

		public WeightedAStar(Node initialState, ArrayList<ArrayList<Character>> goals,  int W) {
			super(initialState, goals);
			this.W = W;
		}

		@Override
		public int f(Node n) {
			return n.g() + this.W * this.h(n);
		}

		@Override
		public String toString() {
			return String.format("WA*(%d) evaluation", this.W);
		}
	}

	public static class Greedy extends Heuristic {
		public Greedy(Node initialState, ArrayList<ArrayList<Character>> goals) {
			super(initialState, goals);
		}

		@Override
		public int f(Node n) {
			return this.h(n);
		}

		@Override
		public String toString() {
			return "Greedy evaluation";
		}
	}
}
