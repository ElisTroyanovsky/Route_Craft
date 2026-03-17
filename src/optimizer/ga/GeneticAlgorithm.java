package optimizer.ga;

import domain.Location;
import java.util.ArrayList;
import java.util.List;

public class GeneticAlgorithm {

    /* Параметры GA */
    private static final double MUTATION_RATE = 0.10;
    private static final int TOURNAMENT_SIZE = 3;      // Размер турнира
    private static final boolean ELITISM = true;       // Сохранять ли лучший результат

    private Location depot;
    private int numberOfTrucks;

    public GeneticAlgorithm(Location depot, int numberOfTrucks) {
        this.depot = depot;
        this.numberOfTrucks = numberOfTrucks;
    }

    // Эволюция популяции (один шаг/поколение)
    public Population evolvePopulation(Population pop) {
        Population newPopulation = new Population(pop.populationSize(), false, null);

        int elitismOffset = 0;
        if (ELITISM) {
            newPopulation.saveTour(0, pop.getFittest(depot, numberOfTrucks));
            elitismOffset = 1;
        }

        // Crossover (Скрещивание)
        for (int i = elitismOffset; i < newPopulation.populationSize(); i++) {
            RouteDNA parent1 = tournamentSelection(pop);
            RouteDNA parent2 = tournamentSelection(pop);
            RouteDNA child = crossover(parent1, parent2);
            newPopulation.saveTour(i, child);
        }

        // Mutation (Мутация)
        for (int i = elitismOffset; i < newPopulation.populationSize(); i++) {
            mutate(newPopulation.getTour(i));
        }

        return newPopulation;
    }

    // Скрещивание (Ordered Crossover) - чтобы не потерять города
    // Скрещивание (Ordered Crossover) - чтобы не потерять города
    private RouteDNA crossover(RouteDNA parent1, RouteDNA parent2) {
        // 1. Создаем пустой список нужного размера
        // (Убрали ошибочную строку "RouteDNA child = ...")
        List<Location> childTour = new ArrayList<>(parent1.tourSize());
        for(int i = 0; i < parent1.tourSize(); i++) {
            childTour.add(null);
        }

        // 2. Выбираем случайный сегмент от первого родителя
        int startPos = (int) (Math.random() * parent1.tourSize());
        int endPos = (int) (Math.random() * parent1.tourSize());

        // 3. Копируем гены (города) из первого родителя в рамках сегмента
        for (int i = 0; i < parent1.tourSize(); i++) {
            if (startPos < endPos && i > startPos && i < endPos) {
                childTour.set(i, parent1.getLocation(i));
            } else if (startPos > endPos) {
                if (!(i < startPos && i > endPos)) {
                    childTour.set(i, parent1.getLocation(i));
                }
            }
        }

        // 4. Заполняем оставшиеся места генами второго родителя
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

        // 5. ВАЖНО: Создаем объект RouteDNA только здесь, когда список готов
        return new RouteDNA(childTour);
    }

    // Мутация (Swap Mutation) - меняем два города местами
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

    // Выбор родителей (Турнирный отбор)
    private RouteDNA tournamentSelection(Population pop) {
        Population tournament = new Population(TOURNAMENT_SIZE, false, null);
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int randomId = (int) (Math.random() * pop.populationSize());
            tournament.saveTour(i, pop.getTour(randomId));
        }
        return tournament.getFittest(depot, numberOfTrucks);
    }
}