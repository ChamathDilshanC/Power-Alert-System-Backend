package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
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

    @GetMapping("/users")
    public ResponseEntity<ResponseDTO> getAllUsers() {
        try {
            logger.debug("Fetching all users");

            List<UserDTO> users = userService.getAllUsers();

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
}