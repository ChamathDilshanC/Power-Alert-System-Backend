package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.OutageCreateDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.OutageUpdateDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.OutageService;
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
public class OutageController {

    private static final Logger logger = LoggerFactory.getLogger(OutageController.class);

    @Autowired
    private OutageService outageService;

    @Autowired
    private ResponseDTO responseDTO;

    // Public endpoint to get all active outages
    @GetMapping("/public/outages/active")
    public ResponseEntity<ResponseDTO> getAllActiveOutages() {
        try {
            List<OutageDTO> outages = outageService.getAllActiveOutages();
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Active outages retrieved successfully");
            responseDTO.setData(outages);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving active outages: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Public endpoint to get outage by ID
    @GetMapping("/public/outages/{id}")
    public ResponseEntity<ResponseDTO> getOutageById(@PathVariable Long id) {
        try {
            OutageDTO outage = outageService.getOutageById(id);
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outage retrieved successfully");
            responseDTO.setData(outage);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving outage with ID {}: {}", id, e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Public endpoint to get outages by area
    @GetMapping("/public/outages/area/{areaId}")
    public ResponseEntity<ResponseDTO> getOutagesByArea(@PathVariable Long areaId) {
        try {
            List<OutageDTO> outages = outageService.getOutagesByArea(areaId);
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outages for area retrieved successfully");
            responseDTO.setData(outages);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving outages for area {}: {}", areaId, e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Provider endpoint to create outage
    @PostMapping("/provider/outages")
    @PreAuthorize("hasAnyAuthority('ROLE_UTILITY_PROVIDER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> createOutage(@Valid @RequestBody OutageCreateDTO outageCreateDTO) {
        try {
            OutageDTO createdOutage = outageService.createOutage(outageCreateDTO);
            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Outage created successfully");
            responseDTO.setData(createdOutage);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating outage: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Provider endpoint to update outage
    @PutMapping("/provider/outages/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_UTILITY_PROVIDER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> updateOutage(
            @PathVariable Long id,
            @Valid @RequestBody OutageCreateDTO outageCreateDTO) {
        try {
            OutageDTO updatedOutage = outageService.updateOutage(id, outageCreateDTO);
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outage updated successfully");
            responseDTO.setData(updatedOutage);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating outage with ID {}: {}", id, e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Provider endpoint to add update to outage
    @PostMapping("/provider/outages/{id}/updates")
    @PreAuthorize("hasAnyAuthority('ROLE_UTILITY_PROVIDER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> addOutageUpdate(
            @PathVariable Long id,
            @Valid @RequestBody OutageUpdateDTO outageUpdateDTO) {
        try {
            outageUpdateDTO.setOutageId(id); // Ensure the outage ID is set correctly
            OutageDTO updatedOutage = outageService.addOutageUpdate(outageUpdateDTO);
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outage update added successfully");
            responseDTO.setData(updatedOutage);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error adding update to outage with ID {}: {}", id, e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Admin endpoint to cancel outage
    @PutMapping("/admin/outages/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> cancelOutage(@PathVariable Long id) {
        try {
            OutageDTO cancelledOutage = outageService.cancelOutage(id);
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outage cancelled successfully");
            responseDTO.setData(cancelledOutage);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error cancelling outage with ID {}: {}", id, e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // User endpoint to get outages for user's area
    @GetMapping("/user/outages")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> getOutagesForUser() {
        try {
            List<OutageDTO> outages = outageService.getOutagesForCurrentUser();
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User's outages retrieved successfully");
            responseDTO.setData(outages);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving outages for current user: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}