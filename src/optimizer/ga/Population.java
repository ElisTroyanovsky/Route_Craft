package optimizer.ga;

import domain.Location;
import routing.DistanceMatrix;
import java.util.List;

public class Population {
    RouteDNA[] routes;

    public Population(int populationSize, boolean initialise, List<Location> locations) {
        routes = new RouteDNA[populationSize];
        if (initialise) {
            for (int i = 0; i < populationSize(); i++) {
                RouteDNA newTour = new RouteDNA();
                newTour.generateIndividual(locations); // рандомный путь из всех локаций
                saveTour(i, newTour);
            }
        }
    }

    public void saveTour(int index, RouteDNA route) {
        routes[index] = route;
    }

    public RouteDNA getTour(int index) {
        return routes[index];
    }

    public int populationSize() {
        return routes.length;
    }

    // Added matrix
    public RouteDNA getFittest(Location depot, int trucks, DistanceMatrix matrix) {
        RouteDNA fittest = routes[0];
        for (int i = 1; i < populationSize(); i++) {
            if (fittest.getFitness(depot, trucks, matrix) <= getTour(i).getFitness(depot, trucks, matrix)) {
                fittest = getTour(i);
            }
        }
        return fittest;
    }
}