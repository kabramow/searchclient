package searchclient;

public class Point {
    public int x;
    public int y;
    public int previousDistance;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(int x, int y, int previousDistance) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (x != point.x) return false;
        return y == point.y;
    }

}
