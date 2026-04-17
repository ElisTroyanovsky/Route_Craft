package optimizer.aco;

import domain.Location;
import routing.DistanceMatrix;
import optimizer.ga.RouteDNA;
import java.util.ArrayList;
import java.util.List;
// Alpha and Beta are weights for the decision-making process: Alpha controls the influence of pheromones (memory), while Beta controls the influence of distance/heuristic (greedy choice).

public class AntColonyOptimizer {
    // ACO Parameters
    private final double alpha = 1.0;       // Pheromone importance
    private final double beta = 2.0;        // Distance importance (greedy force)
    private final double evaporation = 0.5; // Pheromone evaporation rate
    private final double Q = 500;           // Pheromone deposit factor
    private final int antCount = 30;        // Number of ants in colony

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

        // Initialize pheromones with a small constant value
        int size = allLocations.size() + 1; // +1 for the Hub index
        this.pheromones = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                pheromones[i][j] = 0.1;
            }
        }
    }

    // Run one iteration of the colony
    public List<RouteDNA> runIteration() {
        List<Ant> ants = new ArrayList<>();
        List<RouteDNA> iterationResults = new ArrayList<>();

        // 1. Each ant builds a tour
        for (int i = 0; i < antCount; i++) {
            Ant ant = new Ant(allLocations.size());
            ant.visitTour(hub, allLocations, matrix, pheromones, alpha, beta);
            ants.add(ant);

            // Convert Ant tour to RouteDNA for the Genetic Algorithm later
            iterationResults.add(new RouteDNA(ant.getTour()));
        }
        // Evaporation prevents pheromones from accumulating infinitely and helps the algorithm forget old, sub-optimal paths. Updating pheromones reinforces paths that were part of shorter tours.
        // 2. Evaporate pheromones
        evaporatePheromones();

        // 3. Update pheromones based on ants' performance
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
            double contribution = Q / distance; // This calculates the amount of pheromone to deposit; shorter distances result in higher pheromone concentrations.

            List<Location> tour = ant.getTour();
            // Deposit pheromones on each edge of the tour
            for (int i = 0; i < tour.size() - 1; i++) {
                int from = allLocations.indexOf(tour.get(i));
                int to = allLocations.indexOf(tour.get(i+1));
                pheromones[from][to] += contribution;
            }
        }
    }

    // This method allows the Genetic Algorithm to "smell" the best GA results
    public void reinforcePheromones(RouteDNA bestGAFinalist) {
        double contribution = Q * 2; // GA results are highly valued
        for (int i = 0; i < bestGAFinalist.tourSize() - 1; i++) {
            int from = allLocations.indexOf(bestGAFinalist.getLocation(i));
            int to = allLocations.indexOf(bestGAFinalist.getLocation(i+1));
            if (from != -1 && to != -1) {
                pheromones[from][to] += contribution;
            }
        }
    }
}