package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.OutageGeospatialDTO;
import lk.ijse.poweralert.entity.Outage;

import java.util.List;
import java.util.Map;

/**
 * Service interface for outage geospatial operations
 */
public interface OutageGeospatialService {

    /**
     * Save geospatial data for an outage
     * @param outageGeospatialDTO the data to save
     * @return the saved data
     */
    OutageGeospatialDTO saveGeospatialData(OutageGeospatialDTO outageGeospatialDTO);

    /**
     * Get geospatial data for an outage
     * @param outageId the outage ID
     * @return the geospatial data
     */
    OutageGeospatialDTO getGeospatialDataForOutage(Long outageId);

    /**
     * Update geospatial data for an outage
     * @param outageGeospatialDTO the data to update
     * @return the updated data
     */
    OutageGeospatialDTO updateGeospatialData(OutageGeospatialDTO outageGeospatialDTO);

    /**
     * Delete geospatial data for an outage
     * @param outageId the outage ID
     * @return true if successful
     */
    boolean deleteGeospatialData(Long outageId);

    /**
     * Find outages that contain a specific point
     * @param latitude the latitude
     * @param longitude the longitude
     * @return list of outage IDs
     */
    List<Long> findOutagesContainingPoint(Double latitude, Double longitude);

    /**
     * Check if an address is affected by any active outages
     * @param latitude the latitude
     * @param longitude the longitude
     * @return map of outage IDs to affected status
     */
    Map<Long, Boolean> checkAddressForActiveOutages(Double latitude, Double longitude);

    /**
     * Generate or update the static map URL for an outage
     * @param outageId the outage ID
     * @return the static map URL
     */
    String generateStaticMapForOutage(Long outageId);

    /**
     * Compute the bounding box for a GeoJSON polygon
     * @param geoJson the GeoJSON string
     * @return map with north, south, east, west boundaries
     */
    Map<String, Double> computeBoundingBox(String geoJson);

    /**
     * Analyze geospatial data after outage creation or update
     * This should create or update the geospatial data automatically
     * @param outage the outage entity
     */
    void analyzeOutageGeospatialData(Outage outage);
}