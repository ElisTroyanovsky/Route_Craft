package optimizer.ga;

import domain.Location;
import routing.DistanceMatrix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {

    // The Genetic Algorithm mimics natural selection (crossover, mutation). Tournament selection picks a random group of individuals and selects the best one among them to be a parent.

    private static final double MUTATION_RATE = 0.10;
    private static final int TOURNAMENT_SIZE = 3;
    private static final boolean ELITISM = true;
    private static final Random RANDOM = new Random();

    private Location depot;
    private int numberOfTrucks;
    private DistanceMatrix matrix;

    public GeneticAlgorithm(Location depot, int numberOfTrucks, DistanceMatrix matrix) {
        this.depot = depot;
        this.numberOfTrucks = numberOfTrucks;
        this.matrix = matrix;
    }

    public Population evolvePopulation(Population pop) {
        Population newPopulation = new Population(pop.populationSize(), false, null);

        // Elitism preserves the best individual from the previous generation to ensure that the quality of the population does not decrease over time.
        int elitismOffset = 0;
        if (ELITISM) {
            newPopulation.saveTour(0, pop.getFittest(depot, numberOfTrucks, matrix));
            elitismOffset = 1;
        }

        for (int i = elitismOffset; i < newPopulation.populationSize(); i++) {
            RouteDNA parent1 = tournamentSelection(pop);
            RouteDNA parent2 = tournamentSelection(pop);
            RouteDNA child = crossover(parent1, parent2);
            newPopulation.saveTour(i, child);
        }

        for (int i = elitismOffset; i < newPopulation.populationSize(); i++) {
            mutate(newPopulation.getTour(i));
        }

        return newPopulation;
    }

    // Order Crossover (OX): copies a random segment from parent1, then fills
    // the rest with parent2's genes in order (skipping duplicates).
    private RouteDNA crossover(RouteDNA parent1, RouteDNA parent2) {
        int size = parent1.tourSize();
        List<Location> childTour = new ArrayList<>(Collections.nCopies(size, null));

        // Pick two random cut points and make sure start <= end
        int start = (int) (RANDOM.nextDouble() * size);
        int end   = (int) (RANDOM.nextDouble() * size);
        if (start > end) { int tmp = start; start = end; end = tmp; }

        // Copy the segment [start..end] from parent1 into the child
        for (int i = start; i <= end; i++) {
            childTour.set(i, parent1.getLocation(i));
        }

        // Fill the remaining null slots with parent2's genes, preserving their order
        int childIdx = 0;
        for (int i = 0; i < size; i++) {
            if (!childTour.contains(parent2.getLocation(i))) {
                while (childTour.get(childIdx) != null) childIdx++;
                childTour.set(childIdx, parent2.getLocation(i));
            }
        }

        return new RouteDNA(childTour);
    }

    private void mutate(RouteDNA tour) {
        for(int tourPos1=0; tourPos1 < tour.tourSize(); tourPos1++){
            if(RANDOM.nextDouble() < MUTATION_RATE){
                int tourPos2 = (int) (tour.tourSize() * RANDOM.nextDouble());

                Location city1 = tour.getLocation(tourPos1);
                Location city2 = tour.getLocation(tourPos2);

                tour.setLocation(tourPos2, city1);
                tour.setLocation(tourPos1, city2);
            }
        }
    }

    private RouteDNA tournamentSelection(Population pop) {
        Population tournament = new Population(TOURNAMENT_SIZE, false, null);
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomId = (int) (RANDOM.nextDouble() * pop.populationSize());
            tournament.saveTour(i, pop.getTour(randomId));
        }
        return tournament.getFittest(depot, numberOfTrucks, matrix);
    }
}