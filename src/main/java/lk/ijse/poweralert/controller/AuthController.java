package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.*;
import lk.ijse.poweralert.enums.AppEnums;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.JwtUtil;
import lk.ijse.poweralert.util.VarList;
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

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ResponseDTO responseDTO;

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            // User is authenticated, get user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserDTO userDTO = userService.getUserByEmail(userDetails.getUsername());

            // Generate JWT token
            String token = jwtUtil.generateToken(userDTO);

            // Create auth response
            AuthDTO authDTO = new AuthDTO();
            authDTO.setEmail(userDTO.getEmail());
            authDTO.setToken(token);
            authDTO.setRole(userDTO.getRole().name()); // Convert enum to string using name()
            authDTO.setUsername(userDTO.getUsername());

            // Set response DTO
            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Authentication successful");

            userService.updateLastLogin(userDTO.getEmail());


            responseDTO.setData(authDTO);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (BadCredentialsException e) {
            responseDTO.setCode(VarList.Unauthorized);
            responseDTO.setMessage("Invalid credentials");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

            // Force role to be USER for normal registration
            // This prevents unauthorized admin creation
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