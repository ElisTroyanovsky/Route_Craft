package optimizer.ga;

import domain.Location;
import routing.DistanceMatrix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteDNA {
    private List<Location> tour = new ArrayList<>();
    private double fitness = 0;
    private double distance = 0;

    public RouteDNA() {}

    public RouteDNA(List<Location> tour) {
        this.tour = new ArrayList<>(tour);
    }

    public void generateIndividual(List<Location> allLocations) {
        tour = new ArrayList<>(allLocations);
        Collections.shuffle(tour);
    }

    public Location getLocation(int index) {
        return tour.get(index);
    }

    public void setLocation(int index, Location location) {
        tour.set(index, location);
        // invalidate cached values so they're recalculated on next access
        fitness = 0;
        distance = 0;
    }

    // total distance when the tour is split evenly across trucks, each making a hub → segment → hub trip
    public double getDistance(Location depot, int numberOfTrucks, DistanceMatrix matrix) {
        if (distance == 0) {
            double totalDist = 0;
            int stopsPerTruck = (int) Math.ceil((double) tour.size() / numberOfTrucks);

            for (int t = 0; t < numberOfTrucks; t++) {
                int start = t * stopsPerTruck;
                int end = Math.min(start + stopsPerTruck, tour.size());

                if (start >= end) break;

                totalDist += matrix.getDistance(depot, tour.get(start));

                for (int i = start; i < end - 1; i++) {
                    totalDist += matrix.getDistance(tour.get(i), tour.get(i + 1));
                }

                totalDist += matrix.getDistance(tour.get(end - 1), depot);
            }
            distance = totalDist;
        }
        return distance;
    }

    // fitness is the inverse of distance — shorter routes score higher
    public double getFitness(Location depot, int trucks, DistanceMatrix matrix) {
        if (fitness == 0) {
            fitness = 1 / getDistance(depot, trucks, matrix);
        }
        return fitness;
    }

    public int tourSize() {
        return tour.size();
    }

    public boolean containsLocation(Location location) {
        return tour.contains(location);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Location l : tour) {
            sb.append(l.getId()).append(" -> ");
        }
        return sb.toString();
    }

    public void printTruckRoutes(Location depot, int numberOfTrucks, DistanceMatrix matrix) {
        int stopsPerTruck = (int) Math.ceil((double) tour.size() / numberOfTrucks);

        System.out.println("\n=== DETAILED TRUCK ROUTES ===");
        for (int t = 0; t < numberOfTrucks; t++) {
            int start = t * stopsPerTruck;
            int end = Math.min(start + stopsPerTruck, tour.size());

            if (start >= end) break;

            System.out.print("Truck " + (t + 1) + ": " + depot.getId() + " -> ");

            double truckDist = 0;
            truckDist += matrix.getDistance(depot, tour.get(start));

            for (int i = start; i < end; i++) {
                System.out.print(tour.get(i).getId() + " -> ");
                if (i < end - 1) {
                    truckDist += matrix.getDistance(tour.get(i), tour.get(i + 1));
                }
            }

            truckDist += matrix.getDistance(tour.get(end - 1), depot);

            System.out.println(depot.getId());
            System.out.println("   (Route Segment Distance: " + String.format("%.2f", truckDist) + " km)");
        }
    }
}
