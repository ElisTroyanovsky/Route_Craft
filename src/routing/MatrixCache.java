package routing;

import domain.Location;
import java.util.HashMap;
import java.util.Map;

public class MatrixCache implements DistanceMatrix {
    // Double hash table: "ID from" -> ("ID to" -> "Distance in km")
    private final Map<String, Map<String, Double>> cache = new HashMap<>();

    public void saveDistance(String fromId, String toId, double distanceKm) {
        cache.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, distanceKm);
    }

    @Override
    public double getDistance(Location from, Location to) {
        // If it's the same point, distance is 0
        if (from.getId().equals(to.getId())) {
            return 0.0;
        }

        // Retrieve distance from cache
        if (cache.containsKey(from.getId()) && cache.get(from.getId()).containsKey(to.getId())) {
            return cache.get(from.getId()).get(to.getId());
        }

        // Fallback: if there is no route (e.g., a point on an island without bridges)
        // We issue a giant penalty so that the algorithms don't go there
        return 999999.0;
    }
}