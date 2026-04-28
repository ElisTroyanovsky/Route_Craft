package domain;

import java.util.ArrayList;
import java.util.List;

public class Truck {
    private int id;
    private Location depot; // the point where every truck starts and ends its trip
    private List<Location> route; // delivery stops, hub not included

    public Truck(int id, Location depot) {
        this.id = id;
        this.depot = depot;
        this.route = new ArrayList<>();
    }

    public void addStop(Location location) {
        this.route.add(location);
    }

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

    // calculates the full round-trip: depot → stops → depot
    public double getTotalDistance() {
        if (route == null || route.isEmpty()) {
            return 0.0;
        }

        double totalDist = 0.0;

        // depot to first stop
        totalDist += depot.distanceTo(route.get(0));

        // stop to stop
        for (int i = 0; i < route.size() - 1; i++) {
            totalDist += route.get(i).distanceTo(route.get(i + 1));
        }

        // last stop back to depot
        totalDist += route.get(route.size() - 1).distanceTo(depot);

        return totalDist;
    }

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