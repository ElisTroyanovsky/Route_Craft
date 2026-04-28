package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import domain.Location;
import optimizer.GreedySolver;
import optimizer.TwoOptOptimizer;
import optimizer.aco.AntColonyOptimizer;
import optimizer.ga.GeneticAlgorithm;
import optimizer.ga.Population;
import optimizer.ga.RouteDNA;
import routing.GoogleMapsRoutingService;
import routing.MatrixCache;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RouteServer {

    public static void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/api/optimize", new OptimizeHandler());
            server.createContext("/", new StaticFileHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("✅ Server is running → http://localhost:8080");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String fileName = path.equals("/") ? "index.html" : path.replaceFirst("^/", "");

            if (!fileName.equals("index.html") && !fileName.equals("config.js")) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            File file = new File(fileName);
            if (!file.exists()) {
                String msg = "File not found: " + fileName;
                exchange.sendResponseHeaders(404, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
                exchange.close();
                return;
            }

            String contentType = fileName.endsWith(".html") ? "text/html; charset=UTF-8" : "application/javascript";
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
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
                String jsonBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // --- Parse request using Gson (safe, works with any field order or special chars) ---
                JsonObject body = JsonParser.parseString(jsonBody).getAsJsonObject();
                int trucks = body.get("trucks").getAsInt();
                Location hub = parseHub(body.getAsJsonObject("hub"));
                List<Location> deliveryPoints = parseLocations(body.getAsJsonArray("locations"));

                MatrixCache cache = GoogleMapsRoutingService.fetchDistanceMatrix(hub, deliveryPoints);
                List<String> log = new ArrayList<>();

                // --- 1. Greedy Baseline ---
                List<List<Location>> greedyRes = GreedySolver.solve(hub, deliveryPoints, trucks, cache);
                double greedyDistance = calculateTotalDistance(hub, greedyRes, cache);
                log.add("Greedy Baseline: " + String.format("%.2f", greedyDistance) + " km");

                AntColonyOptimizer aco = new AntColonyOptimizer(hub, deliveryPoints, cache, trucks);
                GeneticAlgorithm ga = new GeneticAlgorithm(hub, trucks, cache);

                RouteDNA globalBestDNA = null;
                double absoluteMinDist = Double.MAX_VALUE;

                // Population lives outside the loop so GA evolution accumulates across cycles
                Population pop = new Population(100, true, deliveryPoints);

                // --- 2. Hybrid Optimization Loop (15 Cycles) ---
                for (int cycle = 1; cycle <= 15; cycle++) {
                    // A. ACO PHASE: ants explore routes and update pheromones
                    List<RouteDNA> acoElite = null;
                    for (int i = 0; i < 100; i++) {
                        acoElite = aco.runIteration();
                    }
                    double acoDist = acoElite.get(0).getDistance(hub, trucks, cache);

                    // B. Inject ACO elite into GA population (first 20 slots)
                    for (int i = 0; i < Math.min(20, acoElite.size()); i++) {
                        pop.saveTour(i, acoElite.get(i));
                    }
                    // Also keep the best result found so far (slot 20, after the 20 ACO elites)
                    if (globalBestDNA != null) {
                        pop.saveTour(20, globalBestDNA);
                    }

                    // C. GA PHASE: evolve the population
                    for (int gen = 0; gen < 1000; gen++) {
                        pop = ga.evolvePopulation(pop);
                    }

                    RouteDNA cycleBest = pop.getFittest(hub, trucks, cache);
                    double gaDist = cycleBest.getDistance(hub, trucks, cache);

                    if (gaDist < absoluteMinDist) {
                        absoluteMinDist = gaDist;
                        globalBestDNA = cycleBest;
                    }

                    // D. Feed GA's best result back into ACO pheromones (cross-pollination)
                    aco.reinforcePheromones(globalBestDNA);

                    if (cycle % 5 == 0) {
                        log.add(String.format("Cycle %d | ACO: %.2f -> GA: %.2f", cycle, acoDist, gaDist));
                    }
                }

                if (globalBestDNA == null) {
                    sendErrorResponse(exchange, "No valid route found");
                    return;
                }

                // --- 3. Final polish with 2-opt and send response ---
                sendSuccessResponse(exchange, globalBestDNA, trucks, greedyDistance, log, cache, hub);

            } catch (Exception e) {
                e.printStackTrace();
                sendErrorResponse(exchange, "Optimization failed: " + e.getMessage());
            }
        }

        private void sendSuccessResponse(HttpExchange exchange, RouteDNA best, int trucks, double greedyDist, List<String> log, MatrixCache cache, Location hub) throws IOException {
            // 1. Prepare raw routes from GA (Before 2-opt)
            List<List<Location>> gaRoutes = new ArrayList<>();
            int stops = (int) Math.ceil((double)best.tourSize() / trucks);

            for (int t = 0; t < trucks; t++) {
                int start = t * stops;
                int end = Math.min(start + stops, best.tourSize());
                if (start < end) {
                    List<Location> route = new ArrayList<>();
                    for (int i = start; i < end; i++) route.add(best.getLocation(i));
                    gaRoutes.add(route);
                }
            }
            double distGA = calculateTotalDistance(hub, gaRoutes, cache);

            // 2. Prepare polished routes (After 2-opt)
            List<List<Location>> polishedRoutes = new ArrayList<>();
            for (List<Location> route : gaRoutes) {
                polishedRoutes.add(TwoOptOptimizer.optimize(route, hub, cache));
            }
            double dist2Opt = calculateTotalDistance(hub, polishedRoutes, cache);

            // 3. Compare and pick the best overall result
            List<List<Location>> finalRoutes;
            double finalDist;

            if (dist2Opt < distGA) {
                finalRoutes = polishedRoutes;
                finalDist = dist2Opt;
                log.add("FINAL 2-opt: " + String.format("%.2f", finalDist) + " km (Improved!)");
            } else {
                finalRoutes = gaRoutes;
                finalDist = distGA;
                log.add("FINAL 2-opt: " + String.format("%.2f", finalDist) + " km (No changes)");
            }

            // 4. Build JSON Response
            StringBuilder json = new StringBuilder("{\"status\": \"success\", ");
            json.append("\"greedyDistance\": ").append(greedyDist).append(", ");
            json.append("\"gaDistance\": ").append(finalDist).append(", ");

            json.append("\"log\": [");
            for (int i = 0; i < log.size(); i++) {
                json.append("\"").append(log.get(i)).append("\"").append(i < log.size() - 1 ? "," : "");
            }
            json.append("], \"routes\": [");

            for (int t = 0; t < finalRoutes.size(); t++) {
                json.append("[");
                List<Location> route = finalRoutes.get(t);
                for (int i = 0; i < route.size(); i++) {
                    Location l = route.get(i);
                    json.append("{\"lat\": ").append(l.getX()).append(", \"lng\": ").append(l.getY()).append("}");
                    if (i < route.size() - 1) json.append(",");
                }
                json.append("]").append(t < finalRoutes.size() - 1 ? "," : "");
            }
            json.append("]}");

            byte[] res = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, res.length);
            try (var os = exchange.getResponseBody()) {
                os.write(res);
            }
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

        private Location parseHub(JsonObject hubJson) {
            double lat = hubJson.get("lat").getAsDouble();
            double lng = hubJson.get("lng").getAsDouble();
            return new Location("HUB", lat, lng);
        }

        private List<Location> parseLocations(JsonArray locationsJson) {
            List<Location> locs = new ArrayList<>();
            for (int i = 0; i < locationsJson.size(); i++) {
                JsonObject loc = locationsJson.get(i).getAsJsonObject();
                String id  = loc.get("id").getAsString();
                double lat = loc.get("lat").getAsDouble();
                double lng = loc.get("lng").getAsDouble();
                locs.add(new Location(id, lat, lng));
            }
            return locs;
        }

        private void sendErrorResponse(HttpExchange exchange, String msg) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] error = ("{\"status\": \"error\", \"message\": \"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, error.length);
            try (var os = exchange.getResponseBody()) {
                os.write(error);
            }
        }
    }
}