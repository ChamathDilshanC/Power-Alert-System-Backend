package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public UserDTO registerUser(UserCreateDTO userCreateDTO) {
        logger.info("Registering new user with username: {}", userCreateDTO.getUsername());

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
        return convertToDTO(savedUser);
    }


    @Override
    public UserDTO getUserByEmail(String email) {
        logger.info("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        logger.info("Fetching user with username: {}", username);
        User user = userRepository.findByUsernameWithCollections(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        return convertToDTO(user);
    }

    @Override
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
    public void updateLastLogin(String email) {
        logger.info("Updating last login time for user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserDTO getUserById(Long id) {
        logger.info("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        return convertToDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        logger.info("Fetching all users");
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public UserDTO deactivateUser(Long id) {
        logger.info("Deactivating user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setActive(false);
        userRepository.save(user);

        return convertToDTO(user);
    }

    private UserDTO convertToDTO(User user) {
        try {
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
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

            // Empty collections to avoid null pointer exceptions
            userDTO.setAddresses(new ArrayList<>());
            userDTO.setNotificationPreferences(new ArrayList<>());

            return userDTO;
        }
    }
}