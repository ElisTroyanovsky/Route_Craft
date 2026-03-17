package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import domain.Location;
import optimizer.ga.GeneticAlgorithm;
import optimizer.ga.Population;
import optimizer.ga.RouteDNA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            System.out.println("🚀 Local API Server is running on http://localhost:8080");
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
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String jsonBody = new String(is.readAllBytes());

                try {
                    // --- 1. ПАРСИМ JSON РУКАМИ (Быстрое решение для демо) ---
                    int trucks = Integer.parseInt(jsonBody.split("\"trucks\":")[1].split(",")[0].trim());

                    String hubPart = jsonBody.split("\"hub\":")[1].split("\\}")[0];
                    double hubLat = Double.parseDouble(hubPart.split("\"lat\":\"")[1].split("\"")[0]);
                    double hubLng = Double.parseDouble(hubPart.split("\"lng\":\"")[1].split("\"")[0]);
                    Location hub = new Location("HUB", hubLat, hubLng);

                    List<Location> deliveryPoints = new ArrayList<>();
                    String locsPart = jsonBody.split("\"locations\":\\[")[1].split("\\]")[0];
                    String[] locBlocks = locsPart.split("\\}");

                    for (String block : locBlocks) {
                        if (block.contains("\"id\"")) {
                            String id = block.split("\"id\":\"")[1].split("\"")[0];
                            double lat = Double.parseDouble(block.split("\"lat\":\"")[1].split("\"")[0]);
                            double lng = Double.parseDouble(block.split("\"lng\":\"")[1].split("\"")[0]);
                            deliveryPoints.add(new Location(id, lat, lng)); // Lat/Lng используем как X/Y
                        }
                    }

                    System.out.println("Data parsed! Running GA for " + deliveryPoints.size() + " points and " + trucks + " trucks...");

                    // --- 2. ЗАПУСКАЕМ ТВОЙ ГЕНЕТИЧЕСКИЙ АЛГОРИТМ ---
                    Population pop = new Population(100, true, deliveryPoints);
                    GeneticAlgorithm ga = new GeneticAlgorithm(hub, trucks);

                    // 500 поколений хватит, чтобы браузер не ждал слишком долго
                    for (int i = 0; i < 500; i++) {
                        pop = ga.evolvePopulation(pop);
                    }
                    RouteDNA bestRoute = pop.getFittest(hub, trucks);

                    // --- 3. ФОРМИРУЕМ ОТВЕТНЫЙ JSON ДЛЯ САЙТА ---
                    StringBuilder responseJson = new StringBuilder();
                    responseJson.append("{\"status\": \"success\", \"routes\": [");

                    int stopsPerTruck = (int) Math.ceil((double)bestRoute.tourSize() / trucks);
                    for (int t = 0; t < trucks; t++) {
                        responseJson.append("[");
                        int start = t * stopsPerTruck;
                        int end = Math.min(start + stopsPerTruck, bestRoute.tourSize());

                        for (int i = start; i < end; i++) {
                            Location loc = bestRoute.getLocation(i);
                            responseJson.append(String.format("{\"lat\": %s, \"lng\": %s}", loc.getX(), loc.getY()));
                            if (i < end - 1) responseJson.append(",");
                        }
                        responseJson.append("]");
                        if (t < trucks - 1) responseJson.append(",");
                    }
                    responseJson.append("]}");

                    // Отправляем ответ!
                    byte[] responseBytes = responseJson.toString().getBytes();
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();

                    System.out.println("Optimization complete. Routes sent to browser!");

                } catch (Exception e) {
                    e.printStackTrace();
                    String error = "{\"status\": \"error\", \"message\": \"Failed to parse or calculate\"}";
                    exchange.sendResponseHeaders(500, error.getBytes().length);
                    exchange.getResponseBody().write(error.getBytes());
                    exchange.getResponseBody().close();
                }
            }
        }
    }
}