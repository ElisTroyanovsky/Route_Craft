package evaluation;

import domain.Truck;
import java.util.List;

public class PenaltyCalculator {

    // КОНСТАНТЫ (ВЕСА) - Настрой их под себя
    // Насколько сильно мы ненавидим, когда один водитель пашет, а другие отдыхают
    private static final double IMBALANCE_PENALTY_FACTOR = 2.0;

    // Штраф за пустой грузовик (если мы обязаны использовать все машины)
    private static final double EMPTY_TRUCK_PENALTY = 1000.0;

    // Лимит остановок на один грузовик (чтобы не перегружать одного)
    private static final int MAX_STOPS_PER_TRUCK = 10;
    private static final double OVERLOAD_PENALTY = 500.0;

    // Награда за идеальный баланс (вычитаем из общей "стоимости")
    private static final double PERFECT_BALANCE_REWARD = 200.0;

    /**
     * Главный метод расчета всех штрафов и бонусов
     */
    public double calculateTotalPenalty(List<Truck> trucks) {
        double totalPenalty = 0.0;

        totalPenalty += calculateImbalancePenalty(trucks);
        totalPenalty += calculateEmptyTrucksPenalty(trucks);
        totalPenalty += calculateOverloadPenalty(trucks);

        // Награды мы вычитаем (уменьшаем "боль" алгоритма)
        totalPenalty -= calculateBalanceReward(trucks);

        return totalPenalty;
    }

    // 1. Штраф за Дисбаланс (Imbalance)
    // Мы берем разницу между самым длинным и самым коротким маршрутом.
    private double calculateImbalancePenalty(List<Truck> trucks) {
        double maxDist = 0;
        double minDist = Double.MAX_VALUE;

        for (Truck t : trucks) {
            double dist = t.getTotalDistance();
            if (dist > maxDist) maxDist = dist;
            if (dist < minDist) minDist = dist;
        }

        // Если разница огромная, штраф будет большим.
        // Это заставит алгоритм перекидывать точки от перегруженного к свободному.
        double difference = maxDist - minDist;
        return difference * IMBALANCE_PENALTY_FACTOR;
    }

    // 2. Штраф за простой техники
    private double calculateEmptyTrucksPenalty(List<Truck> trucks) {
        double penalty = 0;
        for (Truck t : trucks) {
            // Если у грузовика 0 точек (или только Хаб), штрафуем
            if (t.getRoute().isEmpty() || t.getRoute().size() <= 2) { // <=2 если считаем HUB->HUB как пустой
                penalty += EMPTY_TRUCK_PENALTY;
            }
        }
        return penalty;
    }

    // 3. Штраф за перегруз (слишком много точек у одного)
    private double calculateOverloadPenalty(List<Truck> trucks) {
        double penalty = 0;
        for (Truck t : trucks) {
            // Исключаем Хаб из подсчета остановок, если нужно
            int stops = t.getRoute().size();
            if (stops > MAX_STOPS_PER_TRUCK) {
                // Штрафуем за каждую лишнюю точку
                penalty += (stops - MAX_STOPS_PER_TRUCK) * OVERLOAD_PENALTY;
            }
        }
        return penalty;
    }

    // 4. Награда (Reward) за хороший баланс
    private double calculateBalanceReward(List<Truck> trucks) {
        double maxDist = 0;
        double minDist = Double.MAX_VALUE;

        for (Truck t : trucks) {
            double dist = t.getTotalDistance();
            if (dist > maxDist) maxDist = dist;
            if (dist < minDist) minDist = dist;
        }

        // Если разница между самым загруженным и самым свободным меньше 10%
        if ((maxDist - minDist) < (maxDist * 0.1)) {
            return PERFECT_BALANCE_REWARD;
        }
        return 0.0;
    }
}