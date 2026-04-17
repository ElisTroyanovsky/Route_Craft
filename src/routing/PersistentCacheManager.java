package routing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PersistentCacheManager {
    private static final String CACHE_FILE = "matrix_cache.json";
    private static Map<String, Double> cache = new HashMap<>();
    private static final Gson gson = new Gson();

    static {
        loadCache();
    }

    private static void loadCache() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            cache = gson.fromJson(reader, new TypeToken<Map<String, Double>>(){}.getType());
        } catch (IOException e) {
            System.err.println("Could not load cache: " + e.getMessage());
        }
    }

    public static synchronized void saveCache() {
        try (Writer writer = new FileWriter(CACHE_FILE)) {
            gson.toJson(cache, writer);
        } catch (IOException e) {
            System.err.println("Could not save cache: " + e.getMessage());
        }
    }

    public static Double get(double lat1, double lng1, double lat2, double lng2) {
        String key = makeKey(lat1, lng1, lat2, lng2);
        return cache.get(key);
    }

    public static void put(double lat1, double lng1, double lat2, double lng2, double distance) {
        cache.put(makeKey(lat1, lng1, lat2, lng2), distance);
    }

    private static String makeKey(double lat1, double lng1, double lat2, double lng2) {
        // Round to 5 decimal places to avoid duplicates due to precision errors
        return String.format("%.5f,%.5f:%.5f,%.5f", lat1, lng1, lat2, lng2);
    }
}