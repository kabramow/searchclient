package searchclient;

import java.util.*;
import searchclient.NotImplementedException;

/**
 * We implemented both the Manhattan Distance and "Real" Distance for our heuristic.
 * The "Real" Distance is the shortest distance between two given points, taking into account the walls of a
 *  level. We guarantee finding the shortest distance by using BFS to calculate the "Real" Distance. 
 *
 * However, we ended up USING only the "Real" Distance, since it makes a better metric for the best-first heuristic 
 *  than the simple Manhattan distance. In a level with many walls between an a goal and a compatible 'key,' the 
 *  Manhattan distance could actually be detrimental to the success of the heuristic. 
 *
 * We also created a Point class which simply makes dealing with X,Y coordinates simpler and more readable.
 */
public abstract class Heuristic implements Comparator<Node> {
    // Note: character is a goal represented by an lowercase letter, and the value is is a list of locations of that goal
    // This stores a given goal (character) and all of the points at which that goal is found. 
    HashMap<Character, ArrayList<Point>> goalLocations;

    /** pointDistances is a representation of how we are going to store the distances between points. We will have a grid that is the 
     * same size as the level. Then for each coordinate on the grid, there will be another grid that is the same size as the
     * level grid.*/
    // Original explanation: a grid where the values for each part of the grid are also a grid and that grid maps the distance 
    // from the point referenced by the higher grid

    /*        0 1 2
              0 X + +
              1 + + +
              2 + + +

              In the X grid the following grid is stored; the value at each coordinate gives the distance between the '0-point'
              and the given coordinate.

              0 1 2
              1 2 3
              2 3 4
              */
    ArrayList<ArrayList<int[][]>> pointDistances;

    /**
     * Constructor for Heuristic
     */
    public Heuristic(Node initialState, ArrayList<ArrayList<Character>> goals, ArrayList<ArrayList<Boolean>> walls) {
        // Here's a chance to pre-process the static parts of the level.

        // Here we make hashmap of goals for efficient look up of nearest goal
        goalLocations = new HashMap<>();

        // Instantiate pointDistances since it was only declared before the invocation of the Heuristic constructor. 
        pointDistances = new ArrayList<>();

        // Here we find the longest row length of the level, through the goals-representation.
        int longestRowLength = 0;
        for(int i = 0; i < goals.size(); i++){
            if(goals.get(i).size() > longestRowLength){
                longestRowLength = goals.get(i).size();
            }
        }

        // Number of rows in goals
        int rowCount = goals.size();
        // Technically redundant but easier to read later code
        int columnCount = longestRowLength;

        /**
         * When we store our point distances, we need to take walls into account. So, we will store a
         *  'dummy' grid to as a wall to indicate that that particular coordinate is not to be used in 
         *  calculating the "Real" Distance. 
         */
        // The 'dummy' wall, AKA a filler object if a point is a wall in the point distances array object
        int[][] wallFiller = new int[rowCount][columnCount];
        int WALL_INT_CONSTANT = 100000000;

        // Populate wall filler
        for(int row = 0; row < rowCount; row++){
            for(int col = 0; col < columnCount; col++){
                wallFiller[row][col] = WALL_INT_CONSTANT;
            }
        }

        // Loop through the nested goals array to find and store goal locations
        for(int row = 0; row < goals.size(); row++){
            ArrayList<int[][]> currentRow = new ArrayList<>();
            pointDistances.add(currentRow);
            for(int col = 0; col < goals.get(row).size(); col++){
                char currentChar = goals.get(row).get(col);

                // Determine the "Real" distance from one point to another for every point in the array
                /***********************************************************************************/

                // If current value is a wall, then add the wall filler to pointDistances. 
                if(walls.get(row).get(col)){
                    currentRow.add(wallFiller);
                }

                // If it is not a wall then do BFS to find distance from current point to
                // all other non-wall points and store the results. 
                else{
                    // Represents the grid that is the distance between current point and other points
                    int[][] subGrid = new int[rowCount][columnCount];

                    currentRow.add(subGrid);

                    /** Start BFS from the current coordinate.
                     * Note that the first part of this code-chunk is not performing BFS.
                     */
                    // 1. First, we do a simple calculation from the 'beginning' of the grid, (0,0)
                    Queue<PointNode> frontier = new LinkedList<>();
                    HashSet<Point> frontierSet = new HashSet<>();
                    // keep track of points already visited
                    HashSet<Point> visited = new HashSet<>();

                    // if point is not 0,0 we want to back trace already performed calculations
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

                    // 2. Now, fill in the rest of the grid with BFS. 
                    frontier.add(new PointNode(row, col, -1));
                    frontierSet.add(new Point(row, col));
                    while(!frontier.isEmpty()){
                        PointNode currentPointNode = frontier.poll();
                        int currentX = currentPointNode.getX();
                        int currentY = currentPointNode.getY();
                        Point currentPoint = new Point(currentX, currentY);
                        frontierSet.remove(currentPoint);

                        // update grid
                        // check if current point is a wall - if it is add wall filler value
                        if (walls.get(currentX).get(currentY)){
                            subGrid[currentX][currentY] = WALL_INT_CONSTANT;
                        }
                        // if the current point isn't a wall, continue with BFS algorithm and follow those 'children'
                        //  coordinates.
                        else {
                            int currentDistance = currentPointNode.getPreviousDistance() + 1;
                            subGrid[currentX][currentY] = currentDistance;
                            /* Add points around it to frontier if they aren't already visited or in frontier.
                             * This will be in a 4-unit 'cross' around the current point. The coordinates directly
                             * above, below, to the left, and to the right will be checked. */
                            // See if point above is anything
                            if (currentY > 0) {
                                Point abovePoint = new Point(currentX, currentY-1);
                                if(!visited.contains(abovePoint) && !frontierSet.contains(abovePoint)){
                                    frontier.add(new PointNode(currentX, currentY-1, currentDistance));
                                    frontierSet.add(abovePoint);
                                }
                            }
                            // See if point below is anything
                            if (currentY < walls.size()-2) {
                                Point belowPoint = new Point(currentX, currentY+1);
                                if(!visited.contains(belowPoint) && !frontierSet.contains(belowPoint)){
                                    frontier.add(new PointNode(currentX, currentY+1, currentDistance));
                                    frontierSet.add(belowPoint);
                                }
                            }
                            // See if point to the left is anything
                            if (currentX > 0) {
                                Point leftPoint = new Point(currentX-1, currentY);
                                if(!visited.contains(leftPoint) && !frontierSet.contains(leftPoint)){
                                    frontier.add(new PointNode(currentX-1, currentY, currentDistance));
                                    frontierSet.add(leftPoint);
                                }
                            }
                            //see if point to the right is anything
                            if (currentX < walls.get(currentX).size()-2) {
                                Point rightPoint = new Point(currentX+1, currentY);
                                if(!visited.contains(rightPoint) && !frontierSet.contains(rightPoint)){
                                    frontier.add(new PointNode(currentX+1, currentY, currentDistance));
                                    frontierSet.add(rightPoint);
                                }
                            }
                        }
                        // Mark the current point as visited
                        visited.add(currentPoint);
                    }
                }

                /***********************************************************************************/
                //The following code-chunk is finding all the locations of goals and adding to 
                // the hashmap tracking goal locations.
                /***********************************************************************************/
                if(currentChar != '\u0000'){
                    // Set array with location of point
                    Point currentLocation = new Point(row, col);
                    ArrayList<Point> currentLocationsOfGoal;
                    // If there are already locations of char listed, then find them
                    if(goalLocations.containsKey(currentChar)){
                        currentLocationsOfGoal = goalLocations.get(currentChar);
                    }
                    // If there are not locations of char found initialize a new array containing them
                    else{
                        currentLocationsOfGoal = new ArrayList<>();
                    }
                    // Then add new location found to list of locations
                    currentLocationsOfGoal.add(currentLocation);
                    // And add updated locations to hashmap
                    goalLocations.put(currentChar, currentLocationsOfGoal);
                }
            }
        }
    }


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
                            int distance = distanceBetweenTwoPoints(row, col, goalRow, goalCol);
                            //= manhattanDistance(row, col, goalRow, goalCol);
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

    // Finds manhattan distance of two points aka x distance away + y distance away
    public int manhattanDistance(int x1, int y1, int x2, int y2){
        return (x1 - x2) + (y2 - y1);
    }

    // Find the shortest "Real" Distance between a point and and another point. 
    public int distanceBetweenTwoPoints(int x1, int y1, int x2, int y2){
        return pointDistances.get(x1).get(y1)[x2][y2];
    }

    // Simply a debugMapper. 
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
