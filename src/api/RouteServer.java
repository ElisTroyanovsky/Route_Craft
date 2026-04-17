package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import domain.Location;
import optimizer.GreedySolver;
import optimizer.TwoOptOptimizer; // Don't forget that this class must be created
import optimizer.aco.AntColonyOptimizer;
import optimizer.ga.GeneticAlgorithm;
import optimizer.ga.Population;
import optimizer.ga.RouteDNA;
import routing.GoogleMapsRoutingService;
import routing.MatrixCache;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class RouteServer {

    public static void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/api/optimize", new OptimizeHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server is running on http://localhost:63342/Route_Craft/index.html?_ijt=gomvlrdskf5qvb1kc291hi17v8&_ij_reload=RELOAD_ON_SAVE");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    static class OptimizeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                handlePostRequest(exchange);
            }
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                String jsonBody = new String(is.readAllBytes());

                // --- 1. DATA PREPARATION ---
                int trucks = Integer.parseInt(jsonBody.split("\"trucks\":")[1].split(",")[0].trim());
                Location hub = parseHub(jsonBody);
                List<Location> deliveryPoints = parseLocations(jsonBody);

                MatrixCache cache = GoogleMapsRoutingService.fetchDistanceMatrix(hub, deliveryPoints);

                // --- 2. SUPER-HYBRID CYCLE ---
                List<String> log = new ArrayList<>();

                // Greedy Baseline
                List<List<Location>> greedyRes = GreedySolver.solve(hub, deliveryPoints, trucks, cache);
                double greedyDistance = calculateTotalDistance(hub, greedyRes, cache);
                log.add("Greedy Baseline: " + String.format("%.2f", greedyDistance) + " km");

                AntColonyOptimizer aco = new AntColonyOptimizer(hub, deliveryPoints, cache, trucks);
                GeneticAlgorithm ga = new GeneticAlgorithm(hub, trucks, cache);

                RouteDNA globalBestDNA = null;
                double absoluteMinDist = Double.MAX_VALUE;

                // Your settings: 15 cycles
                for (int cycle = 1; cycle <= 15; cycle++) {
                    // A. ANT PHASE
                    List<RouteDNA> acoElite = new ArrayList<>();
                    for (int i = 0; i < 100; i++) {
                        acoElite = aco.runIteration();
                    }

                    // B. GENETIC PHASE
                    Population pop = new Population(100, true, deliveryPoints);
                    for (int i = 0; i < Math.min(20, acoElite.size()); i++) { // To use only the top 20 best results from the Ant Colony to form the initial Genetic Algorithm population (elitism from ACO to GA).
                        pop.saveTour(i, acoElite.get(i));
                    }

                    if (globalBestDNA != null) { // To preserve the best result found across all cycles (Global Elitism), ensuring we never lose the overall best solution.
                        pop.saveTour(21, globalBestDNA);
                    }

                    // 1000 generations
                    for (int gen = 0; gen < 1000; gen++) {
                        pop = ga.evolvePopulation(pop);
                    }

                    RouteDNA cycleBest = pop.getFittest(hub, trucks, cache);
                    double cycleDist = cycleBest.getDistance(hub, trucks, cache);

                    if (cycleDist < absoluteMinDist) {
                        absoluteMinDist = cycleDist;
                        globalBestDNA = cycleBest;
                        System.out.println("New Global Record: " + absoluteMinDist);
                    }

                    aco.reinforcePheromones(globalBestDNA);
                    log.add("Cycle " + cycle + " | Current: " + String.format("%.2f", cycleDist) + " km");
                }

                // --- 3. SEND RESULT (With final 2-Opt polishing) ---
                sendSuccessResponse(exchange, globalBestDNA, trucks, greedyDistance, log, cache, hub);

            } catch (Exception e) {
                e.printStackTrace();
                sendErrorResponse(exchange, "Optimization failed: " + e.getMessage());
            }
        }

        private void sendSuccessResponse(HttpExchange exchange, RouteDNA best, int trucks, double greedyDist, List<String> log, MatrixCache cache, Location hub) throws IOException {
            // Split the overall sequence into individual routes
            List<List<Location>> optimizedRoutes = new ArrayList<>();
            int stops = (int) Math.ceil((double)best.tourSize() / trucks);

            for (int t = 0; t < trucks; t++) {
                int start = t * stops;
                int end = Math.min(start + stops, best.tourSize());

                if (start < end) {
                    List<Location> rawRoute = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        rawRoute.add(best.getLocation(i));
                    }
                    // APPLY 2-OPT to each truck's route
                    List<Location> polished = TwoOptOptimizer.optimize(rawRoute, hub, cache);
                    optimizedRoutes.add(polished);
                }
            }

            // Recalculate distance AFTER 2-opt
            double finalDistAfter2Opt = calculateTotalDistance(hub, optimizedRoutes, cache);

            StringBuilder json = new StringBuilder("{\"status\": \"success\", ");
            json.append("\"greedyDistance\": ").append(greedyDist).append(", ");
            json.append("\"gaDistance\": ").append(finalDistAfter2Opt).append(", ");

            json.append("\"log\": [");
            for (int i = 0; i < log.size(); i++) {
                json.append("\"").append(log.get(i)).append("\"").append(i < log.size() - 1 ? "," : "");
            }
            json.append("], \"routes\": [");

            for (int t = 0; t < optimizedRoutes.size(); t++) {
                json.append("[");
                List<Location> route = optimizedRoutes.get(t);
                for (int i = 0; i < route.size(); i++) {
                    Location l = route.get(i);
                    json.append("{\"lat\": ").append(l.getX()).append(", \"lng\": ").append(l.getY()).append("}");
                    if (i < route.size() - 1) json.append(",");
                }
                json.append("]").append(t < optimizedRoutes.size() - 1 ? "," : "");
            }
            json.append("]}");

            byte[] res = json.toString().getBytes();
            exchange.sendResponseHeaders(200, res.length);
            exchange.getResponseBody().write(res);
            exchange.getResponseBody().close();
        }

        private double calculateTotalDistance(Location hub, List<List<Location>> routes, MatrixCache cache) {
            double total = 0;
            for (List<Location> r : routes) {
                Location curr = hub;
                for (Location l : r) {
                    total += cache.getDistance(curr, l);
                    curr = l;
                }
                total += cache.getDistance(curr, hub);
            }
            return total;
        }

        private Location parseHub(String json) {
            String hubPart = json.split("\"hub\":")[1].split("\\}")[0];
            double lat = Double.parseDouble(hubPart.split("\"lat\":\"")[1].split("\"")[0]);
            double lng = Double.parseDouble(hubPart.split("\"lng\":\"")[1].split("\"")[0]);
            return new Location("HUB", lat, lng);
        }

        private List<Location> parseLocations(String json) {
            List<Location> locs = new ArrayList<>();
            String part = json.split("\"locations\":\\[")[1].split("\\]")[0];
            for (String block : part.split("\\}")) {
                if (block.contains("\"id\"")) {
                    String id = block.split("\"id\":\"")[1].split("\"")[0];
                    double lat = Double.parseDouble(block.split("\"lat\":\"")[1].split("\"")[0]);
                    double lng = Double.parseDouble(block.split("\"lng\":\"")[1].split("\"")[0]);
                    locs.add(new Location(id, lat, lng));
                }
            }
            return locs;
        }

        private void sendErrorResponse(HttpExchange exchange, String msg) throws IOException {
            String error = "{\"status\": \"error\", \"message\": \"" + msg + "\"}";
            exchange.sendResponseHeaders(500, error.getBytes().length);
            exchange.getResponseBody().write(error.getBytes());
            exchange.getResponseBody().close();
        }
    }
}