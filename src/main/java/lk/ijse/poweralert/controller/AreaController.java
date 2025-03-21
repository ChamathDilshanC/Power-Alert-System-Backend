package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.AreaDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.dto.UtilityProviderDTO;
import lk.ijse.poweralert.service.AreaService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AreaController {

    private static final Logger logger = LoggerFactory.getLogger(AreaController.class);

    @Autowired
    private AreaService areaService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all areas (public)
     */
    @GetMapping("/public/areas")
    public ResponseEntity<ResponseDTO> getAllAreas() {
        try {
            logger.debug("Fetching all areas");

            List<AreaDTO> areas = areaService.getAllAreas();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Areas retrieved successfully");
            responseDTO.setData(areas);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving areas: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get a specific area by ID (public)
     */
    @GetMapping("/public/areas/{id}")
    public ResponseEntity<ResponseDTO> getAreaById(@PathVariable Long id) {
        try {
            logger.debug("Fetching area with ID: {}", id);

            AreaDTO area = areaService.getAreaById(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Area retrieved successfully");
            responseDTO.setData(area);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving area with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a new area (admin only)
     */
    @PostMapping("/admin/areas")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> createArea(@Valid @RequestBody AreaDTO areaDTO) {
        try {
            logger.debug("Creating new area: {}", areaDTO.getName());

            AreaDTO createdArea = areaService.createArea(areaDTO);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Area created successfully");
            responseDTO.setData(createdArea);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating area: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an area (admin only)
     */
    @PutMapping("/admin/areas/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> updateArea(
            @PathVariable Long id,
            @Valid @RequestBody AreaDTO areaDTO) {
        try {
            logger.debug("Updating area with ID: {}", id);

            // Ensure ID matches the path variable
            areaDTO.setId(id);

            AreaDTO updatedArea = areaService.updateArea(areaDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Area updated successfully");
            responseDTO.setData(updatedArea);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating area with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete an area (admin only)
     */
    @DeleteMapping("/admin/areas/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> deleteArea(@PathVariable Long id) {
        try {
            logger.debug("Deleting area with ID: {}", id);

            boolean deleted = areaService.deleteArea(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Area deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting area with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get utility providers for an area (public)
     */
    @GetMapping("/public/areas/{id}/utility-providers")
    public ResponseEntity<ResponseDTO> getUtilityProvidersForArea(@PathVariable Long id) {
        try {
            logger.debug("Fetching utility providers for area ID: {}", id);

            List<UtilityProviderDTO> providers = areaService.getUtilityProvidersForArea(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility providers retrieved successfully");
            responseDTO.setData(providers);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving utility providers for area ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Link utility provider to an area (admin only)
     */
    @PostMapping("/admin/areas/{areaId}/utility-providers")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> linkUtilityProviderToArea(
            @PathVariable Long areaId,
            @RequestParam Long providerId) {
        try {
            logger.debug("Linking utility provider ID: {} to area ID: {}", providerId, areaId);

            AreaDTO updatedArea = areaService.linkUtilityProviderToArea(areaId, providerId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility provider linked to area successfully");
            responseDTO.setData(updatedArea);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error linking utility provider ID: {} to area ID: {}: {}",
                    providerId, areaId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Unlink utility provider from an area (admin only)
     */
    @DeleteMapping("/admin/areas/{areaId}/utility-providers/{providerId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> unlinkUtilityProviderFromArea(
            @PathVariable Long areaId,
            @PathVariable Long providerId) {
        try {
            logger.debug("Unlinking utility provider ID: {} from area ID: {}", providerId, areaId);

            AreaDTO updatedArea = areaService.unlinkUtilityProviderFromArea(areaId, providerId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility provider unlinked from area successfully");
            responseDTO.setData(updatedArea);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error unlinking utility provider ID: {} from area ID: {}: {}",
                    providerId, areaId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}