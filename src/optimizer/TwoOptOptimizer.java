package optimizer;

import domain.Location;
import routing.MatrixCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwoOptOptimizer {

    // eliminates route crossings by trying all pairs of edges and reversing the segment between
    // them if it reduces total distance; repeats until no improvement is found
    public static List<Location> optimize(List<Location> route, Location hub, MatrixCache cache) {
        // work on the full round-trip so 2-opt can also fix edges touching the hub
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

        // strip hub endpoints — they're added back by the caller
        return tour.subList(1, tour.size() - 1);
    }

    private static boolean checkImprovement(List<Location> tour, int i, int j, MatrixCache cache) {
        Location a = tour.get(i - 1);
        Location b = tour.get(i);
        Location c = tour.get(j);
        Location d = tour.get(j + 1);

        // cost of current edges a→b and c→d vs. cost after reversal a→c and b→d
        double currentDist = cache.getDistance(a, b) + cache.getDistance(c, d);
        double newDist     = cache.getDistance(a, c) + cache.getDistance(b, d);

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
