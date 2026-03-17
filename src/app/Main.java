package app;

import api.RouteServer;
import domain.Location;
import optimizer.ga.GeneticAlgorithm;
import optimizer.ga.Population;
import optimizer.ga.RouteDNA;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Route Craft System Starting ===");

        // Запускаем HTTP сервер, чтобы слушать запросы от сайта
        RouteServer.startServer();

        // 1. Создаем Склад (Hub)
        Location hub = new Location("HUB", 50, 50);

        // 2. Создаем точки доставки (рандомные координаты 0-100)
        List<Location> deliveryPoints = new ArrayList<>();
        int numberOfPoints = 20; // 20 магазинов
        int numberOfTrucks = 3;  // 3 грузовика

        for (int i = 0; i < numberOfPoints; i++) {
            deliveryPoints.add(new Location("Store_" + i, Math.random() * 100, Math.random() * 100));
        }

        // Доказательство сложности (почему нельзя Brute Force)
        // Факториал 20! - это огромное число
        System.out.println("Problem: " + numberOfPoints + " locations, " + numberOfTrucks + " trucks.");
        System.out.println("Search space is huge. Brute force is impossible.");
        System.out.println("Starting Genetic Algorithm...");

        // 3. Инициализация популяции
        // Размер популяции = 100 решений
        Population pop = new Population(100, true, deliveryPoints);
        GeneticAlgorithm ga = new GeneticAlgorithm(hub, numberOfTrucks);

        System.out.println("Initial Distance: " + String.format("%.2f", pop.getFittest(hub, numberOfTrucks).getDistance(hub, numberOfTrucks)));

        // 4. Запуск цикла эволюции
        // 1000 поколений
        int generations = 1000;
        for (int i = 0; i < generations; i++) {
            pop = ga.evolvePopulation(pop);

            // Вывод прогресса каждые 100 поколений
            if (i % 100 == 0 || i == generations - 1) {
                RouteDNA best = pop.getFittest(hub, numberOfTrucks);
                System.out.println("Generation " + i + " | Best Distance: " + String.format("%.2f", best.getDistance(hub, numberOfTrucks)));
            }
        }

        // 5. Финальный результат
        // ... (начало такое же)

        // 5. Результат
        RouteDNA finalSolution = pop.getFittest(hub, numberOfTrucks);
        System.out.println("\nOptimization Finished.");
        System.out.println("Final Optimized Distance: " + String.format("%.2f", finalSolution.getDistance(hub, numberOfTrucks)));

        finalSolution.printTruckRoutes(hub, numberOfTrucks);
    }
}