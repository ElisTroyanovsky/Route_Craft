package domain;

import java.util.ArrayList;
import java.util.List;

public class Truck {
    private int id;
    private Location depot; // Стартовая и конечная точка (Хаб)
    private List<Location> route; // Только промежуточные остановки (без Хаба)

    public Truck(int id, Location depot) {
        this.id = id;
        this.depot = depot;
        this.route = new ArrayList<>();
    }

    // Добавление одной точки в конец маршрута
    public void addStop(Location location) {
        this.route.add(location);
    }

    // Установка сразу всего списка остановок
    public void setRoute(List<Location> route) {
        this.route = new ArrayList<>(route);
    }

    public List<Location> getRoute() {
        return route;
    }

    public int getId() {
        return id;
    }

    public Location getDepot() {
        return depot;
    }

    /**
     * Главная логика: расчет дистанции с учетом выезда из Хаба и возврата в него.
     */
    public double getTotalDistance() {
        // Если грузовик никуда не едет, его дистанция = 0
        if (route == null || route.isEmpty()) {
            return 0.0;
        }

        double totalDist = 0.0;

        // 1. Дистанция от Хаба до ПЕРВОЙ точки маршрута
        totalDist += depot.distanceTo(route.get(0));

        // 2. Дистанция между всеми промежуточными точками (1 -> 2 -> 3)
        for (int i = 0; i < route.size() - 1; i++) {
            totalDist += route.get(i).distanceTo(route.get(i + 1));
        }

        // 3. Дистанция от ПОСЛЕДНЕЙ точки обратно в Хаб
        totalDist += route.get(route.size() - 1).distanceTo(depot);

        return totalDist;
    }

    /**
     * Удобный метод для вывода маршрута в консоль (для дебага и демонстрации)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Truck ").append(id).append(": [").append(depot.getId()).append("]");

        for (Location loc : route) {
            sb.append(" -> ").append(loc.getId());
        }

        sb.append(" -> [").append(depot.getId()).append("]");
        sb.append(String.format(" | Distance: %.2f", getTotalDistance()));

        return sb.toString();
    }
}