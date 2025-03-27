package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.entity.UserDevice;
import lk.ijse.poweralert.service.UserDeviceService;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user devices
 */
@RestController
@RequestMapping("/api/user/devices")
@CrossOrigin
public class UserDeviceController {

    private static final Logger logger = LoggerFactory.getLogger(UserDeviceController.class);

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private UserService userService;

    @Autowired
    private ResponseDTO responseDTO;

    /** Register a new device for the current user   */
    @PostMapping
    public ResponseEntity<ResponseDTO> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        try {
            logger.info("Registering new device for current user");

            // Get current user ID
            Long userId = getCurrentUserId();

            UserDevice device = userDeviceService.registerDevice(
                    userId,
                    request.getDeviceToken(),
                    request.getDeviceType(),
                    request.getDeviceName(),
                    request.getFcmToken()
            );

            // Create response object with only necessary data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", device.getId());
            responseData.put("deviceToken", device.getDeviceToken());
            responseData.put("deviceType", device.getDeviceType());
            responseData.put("isActive", device.isActive());

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Device registered successfully");
            responseDTO.setData(responseData);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error registering device: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Get all active devices for the current user  */
    @GetMapping
    public ResponseEntity<ResponseDTO> getActiveDevices() {
        try {
            logger.info("Fetching active devices for current user");

            Long userId = getCurrentUserId();

            List<UserDevice> devices = userDeviceService.getActiveDevices(userId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Active devices retrieved successfully");
            responseDTO.setData(devices);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching active devices: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Update FCM token for a device    */
    @PutMapping("/{deviceId}")
    public ResponseEntity<ResponseDTO> updateDevice(
            @PathVariable Long deviceId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        try {
            logger.info("Updating device with ID: {}", deviceId);

            // Verify device belongs to current user
            Long userId = getCurrentUserId();
            UserDevice device = userDeviceService.getDeviceById(deviceId);

            if (!device.getUser().getId().equals(userId)) {
                responseDTO.setCode(VarList.Forbidden);
                responseDTO.setMessage("You do not have permission to update this device");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
            }

            device = userDeviceService.updateDevice(deviceId, request.getFcmToken(), true);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", device.getId());
            responseData.put("deviceToken", device.getDeviceToken());
            responseData.put("isActive", device.isActive());
            responseData.put("updatedAt", device.getUpdatedAt());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Device updated successfully");
            responseDTO.setData(responseData);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating device: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Deactivate a device  */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ResponseDTO> deactivateDevice(@PathVariable Long deviceId) {
        try {
            logger.info("Deactivating device with ID: {}", deviceId);

            Long userId = getCurrentUserId();
            UserDevice device = userDeviceService.getDeviceById(deviceId);

            if (!device.getUser().getId().equals(userId)) {
                responseDTO.setCode(VarList.Forbidden);
                responseDTO.setMessage("You do not have permission to deactivate this device");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
            }

            boolean deactivated = userDeviceService.deactivateDevice(deviceId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Device deactivated successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deactivating device: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Deactivate all devices for the current user  */
    @DeleteMapping
    public ResponseEntity<ResponseDTO> deactivateAllDevices() {
        try {
            logger.info("Deactivating all devices for current user");

            Long userId = getCurrentUserId();

            int count = userDeviceService.deactivateAllUserDevices(userId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("deactivatedCount", count);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("All devices deactivated successfully");
            responseDTO.setData(responseData);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deactivating all devices: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Helper method to get current user ID from security context   */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String username = authentication.getName();

        UserDTO userDTO = userService.getUserByUsername(username);

        return userDTO.getId();
    }

    /** Request object for device registration   */
    static class DeviceRegistrationRequest {
        private String deviceToken;
        private String deviceType;
        private String deviceName;
        private String fcmToken;

        // Getters and setters
        public String getDeviceToken() { return deviceToken; }
        public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    }

    /** Request object for device update */
    static class DeviceUpdateRequest {
        private String fcmToken;

        // Getters and setters
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    }
}