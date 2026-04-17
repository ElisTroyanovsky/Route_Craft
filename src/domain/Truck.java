package domain;

import java.util.ArrayList;
import java.util.List;

public class Truck {
    private int id;
    private Location depot; // Start and end point (Hub)
    private List<Location> route; // Intermediate stops only (without Hub)

    public Truck(int id, Location depot) {
        this.id = id;
        this.depot = depot;
        this.route = new ArrayList<>();
    }

    // Adding a single point to the end of the route
    public void addStop(Location location) {
        this.route.add(location);
    }

    // Setting the entire list of stops at once
    public void setRoute(List<Location> route) {
        this.route = new ArrayList<>(route);
    }

    public List<Location> getRoute() {
        return route;
    }

    public int getId() {
        return id;
    }

    public Location getDepot() {
        return depot;
    }

    /**
     * Main logic: calculating distance considering departure from Hub and return to it.
     */
    public double getTotalDistance() {
        // If the truck is not going anywhere, its distance = 0
        if (route == null || route.isEmpty()) {
            return 0.0;
        }

        double totalDist = 0.0;

        // 1. Distance from Hub to the FIRST point of the route
        totalDist += depot.distanceTo(route.get(0));

        // 2. Distance between all intermediate points (1 -> 2 -> 3)
        for (int i = 0; i < route.size() - 1; i++) {
            totalDist += route.get(i).distanceTo(route.get(i + 1));
        }

        // 3. Distance from the LAST point back to Hub
        totalDist += route.get(route.size() - 1).distanceTo(depot);

        return totalDist;
    }

    /**
     * Convenient method for printing the route to the console (for debug and demonstration)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Truck ").append(id).append(": [").append(depot.getId()).append("]");

        for (Location loc : route) {
            sb.append(" -> ").append(loc.getId());
        }

        sb.append(" -> [").append(depot.getId()).append("]");
        sb.append(String.format(" | Distance: %.2f", getTotalDistance()));

        return sb.toString();
    }
}