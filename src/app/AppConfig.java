package app;

public final class AppConfig {

    private AppConfig() {}

    public static String googleMapsApiKey() {
        String key = System.getenv("GOOGLE_MAPS_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("GOOGLE_MAPS_API_KEY is not set");
        }
        return key.trim();
    }
}