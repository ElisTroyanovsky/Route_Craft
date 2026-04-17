package optimizer;

import domain.Location;
import routing.DistanceMatrix;
import java.util.ArrayList;
import java.util.List;

public class GreedySolver {

    // The formula is "Nearest Neighbor": from the current location, always choose the unvisited point with the minimum distance.

    public static List<List<Location>> solve(Location hub, List<Location> allPoints, int numberOfTrucks, DistanceMatrix matrix) {
        List<List<Location>> finalRoutes = new ArrayList<>();
        List<Location> unvisited = new ArrayList<>(allPoints);

        int maxStopsPerTruck = (int) Math.ceil((double) allPoints.size() / numberOfTrucks);

        for (int t = 0; t < numberOfTrucks; t++) {
            List<Location> currentTruckRoute = new ArrayList<>();
            Location currentLocation = hub;

            while (currentTruckRoute.size() < maxStopsPerTruck && !unvisited.isEmpty()) {
                Location nearestPoint = null;
                double minDistance = Double.MAX_VALUE;

                for (Location candidate : unvisited) {
                    double dist = matrix.getDistance(currentLocation, candidate);

                    if (dist < minDistance) {
                        minDistance = dist;
                        nearestPoint = candidate;
                    }
                }

                if (nearestPoint != null) {
                    currentTruckRoute.add(nearestPoint);
                    unvisited.remove(nearestPoint);
                    currentLocation = nearestPoint;
                }
            }

            finalRoutes.add(currentTruckRoute);
        }

        return finalRoutes;
    }
}