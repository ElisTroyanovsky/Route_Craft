package optimizer.ga;

import domain.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteDNA {
    private List<Location> tour = new ArrayList<>();
    private double fitness = 0;
    private double distance = 0;

    // Пустой конструктор
    public RouteDNA() {}

    // Конструктор с готовым списком городов
    public RouteDNA(List<Location> tour) {
        this.tour = new ArrayList<>(tour);
    }

    // Генерация случайного индивида
    public void generateIndividual(List<Location> allLocations) {
        // Создаем копию и перемешиваем
        tour = new ArrayList<>(allLocations);
        Collections.shuffle(tour);
    }

    // Получить "ген" (локацию) по индексу
    public Location getLocation(int index) {
        return tour.get(index);
    }

    public void setLocation(int index, Location location) {
        tour.set(index, location);
        fitness = 0;
        distance = 0;
    }

    // ВАЖНО: Расчет дистанции для НЕСКОЛЬКИХ ГРУЗОВИКОВ (MTSP)
    // depot - это склад (Hub)
    // numberOfTrucks - на сколько частей делим маршрут
    public double getDistance(Location depot, int numberOfTrucks) {
        if (distance == 0) {
            double totalDist = 0;
            int stopsPerTruck = (int) Math.ceil((double)tour.size() / numberOfTrucks);

            // Разбиваем один длинный список на под-маршруты для каждого грузовика
            for (int t = 0; t < numberOfTrucks; t++) {
                int start = t * stopsPerTruck;
                int end = Math.min(start + stopsPerTruck, tour.size());

                if (start >= end) break; // Если точек меньше, чем грузовиков

                // Грузовик выезжает со склада
                totalDist += depot.distanceTo(tour.get(start));

                // Едет по своим точкам
                for (int i = start; i < end - 1; i++) {
                    totalDist += tour.get(i).distanceTo(tour.get(i + 1));
                }

                // Возвращается на склад
                totalDist += tour.get(end - 1).distanceTo(depot);
            }
            distance = totalDist;
        }
        return distance;
    }

    // Фитнес - это "здоровье" решения. Чем меньше дистанция, тем больше фитнес.
    public double getFitness(Location depot, int trucks) {
        if (fitness == 0) {
            fitness = 1 / getDistance(depot, trucks);
        }
        return fitness;
    }

    public int tourSize() {
        return tour.size();
    }

    public boolean containsLocation(Location location) {
        return tour.contains(location);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Location l : tour) {
            sb.append(l.getId()).append(" -> ");
        }
        return sb.toString();
    }
    // Добавь этот метод, чтобы красиво печатать маршруты по грузовикам
    public void printTruckRoutes(Location depot, int numberOfTrucks) {
        int stopsPerTruck = (int) Math.ceil((double)tour.size() / numberOfTrucks);

        System.out.println("\n=== DETAILED TRUCK ROUTES ===");
        for (int t = 0; t < numberOfTrucks; t++) {
            int start = t * stopsPerTruck;
            int end = Math.min(start + stopsPerTruck, tour.size());

            if (start >= end) break;

            System.out.print("Truck " + (t + 1) + ": " + depot.getId() + " -> "); // Выезд из Хаба

            double truckDist = 0;
            // От Хаба до первой точки
            truckDist += depot.distanceTo(tour.get(start));

            for (int i = start; i < end; i++) {
                System.out.print(tour.get(i).getId() + " -> ");
                // Считаем дистанцию внутри (для проверки)
                if (i < end - 1) {
                    truckDist += tour.get(i).distanceTo(tour.get(i + 1));
                }
            }

            // Возврат в Хаб
            truckDist += tour.get(end - 1).distanceTo(depot);

            System.out.println(depot.getId()); // Возврат в Хаб
            System.out.println("   (Route Segment Distance: " + String.format("%.2f", truckDist) + ")");
        }
    }
}