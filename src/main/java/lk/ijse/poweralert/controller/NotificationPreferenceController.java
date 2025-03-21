package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.NotificationPreferenceDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.NotificationPreferenceService;
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
@RequestMapping("/api/user/notification-preferences")
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
public class NotificationPreferenceController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceController.class);

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all notification preferences for the current user
     */
    @GetMapping
    public ResponseEntity<ResponseDTO> getCurrentUserPreferences() {
        try {
            logger.debug("Fetching notification preferences for current user");

            List<NotificationPreferenceDTO> preferences = notificationPreferenceService.getCurrentUserPreferences();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Notification preferences retrieved successfully");
            responseDTO.setData(preferences);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving notification preferences: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add a new notification preference for current user
     */
    @PostMapping
    public ResponseEntity<ResponseDTO> addPreference(@Valid @RequestBody NotificationPreferenceDTO preferenceDTO) {
        try {
            logger.debug("Adding new notification preference for current user");

            NotificationPreferenceDTO savedPreference = notificationPreferenceService.addPreference(preferenceDTO);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Notification preference added successfully");
            responseDTO.setData(savedPreference);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error adding notification preference: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing notification preference
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO> updatePreference(
            @PathVariable Long id,
            @Valid @RequestBody NotificationPreferenceDTO preferenceDTO) {
        try {
            logger.debug("Updating notification preference with ID: {}", id);

            // Ensure the ID in the path matches the DTO
            preferenceDTO.setId(id);

            NotificationPreferenceDTO updatedPreference = notificationPreferenceService.updatePreference(preferenceDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Notification preference updated successfully");
            responseDTO.setData(updatedPreference);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating notification preference with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a notification preference
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePreference(@PathVariable Long id) {
        try {
            logger.debug("Deleting notification preference with ID: {}", id);

            boolean deleted = notificationPreferenceService.deletePreference(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Notification preference deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting notification preference with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}