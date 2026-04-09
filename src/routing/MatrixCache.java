package routing;

import domain.Location;
import java.util.HashMap;
import java.util.Map;

public class MatrixCache implements DistanceMatrix {
    // Двойная хэш-таблица: "ID откуда" -> ("ID куда" -> "Дистанция в км")
    private final Map<String, Map<String, Double>> cache = new HashMap<>();

    public void saveDistance(String fromId, String toId, double distanceKm) {
        cache.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, distanceKm);
    }

    @Override
    public double getDistance(Location from, Location to) {
        // Если точка та же самая, расстояние 0
        if (from.getId().equals(to.getId())) {
            return 0.0;
        }

        // Достаем расстояние из кэша
        if (cache.containsKey(from.getId()) && cache.get(from.getId()).containsKey(to.getId())) {
            return cache.get(from.getId()).get(to.getId());
        }

        // Фоллбэк: если маршрута нет (например, точка на острове без мостов)
        // Выдаем гигантский штраф, чтобы алгоритмы туда не ехали
        return 999999.0;
    }
}