package optimizer.aco;

import domain.Location;
import routing.DistanceMatrix;
import optimizer.ga.RouteDNA;
import java.util.ArrayList;
import java.util.List;

// alpha and beta are the two weights in the transition probability formula:
// alpha controls how strongly ants follow existing pheromone trails (memory),
// beta controls how strongly they prefer shorter edges (greedy instinct)
public class AntColonyOptimizer {

    private final double alpha      = 1.0;  // pheromone influence
    private final double beta       = 2.0;  // distance influence (higher = more greedy)
    private final double evaporation = 0.5; // fraction of pheromone that evaporates each iteration
    private final double Q          = 500;  // pheromone deposit factor
    private final int antCount      = 30;   // number of ants in the colony

    private double[][] pheromones;
    private final Location hub;
    private final List<Location> allLocations;
    private final DistanceMatrix matrix;
    private final int trucks;

    public AntColonyOptimizer(Location hub, List<Location> allLocations, DistanceMatrix matrix, int trucks) {
        this.hub = hub;
        this.allLocations = allLocations;
        this.matrix = matrix;
        this.trucks = trucks;

        // start all pheromone levels at a small constant so every path has some initial appeal
        int size = allLocations.size() + 1; // +1 for the hub's reserved index
        this.pheromones = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                pheromones[i][j] = 0.1;
            }
        }
    }

    // runs one full ACO iteration: each ant builds a tour, then pheromones evaporate and are updated
    public List<RouteDNA> runIteration() {
        List<Ant> ants = new ArrayList<>();
        List<RouteDNA> iterationResults = new ArrayList<>();

        // 1. each ant independently builds a complete tour
        for (int i = 0; i < antCount; i++) {
            Ant ant = new Ant(allLocations.size());
            ant.visitTour(hub, allLocations, matrix, pheromones, alpha, beta);
            ants.add(ant);

            // wrap the ant's tour in a RouteDNA so it can be injected into the ga population
            iterationResults.add(new RouteDNA(ant.getTour()));
        }

        // 2. evaporate pheromones — prevents infinite accumulation and helps forget old bad paths
        evaporatePheromones();

        // 3. deposit pheromones proportional to route quality (shorter route = more pheromone)
        updatePheromones(ants);

        return iterationResults;
    }

    private void evaporatePheromones() {
        for (int i = 0; i < pheromones.length; i++) {
            for (int j = 0; j < pheromones[i].length; j++) {
                pheromones[i][j] *= (1.0 - evaporation);
            }
        }
    }

    private void updatePheromones(List<Ant> ants) {
        for (Ant ant : ants) {
            double distance = ant.calculateTourDistance(hub, matrix, trucks);
            double contribution = Q / distance; // shorter tours deposit more pheromone

            List<Location> tour = ant.getTour();
            for (int i = 0; i < tour.size() - 1; i++) {
                int from = allLocations.indexOf(tour.get(i));
                int to   = allLocations.indexOf(tour.get(i + 1));
                pheromones[from][to] += contribution;
            }
        }
    }

    // reinforces pheromones along the best GA route so ants explore the same region next cycle
    public void reinforcePheromones(RouteDNA bestGAFinalist) {
        double contribution = Q * 2; // double deposit so ants strongly prefer ga-proven paths
        for (int i = 0; i < bestGAFinalist.tourSize() - 1; i++) {
            int from = allLocations.indexOf(bestGAFinalist.getLocation(i));
            int to   = allLocations.indexOf(bestGAFinalist.getLocation(i + 1));
            if (from != -1 && to != -1) {
                pheromones[from][to] += contribution;
            }
        }
    }
}
