package lk.ijse.poweralert.controller;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.GeographicService;
import lk.ijse.poweralert.util.GeoJsonUtil;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for geographic operations
 */
@RestController
@RequestMapping("/api/geo")
public class GeographicController {

    private static final Logger logger = LoggerFactory.getLogger(GeographicController.class);

    @Autowired
    private GeographicService geographicService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Geocode an address to coordinates
     */
    @GetMapping("/geocode")
    public ResponseEntity<ResponseDTO> geocodeAddress(@RequestParam String address) {
        try {
            logger.info("Geocoding address: {}", address);

            Map<String, Double> coordinates = geographicService.geocodeAddress(address);

            if (coordinates == null) {
                responseDTO.setCode(VarList.Not_Found);
                responseDTO.setMessage("No coordinates found for this address");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Address geocoded successfully");
            responseDTO.setData(coordinates);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error geocoding address: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reverse geocode coordinates to address
     */
    @GetMapping("/reverse-geocode")
    public ResponseEntity<ResponseDTO> reverseGeocode(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            logger.info("Reverse geocoding coordinates: {}, {}", latitude, longitude);

            String address = geographicService.reverseGeocode(latitude, longitude);

            if (address == null) {
                responseDTO.setCode(VarList.Not_Found);
                responseDTO.setMessage("No address found for these coordinates");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            Map<String, String> result = new HashMap<>();
            result.put("address", address);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Coordinates reverse geocoded successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error reverse geocoding: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get static map URL for an outage area
     */
    @GetMapping("/static-map")
    public ResponseEntity<ResponseDTO> getStaticMapUrl(
            @RequestParam String geoJson,
            @RequestParam(defaultValue = "600") int width,
            @RequestParam(defaultValue = "400") int height,
            @RequestParam(required = false) String style) {
        try {
            logger.info("Generating static map URL for GeoJSON with style: {}", style);

            String mapUrl;
            if (style != null && !style.isEmpty()) {
                mapUrl = geographicService.generateStaticMapUrl(geoJson, width, height, style);
            } else {
                mapUrl = geographicService.generateStaticMapUrl(geoJson, width, height);
            }

            if (mapUrl == null) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Could not generate map from provided GeoJSON");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("mapUrl", mapUrl);
            result.put("width", width);
            result.put("height", height);
            if (style != null) {
                result.put("style", style);
            }

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Static map URL generated successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating static map URL: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check if a location is in an outage area
     */
    @GetMapping("/check-location")
    public ResponseEntity<ResponseDTO> checkLocationInOutageArea(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam String outageAreaJson) {
        try {
            logger.info("Checking if location is in outage area: {}, {}", latitude, longitude);

            boolean isInArea = geographicService.isAddressInOutageArea(latitude, longitude, outageAreaJson);

            Map<String, Object> result = new HashMap<>();
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            result.put("isInOutageArea", isInArea);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Location checked successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error checking location in outage area: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Find nearby alternative resources
     */
    @GetMapping("/nearby-resources")
    public ResponseEntity<ResponseDTO> findNearbyResources(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm,
            @RequestParam(required = false) String resourceType) {
        try {
            logger.info("Finding nearby resources within {}km of {}, {}", radiusKm, latitude, longitude);

            List<Map<String, Object>> resources = geographicService.findNearbyResources(
                    latitude, longitude, radiusKm, resourceType);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Nearby resources found successfully");
            responseDTO.setData(resources);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error finding nearby resources: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Calculate distance between two points
     */
    @GetMapping("/distance")
    public ResponseEntity<ResponseDTO> calculateDistance(
            @RequestParam double lat1,
            @RequestParam double lon1,
            @RequestParam double lat2,
            @RequestParam double lon2) {
        try {
            logger.info("Calculating distance between points");

            double distance = geographicService.calculateDistance(lat1, lon1, lat2, lon2);

            Map<String, Object> result = new HashMap<>();
            result.put("startPoint", Map.of("latitude", lat1, "longitude", lon1));
            result.put("endPoint", Map.of("latitude", lat2, "longitude", lon2));
            result.put("distanceKm", Math.round(distance * 100) / 100.0); // Round to 2 decimal places

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Distance calculated successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error calculating distance: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convert GeoJSON to Polygon
     */
    @PostMapping("/convert-geojson")
    public ResponseEntity<ResponseDTO> convertGeoJsonToPolygon(@RequestBody String geoJson) {
        try {
            logger.info("Converting GeoJSON to Polygon");

            Polygon polygon = geographicService.geoJsonToPolygon(geoJson);

            if (polygon == null) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Invalid GeoJSON format or not a polygon");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            // Calculate additional information about the polygon
            Map<String, Object> result = new HashMap<>();

            // Get coordinates
            List<List<Point>> coordinates = polygon.coordinates();
            result.put("coordinateCount", coordinates.get(0).size());

            // Calculate bounding box
            Map<String, Double> boundingBox = GeoJsonUtil.calculateBoundingBox(polygon);
            result.put("boundingBox", boundingBox);

            // Calculate center
            Point center = GeoJsonUtil.calculatePolygonCenter(polygon);
            if (center != null) {
                result.put("center", Map.of(
                        "latitude", center.latitude(),
                        "longitude", center.longitude()
                ));
            }

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("GeoJSON converted to Polygon successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error converting GeoJSON to Polygon: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Simplify GeoJSON Polygon
     */
    @PostMapping("/simplify-geojson")
    public ResponseEntity<ResponseDTO> simplifyGeoJson(
            @RequestBody String geoJson,
            @RequestParam(defaultValue = "0.0001") double tolerance) {
        try {
            logger.info("Simplifying GeoJSON Polygon with tolerance: {}", tolerance);

            // Convert to Polygon
            Polygon polygon = geographicService.geoJsonToPolygon(geoJson);

            if (polygon == null) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Invalid GeoJSON format or not a polygon");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            // Get original point count
            int originalPointCount = polygon.coordinates().get(0).size();

            // Simplify polygon
            Polygon simplifiedPolygon = GeoJsonUtil.simplifyPolygon(polygon, tolerance);

            // Get simplified point count
            int simplifiedPointCount = simplifiedPolygon.coordinates().get(0).size();

            // Convert back to GeoJSON
            String simplifiedGeoJson = geographicService.polygonToGeoJson(simplifiedPolygon);

            Map<String, Object> result = new HashMap<>();
            result.put("originalGeoJson", geoJson);
            result.put("simplifiedGeoJson", simplifiedGeoJson);
            result.put("originalPointCount", originalPointCount);
            result.put("simplifiedPointCount", simplifiedPointCount);
            result.put("reductionPercentage",
                    Math.round((1 - ((double) simplifiedPointCount / originalPointCount)) * 100) + "%");

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("GeoJSON simplified successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error simplifying GeoJSON: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Style GeoJSON
     */
    @PostMapping("/style-geojson")
    public ResponseEntity<ResponseDTO> styleGeoJson(
            @RequestBody String geoJson,
            @RequestParam(defaultValue = "#FF0000") String fillColor,
            @RequestParam(defaultValue = "#000000") String strokeColor,
            @RequestParam(defaultValue = "2.0") double strokeWidth,
            @RequestParam(defaultValue = "0.5") double fillOpacity) {
        try {
            logger.info("Styling GeoJSON");

            String styledGeoJson = GeoJsonUtil.addStyleToGeoJson(
                    geoJson, fillColor, strokeColor, strokeWidth, fillOpacity);

            Map<String, Object> result = new HashMap<>();
            result.put("originalGeoJson", geoJson);
            result.put("styledGeoJson", styledGeoJson);
            result.put("style", Map.of(
                    "fillColor", fillColor,
                    "strokeColor", strokeColor,
                    "strokeWidth", strokeWidth,
                    "fillOpacity", fillOpacity
            ));

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("GeoJSON styled successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error styling GeoJSON: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}