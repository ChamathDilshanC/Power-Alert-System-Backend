package lk.ijse.poweralert.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.*;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums;
import lk.ijse.poweralert.service.EmailService;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.JwtUtil;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ResponseDTO responseDTO;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequest, HttpServletRequest request) {
        try {
            logger.info("Login attempt for user: {}", authRequest.getUsername());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Get basic user info without eagerly loading collections
            UserDTO userDTO = userService.getUserBasicInfo(userDetails.getUsername());

            // Generate JWT token
            String token = jwtUtil.generateToken(userDTO);

            // Auth Response
            AuthDTO authDTO = new AuthDTO();
            authDTO.setEmail(userDTO.getEmail());
            authDTO.setToken(token);
            authDTO.setRole(userDTO.getRole().name());
            authDTO.setUsername(userDTO.getUsername());

            // Set response DTO
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Authentication successful");
            responseDTO.setData(authDTO);

            // Update last login
            userService.updateLastLogin(userDTO.getEmail());

            // Send login notification email asynchronously
            String ipAddress = getClientIp(request);
            String device = request.getHeader("User-Agent");
            String location = "Unknown"; // You could integrate with a geolocation service here

            // Convert UserDTO to User entity and send notification
            // This depends on how your service is designed
            User user = userService.getUserEntityByUsername(userDTO.getUsername());
            emailService.sendLoginNotificationEmail(user, ipAddress, device, location);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (BadCredentialsException e) {
            logger.error("Authentication failed: Invalid credentials");
            responseDTO.setCode(VarList.Unauthorized);
            responseDTO.setMessage("Invalid credentials");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.error("Login error: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper method to get client IP address
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO> register(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        try {
            // Check if user already exists
            if (userService.existsByEmail(userCreateDTO.getEmail())) {
                responseDTO.setCode(VarList.Conflict);
                responseDTO.setMessage("Email is already registered");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.CONFLICT);
            }

            if (userCreateDTO.getRole() == AppEnums.Role.ADMIN) {
                userCreateDTO.setRole(AppEnums.Role.USER);
            }

            // Register new user
            UserDTO registeredUser = userService.registerUser(userCreateDTO);

            // Generate JWT token
            String token = jwtUtil.generateToken(registeredUser);

            // Create auth response
            AuthDTO authDTO = new AuthDTO();
            authDTO.setEmail(registeredUser.getEmail());
            authDTO.setToken(token);
            authDTO.setRole(registeredUser.getRole().name());
            authDTO.setUsername(userCreateDTO.getUsername());

            // Set response DTO
            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("User registered successfully");
            responseDTO.setData(authDTO);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception e) {
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}