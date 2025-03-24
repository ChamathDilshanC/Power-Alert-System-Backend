package lk.ijse.poweralert.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.api.staticmap.v1.MapboxStaticMap;
import com.mapbox.geojson.*;
import lk.ijse.poweralert.entity.AlternativeResource;
import lk.ijse.poweralert.enums.AppEnums;
import lk.ijse.poweralert.repository.AlternativeResourceRepository;
import lk.ijse.poweralert.service.GeographicService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeographicServiceImpl implements GeographicService {

    private static final Logger logger = LoggerFactory.getLogger(GeographicServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Define map styles as constants since StaticMapCriteria is not available
    private static final String STREETS_STYLE = "streets-v11";
    private static final String SATELLITE_STYLE = "satellite-v9";
    private static final String OUTDOORS_STYLE = "outdoors-v11";
    private static final String LIGHT_STYLE = "light-v10";
    private static final String DARK_STYLE = "dark-v10";

    @Value("${mapbox.api-key}")
    private String mapboxApiKey;

    @Autowired
    private MapboxGeocoding.Builder mapboxGeocodingBuilder;

    @Autowired
    private MapboxStaticMap.Builder mapboxStaticMapBuilder;

    @Autowired
    private AlternativeResourceRepository alternativeResourceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public boolean isPointInPolygon(Point point, Polygon polygon) {
        try {
            // Convert MapBox Geometry to JTS Geometry
            org.locationtech.jts.geom.Point jtsPoint = geometryFactory.createPoint(
                    new Coordinate(point.longitude(), point.latitude()));

            List<List<Point>> coordinates = polygon.coordinates();
            if (coordinates.isEmpty()) {
                return false;
            }

            List<Point> outerRing = coordinates.get(0);
            Coordinate[] coords = new Coordinate[outerRing.size()];

            for (int i = 0; i < outerRing.size(); i++) {
                Point p = outerRing.get(i);
                coords[i] = new Coordinate(p.longitude(), p.latitude());
            }

            org.locationtech.jts.geom.LinearRing shell = geometryFactory.createLinearRing(coords);
            org.locationtech.jts.geom.Polygon jtsPolygon = geometryFactory.createPolygon(shell);

            return jtsPolygon.contains(jtsPoint);
        } catch (Exception e) {
            logger.error("Error checking if point is in polygon: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isAddressInOutageArea(double latitude, double longitude, String outageAreaJson) {
        try {
            Point point = Point.fromLngLat(longitude, latitude);
            Polygon polygon = geoJsonToPolygon(outageAreaJson);

            if (polygon == null) {
                return false;
            }

            return isPointInPolygon(point, polygon);
        } catch (Exception e) {
            logger.error("Error checking if address is in outage area: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Polygon geoJsonToPolygon(String geoJsonString) {
        try {
            // If the geoJson is a Feature or FeatureCollection, extract the geometry
            if (geoJsonString.contains("\"type\":\"Feature\"") ||
                    geoJsonString.contains("\"type\":\"FeatureCollection\"")) {

                if (geoJsonString.contains("\"type\":\"Feature\"")) {
                    Feature feature = Feature.fromJson(geoJsonString);
                    if (feature.geometry() instanceof Polygon) {
                        return (Polygon) feature.geometry();
                    }
                } else {
                    FeatureCollection featureCollection = FeatureCollection.fromJson(geoJsonString);
                    List<Feature> features = featureCollection.features();

                    if (features != null && !features.isEmpty()) {
                        Feature firstFeature = features.get(0);
                        if (firstFeature.geometry() instanceof Polygon) {
                            return (Polygon) firstFeature.geometry();
                        }
                    }
                }

                return null;
            } else {
                // Direct polygon GeoJSON
                return Polygon.fromJson(geoJsonString);
            }
        } catch (Exception e) {
            logger.error("Error converting GeoJSON to Polygon: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String polygonToGeoJson(Polygon polygon) {
        try {
            return polygon.toJson();
        } catch (Exception e) {
            logger.error("Error converting Polygon to GeoJSON: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String generateStaticMapUrl(String outageAreaJson, int width, int height) {
        return generateStaticMapUrl(outageAreaJson, width, height, STREETS_STYLE);
    }

    @Override
    public String generateStaticMapUrl(String outageAreaJson, int width, int height, String styleId) {
        try {
            // Parse the GeoJSON
            Polygon polygon = geoJsonToPolygon(outageAreaJson);

            if (polygon == null) {
                logger.warn("Failed to parse GeoJSON to polygon");
                return null;
            }

            // Calculate the center of the polygon
            List<Point> points = polygon.coordinates().get(0);
            if (points.isEmpty()) {
                logger.warn("Polygon has no coordinates");
                return null;
            }

            double sumLat = 0;
            double sumLon = 0;

            for (Point point : points) {
                sumLat += point.latitude();
                sumLon += point.longitude();
            }

            double centerLat = sumLat / points.size();
            double centerLon = sumLon / points.size();

            // Calculate appropriate zoom level based on bounding box
            double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;
            double minLon = Double.MAX_VALUE, maxLon = Double.MIN_VALUE;

            for (Point point : points) {
                minLat = Math.min(minLat, point.latitude());
                maxLat = Math.max(maxLat, point.latitude());
                minLon = Math.min(minLon, point.longitude());
                maxLon = Math.max(maxLon, point.longitude());
            }

            // Calculate zoom level - will adjust based on area's size
            double latDiff = maxLat - minLat;
            double lonDiff = maxLon - minLon;
            double maxDiff = Math.max(latDiff, lonDiff);

            // Determine zoom level based on the size of the area
            // Smaller values = more zoomed out
            int zoomLevel = 14; // default zoom
            if (maxDiff > 0.5) zoomLevel = 8;
            else if (maxDiff > 0.2) zoomLevel = 10;
            else if (maxDiff > 0.1) zoomLevel = 11;
            else if (maxDiff > 0.05) zoomLevel = 12;
            else if (maxDiff > 0.01) zoomLevel = 13;

            // Validate and use the provided style ID or fall back to default
            String validStyleId = styleId;
            if (styleId == null || styleId.isEmpty()) {
                validStyleId = STREETS_STYLE;
            }

            // Create a GeoJSON Feature for the polygon with styling
            Feature polygonFeature = Feature.fromGeometry(polygon);

            // Generate the static map URL
            MapboxStaticMap staticMap = mapboxStaticMapBuilder
                    .styleId(validStyleId)
                    .cameraPoint(Point.fromLngLat(centerLon, centerLat))
                    .cameraZoom(zoomLevel)
                    .width(width)
                    .height(height)
                    .geoJson(polygon)
                    .build();

            String url = staticMap.url().toString();
            logger.debug("Generated static map URL with style: {}, zoom: {}", validStyleId, zoomLevel);
            return url;
        } catch (Exception e) {
            logger.error("Error generating static map URL: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> findNearbyResources(double latitude, double longitude, double radiusKm, String resourceType) {
        try {
            // Get all resources
            List<AlternativeResource> allResources = alternativeResourceRepository.findByIsActiveTrue();

            // Filter by resource type if specified
            if (resourceType != null && !resourceType.isEmpty()) {
                try {
                    AppEnums.ResourceType type = AppEnums.ResourceType.valueOf(resourceType.toUpperCase());
                    allResources = allResources.stream()
                            .filter(r -> r.getType() == type)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid resource type: {}", resourceType);
                }
            }

            // Filter by distance
            List<Map<String, Object>> nearbyResources = new ArrayList<>();

            for (AlternativeResource resource : allResources) {
                double distance = calculateDistance(latitude, longitude,
                        resource.getLatitude(), resource.getLongitude());

                if (distance <= radiusKm) {
                    Map<String, Object> resourceMap = new HashMap<>();
                    resourceMap.put("id", resource.getId());
                    resourceMap.put("name", resource.getName());
                    resourceMap.put("type", resource.getType().name());
                    resourceMap.put("address", resource.getAddress());
                    resourceMap.put("latitude", resource.getLatitude());
                    resourceMap.put("longitude", resource.getLongitude());
                    resourceMap.put("distance", Math.round(distance * 10) / 10.0); // Round to 1 decimal place
                    resourceMap.put("operatingHours", resource.getOperatingHours());
                    resourceMap.put("contactNumber", resource.getContactNumber());

                    nearbyResources.add(resourceMap);
                }
            }

            // Sort by distance
            nearbyResources.sort((r1, r2) -> {
                Double d1 = (Double) r1.get("distance");
                Double d2 = (Double) r2.get("distance");
                return d1.compareTo(d2);
            });

            return nearbyResources;
        } catch (Exception e) {
            logger.error("Error finding nearby resources: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula to calculate distance between two points on Earth
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    @Override
    public Map<String, Double> geocodeAddress(String address) {
        try {
            MapboxGeocoding geocoding = mapboxGeocodingBuilder
                    .query(address)
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .build();

            Response<GeocodingResponse> response = geocoding.executeCall();

            if (!response.isSuccessful() || response.body() == null) {
                logger.error("Geocoding request failed: {}", response.message());
                return null;
            }

            List<CarmenFeature> results = response.body().features();

            if (results.isEmpty()) {
                logger.warn("No geocoding results found for address: {}", address);
                return null;
            }

            // Get the first result
            CarmenFeature feature = results.get(0);
            Point point = (Point) feature.geometry();

            Map<String, Double> coordinates = new HashMap<>();
            coordinates.put("latitude", point.latitude());
            coordinates.put("longitude", point.longitude());

            return coordinates;
        } catch (Exception e) {
            logger.error("Error geocoding address: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String reverseGeocode(double latitude, double longitude) {
        try {
            MapboxGeocoding geocoding = mapboxGeocodingBuilder
                    .query(Point.fromLngLat(longitude, latitude))
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .build();

            Response<GeocodingResponse> response = geocoding.executeCall();

            if (!response.isSuccessful() || response.body() == null) {
                logger.error("Reverse geocoding request failed: {}", response.message());
                return null;
            }

            List<CarmenFeature> results = response.body().features();

            if (results.isEmpty()) {
                logger.warn("No reverse geocoding results found for coordinates: {}, {}", latitude, longitude);
                return null;
            }

            // Get the first result
            CarmenFeature feature = results.get(0);
            return feature.placeName();
        } catch (Exception e) {
            logger.error("Error reverse geocoding: {}", e.getMessage(), e);
            return null;
        }
    }
}