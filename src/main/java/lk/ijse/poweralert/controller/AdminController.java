package lk.ijse.poweralert.controller;

import com.google.firebase.database.annotations.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.enums.AppEnums.Role;
import lk.ijse.poweralert.job.AdvanceNotificationJob;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ResponseDTO responseDTO;

    @Autowired
    private AdvanceNotificationJob advanceNotificationJob;

    @GetMapping("/debug-role")
    public ResponseEntity<String> debugRole(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication found");
        }

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        logger.debug("User {} has authorities: {}", authentication.getName(), authorities);

        return ResponseEntity.ok("Your authorities: " + authorities);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<ResponseDTO> registerAdmin(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        try {
            logger.debug("Attempting to register admin user: {}", userCreateDTO.getEmail());

            // Force role to be ADMIN
            userCreateDTO.setRole(Role.ADMIN);

            UserDTO registeredUser = userService.registerUser(userCreateDTO);

            logger.info("Admin user registered successfully: {}", registeredUser.getEmail());

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Admin user registered successfully");
            responseDTO.setData(registeredUser);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error registering admin user: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO> registerUser(@Valid @RequestBody UserCreateDTO userCreateDTO,
                                                    @Nullable BindingResult bindingResult) {
        try {
            // Check for validation errors
            if (bindingResult != null && bindingResult.hasErrors()) {
                // Get all validation errors
                List<String> errors = bindingResult.getAllErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.toList());

                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Validation failed: " + String.join(", ", errors));
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            // Process the request
            UserDTO registeredUser = userService.registerUser(userCreateDTO);
            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("User registered successfully");
            responseDTO.setData(registeredUser);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ResponseDTO> getAllUsers() {
        try {
            logger.debug("Fetching all users");

            List<UserDTO> users = userService.getAllUsers();

            // For debugging - log the active status of each user
            users.forEach(user ->
                    logger.debug("User ID: {}, Username: {}, Active: {}",
                            user.getId(), user.getUsername(), user.isActive()));

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Users retrieved successfully");
            responseDTO.setData(users);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving users: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ResponseDTO> getUserById(@PathVariable Long id) {
        try {
            logger.debug("Fetching user with ID: {}", id);

            UserDTO user = userService.getUserById(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User retrieved successfully");
            responseDTO.setData(user);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving user with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ResponseDTO> deactivateUser(@PathVariable Long id) {
        try {
            logger.debug("Deactivating user with ID: {}", id);

            UserDTO deactivatedUser = userService.deactivateUser(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User deactivated successfully");
            responseDTO.setData(deactivatedUser);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deactivating user with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/trigger-advance-notifications")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> triggerAdvanceNotifications() {
        try {
            logger.info("Admin manually triggering advance notifications job");

            advanceNotificationJob.sendAdvanceNotifications();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Advance notifications job triggered successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error triggering advance notifications job: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        try {
            logger.debug("Updating user with ID: {}", id);

            // Ensure the ID in the path matches the ID in the DTO
            userDTO.setId(id);

            UserDTO updatedUser = userService.updateUser(userDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User updated successfully");
            responseDTO.setData(updatedUser);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating user with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ResponseDTO> toggleUserStatus(@PathVariable Long id) {
        try {
            logger.debug("Toggling status for user with ID: {}", id);

            UserDTO user = userService.getUserById(id);

            // Toggle the status
            boolean newStatus = !user.isActive();
            UserDTO updatedUser = userService.updateUserStatus(id, newStatus);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User status updated successfully");
            responseDTO.setData(updatedUser);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error toggling user status with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ResponseDTO> resetUserPassword(@PathVariable Long id) {
        try {
            logger.debug("Resetting password for user with ID: {}", id);

            // Generate a random password or use a default one
            String newPassword = generateRandomPassword(); // Implement this method
            UserDTO updatedUser = userService.resetUserPassword(id, newPassword);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User password reset successfully");

            // Create a map to hold the user and new password
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("user", updatedUser);
            responseMap.put("newPassword", newPassword);

            responseDTO.setData(responseMap);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error resetting password for user with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper method to generate a random password
    private String generateRandomPassword() {
        // Generate a secure random password with required complexity
        int length = 12;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        SecureRandom random = new SecureRandom();

        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            password.append(characters.charAt(randomIndex));
        }

        return password.toString();
    }
}