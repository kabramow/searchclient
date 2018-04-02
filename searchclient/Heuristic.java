package searchclient;

import java.util.*;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	//character is a goal represent by an lowercase letter, and the value is is a list of locations of that goal
	HashMap<Character, ArrayList<Point>> goalLocations;

	//a grid where the values for each part of the grid are also a grid and that grid maps the distance from the point
	// referenced by the higher grid
	/*        0 1 2
			0 X + +
			1 + + +
			2 + + +

			In the X grid the following grid is stored

			0 1 2
			1 2 3
			2 3 4
	 */
	ArrayList<ArrayList<int[][]>> pointDistances;
	public Heuristic(Node initialState, ArrayList<ArrayList<Character>> goals, ArrayList<ArrayList<Boolean>> walls) {
		// Here's a chance to pre-process the static parts of the level.

		//make hashmap of goals for easy look up of nearest goal
		goalLocations = new HashMap<>();

        pointDistances = new ArrayList<>();

        //finds the longest row length
        int longestRowLength = 0;
        for(int i = 0; i < goals.size(); i++){
            if(goals.get(i).size() > longestRowLength){
                longestRowLength = goals.get(i).size();
            }
        }

        //number of rows in goals
        int rowCount = goals.size();
        //technically redundant but easier to read later code
        int columnCount = longestRowLength;

        //a filler object if a point is a wall in the point distances array object
        int[][] wallFiller = new int[rowCount][columnCount];
        int WALL_INT_CONSTANT = 100000000;

        //populate wall filler
        for(int row = 0; row < rowCount; row++){
            for(int col = 0; col < columnCount; col++){
                wallFiller[row][col] = WALL_INT_CONSTANT;
            }
        }

		//loop through the goals nested array to find goal locations
		for(int row = 0; row < goals.size(); row++){
			for(int col = 0; col < goals.get(row).size(); col++){
				char currentChar = goals.get(row).get(col);


				//This process is finding all the locations of goals and adding to a hashmap thing
                //********//********//********//********//********//********//
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
                //********//********//********//********//********//********//

                //determine distance from one point to another for every point in the array
                //++//++//++//++//++//++//++//++//++//++//++//++//++//++//++//
                ArrayList<int[][]> currentRow = new ArrayList<>();
                pointDistances.add(currentRow);
                //if current value is a wall
                if(walls.get(row).get(col)){
                    currentRow.add(wallFiller);
                }
                //if it is not a wall then do BFS to find distance from current point to
                //all other non-wall points
                else{
                    //represents the grid that is the distance between current point and other points
                    int[][] subGrid = new int[rowCount][columnCount];

                    //keep track of points already visited
                    HashSet<Point> frontierSet = new HashSet<>();
                    //TODO refine visited to be more efficient?
                    HashSet<Point> visited = new HashSet<>();
                    Queue<Point> frontier = new LinkedList<>();

                    //if point is not 0,0 we want to back trace already performed calculations
                    if(row != 0 && col != 0){
                        for(int i = 0; i <= row; i++){
                            for(int j = 0; j < col; j++){
                                //if point is a wall add a filler max int
                                if(pointDistances.get(i).get(j) == wallFiller){
                                    subGrid[i][j] = WALL_INT_CONSTANT;
                                }
                                //otherwise see previously calculated values
                                else {
                                    subGrid[i][j] = distanceBetweenTwoPoints(i, j, row, col);
                                    visited.add(new Point(i,j));
                                }
                            }
                        }
                    }
                    //fill in the rest of the grid with BFS
                    Point firstPoint = new Point(row, col, -1);
                    frontier.add(firstPoint);
                    frontierSet.add(firstPoint);
                    while(!frontier.isEmpty()){
                        Point currentPoint = frontier.poll();
                        frontierSet.remove(currentPoint);
                        //update grid
                        int currentX = currentPoint.getX();
                        int currentY = currentPoint.getY();
                        //check if current point is a wall - if it is add wall filler value
                        if (walls.get(currentX).get(currentY)){
                            subGrid[currentX][currentY] = WALL_INT_CONSTANT;
                        }
                        //if it isn't expand upon it
                        else {
                            int currentDistance = currentPoint.getPreviousDistance() + 1;
                            subGrid[currentX][currentY] = currentDistance;
                            //add points around it to frontier if they aren't already visited or in frontier
                            //see if point above is anything
                            if (currentY > 0) {
                                Point abovePoint = new Point(currentX, currentY-1, currentDistance);
                                if(!visited.contains(abovePoint) || !frontierSet.contains(abovePoint)){
                                    frontier.add(abovePoint);
                                }
                            }
                            //see if point below is anything
                            if (currentY < rowCount-1) {
                                Point belowPoint = new Point(currentX, currentY+1, currentDistance);
                                if(!visited.contains(belowPoint) || !frontierSet.contains(belowPoint)){
                                    frontier.add(belowPoint);
                                }
                            }
                            //see if point to the left is anything
                            if (currentX > 0) {
                                Point leftPoint = new Point(currentX-1, currentY, currentDistance);
                                if(!visited.contains(leftPoint) || !frontierSet.contains(leftPoint)){
                                    frontier.add(leftPoint);
                                }
                            }
                            //see if point to the right is anything
                            if (currentX < columnCount-1) {
                                Point rightPoint = new Point(currentX+1, currentY, currentDistance);
                                if(!visited.contains(rightPoint) || !frontierSet.contains(rightPoint)){
                                    frontier.add(rightPoint);
                                }
                            }
                        }
                        visited.add(currentPoint);
                    }
                }
                //++//++//++//++//++//++//++//++//++//++//++//++//++//++//++//
			}
		}
		System.err.println(goalLocations.toString());

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

	public int distanceBetweenTwoPoints(int x1, int y1, int x2, int y2){
	    return pointDistances.get(x1).get(y1)[x2][y2];
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
		public AStar(Node initialState, ArrayList<ArrayList<Character>> goals, ArrayList<ArrayList<Boolean>> walls) {
			super(initialState, goals, walls);
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

		public WeightedAStar(Node initialState, ArrayList<ArrayList<Character>> goals, ArrayList<ArrayList<Boolean>> walls, int W) {
			super(initialState, goals, walls);
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
		public Greedy(Node initialState, ArrayList<ArrayList<Character>> goals, ArrayList<ArrayList<Boolean>> walls) {
			super(initialState, goals, walls);
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
