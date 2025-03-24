package lk.ijse.poweralert.service.impl;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import lk.ijse.poweralert.dto.OutageGeospatialDTO;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.OutageGeospatialData;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.repository.OutageGeospatialRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.service.GeographicService;
import lk.ijse.poweralert.service.OutageGeospatialService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutageGeospatialServiceImpl implements OutageGeospatialService {

    private static final Logger logger = LoggerFactory.getLogger(OutageGeospatialServiceImpl.class);

    @Autowired
    private OutageGeospatialRepository outageGeospatialRepository;

    @Autowired
    private OutageRepository outageRepository;

    @Autowired
    private GeographicService geographicService;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${mapbox.static-map.width:800}")
    private int staticMapWidth;

    @Value("${mapbox.static-map.height:600}")
    private int staticMapHeight;

    @Override
    @Transactional
    public OutageGeospatialDTO saveGeospatialData(OutageGeospatialDTO outageGeospatialDTO) {
        logger.info("Saving geospatial data for outage ID: {}", outageGeospatialDTO.getOutageId());

        // Verify outage exists
        Outage outage = outageRepository.findById(outageGeospatialDTO.getOutageId())
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageGeospatialDTO.getOutageId()));

        // Check if data already exists for this outage
        Optional<OutageGeospatialData> existingData = outageGeospatialRepository.findByOutageId(outage.getId());
        if (existingData.isPresent()) {
            logger.warn("Geospatial data already exists for outage ID: {}, updating instead", outage.getId());
            return updateGeospatialData(outageGeospatialDTO);
        }

        // Create new entity
        OutageGeospatialData geospatialData = new OutageGeospatialData();
        geospatialData.setOutage(outage);
        geospatialData.setGeoJson(outageGeospatialDTO.getGeoJson());

        // Compute center and bounding box if not provided
        if (outageGeospatialDTO.getCenterLatitude() == null || outageGeospatialDTO.getCenterLongitude() == null ||
                outageGeospatialDTO.getBoundingBoxNorth() == null) {

            Map<String, Double> boundingBox = computeBoundingBox(outageGeospatialDTO.getGeoJson());
            if (boundingBox != null) {
                if (outageGeospatialDTO.getCenterLatitude() == null) {
                    double centerLat = (boundingBox.get("north") + boundingBox.get("south")) / 2;
                    geospatialData.setCenterLatitude(centerLat);
                }

                if (outageGeospatialDTO.getCenterLongitude() == null) {
                    double centerLon = (boundingBox.get("east") + boundingBox.get("west")) / 2;
                    geospatialData.setCenterLongitude(centerLon);
                }

                geospatialData.setBoundingBoxNorth(boundingBox.get("north"));
                geospatialData.setBoundingBoxSouth(boundingBox.get("south"));
                geospatialData.setBoundingBoxEast(boundingBox.get("east"));
                geospatialData.setBoundingBoxWest(boundingBox.get("west"));
            }
        } else {
            // Use provided values
            geospatialData.setCenterLatitude(outageGeospatialDTO.getCenterLatitude());
            geospatialData.setCenterLongitude(outageGeospatialDTO.getCenterLongitude());
            geospatialData.setBoundingBoxNorth(outageGeospatialDTO.getBoundingBoxNorth());
            geospatialData.setBoundingBoxSouth(outageGeospatialDTO.getBoundingBoxSouth());
            geospatialData.setBoundingBoxEast(outageGeospatialDTO.getBoundingBoxEast());
            geospatialData.setBoundingBoxWest(outageGeospatialDTO.getBoundingBoxWest());
        }

        // Set affected population estimate if provided
        geospatialData.setAffectedPopulationEstimate(outageGeospatialDTO.getAffectedPopulationEstimate());

        // Generate static map URL if not provided
        if (outageGeospatialDTO.getStaticMapUrl() == null || outageGeospatialDTO.getStaticMapUrl().isEmpty()) {
            String staticMapUrl = geographicService.generateStaticMapUrl(
                    outageGeospatialDTO.getGeoJson(), staticMapWidth, staticMapHeight);
            geospatialData.setStaticMapUrl(staticMapUrl);
        } else {
            geospatialData.setStaticMapUrl(outageGeospatialDTO.getStaticMapUrl());
        }

        // Save entity
        OutageGeospatialData savedData = outageGeospatialRepository.save(geospatialData);
        logger.info("Geospatial data saved for outage ID: {}", outage.getId());

        // Update the outage's geographical area JSON for backward compatibility
        outage.setGeographicalAreaJson(outageGeospatialDTO.getGeoJson());
        outage.setUpdatedAt(LocalDateTime.now());
        outageRepository.save(outage);

        return convertToDTO(savedData);
    }

    @Override
    @Transactional(readOnly = true)
    public OutageGeospatialDTO getGeospatialDataForOutage(Long outageId) {
        logger.info("Getting geospatial data for outage ID: {}", outageId);

        // Verify outage exists
        if (!outageRepository.existsById(outageId)) {
            throw new EntityNotFoundException("Outage not found with ID: " + outageId);
        }

        // Get geospatial data
        OutageGeospatialData geospatialData = outageGeospatialRepository.findByOutageId(outageId)
                .orElseThrow(() -> new EntityNotFoundException("Geospatial data not found for outage ID: " + outageId));

        return convertToDTO(geospatialData);
    }

    @Override
    @Transactional
    public OutageGeospatialDTO updateGeospatialData(OutageGeospatialDTO outageGeospatialDTO) {
        logger.info("Updating geospatial data for outage ID: {}", outageGeospatialDTO.getOutageId());

        // Verify outage exists
        Outage outage = outageRepository.findById(outageGeospatialDTO.getOutageId())
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageGeospatialDTO.getOutageId()));

        // Get existing data
        OutageGeospatialData geospatialData = outageGeospatialRepository.findByOutageId(outage.getId())
                .orElseThrow(() -> new EntityNotFoundException("Geospatial data not found for outage ID: " + outage.getId()));

        // Update fields
        geospatialData.setGeoJson(outageGeospatialDTO.getGeoJson());

        // Recompute center and bounding box if GeoJSON changed
        Map<String, Double> boundingBox = computeBoundingBox(outageGeospatialDTO.getGeoJson());
        if (boundingBox != null) {
            double centerLat = (boundingBox.get("north") + boundingBox.get("south")) / 2;
            double centerLon = (boundingBox.get("east") + boundingBox.get("west")) / 2;

            geospatialData.setCenterLatitude(centerLat);
            geospatialData.setCenterLongitude(centerLon);
            geospatialData.setBoundingBoxNorth(boundingBox.get("north"));
            geospatialData.setBoundingBoxSouth(boundingBox.get("south"));
            geospatialData.setBoundingBoxEast(boundingBox.get("east"));
            geospatialData.setBoundingBoxWest(boundingBox.get("west"));
        }

        // Update affected population estimate if provided
        if (outageGeospatialDTO.getAffectedPopulationEstimate() != null) {
            geospatialData.setAffectedPopulationEstimate(outageGeospatialDTO.getAffectedPopulationEstimate());
        }

        // Regenerate static map URL
        String staticMapUrl = geographicService.generateStaticMapUrl(
                outageGeospatialDTO.getGeoJson(), staticMapWidth, staticMapHeight);
        geospatialData.setStaticMapUrl(staticMapUrl);

        // Save entity
        OutageGeospatialData updatedData = outageGeospatialRepository.save(geospatialData);
        logger.info("Geospatial data updated for outage ID: {}", outage.getId());

        // Update the outage's geographical area JSON for backward compatibility
        outage.setGeographicalAreaJson(outageGeospatialDTO.getGeoJson());
        outage.setUpdatedAt(LocalDateTime.now());
        outageRepository.save(outage);

        return convertToDTO(updatedData);
    }

    @Override
    @Transactional
    public boolean deleteGeospatialData(Long outageId) {
        logger.info("Deleting geospatial data for outage ID: {}", outageId);

        // Verify outage exists
        if (!outageRepository.existsById(outageId)) {
            throw new EntityNotFoundException("Outage not found with ID: " + outageId);
        }

        // Get geospatial data
        OutageGeospatialData geospatialData = outageGeospatialRepository.findByOutageId(outageId)
                .orElseThrow(() -> new EntityNotFoundException("Geospatial data not found for outage ID: " + outageId));

        // Delete geospatial data
        outageGeospatialRepository.delete(geospatialData);
        logger.info("Geospatial data deleted for outage ID: {}", outageId);

        // Clear the outage's geographical area JSON for backward compatibility
        Outage outage = outageRepository.findById(outageId).orElse(null);
        if (outage != null) {
            outage.setGeographicalAreaJson(null);
            outage.setUpdatedAt(LocalDateTime.now());
            outageRepository.save(outage);
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findOutagesContainingPoint(Double latitude, Double longitude) {
        logger.info("Finding outages containing point: {}, {}", latitude, longitude);

        // First do a basic bounding box search
        List<OutageGeospatialData> candidates = outageGeospatialRepository.findOutagesContainingPoint(latitude, longitude);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Then do a more precise check with actual polygon
        List<Long> outageIds = new ArrayList<>();
        Point point = Point.fromLngLat(longitude, latitude);

        for (OutageGeospatialData data : candidates) {
            try {
                Polygon polygon = geographicService.geoJsonToPolygon(data.getGeoJson());
                if (polygon != null && geographicService.isPointInPolygon(point, polygon)) {
                    outageIds.add(data.getOutage().getId());
                }
            } catch (Exception e) {
                logger.error("Error checking point in polygon for outage ID {}: {}",
                        data.getOutage().getId(), e.getMessage(), e);
            }
        }

        return outageIds;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> checkAddressForActiveOutages(Double latitude, Double longitude) {
        logger.info("Checking address for active outages: {}, {}", latitude, longitude);

        // Get all active outages
        List<Outage> activeOutages = outageRepository.findByStatusIn(
                Arrays.asList(OutageStatus.SCHEDULED, OutageStatus.ONGOING));

        if (activeOutages.isEmpty()) {
            return Collections.emptyMap();
        }

        // Get outage IDs that contain the point
        List<Long> affectingOutageIds = findOutagesContainingPoint(latitude, longitude);

        // Create result map
        Map<Long, Boolean> result = new HashMap<>();
        for (Outage outage : activeOutages) {
            result.put(outage.getId(), affectingOutageIds.contains(outage.getId()));
        }

        return result;
    }

    @Override
    @Transactional
    public String generateStaticMapForOutage(Long outageId) {
        logger.info("Generating static map for outage ID: {}", outageId);

        // Verify outage exists
        if (!outageRepository.existsById(outageId)) {
            throw new EntityNotFoundException("Outage not found with ID: " + outageId);
        }

        // Get geospatial data
        OutageGeospatialData geospatialData = outageGeospatialRepository.findByOutageId(outageId)
                .orElseThrow(() -> new EntityNotFoundException("Geospatial data not found for outage ID: " + outageId));

        // Generate static map URL
        String staticMapUrl = geographicService.generateStaticMapUrl(
                geospatialData.getGeoJson(), staticMapWidth, staticMapHeight);

        // Update entity
        geospatialData.setStaticMapUrl(staticMapUrl);
        outageGeospatialRepository.save(geospatialData);

        return staticMapUrl;
    }

    @Override
    public Map<String, Double> computeBoundingBox(String geoJson) {
        try {
            Polygon polygon = geographicService.geoJsonToPolygon(geoJson);

            if (polygon == null) {
                return null;
            }

            List<List<Point>> coordinates = polygon.coordinates();
            if (coordinates.isEmpty() || coordinates.get(0).isEmpty()) {
                return null;
            }

            List<Point> points = coordinates.get(0);

            // Initialize with first point
            double north = points.get(0).latitude();
            double south = points.get(0).latitude();
            double east = points.get(0).longitude();
            double west = points.get(0).longitude();

            // Find boundaries
            for (Point point : points) {
                north = Math.max(north, point.latitude());
                south = Math.min(south, point.latitude());
                east = Math.max(east, point.longitude());
                west = Math.min(west, point.longitude());
            }

            Map<String, Double> boundingBox = new HashMap<>();
            boundingBox.put("north", north);
            boundingBox.put("south", south);
            boundingBox.put("east", east);
            boundingBox.put("west", west);

            return boundingBox;
        } catch (Exception e) {
            logger.error("Error computing bounding box: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional
    public void analyzeOutageGeospatialData(Outage outage) {
        logger.info("Analyzing geospatial data for outage ID: {}", outage.getId());

        // Check if the outage has geographical area JSON
        String geoJson = outage.getGeographicalAreaJson();
        if (geoJson == null || geoJson.isEmpty()) {
            logger.warn("Outage ID: {} has no geographical area JSON", outage.getId());
            return;
        }

        // Check if geospatial data already exists
        Optional<OutageGeospatialData> existingData = outageGeospatialRepository.findByOutageId(outage.getId());

        if (existingData.isPresent()) {
            // Update existing data
            OutageGeospatialDTO dto = new OutageGeospatialDTO();
            dto.setId(existingData.get().getId());
            dto.setOutageId(outage.getId());
            dto.setGeoJson(geoJson);
            updateGeospatialData(dto);
        } else {
            // Create new data
            OutageGeospatialDTO dto = new OutageGeospatialDTO();
            dto.setOutageId(outage.getId());
            dto.setGeoJson(geoJson);
            saveGeospatialData(dto);
        }
    }

    /**
     * Convert OutageGeospatialData entity to DTO
     * @param geospatialData the entity to convert
     * @return the DTO
     */
    private OutageGeospatialDTO convertToDTO(OutageGeospatialData geospatialData) {
        OutageGeospatialDTO dto = modelMapper.map(geospatialData, OutageGeospatialDTO.class);
        dto.setOutageId(geospatialData.getOutage().getId());
        return dto;
    }
}