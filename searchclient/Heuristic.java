package searchclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	//character is a goal represent by an lowercase letter, and the value is is a list of locations of that goal
	HashMap<Character, ArrayList<Point>> goalLocations;
	public Heuristic(Node initialState, ArrayList<ArrayList<Character>> goals) {
		// Here's a chance to pre-process the static parts of the level.

		//make hashmap of goals for easy look up of nearest goal
		goalLocations = new HashMap<>();
		//loop through the goals nested array to find goal locations
		for(int row = 0; row < goals.size(); row++){
			for(int col = 0; col < goals.get(row).size(); col++){
				char currentChar = goals.get(row).get(col);
				if(currentChar != '\u0000'){
					//set array with location of point
					Point currentLocation = new Point(row, col);
					ArrayList<Point> currentLocationsOfGoal;
					//if there are already locations of char listed, then find them
					if(goalLocations.containsKey(currentChar)){
						currentLocationsOfGoal = goalLocations.get(currentChar);
					}
					//if there are not locations of char found initialize a new array containing them
					else{
						currentLocationsOfGoal = new ArrayList<>();
					}
					//then add new location found to list of locations
					currentLocationsOfGoal.add(currentLocation);
					//and add updated locations to hashmap
					goalLocations.put(currentChar, currentLocationsOfGoal);
				}
			}
		}
		System.err.println(goalLocations.toString());
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
						//find goal locations
						ArrayList<Point> currentGoalLocations = goalLocations.get(currentChar);
						int closestDistance = 100000000;
						int closestX = -10;
						int closestY = -10;
						for(Point location : currentGoalLocations){
							int goalRow = location.getX();
							int goalCol = location.getY();
							//find manhattan distance
							int distance = manhattanDistance(row, col, goalRow, goalCol);
							if(distance < closestDistance){
								closestDistance = distance;
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
	}


	//finds manhattan distance of two points aka x distance away + y distance away
	public int manhattanDistance(int x1, int y1, int x2, int y2){
		return (x1 - x2) + (y2 - y1);
	}

	public String debugMapperToString(Object[] mapper){
		return "For char " + mapper[0] + " in (" + mapper[1] + ", " + mapper[2]
				+ ") the closest goal is (" + mapper[3] + ", " + mapper[4] + ") with a distance of " + mapper[5];
	}

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
