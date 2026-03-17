package evaluation;

import domain.Truck;
import java.util.List;

public class FitnessFunction {

    private PenaltyCalculator penaltyCalculator = new PenaltyCalculator();

    public double evaluate(List<Truck> trucks) {
        // 1. Чистая дистанция (физика)
        double totalDistance = 0;
        for (Truck t : trucks) {
            totalDistance += t.getTotalDistance();
        }

        // 2. Штрафы и награды (бизнес-логика)
        double penalties = penaltyCalculator.calculateTotalPenalty(trucks);

        // 3. Итоговая "Стоимость" решения
        // Чем меньше score, тем лучше решение.
        double finalScore = totalDistance + penalties;

        // В Генетическом алгоритме Fitness обычно чем БОЛЬШЕ, тем лучше.
        // Поэтому мы переворачиваем значение.
        // Защита от деления на ноль.
        if (finalScore == 0) return Double.MAX_VALUE;

        return 1.0 / finalScore;
    }
}