package routing;
/**
 * Google Maps implementation of RoutingService.
 * Queries the API to build a full DistanceMatrix for depot + customers.
 *
 * TODO:
 * - Add API key handling (env/config, never commit keys).
 * - Implement batching and quota-aware requests.
 * - Parse response into DistanceMatrix and handle errors gracefully.
 */

public class GoogleMapsRoutingService implements RoutingService {

    private final String apiKey;

    public GoogleMapsRoutingService(String apiKey) {
        this.apiKey = apiKey;
    }
}
