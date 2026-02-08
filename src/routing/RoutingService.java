package routing;
/**
 * External routing provider abstraction (Google Maps, mock, etc.).
 * Responsible for building a DistanceMatrix for a given Problem.
 *
 * TODO:
 * - Define buildMatrix(Problem) signature.
 * - Add rate-limiting and retry strategy (if using real API).
 * - Support caching integration.
 */
public interface RoutingService {
}
