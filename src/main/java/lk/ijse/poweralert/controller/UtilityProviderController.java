package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.*;
import lk.ijse.poweralert.enums.AppEnums;
import lk.ijse.poweralert.enums.AppEnums.Role;
import lk.ijse.poweralert.service.UtilityProviderService;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provider")
public class UtilityProviderController {

    private static final Logger logger = LoggerFactory.getLogger(UtilityProviderController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UtilityProviderService utilityProviderService;

    @Autowired
    private ResponseDTO responseDTO;

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> registerUtilityProvider(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        try {
            logger.debug("Attempting to register utility provider: {}", userCreateDTO.getEmail());

            // Force role to be UTILITY_PROVIDER
            userCreateDTO.setRole(Role.UTILITY_PROVIDER);

            // Register the user
            UserDTO registeredUser = userService.registerUser(userCreateDTO);

            // Create corresponding utility provider entry
            UtilityProviderDTO providerDTO = new UtilityProviderDTO();
            providerDTO.setName(userCreateDTO.getUsername()); // Or some other name if provided
            providerDTO.setContactEmail(userCreateDTO.getEmail());
            providerDTO.setContactPhone(userCreateDTO.getPhoneNumber());
            // Set any other fields as needed
            providerDTO.setType(AppEnums.UtilityType.ELECTRICITY); // Default type, can be parameterized

            // Save the utility provider
            UtilityProviderDTO savedProvider = utilityProviderService.createUtilityProvider(providerDTO, registeredUser.getId());

            logger.info("Utility provider registered successfully: {}", registeredUser.getEmail());

            // Include both user and provider details in response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", registeredUser);
            responseData.put("provider", savedProvider);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Utility provider registered successfully");
            responseDTO.setData(responseData);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error registering utility provider: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /** Get all utility providers    */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> getAllUtilityProviders() {
        try {
            logger.debug("Fetching all utility providers");

            List<UtilityProviderDTO> providers = utilityProviderService.getAllUtilityProviders();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility providers retrieved successfully");
            responseDTO.setData(providers);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving utility providers: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Get utility provider by ID   */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> getUtilityProviderById(@PathVariable Long id) {
        try {
            logger.debug("Fetching utility provider with ID: {}", id);

            UtilityProviderDTO provider = utilityProviderService.getUtilityProviderById(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility provider retrieved successfully");
            responseDTO.setData(provider);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving utility provider with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Update utility provider details */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> updateUtilityProvider(
            @PathVariable Long id,
            @Valid @RequestBody UtilityProviderDTO utilityProviderDTO) {
        try {
            logger.debug("Updating utility provider with ID: {}", id);

            // Ensure ID matches path variable
            utilityProviderDTO.setId(id);

            UtilityProviderDTO updatedProvider = utilityProviderService.updateUtilityProvider(utilityProviderDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility provider updated successfully");
            responseDTO.setData(updatedProvider);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating utility provider with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Get outages for a utility provider   */
    @GetMapping("/outages")
    @PreAuthorize("hasAuthority('ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> getProviderOutages() {
        try {
            logger.debug("Fetching outages for current utility provider");

            List<OutageDTO> outages = utilityProviderService.getOutagesForCurrentProvider();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Provider outages retrieved successfully");
            responseDTO.setData(outages);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving outages for provider: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}