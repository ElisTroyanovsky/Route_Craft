package domain;

public class Location {
    private String id;
    private double x;
    private double y;

    public Location(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    // ИМЕННО ЭТОТ МЕТОД ИЩЕТ TRUCK
    public double distanceTo(Location other) {
        double xDist = Math.abs(this.x - other.x);
        double yDist = Math.abs(this.y - other.y);
        return Math.sqrt(xDist * xDist + yDist * yDist);
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }

    @Override
    public String toString() { return id; }
}