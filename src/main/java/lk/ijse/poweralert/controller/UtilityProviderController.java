package lk.ijse.poweralert.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.*;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.entity.UtilityProvider;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provider")
@CrossOrigin
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
    public ResponseEntity<ResponseDTO> registerUtilityProvider(@Valid @RequestBody ProviderRegistrationDTO registrationDTO) {
        try {
            logger.debug("Attempting to register utility provider: {}", registrationDTO.getContactEmail());

            // Create user with role UTILITY_PROVIDER
            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setUsername(registrationDTO.getUsername());
            userCreateDTO.setPassword(registrationDTO.getPassword());
            userCreateDTO.setEmail(registrationDTO.getContactEmail());
            userCreateDTO.setPhoneNumber(registrationDTO.getContactPhone());
            userCreateDTO.setRole(Role.UTILITY_PROVIDER);

            // Register the user
            UserDTO registeredUser = userService.registerUser(userCreateDTO);

            // Create corresponding utility provider entry
            UtilityProviderDTO providerDTO = new UtilityProviderDTO();
            providerDTO.setName(registrationDTO.getName());
            providerDTO.setType(registrationDTO.getType());
            providerDTO.setContactEmail(registrationDTO.getContactEmail());
            providerDTO.setContactPhone(registrationDTO.getContactPhone());
            providerDTO.setWebsite(registrationDTO.getWebsite());

            // Save the utility provider and link it to service areas
            UtilityProviderDTO savedProvider = utilityProviderService.createUtilityProvider(providerDTO, registeredUser.getId());

            if (registrationDTO.getServiceAreaIds() != null && !registrationDTO.getServiceAreaIds().isEmpty()) {
                utilityProviderService.linkProviderToAreas(savedProvider.getId(), registrationDTO.getServiceAreaIds());
            }

            // Prepare response
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

            // Log the data we're sending to the service for debugging
            logger.debug("Update data being sent to service: {}", utilityProviderDTO);

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

    @GetMapping("/public/utility-providers")
    public ResponseEntity<ResponseDTO> getAllPublicUtilityProviders() {
        try {
            logger.debug("Fetching all utility providers (public endpoint)");

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> deleteUtilityProvider(@PathVariable Long id) {
        try {
            boolean deleted = utilityProviderService.deleteUtilityProvider(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Utility provider deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            responseDTO.setCode(VarList.Not_Found);
            responseDTO.setMessage(e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Log the full stack trace for debugging
            logger.error("Error deleting utility provider with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}