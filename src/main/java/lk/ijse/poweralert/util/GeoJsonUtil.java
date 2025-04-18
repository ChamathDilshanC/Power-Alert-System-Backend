package lk.ijse.poweralert.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Utility class for GeoJSON operations
 */
public class GeoJsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(GeoJsonUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Simplify polygon by reducing the number of points
     * This is useful for reducing the size of GeoJSON for static maps
     *
     * @param polygon The polygon to simplify
     * @param tolerance The simplification tolerance (higher = more points removed)
     * @return Simplified polygon
     */
    public static Polygon simplifyPolygon(Polygon polygon, double tolerance) {
        if (polygon == null) {
            return null;
        }

        List<List<Point>> coordinates = polygon.coordinates();
        if (coordinates.isEmpty() || coordinates.get(0).isEmpty()) {
            return polygon;
        }

        // Simplify only the outer ring (first list of points)
        List<Point> outerRing = coordinates.get(0);
        List<Point> simplifiedRing = simplifyPoints(outerRing, tolerance);

        // Ensure the simplified ring has at least 4 points (3 + closing point)
        if (simplifiedRing.size() < 4) {
            return polygon; // Return original if too few points
        }

        // Create a new polygon with simplified outer ring
        List<List<Point>> simplifiedCoordinates = new ArrayList<>();
        simplifiedCoordinates.add(simplifiedRing);

        // Add any inner rings (holes) unchanged
        for (int i = 1; i < coordinates.size(); i++) {
            simplifiedCoordinates.add(coordinates.get(i));
        }

        return Polygon.fromLngLats(simplifiedCoordinates);
    }

    /**
     * Simplify a list of points using the Ramer-Douglas-Peucker algorithm
     *
     * @param points The points to simplify
     * @param tolerance The simplification tolerance
     * @return Simplified list of points
     */
    private static List<Point> simplifyPoints(List<Point> points, double tolerance) {
        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }

        // Find the point with the maximum distance
        double maxDistance = 0;
        int index = 0;
        for (int i = 1; i < points.size() - 1; i++) {
            double distance = perpendicularDistance(points.get(i),
                    points.get(0), points.get(points.size() - 1));
            if (distance > maxDistance) {
                maxDistance = distance;
                index = i;
            }
        }

        // If max distance is greater than tolerance, recursively simplify
        List<Point> result = new ArrayList<>();
        if (maxDistance > tolerance) {
            // Recursive call
            List<Point> firstPart = simplifyPoints(
                    points.subList(0, index + 1), tolerance);
            List<Point> secondPart = simplifyPoints(
                    points.subList(index, points.size()), tolerance);

            // Build the result list
            result.addAll(firstPart.subList(0, firstPart.size() - 1));
            result.addAll(secondPart);
        } else {
            // Below tolerance, return just the endpoints
            result.add(points.get(0));
            result.add(points.get(points.size() - 1));
        }

        return result;
    }

    /**
     * Calculate the perpendicular distance from a point to a line
     *
     * @param point The point
     * @param lineStart Start of the line
     * @param lineEnd End of the line
     * @return Distance in degrees
     */
    private static double perpendicularDistance(Point point, Point lineStart, Point lineEnd) {
        double x = point.longitude();
        double y = point.latitude();
        double x1 = lineStart.longitude();
        double y1 = lineStart.latitude();
        double x2 = lineEnd.longitude();
        double y2 = lineEnd.latitude();

        // Line case
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;

        // Handle special case of vertical line to avoid division by zero
        if (Math.abs(deltaX) < 1e-9) {
            return Math.abs(x - x1);
        }

        // Handle special case of horizontal line to avoid division by zero
        if (Math.abs(deltaY) < 1e-9) {
            return Math.abs(y - y1);
        }

        // Compute shortest distance using Perpendicular distance formula
        double normalLength = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return Math.abs((deltaY * x - deltaX * y + x2 * y1 - y2 * x1) / normalLength);
    }

    /**
     * Add style properties to a GeoJSON Feature or FeatureCollection
     *
     * @param geoJson The GeoJSON string
     * @param fillColor The fill color (e.g., "#FF0000")
     * @param strokeColor The stroke color (e.g., "#0000FF")
     * @param strokeWidth The stroke width
     * @param fillOpacity The fill opacity (0.0 to 1.0)
     * @return Styled GeoJSON string
     */
    public static String addStyleToGeoJson(String geoJson, String fillColor, String strokeColor,
                                           double strokeWidth, double fillOpacity) {
        try {
            JsonNode jsonNode = objectMapper.readTree(geoJson);

            // Handle both Feature and FeatureCollection
            if (jsonNode.has("type")) {
                String type = jsonNode.get("type").asText();

                if ("Feature".equals(type)) {
                    // Add style to a single feature
                    addStyleToFeature((ObjectNode) jsonNode, fillColor, strokeColor, strokeWidth, fillOpacity);
                    return jsonNode.toString();
                } else if ("FeatureCollection".equals(type)) {
                    // Add style to all features in the collection
                    ArrayNode features = (ArrayNode) jsonNode.get("features");
                    for (int i = 0; i < features.size(); i++) {
                        addStyleToFeature((ObjectNode) features.get(i), fillColor, strokeColor, strokeWidth, fillOpacity);
                    }
                    return jsonNode.toString();
                }
            }

            // Return original if not a recognized GeoJSON type
            return geoJson;
        } catch (IOException e) {
            logger.error("Error adding style to GeoJSON: {}", e.getMessage(), e);
            return geoJson; // Return original on error
        }
    }

    /**
     * Add style properties to a Feature node
     */
    private static void addStyleToFeature(ObjectNode feature, String fillColor, String strokeColor,
                                          double strokeWidth, double fillOpacity) {
        if (!feature.has("properties") || feature.get("properties").isNull()) {
            feature.set("properties", objectMapper.createObjectNode());
        }

        ObjectNode properties = (ObjectNode) feature.get("properties");

        // Add style properties
        if (fillColor != null) properties.put("fill", fillColor);
        if (strokeColor != null) properties.put("stroke", strokeColor);
        if (strokeWidth > 0) properties.put("stroke-width", strokeWidth);
        if (fillOpacity >= 0 && fillOpacity <= 1) properties.put("fill-opacity", fillOpacity);
    }

    /**
     * Calculate the center point of a polygon
     *
     * @param polygon The polygon
     * @return Center point
     */
    public static Point calculatePolygonCenter(Polygon polygon) {
        if (polygon == null || polygon.coordinates().isEmpty() || polygon.coordinates().get(0).isEmpty()) {
            return null;
        }

        List<Point> points = polygon.coordinates().get(0);
        double sumLat = 0;
        double sumLon = 0;

        for (Point point : points) {
            sumLat += point.latitude();
            sumLon += point.longitude();
        }

        return Point.fromLngLat(sumLon / points.size(), sumLat / points.size());
    }

    /**
     * Calculate the bounding box of a polygon
     *
     * @param polygon The polygon
     * @return Map with north, south, east, west coordinates
     */
    public static Map<String, Double> calculateBoundingBox(Polygon polygon) {
        if (polygon == null || polygon.coordinates().isEmpty() || polygon.coordinates().get(0).isEmpty()) {
            return null;
        }

        List<Point> points = polygon.coordinates().get(0);
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;

        for (Point point : points) {
            minLat = Math.min(minLat, point.latitude());
            maxLat = Math.max(maxLat, point.latitude());
            minLon = Math.min(minLon, point.longitude());
            maxLon = Math.max(maxLon, point.longitude());
        }

        Map<String, Double> boundingBox = new HashMap<>();
        boundingBox.put("north", maxLat);
        boundingBox.put("south", minLat);
        boundingBox.put("east", maxLon);
        boundingBox.put("west", minLon);

        return boundingBox;
    }

    /**
     * Create a polygon from points
     *
     * @param points List of points (latitude, longitude pairs)
     * @return Polygon
     */
    public static Polygon createPolygonFromPoints(List<double[]> points) {
        if (points.size() < 3) {
            return null;
        }

        List<Point> polygonPoints = new ArrayList<>();
        for (double[] point : points) {
            polygonPoints.add(Point.fromLngLat(point[1], point[0])); // Note: [lat, lon] -> [lon, lat]
        }

        // Close the polygon by adding the first point at the end
        if (!polygonPoints.get(0).equals(polygonPoints.get(polygonPoints.size() - 1))) {
            polygonPoints.add(polygonPoints.get(0));
        }

        List<List<Point>> coordinates = Collections.singletonList(polygonPoints);
        return Polygon.fromLngLats(coordinates);
    }

    /**
     * Create a GeoJSON Feature from a Polygon with properties
     *
     * @param polygon The polygon
     * @param properties Map of properties
     * @return Feature
     */
    public static Feature createFeatureFromPolygon(Polygon polygon, Map<String, Object> properties) {
        if (polygon == null) {
            return null;
        }

        // Create feature from polygon
        Feature feature = Feature.fromGeometry(polygon);

        // Add properties if provided
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                feature.addStringProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        return feature;
    }
}