package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.AddressDTO;
import lk.ijse.poweralert.dto.NotificationPreferenceDTO;
import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.entity.User;

import java.util.List;

public interface UserService {

    /**
     * Register a new user
     * @param userCreateDTO the user creation data
     * @return the registered user
     */
    UserDTO registerUser(UserCreateDTO userCreateDTO);

    UserDTO updateUser(UserDTO userDTO);
    /**
     * Get user by email
     * @param email the user email
     * @return the user with the given email
     */
    UserDTO getUserByEmail(String email);

    /**
     * Get user by username with all collections
     * @param username the username
     * @return the user with the given username including collections
     */
    UserDTO getUserByUsername(String username);

    /**
     * Get basic user information by username without loading collections
     * @param username the username
     * @return the user with the given username (basic info only)
     */
    UserDTO getUserBasicInfo(String username);

    /**
     * Get user addresses by user ID
     * @param userId the user ID
     * @return list of user addresses
     */
    List<AddressDTO> getUserAddresses(Long userId);

    /**
     * Get user notification preferences by user ID
     * @param userId the user ID
     * @return list of user notification preferences
     */
    List<NotificationPreferenceDTO> getUserNotificationPreferences(Long userId);

    /**
     * Get user entity by username
     * @param username the username
     * @return the user entity with the given username
     */
    User getUserEntityByUsername(String username);

    /**
     * Check if a user with the given email exists
     * @param email the email
     * @return true if a user with the given email exists
     */
    boolean existsByEmail(String email);

    /**
     * Update the user's last login time
     * @param email the user email
     */
    void updateLastLogin(String email);

    /**
     * Get user by ID
     * @param id the user ID
     * @return the user with the given ID
     */
    UserDTO getUserById(Long id);

    /**
     * Get all users
     * @return list of all users
     */
    List<UserDTO> getAllUsers();

    /**
     * Deactivate a user
     * @param id the user ID
     * @return the deactivated user
     */
    UserDTO deactivateUser(Long id);

    UserDTO updateUserStatus(Long id, boolean newStatus);

    UserDTO resetUserPassword(Long id, String newPassword);
}