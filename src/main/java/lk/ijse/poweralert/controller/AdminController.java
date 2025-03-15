package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.enums.AppEnums.Role;
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

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ResponseDTO responseDTO;

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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<ResponseDTO> registerUser(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        try {
            logger.debug("Attempting to register user: {} with role: {}",
                    userCreateDTO.getEmail(), userCreateDTO.getRole());

            if (userCreateDTO.getRole() != Role.ADMIN && userCreateDTO.getRole() != Role.UTILITY_PROVIDER) {
                logger.warn("Invalid role specified: {}. Setting to UTILITY_PROVIDER by default", userCreateDTO.getRole());
                userCreateDTO.setRole(Role.UTILITY_PROVIDER);
            }

            UserDTO registeredUser = userService.registerUser(userCreateDTO);

            logger.info("User registered successfully: {} with role: {}",
                    registeredUser.getEmail(), registeredUser.getRole());

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("User registered successfully with role: " + registeredUser.getRole());
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
}