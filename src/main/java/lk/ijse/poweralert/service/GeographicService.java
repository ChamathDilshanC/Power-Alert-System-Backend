package lk.ijse.poweralert.service;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.util.List;
import java.util.Map;

/**
 * Service interface for geographic operations
 */
public interface GeographicService {

    /**
     * Check if a point is inside a polygon
     *
     * @param point the point to check
     * @param polygon the polygon to check against
     * @return true if the point is inside the polygon
     */
    boolean isPointInPolygon(Point point, Polygon polygon);

    /**
     * Check if an address is in an outage area
     *
     * @param latitude the latitude of the address
     * @param longitude the longitude of the address
     * @param outageAreaJson the GeoJSON representing the outage area
     * @return true if the address is in the outage area
     */
    boolean isAddressInOutageArea(double latitude, double longitude, String outageAreaJson);

    /**
     * Convert GeoJSON string to a Polygon object
     *
     * @param geoJsonString the GeoJSON string
     * @return the Polygon object
     */
    Polygon geoJsonToPolygon(String geoJsonString);

    /**
     * Convert a Polygon object to GeoJSON string
     *
     * @param polygon the Polygon object
     * @return the GeoJSON string
     */
    String polygonToGeoJson(Polygon polygon);

    /**
     * Generate a static map URL for an outage area
     *
     * @param outageAreaJson the GeoJSON representing the outage area
     * @param width the width of the map image
     * @param height the height of the map image
     * @return the static map URL
     */
    String generateStaticMapUrl(String outageAreaJson, int width, int height);

    String generateStaticMapUrl(String outageAreaJson, int width, int height, String styleId);

    /**
     * Find nearby alternative resources based on location
     *
     * @param latitude the latitude of the location
     * @param longitude the longitude of the location
     * @param radiusKm the search radius in kilometers
     * @param resourceType the type of resource to find (optional)
     * @return list of nearby resources
     */
    List<Map<String, Object>> findNearbyResources(double latitude, double longitude, double radiusKm, String resourceType);

    /**
     * Calculate the distance between two points on Earth
     *
     * @param lat1 the latitude of the first point
     * @param lon1 the longitude of the first point
     * @param lat2 the latitude of the second point
     * @param lon2 the longitude of the second point
     * @return the distance in kilometers
     */
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);

    /**
     * Geocode an address to get coordinates
     *
     * @param address the address to geocode
     * @return map containing latitude and longitude
     */
    Map<String, Double> geocodeAddress(String address);

    /**
     * Reverse geocode coordinates to get an address
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @return the address
     */
    String reverseGeocode(double latitude, double longitude);
}