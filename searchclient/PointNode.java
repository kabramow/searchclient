package searchclient;

/**
 * The PointNode class serves to update the distances when calculating the distances in Heurstic.java.
 * We need to be able to pass a previous distance between two points to, for instance, find that distance
 * + one more space (in multiple directions).
 *
 * It is essentially just a point, but one that derives its previousDistance value from its parent, or a point
 * explored previously in BFS/heuristic.
 */
public class PointNode {
    public int x;
    public int y;
    public int previousDistance;

    public PointNode(int x, int y, int previousDistance) {
        this.x = x;
        this.y = y;
        this.previousDistance = previousDistance;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getPreviousDistance() {
        return previousDistance;
    }

    public void setPreviousDistance(int y) {
        this.previousDistance = previousDistance;
    }

    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", previousDistance=" + previousDistance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointNode pointNode = (PointNode) o;

        if (x != pointNode.x) return false;
        if (y != pointNode.y) return false;
        return previousDistance == pointNode.previousDistance;
    }

}
