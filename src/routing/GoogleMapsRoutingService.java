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

    // google distance matrix api limits requests to 10 origins or destinations per call
    private static final int MAX_CHUNK_SIZE = 10;

    private static String getApiKey() {
        try {
            String content = Files.readString(Path.of("config.js"));
            String[] parts = content.split("GOOGLE_MAPS_API_KEY");
            if (parts.length > 1) {
                return parts[1].split("[\"']")[1];
            }
            throw new RuntimeException("API key not found!");
        } catch (Exception e) {
            System.err.println("❌ Error reading config.js: " + e.getMessage());
            return null;
        }
    }

    public static MatrixCache fetchDistanceMatrix(Location hub, List<Location> points) {
        MatrixCache cache = new MatrixCache();
        String apiKey = getApiKey();

        if (apiKey == null) return cache;

        // combine hub and delivery points into one list for matrix building
        List<Location> allLocations = new ArrayList<>();
        allLocations.add(hub);
        allLocations.addAll(points);

        // split into chunks so each api call stays within the size limit
        List<List<Location>> chunks = new ArrayList<>();
        for (int i = 0; i < allLocations.size(); i += MAX_CHUNK_SIZE) {
            chunks.add(allLocations.subList(i, Math.min(allLocations.size(), i + MAX_CHUNK_SIZE)));
        }

        System.out.println("📦 Total points: " + allLocations.size() + ". Checking cache...");

        for (List<Location> originsChunk : chunks) {
            for (List<Location> destinationsChunk : chunks) {

                // step a: check if every pair in this chunk is already in the persistent cache
                boolean allInCache = true;
                for (Location from : originsChunk) {
                    for (Location to : destinationsChunk) {
                        Double d = PersistentCacheManager.get(from.getX(), from.getY(), to.getX(), to.getY());
                        if (d == null) {
                            allInCache = false;
                            break;
                        }
                    }
                }

                if (allInCache) {
                    // all pairs already cached — load them into the in-memory cache and skip the api call
                    for (Location from : originsChunk) {
                        for (Location to : destinationsChunk) {
                            double d = PersistentCacheManager.get(from.getX(), from.getY(), to.getX(), to.getY());
                            cache.saveDistance(from.getId(), to.getId(), d);
                        }
                    }
                    continue;
                }

                // step b: not cached — fetch from the google distance matrix api
                System.out.println("🌍 Requesting Google API (chunk not found in cache)...");

                try {
                    String originsStr = buildCoordsString(originsChunk);
                    String destsStr = buildCoordsString(destinationsChunk);
                    String urlString = "https://maps.googleapis.com/maps/api/distancematrix/json?origins="
                            + originsStr + "&destinations=" + destsStr + "&key=" + apiKey;

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
                        String jsonResponse = scanner.hasNext() ? scanner.next() : "";
                        scanner.close();

                        // parse response and save to both in-memory cache and disk
                        parseAndSaveChunk(jsonResponse, originsChunk, destinationsChunk, cache);
                        PersistentCacheManager.saveCache();
                    }

                    Thread.sleep(200);

                } catch (Exception e) {
                    System.err.println("Error loading chunk: " + e.getMessage());
                }
            }
        }

        System.out.println("✅ Matrix is ready (caching used)");
        return cache;
    }

    private static String buildCoordsString(List<Location> chunk) throws Exception {
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < chunk.size(); i++) {
            coords.append(chunk.get(i).getX()).append(",").append(chunk.get(i).getY());
            if (i < chunk.size() - 1) coords.append("|");
        }
        return URLEncoder.encode(coords.toString(), StandardCharsets.UTF_8.toString());
    }

    private static void parseAndSaveChunk(String jsonResponse, List<Location> origins, List<Location> destinations, MatrixCache cache) {
        String[] rows = jsonResponse.split("\"elements\"\\s*:\\s*\\[");
        for (int i = 1; i < rows.length; i++) {
            String[] elements = rows[i].split("\"distance\"\\s*:\\s*\\{");
            Location fromPoint = origins.get(i - 1);

            for (int j = 1; j < elements.length; j++) {
                Location toPoint = destinations.get(j - 1);
                try {
                    String valuePart = elements[j].split("\"value\"\\s*:\\s*")[1].split("\\}")[0].trim();
                    double distanceKm = Double.parseDouble(valuePart) / 1000.0;

                    cache.saveDistance(fromPoint.getId(), toPoint.getId(), distanceKm);
                    PersistentCacheManager.put(fromPoint.getX(), fromPoint.getY(), toPoint.getX(), toPoint.getY(), distanceKm);

                } catch (Exception e) {
                    cache.saveDistance(fromPoint.getId(), toPoint.getId(), 9999.0);
                }
            }
        }
    }
}
