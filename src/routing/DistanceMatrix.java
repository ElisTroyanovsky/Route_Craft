package routing;

import domain.Location;

public interface DistanceMatrix {
    double getDistance(Location from, Location to);
}