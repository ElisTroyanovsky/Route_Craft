package routing;

import domain.Location;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GoogleMapsRoutingService {

    // Лимит Google для бесплатных аккаунтов: 100 элементов за 1 запрос (10x10)
    private static final int MAX_CHUNK_SIZE = 10;

    private static String getApiKey() {
        try {
            String content = Files.readString(Path.of("config.js"));
            String[] parts = content.split("GOOGLE_MAPS_API_KEY");
            if (parts.length > 1) {
                return parts[1].split("[\"']")[1];
            }
            throw new RuntimeException("Ключ не найден!");
        } catch (Exception e) {
            System.err.println("❌ Ошибка чтения config.js: " + e.getMessage());
            return null;
        }
    }

    public static MatrixCache fetchDistanceMatrix(Location hub, List<Location> points) {
        MatrixCache cache = new MatrixCache();
        String apiKey = getApiKey();

        if (apiKey == null) return cache;

        List<Location> allLocations = new ArrayList<>();
        allLocations.add(hub);
        allLocations.addAll(points);

        // 1. Дробим общий список на мелкие пачки (чанки)
        List<List<Location>> chunks = new ArrayList<>();
        for (int i = 0; i < allLocations.size(); i += MAX_CHUNK_SIZE) {
            chunks.add(allLocations.subList(i, Math.min(allLocations.size(), i + MAX_CHUNK_SIZE)));
        }

        System.out.println("📦 Точек всего: " + allLocations.size() + ". Разбито на " + chunks.size() + " пачек(и).");

        int requestCount = 1;
        int totalRequests = chunks.size() * chunks.size();

        // 2. Двойной цикл: перебираем каждую пачку с каждой пачкой
        for (List<Location> originsChunk : chunks) {
            for (List<Location> destinationsChunk : chunks) {

                System.out.println("⏳ Отправка запроса " + requestCount + " из " + totalRequests + "...");

                try {
                    String originsStr = buildCoordsString(originsChunk);
                    String destsStr = buildCoordsString(destinationsChunk);

                    String urlString = "https://maps.googleapis.com/maps/api/distancematrix/json?origins="
                            + originsStr + "&destinations=" + destsStr + "&key=" + apiKey;

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() != 200) {
                        System.err.println("Google API Error: " + conn.getResponseCode());
                        continue; // Если ошибка - просто пропускаем этот кусок
                    }

                    Scanner scanner = new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A");
                    String jsonResponse = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    // 3. Парсим именно этот кусок и кладем в общий Кэш
                    parseAndSaveChunk(jsonResponse, originsChunk, destinationsChunk, cache);

                    // 4. ВАЖНО: Делаем паузу, чтобы Google не забанил за DDoS
                    Thread.sleep(200);

                } catch (Exception e) {
                    System.err.println("Ошибка при загрузке пачки: " + e.getMessage());
                }
                requestCount++;
            }
        }

        System.out.println("✅ Полная матрица расстояний собрана в кэш!");
        return cache;
    }

    // Вспомогательный метод для сборки координат в строку
    private static String buildCoordsString(List<Location> chunk) throws Exception {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < chunk.size(); i++) {
            coords.append(chunk.get(i).getX()).append(",").append(chunk.get(i).getY());
            if (i < chunk.size() - 1) coords.append("|");
        }
        return URLEncoder.encode(coords.toString(), StandardCharsets.UTF_8.toString());
    }

    // Вспомогательный метод для парсинга куска JSON
    private static void parseAndSaveChunk(String jsonResponse, List<Location> origins, List<Location> destinations, MatrixCache cache) {
        String[] rows = jsonResponse.split("\"elements\"\\s*:\\s*\\[");
        for (int i = 1; i < rows.length; i++) {
            String[] elements = rows[i].split("\"distance\"\\s*:\\s*\\{");
            Location fromPoint = origins.get(i - 1);

            for (int j = 1; j < elements.length; j++) {
                Location toPoint = destinations.get(j - 1);
                try {
                    String valuePart = elements[j].split("\"value\"\\s*:\\s*")[1].split("\\}")[0].trim();
                    double distanceMeters = Double.parseDouble(valuePart);
                    cache.saveDistance(fromPoint.getId(), toPoint.getId(), distanceMeters / 1000.0);
                } catch (Exception e) {
                    cache.saveDistance(fromPoint.getId(), toPoint.getId(), 999999.0);
                }
            }
        }
    }
}