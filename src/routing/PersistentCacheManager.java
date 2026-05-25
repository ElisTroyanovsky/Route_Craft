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
        loadCache(); // load existing entries from disk on first class use
    }
/// loads matrix_cache.json from disc to HashMap<String, HashMap<String, Double>>.
    private static void loadCache() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            cache = gson.fromJson(reader, new TypeToken<Map<String, Double>>(){}.getType());
        } catch (IOException e) {
            System.err.println("Could not load cache: " + e.getMessage());
        }
    }
/// writes what is currently in the Hashmap to matrix_cache.json
    public static synchronized void saveCache() {
        try (Writer writer = new FileWriter(CACHE_FILE)) {
            gson.toJson(cache, writer);
        } catch (IOException e) {
            System.err.println("Could not save cache: " + e.getMessage());
        }
    }
/// gets the length from HashMap
    public static Double get(double lat1, double lng1, double lat2, double lng2) {
        String key = makeKey(lat1, lng1, lat2, lng2);
        return cache.get(key);
    }
/// puts the length from HashMap
    public static void put(double lat1, double lng1, double lat2, double lng2, double distance) {
        cache.put(makeKey(lat1, lng1, lat2, lng2), distance);
    }
/// creates the unique key for 2 places
    private static String makeKey(double lat1, double lng1, double lat2, double lng2) {
        // 5 decimal places (~1 m precision) prevents duplicate keys from floating-point noise
        return String.format("%.5f,%.5f:%.5f,%.5f", lat1, lng1, lat2, lng2);
    }
}