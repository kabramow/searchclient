package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import searchclient.Command.Type;

public class Node {
	private static final Random RND = new Random(1);

	/*public static int MAX_ROW = 70;
	public static int MAX_COL = 70;*/

	public int agentRow;
	public int agentCol;

	//added these as local variables to compensate for lack
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
	public char[][] boxes; //= new char[MAX_ROW][MAX_COL];

	public Node parent;
	public Command action;

	private int g;
	
	private int _hash = 0;

	public Node(Node parent, int maxRow, int maxCol) {
		this.parent = parent;
		boxes = new char[maxRow][maxCol];
		this.maxRow = maxRow;
		this.maxCol = maxCol;
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
				char b = Character.toLowerCase(boxes[row][col]);
				if (g > 0 && b != g) {
					return false;
				}
			}
		}
		return true;
	}

	//add walls as an argument bc walls is no longer an attribute of node and it is
	//required for
	public ArrayList<Node> getExpandedNodes(ArrayList<ArrayList<Boolean>> walls) {
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
						n.boxes[newBoxRow][newBoxCol] = this.boxes[newAgentRow][newAgentCol];
						n.boxes[newAgentRow][newAgentCol] = 0;
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
						n.boxes[this.agentRow][this.agentCol] = this.boxes[boxRow][boxCol];
						n.boxes[boxRow][boxCol] = 0;
						expandedNodes.add(n);
					}
				}
			}
		}
		Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}

	//added walls as argument because walls is no longer an attribute of node
	private boolean cellIsFree(int row, int col, ArrayList<ArrayList<Boolean>> walls) {
		return !walls.get(row).get(col) && this.boxes[row][col] == 0;
	}

	private boolean boxAt(int row, int col) {
		return this.boxes[row][col] > 0;
	}

	private Node ChildNode() {
		Node copy = new Node(this, maxRow, maxCol);
		for (int row = 0; row < maxRow; row++) {
			System.arraycopy(this.boxes[row], 0, copy.boxes[row], 0, maxCol);
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
			result = prime * result + Arrays.deepHashCode(this.boxes);
			this._hash = result;
		}
		return this._hash;
	}

	@Override
	//removed walls and goals as they are no longer attributes of node
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
		if (!Arrays.deepEquals(this.boxes, other.boxes))
			return false;
		return true;
	}

	//since toString was already being weird for a toString (ie not following normal protocol)
	// we got rid of the override and pass walls and goals as arguments so it prints nicely
	public String prettyPrint(boolean[][] walls, char[][] goals, int maxRow, int maxCol) {
		StringBuilder s = new StringBuilder();
		for (int row = 0; row < maxRow; row++) {
			if (!walls[row][0]) {
				break;
			}
			for (int col = 0; col < maxCol; col++) {
				if (this.boxes[row][col] > 0) {
					s.append(this.boxes[row][col]);
				} else if (goals[row][col] > 0) {
					s.append(goals[row][col]);
				} else if (walls[row][col]) {
					s.append("+");
				} else if (row == this.agentRow && col == this.agentCol) {
					s.append("0");
				} else {
					s.append(" ");
				}
			}
			s.append("\n");
		}
		return s.toString();
	}

}