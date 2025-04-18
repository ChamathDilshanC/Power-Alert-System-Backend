package lk.ijse.poweralert.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lk.ijse.poweralert.dto.AreaDTO;
import lk.ijse.poweralert.dto.UtilityProviderDTO;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.entity.UtilityProvider;
import lk.ijse.poweralert.repository.AreaRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.repository.UtilityProviderRepository;
import lk.ijse.poweralert.service.AreaService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AreaServiceImpl implements AreaService {

    private static final Logger logger = LoggerFactory.getLogger(AreaServiceImpl.class);

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private UtilityProviderRepository utilityProviderRepository;

    @Autowired
    private OutageRepository outageRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AreaDTO> getAllAreas() {
        logger.info("Fetching all areas");

        List<Area> areas = areaRepository.findAll();

        return areas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AreaDTO getAreaById(Long id) {
        logger.info("Fetching area with ID: {}", id);

        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + id));

        return convertToDTO(area);
    }

    @Override
    @Transactional
    public AreaDTO createArea(AreaDTO areaDTO) {
        logger.info("Creating new area: {}", areaDTO.getName());

        // Convert DTO to entity
        Area area = new Area();
        area.setName(areaDTO.getName());
        area.setDistrict(areaDTO.getDistrict());
        area.setProvince(areaDTO.getProvince());
        area.setBoundaryJson(areaDTO.getBoundaryJson());
        area.setCity(areaDTO.getCity());           // Add this line
        area.setPostalCode(areaDTO.getPostalCode()); // Add this line
        area.setUtilityProviders(new ArrayList<>());

        // Save area
        Area savedArea = areaRepository.save(area);
        logger.info("Area created with ID: {}", savedArea.getId());

        return convertToDTO(savedArea);
    }

    @Override
    @Transactional
    public AreaDTO updateArea(AreaDTO areaDTO) {
        logger.info("Updating area with ID: {}", areaDTO.getId());

        // Verify area exists
        Area existingArea = areaRepository.findById(areaDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + areaDTO.getId()));

        // Update fields
        existingArea.setName(areaDTO.getName());
        existingArea.setDistrict(areaDTO.getDistrict());
        existingArea.setProvince(areaDTO.getProvince());
        existingArea.setBoundaryJson(areaDTO.getBoundaryJson());
        existingArea.setCity(areaDTO.getCity());           // Add this line
        existingArea.setPostalCode(areaDTO.getPostalCode()); // Add this line

        // Save updated area
        Area updatedArea = areaRepository.save(existingArea);
        logger.info("Area updated with ID: {}", updatedArea.getId());

        return convertToDTO(updatedArea);
    }

    @Override
    @Transactional
    public boolean deleteArea(Long id) {
        logger.info("Deleting area with ID: {}", id);

        // Verify area exists
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + id));

        // Check if area has any outages
        if (!area.getOutages().isEmpty()) {
            throw new DataIntegrityViolationException("Cannot delete area with associated outages");
        }

        // Check if area has any resources
        if (!area.getAlternativeResources().isEmpty()) {
            throw new DataIntegrityViolationException("Cannot delete area with associated alternative resources");
        }

        // Delete area
        areaRepository.delete(area);
        logger.info("Area deleted with ID: {}", id);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilityProviderDTO> getUtilityProvidersForArea(Long areaId) {
        logger.info("Fetching utility providers for area ID: {}", areaId);

        // Verify area exists
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + areaId));

        return area.getUtilityProviders().stream()
                .map(provider -> modelMapper.map(provider, UtilityProviderDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AreaDTO linkUtilityProviderToArea(Long areaId, Long providerId) {
        logger.info("Linking utility provider ID: {} to area ID: {}", providerId, areaId);

        // Verify area exists
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + areaId));

        // Verify utility provider exists
        UtilityProvider provider = utilityProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + providerId));

        // Check if provider is already linked to area
        if (area.getUtilityProviders().contains(provider)) {
            logger.info("Provider is already linked to area");
            return convertToDTO(area);
        }

        // Link provider to area
        area.getUtilityProviders().add(provider);
        Area updatedArea = areaRepository.save(area);
        logger.info("Utility provider linked to area");

        return convertToDTO(updatedArea);
    }

    @Override
    @Transactional
    public AreaDTO unlinkUtilityProviderFromArea(Long areaId, Long providerId) {
        logger.info("Unlinking utility provider ID: {} from area ID: {}", providerId, areaId);

        // Verify area exists
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + areaId));

        // Verify utility provider exists
        UtilityProvider provider = utilityProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + providerId));

        // Check if provider is linked to area
        if (!area.getUtilityProviders().contains(provider)) {
            logger.info("Provider is not linked to area");
            return convertToDTO(area);
        }

        // Unlink provider from area
        area.getUtilityProviders().remove(provider);
        Area updatedArea = areaRepository.save(area);
        logger.info("Utility provider unlinked from area");

        return convertToDTO(updatedArea);
    }

    /**
     * Convert Area entity to DTO
     * @param area the area entity
     * @return the area DTO
     */
    private AreaDTO convertToDTO(Area area) {
        AreaDTO dto = modelMapper.map(area, AreaDTO.class);

        // Convert utility providers to DTOs
        if (area.getUtilityProviders() != null) {
            dto.setUtilityProviders(area.getUtilityProviders().stream()
                    .map(provider -> modelMapper.map(provider, UtilityProviderDTO.class))
                    .collect(Collectors.toList()));
        } else {
            dto.setUtilityProviders(new ArrayList<>());
        }

        return dto;
    }
}