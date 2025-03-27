package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.OutageGeospatialDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.OutageGeospatialService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for outage geospatial operations
 */
@RestController
@RequestMapping("/api/outages/geo")
@CrossOrigin
public class OutageGeospatialController {

    private static final Logger logger = LoggerFactory.getLogger(OutageGeospatialController.class);

    @Autowired
    private OutageGeospatialService outageGeospatialService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Save geospatial data for an outage
     * Requires admin or utility provider roles
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> saveGeospatialData(@Valid @RequestBody OutageGeospatialDTO outageGeospatialDTO) {
        try {
            logger.info("Saving geospatial data for outage ID: {}", outageGeospatialDTO.getOutageId());

            OutageGeospatialDTO savedData = outageGeospatialService.saveGeospatialData(outageGeospatialDTO);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Geospatial data saved successfully");
            responseDTO.setData(savedData);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error saving geospatial data: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get geospatial data for an outage
     * Public endpoint
     */
    @GetMapping("/{outageId}")
    public ResponseEntity<ResponseDTO> getGeospatialData(@PathVariable Long outageId) {
        try {
            logger.info("Getting geospatial data for outage ID: {}", outageId);

            OutageGeospatialDTO geospatialData = outageGeospatialService.getGeospatialDataForOutage(outageId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Geospatial data retrieved successfully");
            responseDTO.setData(geospatialData);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error getting geospatial data for outage ID {}: {}", outageId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update geospatial data for an outage
     * Requires admin or utility provider roles
     */
    @PutMapping("/{outageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> updateGeospatialData(
            @PathVariable Long outageId,
            @Valid @RequestBody OutageGeospatialDTO outageGeospatialDTO) {
        try {
            logger.info("Updating geospatial data for outage ID: {}", outageId);

            // Ensure IDs match
            outageGeospatialDTO.setOutageId(outageId);

            OutageGeospatialDTO updatedData = outageGeospatialService.updateGeospatialData(outageGeospatialDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Geospatial data updated successfully");
            responseDTO.setData(updatedData);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating geospatial data for outage ID {}: {}", outageId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete geospatial data for an outage
     * Requires admin or utility provider roles
     */
    @DeleteMapping("/{outageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> deleteGeospatialData(@PathVariable Long outageId) {
        try {
            logger.info("Deleting geospatial data for outage ID: {}", outageId);

            boolean deleted = outageGeospatialService.deleteGeospatialData(outageId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Geospatial data deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting geospatial data for outage ID {}: {}", outageId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Find outages that contain a specific point
     * Public endpoint
     */
    @GetMapping("/find")
    public ResponseEntity<ResponseDTO> findOutagesContainingPoint(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            logger.info("Finding outages containing point: {}, {}", latitude, longitude);

            List<Long> outageIds = outageGeospatialService.findOutagesContainingPoint(latitude, longitude);

            Map<String, Object> result = new HashMap<>();
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            result.put("outageIds", outageIds);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outages found successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error finding outages containing point: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check if an address is affected by any active outages
     * Public endpoint
     */
    @GetMapping("/check-address")
    public ResponseEntity<ResponseDTO> checkAddressForActiveOutages(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        try {
            logger.info("Checking address for active outages: {}, {}", latitude, longitude);

            Map<Long, Boolean> affectedStatus = outageGeospatialService.checkAddressForActiveOutages(latitude, longitude);

            Map<String, Object> result = new HashMap<>();
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            result.put("affectedStatus", affectedStatus);
            result.put("isAffected", affectedStatus.containsValue(true));

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Address checked successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error checking address for active outages: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate static map for an outage
     * Public endpoint
     */
    @GetMapping("/{outageId}/static-map")
    public ResponseEntity<ResponseDTO> generateStaticMap(@PathVariable Long outageId) {
        try {
            logger.info("Generating static map for outage ID: {}", outageId);

            String staticMapUrl = outageGeospatialService.generateStaticMapForOutage(outageId);

            Map<String, String> result = new HashMap<>();
            result.put("outageId", outageId.toString());
            result.put("staticMapUrl", staticMapUrl);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Static map generated successfully");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating static map for outage ID {}: {}", outageId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Compute bounding box for a GeoJSON polygon
     * Admin and utility provider endpoint
     */
    @PostMapping("/compute-bounds")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> computeBoundingBox(@RequestBody Map<String, String> request) {
        try {
            logger.info("Computing bounding box for GeoJSON");

            String geoJson = request.get("geoJson");
            if (geoJson == null || geoJson.isEmpty()) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("GeoJSON is required");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            Map<String, Double> boundingBox = outageGeospatialService.computeBoundingBox(geoJson);

            if (boundingBox == null) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Could not compute bounding box from provided GeoJSON");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Bounding box computed successfully");
            responseDTO.setData(boundingBox);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error computing bounding box: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}