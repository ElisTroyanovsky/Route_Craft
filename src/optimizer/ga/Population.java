package optimizer.ga;

import domain.Location;
import java.util.Arrays;
import java.util.List;

public class Population {
    RouteDNA[] routes;

    // Создать популяцию
    public Population(int populationSize, boolean initialise, List<Location> locations) {
        routes = new RouteDNA[populationSize];
        if (initialise) {
            for (int i = 0; i < populationSize(); i++) {
                RouteDNA newTour = new RouteDNA();
                newTour.generateIndividual(locations);
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

    // Найти лучшее решение в этой популяции
    public RouteDNA getFittest(Location depot, int trucks) {
        RouteDNA fittest = routes[0];
        for (int i = 1; i < populationSize(); i++) {
            if (fittest.getFitness(depot, trucks) <= getTour(i).getFitness(depot, trucks)) {
                fittest = getTour(i);
            }
        }
        return fittest;
    }
}