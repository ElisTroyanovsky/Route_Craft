package routing;
/**
 * Precomputed distances (and optionally durations) between all locations.
 * Built once via RoutingService to avoid repeated external API calls.
 *
 * TODO:
 * - Store mapping index -> Location (or id -> index).
 * - Provide getDistance(i, j) and optionally getTime(i, j).
 * - Decide storage type: double[][] or compressed structure.
 */
public class DistanceMatrix {
}
