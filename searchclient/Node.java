package searchclient;

import java.lang.reflect.Array;
import java.util.*;

import searchclient.Command.Type;

/**
 * A Node class to make dealing with coordinates easier.
 */
public class Node {
	private static final Random RND = new Random(1);

	public int agentRow;
	public int agentCol;

	public int maxRow;
	public int maxCol;

	// Arrays are indexed from the top-left of the level, with first index being row and second being column.
	// Row 0: (0,0) (0,1) (0,2) (0,3) ...
	// Row 1: (1,0) (1,1) (1,2) (1,3) ...
	// Row 2: (2,0) (2,1) (2,2) (2,3) ...
	// ...
	// (Start in the top left corner, first go down, then go right)
	// E.g. this.walls[2] is an array of booleans having size MAX_COL.
	// this.walls[row][col] is true if there's a wall at (row, col)
	//

	//got rid of goals and walls as attributes of node and moved them to attributes of search client
	public ArrayList<ArrayList<Character>> boxes;
	//public char[][] boxes;

	public Node parent;
	public Command action;

	private int g;
	
	private int _hash = 0;

	public Node(Node parent, int maxRow, int maxCol) {
		this.parent = parent;
		this.maxRow = maxRow;
		this.maxCol = maxCol;
		//boxes = new char[maxRow][maxCol];
		boxes = new ArrayList<>();
		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
		}
	}

	public int g() {
		return this.g;
	}

	public boolean isInitialState() {
		return this.parent == null;
	}

	//added goals as an argument as it is no longer an attribute of node
	public boolean isGoalState(char[][] goals) {
		for (int row = 1; row < maxRow - 1; row++) {
			for (int col = 1; col < maxCol - 1; col++) {
				char g = goals[row][col];
				char b = Character.toLowerCase(boxes.get(row).get(col));
				//char b = Character.toLowerCase(boxes[row][col]);
				if (g > 0 && b != g) {
					return false;
				}
			}
		}
		return true;
	}

        /* Added walls as an argument to 'getExpandedNodes' since walls is no longer an
         * attribute of the Node class and is required for the method.
         */ 
	public ArrayList<Node> getExpandedNodes(boolean[][] walls) {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
		for (Command c : Command.EVERY) {
			// Determine applicability of action
			int newAgentRow = this.agentRow + Command.dirToRowChange(c.dir1);
			int newAgentCol = this.agentCol + Command.dirToColChange(c.dir1);

			if (c.actionType == Type.Move) {
				// Check if there's a wall or box on the cell to which the agent is moving
				//add walls as an argument bc walls is no longer an attribute of node
				if (this.cellIsFree(newAgentRow, newAgentCol, walls)) {
					Node n = this.ChildNode();
					n.action = c;
					n.agentRow = newAgentRow;
					n.agentCol = newAgentCol;
					expandedNodes.add(n);
				}
			} else if (c.actionType == Type.Push) {
				// Make sure that there's actually a box to move
				if (this.boxAt(newAgentRow, newAgentCol)) {
					int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
					int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
					// .. and that new cell of box is free
					//added walls to cellIsFree in line with changes made to the function
					if (this.cellIsFree(newBoxRow, newBoxCol, walls)) {
						Node n = this.ChildNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;

                                                // Value to set 
						//char newColVal = this.boxes[newAgentRow][newAgentCol];
						char newColVal = this.boxes.get(newAgentRow).get(newAgentCol);
						n.boxes.get(newBoxRow).set(newBoxCol, newColVal);
						//n.boxes[newBoxRow][newBoxCol] = newColVal;
						n.boxes.get(newAgentRow).set(newAgentCol,'\u0000');
						//n.boxes[newAgentRow][newAgentCol] = '\u0000';
						expandedNodes.add(n);
					}
				}
			} else if (c.actionType == Type.Pull) {
				// Cell is free where agent is going
				//added walss to cellIsFree in line with changes made to the function
				if (this.cellIsFree(newAgentRow, newAgentCol, walls)) {
					int boxRow = this.agentRow + Command.dirToRowChange(c.dir2);
					int boxCol = this.agentCol + Command.dirToColChange(c.dir2);
					// .. and there's a box in "dir2" of the agent
					if (this.boxAt(boxRow, boxCol)) {
						Node n = this.ChildNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;
                                                // Do what the original did, but using our ArrayList structure.
						n.boxes.get(this.agentRow).set(this.agentCol, this.boxes.get(boxRow).get(boxCol));
						//n.boxes[this.agentRow][this.agentCol] = this.boxes[boxRow][boxCol];
						n.boxes.get(boxRow).set(boxCol,'\u0000');
						//n.boxes[boxRow][boxCol] = '\u0000';
						expandedNodes.add(n);
					}
				}
			}
		}
		Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}

	// Added walls as argument because walls is no longer an attribute of node
	private boolean cellIsFree(int row, int col, boolean[][] walls) {
		return !walls[row][col] && this.boxes.get(row).get(col) == 0;
		//return !walls[row][col] && this.boxes[row][col] == 0;
	}

	private boolean boxAt(int row, int col) {
		return this.boxes.get(row).get(col) > 0;
		//return this.boxes[row][col] > 0;
	}

	private Node ChildNode() {
		Node copy = new Node(this, maxRow, maxCol);
		copy.boxes = new ArrayList<>();
		for(int row = 0; row < maxRow; row++){
			copy.boxes.add(new ArrayList<>());
			for (int col = 0; col < maxCol; col++){
				copy.boxes.get(row).add(this.boxes.get(row).get(col));
				//copy.boxes[row][col] =  this.boxes[row][col];
 			}
		}

		return copy;
	}

	public LinkedList<Node> extractPlan() {
		LinkedList<Node> plan = new LinkedList<Node>();
		Node n = this;
		while (!n.isInitialState()) {
			plan.addFirst(n);
			n = n.parent;
		}
		return plan;
	}

	@Override
	public int hashCode() {
		if (this._hash == 0) {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.agentCol;
			result = prime * result + this.agentRow;
			result = prime * result + this.boxes.hashCode(); // get hash code for ArrayList boxes. 
			this._hash = result;
		}
		return this._hash;
	}

	@Override
	// Removed walls and goals as they are no longer attributes of node
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (this.agentRow != other.agentRow || this.agentCol != other.agentCol)
			return false;
		if (!boxesEquals(this.boxes, other.boxes))
			return false;
		return true;
	}

	public boolean boxesEquals(ArrayList<ArrayList<Character>> thisBoxes, ArrayList<ArrayList<Character>> otherBoxes){
	//public boolean boxesEquals(char[][] thisBoxes, char[][]  otherBoxes){
		for (int i = 0; i < maxRow; i++) {
			for (int j = 0; j < maxCol; j++) {
				if(thisBoxes.get(i).get(j) != otherBoxes.get(i).get(j)) {
				//if (thisBoxes[i][j] != otherBoxes[i][j]) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "Node{" +
				"agentRow=" + agentRow +
				", agentCol=" + agentCol +
				", maxRow=" + maxRow +
				", maxCol=" + maxCol +
				//", boxes=" + Arrays.toString(boxes) +
				", parent=" + parent +
				", action=" + action +
				", g=" + g +
				'}';
	}
}
