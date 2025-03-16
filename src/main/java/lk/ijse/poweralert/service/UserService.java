package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.entity.User;

public interface UserService {

    /**
     * Register a new user
     * @param userCreateDTO the user creation data
     * @return the registered user
     */
    UserDTO registerUser(UserCreateDTO userCreateDTO);

    /**
     * Get user by email
     * @param email the user email
     * @return the user with the given email
     */
    UserDTO getUserByEmail(String email);

    /**
     * Get user by username
     * @param username the username
     * @return the user with the given username
     */
    UserDTO getUserByUsername(String username);

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
}