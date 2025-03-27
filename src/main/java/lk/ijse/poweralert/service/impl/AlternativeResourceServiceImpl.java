package lk.ijse.poweralert.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lk.ijse.poweralert.dto.AlternativeResourceDTO;
import lk.ijse.poweralert.dto.ResourceImageDTO;
import lk.ijse.poweralert.entity.AlternativeResource;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.repository.AlternativeResourceRepository;
import lk.ijse.poweralert.repository.AreaRepository;
import lk.ijse.poweralert.service.AlternativeResourceService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlternativeResourceServiceImpl implements AlternativeResourceService {

    private static final Logger logger = LoggerFactory.getLogger(AlternativeResourceServiceImpl.class);

    @Autowired
    private AlternativeResourceRepository alternativeResourceRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AlternativeResourceDTO> getAllResources() {
        logger.info("Fetching all alternative resources");

        List<AlternativeResource> resources = alternativeResourceRepository.findByIsActiveTrue();

        return resources.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlternativeResourceDTO getResourceById(Long id) {
        logger.info("Fetching alternative resource with ID: {}", id);

        AlternativeResource resource = alternativeResourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alternative resource not found with ID: " + id));

        return convertToDTO(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlternativeResourceDTO> getResourcesByArea(Long areaId) {
        logger.info("Fetching alternative resources for area ID: {}", areaId);

        // Verify area exists
        if (!areaRepository.existsById(areaId)) {
            throw new EntityNotFoundException("Area not found with ID: " + areaId);
        }

        List<AlternativeResource> resources = alternativeResourceRepository.findByAreaIdAndIsActiveTrue(areaId);

        return resources.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlternativeResourceDTO addResource(AlternativeResourceDTO resourceDTO) {
        logger.info("Adding new alternative resource");

        // Verify area exists
        Area area = areaRepository.findById(resourceDTO.getAreaId())
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + resourceDTO.getAreaId()));

        // Create and save new resource
        AlternativeResource resource = convertToEntity(resourceDTO);
        resource.setArea(area);
        resource.setActive(true);

        AlternativeResource savedResource = alternativeResourceRepository.save(resource);
        logger.info("Alternative resource saved with ID: {}", savedResource.getId());

        return convertToDTO(savedResource);
    }

    @Override
    @Transactional
    public AlternativeResourceDTO updateResource(AlternativeResourceDTO resourceDTO) {
        logger.info("Updating alternative resource with ID: {}", resourceDTO.getId());

        // Verify resource exists
        AlternativeResource existingResource = alternativeResourceRepository.findById(resourceDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException("Alternative resource not found with ID: " + resourceDTO.getId()));

        // Verify area exists if area is being changed
        if (!existingResource.getArea().getId().equals(resourceDTO.getAreaId())) {
            Area area = areaRepository.findById(resourceDTO.getAreaId())
                    .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + resourceDTO.getAreaId()));
            existingResource.setArea(area);
        }

        // Update fields
        existingResource.setName(resourceDTO.getName());
        existingResource.setDescription(resourceDTO.getDescription());
        existingResource.setType(resourceDTO.getType());
        existingResource.setAddress(resourceDTO.getAddress());
        existingResource.setLatitude(resourceDTO.getLatitude());
        existingResource.setLongitude(resourceDTO.getLongitude());
        existingResource.setContactNumber(resourceDTO.getContactNumber());
        existingResource.setOperatingHours(resourceDTO.getOperatingHours());
        existingResource.setActive(resourceDTO.isActive());

        // Save updated resource
        AlternativeResource updatedResource = alternativeResourceRepository.save(existingResource);
        logger.info("Alternative resource updated with ID: {}", updatedResource.getId());

        return convertToDTO(updatedResource);
    }

    @Override
    @Transactional
    public boolean deleteResource(Long id) {
        logger.info("Deleting alternative resource with ID: {}", id);

        // Verify resource exists
        AlternativeResource resource = alternativeResourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alternative resource not found with ID: " + id));

        // Soft delete by setting active to false
        resource.setActive(false);
        alternativeResourceRepository.save(resource);

        logger.info("Alternative resource soft-deleted with ID: {}", id);
        return true;
    }

    /**
     * Convert AlternativeResourceDTO to entity
     * @param dto the DTO to convert
     * @return the entity
     */
    private AlternativeResource convertToEntity(AlternativeResourceDTO dto) {
        return modelMapper.map(dto, AlternativeResource.class);
    }

    // Add these new methods to your existing service implementation

    @Override
    @Transactional
    public AlternativeResourceDTO uploadResourceImage(Long resourceId, MultipartFile file) throws IOException {
        logger.info("Uploading image for resource with ID: {}", resourceId);

        // Verify resource exists
        AlternativeResource resource = alternativeResourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative resource not found with ID: " + resourceId));

        // Save image data
        resource.setImageData(file.getBytes());
        resource.setImageContentType(file.getContentType());
        resource.setImageName(file.getOriginalFilename());

        AlternativeResource savedResource = alternativeResourceRepository.save(resource);
        logger.info("Image uploaded for resource with ID: {}", resourceId);

        return convertToDTO(savedResource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceImageDTO getResourceImage(Long resourceId) {
        logger.info("Fetching image for resource with ID: {}", resourceId);

        AlternativeResource resource = alternativeResourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative resource not found with ID: " + resourceId));

        if (resource.getImageData() == null) {
            throw new EntityNotFoundException("No image found for resource with ID: " + resourceId);
        }

        return ResourceImageDTO.builder()
                .resourceId(resourceId)
                .imageName(resource.getImageName())
                .contentType(resource.getImageContentType())
                .data(resource.getImageData())
                .build();
    }

    @Override
    @Transactional
    public boolean deleteResourceImage(Long resourceId) {
        logger.info("Deleting image for resource with ID: {}", resourceId);

        AlternativeResource resource = alternativeResourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Alternative resource not found with ID: " + resourceId));

        resource.setImageData(null);
        resource.setImageContentType(null);
        resource.setImageName(null);

        alternativeResourceRepository.save(resource);
        logger.info("Image deleted for resource with ID: {}", resourceId);

        return true;
    }

    // Modify your existing convertToDTO method to include image information
    private AlternativeResourceDTO convertToDTO(AlternativeResource resource) {
        AlternativeResourceDTO dto = modelMapper.map(resource, AlternativeResourceDTO.class);
        dto.setAreaId(resource.getArea().getId());

        // Set image metadata (but not the actual binary data)
        dto.setImageName(resource.getImageName());
        dto.setImageContentType(resource.getImageContentType());
        dto.setHasImage(resource.getImageData() != null);

        return dto;
    }
}