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

    // Ant builds a full tour starting and ending at the Hub
    public void visitTour(Location hub, List<Location> allLocations, DistanceMatrix matrix, double[][] pheromones, double alpha, double beta) {
        tour.clear();
        for (int i = 0; i < numberOfLocations; i++) visited[i] = false;

        Location currentLocation = hub;

        // Visit all delivery points
        for (int i = 0; i < allLocations.size(); i++) {
            Location nextLocation = selectNextLocation(currentLocation, allLocations, matrix, pheromones, alpha, beta);
            tour.add(nextLocation);
            visited[allLocations.indexOf(nextLocation)] = true;
            currentLocation = nextLocation;
        }
    }

    // Probability-based selection of the next city
    private Location selectNextLocation(Location current, List<Location> allLocations, DistanceMatrix matrix, double[][] pheromones, double alpha, double beta) {
        double[] probabilities = new double[allLocations.size()];
        double sum = 0.0;

        int fromIdx = getIndex(current, allLocations);

        // Calculate probability for each unvisited location

        // This is the Transition Probability formula: it combines local desirability (1/distance) with global memory (pheromone levels) to determine the likelihood of choosing a specific next city.
        for (int i = 0; i < allLocations.size(); i++) {
            if (!visited[i]) {
                double distance = matrix.getDistance(current, allLocations.get(i));
                // Pheromone influence (alpha) and Distance influence (beta)
                // Math.pow(pheromone, alpha) * Math.pow(1/distance, beta)
                probabilities[i] = Math.pow(pheromones[fromIdx][i], alpha) * Math.pow(1.0 / distance, beta);
                sum += probabilities[i];
            }
        }

        // Roulette wheel selection
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

        // Fallback: all probabilities were 0 (e.g. unreachable points with distance=999999).
        // Pick the first unvisited location to avoid returning null.
        for (int i = 0; i < allLocations.size(); i++) {
            if (!visited[i]) return allLocations.get(i);
        }
        return null;
    }

    private int getIndex(Location loc, List<Location> allLocations) {
        // Hub is not in allLocations, so it gets the reserved last index in the pheromone matrix
        if (loc.getId().equals("HUB")) return allLocations.size();
        return allLocations.indexOf(loc);
    }

    public List<Location> getTour() {
        return tour;
    }

    // Total distance of the ant's route (including return to hub)
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