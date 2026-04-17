package optimizer;

import domain.Location;
import routing.MatrixCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwoOptOptimizer {
    // This algorithm optimizes a route by swapping edges to eliminate "crossings" (intersections). It repeatedly checks if reversing a segment of the path results in a shorter total distance.

    public static List<Location> optimize(List<Location> route, Location hub, MatrixCache cache) {
        // Create a full loop: Hub -> Points -> Hub
        List<Location> tour = new ArrayList<>();
        tour.add(hub);
        tour.addAll(route);
        tour.add(hub);

        boolean improvement = true;
        while (improvement) {
            improvement = false;
            for (int i = 1; i < tour.size() - 2; i++) {
                for (int j = i + 1; j < tour.size() - 1; j++) {
                    if (checkImprovement(tour, i, j, cache)) {
                        reverseSegment(tour, i, j);
                        improvement = true;
                    }
                }
            }
        }

        // Return only points (without hub at start and end)
        return tour.subList(1, tour.size() - 1);
    }

    private static boolean checkImprovement(List<Location> tour, int i, int j, MatrixCache cache) {
        Location a = tour.get(i - 1);
        Location b = tour.get(i);
        Location c = tour.get(j);
        Location d = tour.get(j + 1);

        // Current distance of the segment edges
        double currentDist = cache.getDistance(a, b) + cache.getDistance(c, d);
        // Distance after "crossing"
        double newDist = cache.getDistance(a, c) + cache.getDistance(b, d);

        return newDist < currentDist;
    }

    private static void reverseSegment(List<Location> tour, int i, int j) {
        while (i < j) {
            Collections.swap(tour, i, j);
            i++;
            j--;
        }
    }
}