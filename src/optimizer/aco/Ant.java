package optimizer.aco;

import domain.Location;
import routing.DistanceMatrix;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ant {
    private static final Random RANDOM = new Random();

    private final List<Location> tour = new ArrayList<>();
    private final boolean[] visited;
    private final int numberOfLocations;

    public Ant(int numberOfLocations) {
        this.numberOfLocations = numberOfLocations;
        this.visited = new boolean[numberOfLocations];
    }

    // builds a complete tour by visiting every delivery point exactly once
    public void visitTour(Location hub, List<Location> allLocations, DistanceMatrix matrix, double[][] pheromones, double alpha, double beta) {
        tour.clear();
        for (int i = 0; i < numberOfLocations; i++) visited[i] = false;

        Location currentLocation = hub;

        for (int i = 0; i < allLocations.size(); i++) {
            Location nextLocation = selectNextLocation(currentLocation, allLocations, matrix, pheromones, alpha, beta);
            tour.add(nextLocation);
            visited[allLocations.indexOf(nextLocation)] = true;
            currentLocation = nextLocation;
        }
    }

    // picks the next unvisited location using weighted probabilities
    private Location selectNextLocation(Location current, List<Location> allLocations, DistanceMatrix matrix, double[][] pheromones, double alpha, double beta) {
        double[] probabilities = new double[allLocations.size()];
        double sum = 0.0;

        int fromIdx = getIndex(current, allLocations);

        // transition probability: pheromone^alpha * (1/distance)^beta
        // combines memory (pheromone trail) with local desirability (shorter = better)
        for (int i = 0; i < allLocations.size(); i++) {
            if (!visited[i]) {
                double distance = matrix.getDistance(current, allLocations.get(i));
                probabilities[i] = Math.pow(pheromones[fromIdx][i], alpha) * Math.pow(1.0 / distance, beta);
                sum += probabilities[i];
            }
        }

        // roulette-wheel selection: spin a random value and accumulate until we land on a slot
        double randomValue = RANDOM.nextDouble() * sum;
        double currentSum = 0.0;

        for (int i = 0; i < allLocations.size(); i++) {
            if (!visited[i]) {
                currentSum += probabilities[i];
                if (currentSum >= randomValue) {
                    return allLocations.get(i);
                }
            }
        }

        // fallback: all probabilities were 0 (e.g. unreachable points with distance=999999).
        // pick the first unvisited location to avoid returning null.
        for (int i = 0; i < allLocations.size(); i++) {
            if (!visited[i]) return allLocations.get(i);
        }
        return null;
    }

    private int getIndex(Location loc, List<Location> allLocations) {
        // hub is not in allLocations so it uses the last reserved index in the pheromone matrix
        if (loc.getId().equals("HUB")) return allLocations.size();
        return allLocations.indexOf(loc);
    }

    public List<Location> getTour() {
        return tour;
    }

    // calculates total distance when the tour is split across trucks (each truck: hub → segment → hub)
    public double calculateTourDistance(Location hub, DistanceMatrix matrix, int trucks) {
        double dist = 0;
        int stopsPerTruck = (int) Math.ceil((double) tour.size() / trucks);

        for (int t = 0; t < trucks; t++) {
            int start = t * stopsPerTruck;
            int end = Math.min(start + stopsPerTruck, tour.size());
            if (start >= end) break;

            dist += matrix.getDistance(hub, tour.get(start));
            for (int i = start; i < end - 1; i++) {
                dist += matrix.getDistance(tour.get(i), tour.get(i + 1));
            }
            dist += matrix.getDistance(tour.get(end - 1), hub);
        }
        return dist;
    }
}
