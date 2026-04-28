package routing;

import domain.Location;
import java.util.HashMap;
import java.util.Map;

public class MatrixCache implements DistanceMatrix {
    // two-level map: from-id → to-id → distance in km
    private final Map<String, Map<String, Double>> cache = new HashMap<>();

    public void saveDistance(String fromId, String toId, double distanceKm) {
        cache.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, distanceKm);
    }

    @Override
    public double getDistance(Location from, Location to) {
        if (from.getId().equals(to.getId())) {
            return 0.0;
        }

        if (cache.containsKey(from.getId()) && cache.get(from.getId()).containsKey(to.getId())) {
            return cache.get(from.getId()).get(to.getId());
        }

        // large penalty for missing pairs (e.g. unreachable locations) so algorithms avoid them
        return 999999.0;
    }
}