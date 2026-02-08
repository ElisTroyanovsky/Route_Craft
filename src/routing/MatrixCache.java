package routing;
/**
 * Cache for DistanceMatrix results to reduce API costs and speed up repeated runs.
 * Can be implemented in-memory, file-based, or via storage layer.
 *
 * TODO:
 * - Define cache key (hash of locations + provider + options).
 * - Implement save/load (JSON/binary/DB).
 * - Add TTL / invalidation strategy (optional).
 */
public class MatrixCache {
}
