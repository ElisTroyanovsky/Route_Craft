package optimizer.ga;

import domain.Location;
import routing.DistanceMatrix;
import java.util.ArrayList;
import java.util.List;

public class GeneticAlgorithm {

    // The Genetic Algorithm mimics natural selection (crossover, mutation). Tournament selection picks a random group of individuals and selects the best one among them to be a parent.

    private static final double MUTATION_RATE = 0.10;
    private static final int TOURNAMENT_SIZE = 3;
    private static final boolean ELITISM = true;

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
            newPopulation.saveTour(0, pop.getFittest(depot, numberOfTrucks, matrix)); // Pass the matrix
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

    private RouteDNA crossover(RouteDNA parent1, RouteDNA parent2) {
        List<Location> childTour = new ArrayList<>(parent1.tourSize());
        for(int i = 0; i < parent1.tourSize(); i++) {
            childTour.add(null);
        }

        int startPos = (int) (Math.random() * parent1.tourSize());
        int endPos = (int) (Math.random() * parent1.tourSize());

        for (int i = 0; i < parent1.tourSize(); i++) {
            if (startPos < endPos && i > startPos && i < endPos) {
                childTour.set(i, parent1.getLocation(i));
            } else if (startPos > endPos) {
                if (!(i < startPos && i > endPos)) {
                    childTour.set(i, parent1.getLocation(i));
                }
            }
        }

        for (int i = 0; i < parent2.tourSize(); i++) {
            if (!childTour.contains(parent2.getLocation(i))) {
                for (int ii = 0; ii < childTour.size(); ii++) {
                    if (childTour.get(ii) == null) {
                        childTour.set(ii, parent2.getLocation(i));
                        break;
                    }
                }
            }
        }

        return new RouteDNA(childTour);
    }

    private void mutate(RouteDNA tour) {
        for(int tourPos1=0; tourPos1 < tour.tourSize(); tourPos1++){
            if(Math.random() < MUTATION_RATE){
                int tourPos2 = (int) (tour.tourSize() * Math.random());

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
            int randomId = (int) (Math.random() * pop.populationSize());
            tournament.saveTour(i, pop.getTour(randomId));
        }
        return tournament.getFittest(depot, numberOfTrucks, matrix); // Pass the matrix
    }
}