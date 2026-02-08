package domain;

/**
 * Immutable (or mostly immutable) geographic point used by routing and optimization.
 * Represents a delivery stop (customer) or a depot (base class).
 *
 * TODO:
 * - Decide identifier strategy (int index vs UUID/string).
 * - Add optional fields: address, demand, serviceTime, tags.
 * - Implement equals/hashCode based on id.
 */

public class Location {
    private int locationID;
    private String locationName;
    private double longitude;
    private double latitude;
    private static int idCounter = 0; // location 0 is  always hub
    public Location(String locationName, double longitude, double latitude) {
        this.locationID = idCounter++;
        this.locationName = locationName;
        this.longitude = longitude;
        this.latitude = latitude;
    }
    public int getLocationID() {
        return locationID;
    }
    public String getLocationName() {
        return locationName;
    }
    public double getLongitude() {
        return longitude;
    }
    public double getLatitude() {
        return latitude;
    }
}
