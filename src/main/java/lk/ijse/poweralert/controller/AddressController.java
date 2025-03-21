package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.AddressDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.AddressService;
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
@RequestMapping("/api/user/addresses")
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
public class AddressController {

    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);

    @Autowired
    private AddressService addressService;

    @Autowired
    private ResponseDTO responseDTO;

    /** Get all addresses for the current user*/
    @GetMapping
    public ResponseEntity<ResponseDTO> getCurrentUserAddresses() {
        try {
            logger.debug("Fetching addresses for current user");

            List<AddressDTO> addresses = addressService.getCurrentUserAddresses();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Addresses retrieved successfully");
            responseDTO.setData(addresses);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving addresses: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Get address by ID for current user   */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> getAddressById(@PathVariable Long id) {
        try {
            logger.debug("Fetching address with ID: {}", id);

            AddressDTO address = addressService.getAddressById(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Address retrieved successfully");
            responseDTO.setData(address);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving address with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Add a new address for current user   */
    @PostMapping
    public ResponseEntity<ResponseDTO> addAddress(@Valid @RequestBody AddressDTO addressDTO) {
        try {
            logger.debug("Adding new address for current user");

            AddressDTO savedAddress = addressService.addAddress(addressDTO);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Address added successfully");
            responseDTO.setData(savedAddress);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error adding address: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Update an existing address   */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO addressDTO) {
        try {
            logger.debug("Updating address with ID: {}", id);

            AddressDTO updatedAddress = addressService.updateAddress(id, addressDTO);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Address updated successfully");
            responseDTO.setData(updatedAddress);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating address with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Delete an address    */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteAddress(@PathVariable Long id) {
        try {
            logger.debug("Deleting address with ID: {}", id);

            boolean deleted = addressService.deleteAddress(id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Address deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error deleting address with ID {}: {}", id, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}