package lk.ijse.poweralert.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.AlternativeResourceDTO;
import lk.ijse.poweralert.dto.ResourceImageDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.AlternativeResourceService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AlternativeResourceController {

    private static final Logger logger = LoggerFactory.getLogger(AlternativeResourceController.class);

    @Autowired
    private AlternativeResourceService alternativeResourceService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all alternative resources (public)
     */
    @GetMapping("/public/alternative-resources")
    public ResponseEntity<ResponseDTO> getAllResources() {
        try {
            logger.debug("Fetching all alternative resources");

            List<AlternativeResourceDTO> resources = alternativeResourceService.getAllResources();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Alternative resources retrieved successfully");
            responseDTO.setData(resources);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving alternative resources: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get a specific alternative resource by ID (public)
     */
    @GetMapping("/public/alternative-resources/{id}")
    public ResponseEntity<ResponseDTO> getResourceById(@PathVariable Long id) {
        try {
            logger.debug("Fetching alternative resource with ID: {}", id);

            AlternativeResourceDTO resource = alternativeResourceService.getResourceById(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Alternative resource retrieved successfully");
            responseDTO.setData(resource);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving alternative resource with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get resources by area ID (public)
     */
    @GetMapping("/public/areas/{areaId}/alternative-resources")
    public ResponseEntity<ResponseDTO> getResourcesByArea(@PathVariable Long areaId) {
        try {
            logger.debug("Fetching alternative resources for area ID: {}", areaId);

            List<AlternativeResourceDTO> resources = alternativeResourceService.getResourcesByArea(areaId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Alternative resources for area retrieved successfully");
            responseDTO.setData(resources);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving alternative resources for area ID {}: {}", areaId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add a new alternative resource (admin only)
     */
    @PostMapping("/admin/alternative-resources")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> addResource(@Valid @RequestBody AlternativeResourceDTO resourceDTO) {
        try {
            logger.debug("Adding new alternative resource");

            AlternativeResourceDTO savedResource = alternativeResourceService.addResource(resourceDTO);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Alternative resource added successfully");
            responseDTO.setData(savedResource);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error adding alternative resource: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing alternative resource (admin only)
     */
    @PutMapping("/admin/alternative-resources/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody AlternativeResourceDTO resourceDTO) {
        try {
            logger.debug("Updating alternative resource with ID: {}", id);

            // Ensure ID matches the path variable
            resourceDTO.setId(id);

            AlternativeResourceDTO updatedResource = alternativeResourceService.updateResource(resourceDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Alternative resource updated successfully");
            responseDTO.setData(updatedResource);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating alternative resource with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete an alternative resource (admin only)
     */
    @DeleteMapping("/admin/alternative-resources/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> deleteResource(@PathVariable Long id) {
        try {
            logger.debug("Deleting alternative resource with ID: {}", id);

            boolean deleted = alternativeResourceService.deleteResource(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Alternative resource deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting alternative resource with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // Add these endpoints to your existing controller

    /**
     * Upload an image for an alternative resource
     */
    @PostMapping("/admin/alternative-resources/{id}/image")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> uploadResourceImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            logger.debug("Uploading image for alternative resource with ID: {}", id);

            if (file.isEmpty()) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Please select a file to upload");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            // Validate file type
            if (!file.getContentType().startsWith("image/")) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Only image files are allowed");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            AlternativeResourceDTO updatedResource = alternativeResourceService.uploadResourceImage(id, file);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Image uploaded successfully");
            responseDTO.setData(updatedResource);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error uploading image for resource with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the image of an alternative resource (public)
     */
    @GetMapping("/public/alternative-resources/{id}/image")
    public ResponseEntity<?> getResourceImage(@PathVariable Long id) {
        try {
            logger.debug("Fetching image for alternative resource with ID: {}", id);

            ResourceImageDTO imageDTO = alternativeResourceService.getResourceImage(id);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(imageDTO.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + imageDTO.getImageName() + "\"")
                    .body(imageDTO.getData());
        } catch (EntityNotFoundException e) {
            logger.error("Image not found for resource with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving image for resource with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete an image of an alternative resource (admin only)
     */
    @DeleteMapping("/admin/alternative-resources/{id}/image")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> deleteResourceImage(@PathVariable Long id) {
        try {
            logger.debug("Deleting image for alternative resource with ID: {}", id);

            boolean deleted = alternativeResourceService.deleteResourceImage(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Image deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting image for resource with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}