package lk.ijse.poweralert.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lk.ijse.poweralert.dto.AddressDTO;
import lk.ijse.poweralert.entity.Address;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.AddressRepository;
import lk.ijse.poweralert.service.AddressService;
import lk.ijse.poweralert.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressServiceImpl.class);

    private final AddressRepository addressRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Autowired
    public AddressServiceImpl(
            AddressRepository addressRepository,
            UserService userService,
            ModelMapper modelMapper) {
        this.addressRepository = addressRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public AddressDTO addAddress(AddressDTO addressDTO) {
        logger.info("Adding new address for current user");

        // Get current user
        User user = getCurrentUser();

        // Create new address entity
        Address address = modelMapper.map(addressDTO, Address.class);
        address.setUser(user);

        // If this is the first address, make it primary
        if (user.getAddresses().isEmpty()) {
            address.setPrimary(true);
        }

        // If the new address is set as primary, update other addresses
        if (address.isPrimary()) {
            user.getAddresses().forEach(a -> {
                if (a.isPrimary()) {
                    a.setPrimary(false);
                    addressRepository.save(a);
                }
            });
        }

        // Save the new address
        Address savedAddress = addressRepository.save(address);
        logger.info("Address added with ID: {}", savedAddress.getId());

        return convertToDTO(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getCurrentUserAddresses() {
        logger.info("Fetching addresses for current user");

        // Get current user
        User user = getCurrentUser();

        // Get addresses for the user
        List<Address> addresses = addressRepository.findByUserIdOrderByIsPrimaryDesc(user.getId());

        return addresses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long id) {
        logger.info("Fetching address with ID: {}", id);

        // Get current user
        User user = getCurrentUser();

        // Find address by ID for the current user
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + id));

        return convertToDTO(address);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long id, AddressDTO addressDTO) {
        logger.info("Updating address with ID: {}", id);

        // Get current user
        User user = getCurrentUser();

        // Find address by ID for the current user
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + id));

        // Update address fields
        address.setAddressLine1(addressDTO.getAddressLine1());
        address.setAddressLine2(addressDTO.getAddressLine2());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setLatitude(addressDTO.getLatitude());
        address.setLongitude(addressDTO.getLongitude());

        // If setting as primary, update other addresses
        if (addressDTO.isPrimary() && !address.isPrimary()) {
            user.getAddresses().forEach(a -> {
                if (a.isPrimary()) {
                    a.setPrimary(false);
                    addressRepository.save(a);
                }
            });
            address.setPrimary(true);
        }

        // Save updated address
        Address updatedAddress = addressRepository.save(address);
        logger.info("Address updated with ID: {}", updatedAddress.getId());

        return convertToDTO(updatedAddress);
    }

    @Override
    @Transactional
    public boolean deleteAddress(Long id) {
        logger.info("Deleting address with ID: {}", id);

        // Get current user
        User user = getCurrentUser();

        // Find address by ID for the current user
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + id));

        // If deleting primary address, make another address primary if available
        if (address.isPrimary()) {
            List<Address> otherAddresses = addressRepository.findByUserIdAndIdNot(user.getId(), id);
            if (!otherAddresses.isEmpty()) {
                Address newPrimary = otherAddresses.get(0);
                newPrimary.setPrimary(true);
                addressRepository.save(newPrimary);
            }
        }

        // Delete the address
        addressRepository.delete(address);
        logger.info("Address deleted with ID: {}", id);

        return true;
    }

    /**
     * Get the current logged-in user
     * @return the user entity
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserEntityByUsername(username);
    }

    /**
     * Convert Address entity to AddressDTO
     * @param address the address entity
     * @return the address DTO
     */
    private AddressDTO convertToDTO(Address address) {
        return modelMapper.map(address, AddressDTO.class);
    }
}