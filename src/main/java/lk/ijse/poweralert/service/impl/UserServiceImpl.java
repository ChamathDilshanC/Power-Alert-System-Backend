package lk.ijse.poweralert.service.impl;

import jakarta.validation.ValidationException;
import lk.ijse.poweralert.dto.AddressDTO;
import lk.ijse.poweralert.dto.NotificationPreferenceDTO;
import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.entity.Address;
import lk.ijse.poweralert.entity.NotificationPreference;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.PhoneNumberValidator;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            ModelMapper modelMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDTO registerUser(UserCreateDTO userCreateDTO) {
        logger.info("Registering new user with username: {}", userCreateDTO.getUsername());

        String phoneNumber = userCreateDTO.getPhoneNumber();
        if (!PhoneNumberValidator.isValidSriLankanMobile(phoneNumber)) {
            throw new ValidationException("Invalid Sri Lankan mobile number. It should start with +94 followed by 9 digits.");
        }

        // Format phone number if needed
        phoneNumber = PhoneNumberValidator.formatPhoneNumber(phoneNumber);
        userCreateDTO.setPhoneNumber(phoneNumber);

        // Create user entity
        User user = new User();
        user.setUsername(userCreateDTO.getUsername());
        user.setEmail(userCreateDTO.getEmail());
        user.setPhoneNumber(userCreateDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setRole(userCreateDTO.getRole());
        user.setPreferredLanguage(userCreateDTO.getPreferredLanguage());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // Initialize empty collections to avoid null pointer exceptions
        user.setAddresses(new ArrayList<>());
        user.setNotificationPreferences(new ArrayList<>());

        // Save user
        User savedUser = userRepository.save(user);
        logger.info("User registered with ID: {}", savedUser.getId());

        // Map to DTO and return
        return convertToBasicDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        logger.info("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        return convertToBasicDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        logger.info("Fetching user with username: {}", username);
        User user = getUserEntityByUsername(username);

        // Explicitly initialize the collections to avoid lazy loading issues
        if (!Hibernate.isInitialized(user.getAddresses())) {
            Hibernate.initialize(user.getAddresses());
        }

        if (!Hibernate.isInitialized(user.getNotificationPreferences())) {
            Hibernate.initialize(user.getNotificationPreferences());
        }

        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserBasicInfo(String username) {
        logger.info("Fetching basic info for user with username: {}", username);
        User user = getUserEntityByUsername(username);

        // Do not initialize collections
        return convertToBasicDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses(Long userId) {
        logger.info("Fetching addresses for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Initialize the addresses collection if it hasn't been initialized
        if (!Hibernate.isInitialized(user.getAddresses())) {
            Hibernate.initialize(user.getAddresses());
        }

        return user.getAddresses().stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDTO> getUserNotificationPreferences(Long userId) {
        logger.info("Fetching notification preferences for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Initialize the notification preferences collection if it hasn't been initialized
        if (!Hibernate.isInitialized(user.getNotificationPreferences())) {
            Hibernate.initialize(user.getNotificationPreferences());
        }

        return user.getNotificationPreferences().stream()
                .map(pref -> modelMapper.map(pref, NotificationPreferenceDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        logger.info("Fetching user entity with username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updateLastLogin(String email) {
        logger.info("Updating last login time for user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        // Explicitly set the active status to ensure it's mapped correctly
        userDTO.setActive(user.isActive());

        return userDTO;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> {
                    UserDTO dto = modelMapper.map(user, UserDTO.class);
                    // Explicitly set the active status
                    dto.setActive(user.isActive());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO deactivateUser(Long id) {
        logger.info("Deactivating user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setActive(false);
        userRepository.save(user);

        return convertToBasicDTO(user);
    }

    /**
     * Convert User entity to UserDTO including collections
     * @param user the user entity
     * @return the user DTO with collections
     */
    private UserDTO convertToDTO(User user) {
        try {
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);

            // Ensure collections are never null in the DTO
            if (userDTO.getAddresses() == null) {
                userDTO.setAddresses(new ArrayList<>());
            }

            if (userDTO.getNotificationPreferences() == null) {
                userDTO.setNotificationPreferences(new ArrayList<>());
            }

            return userDTO;
        } catch (Exception e) {
            logger.error("Error mapping User to UserDTO: {}", e.getMessage(), e);
            // Create a manual mapping as fallback
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setPhoneNumber(user.getPhoneNumber());
            userDTO.setRole(user.getRole());
            userDTO.setPreferredLanguage(user.getPreferredLanguage());
            userDTO.setActive(user.isActive());
            userDTO.setCreatedAt(user.getCreatedAt());
            userDTO.setLastLoginAt(user.getLastLoginAt());
            userDTO.setAddresses(new ArrayList<>());
            userDTO.setNotificationPreferences(new ArrayList<>());
            return userDTO;
        }
    }

    /**
     * Convert User entity to UserDTO without including collections
     * @param user the user entity
     * @return the user DTO without collections
     */
    private UserDTO convertToBasicDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setRole(user.getRole());
        userDTO.setPreferredLanguage(user.getPreferredLanguage());
        userDTO.setActive(user.isActive());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setLastLoginAt(user.getLastLoginAt());

        // Initialize empty collections
        userDTO.setAddresses(new ArrayList<>());
        userDTO.setNotificationPreferences(new ArrayList<>());

        return userDTO;
    }

    public UserDTO updateUserStatus(Long id, boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setActive(isActive);
        user = userRepository.save(user);

        return modelMapper.map(user, UserDTO.class);
    }

    public UserDTO resetUserPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        user = userRepository.save(user);

        return modelMapper.map(user, UserDTO.class);
    }


    @Override
    public UserDTO updateUser(UserDTO userDTO) {
        User user = userRepository.findById(userDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userDTO.getId()));

        // Update the user properties
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setRole(userDTO.getRole());
        user.setPreferredLanguage(userDTO.getPreferredLanguage());
        user.setActive(userDTO.isActive());

        // Save the updated user
        user = userRepository.save(user);

        // Return the updated user DTO
        return modelMapper.map(user, UserDTO.class);
    }


}