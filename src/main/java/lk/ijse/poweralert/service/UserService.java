package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.UserCreateDTO;
import lk.ijse.poweralert.dto.UserDTO;
import lk.ijse.poweralert.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    UserDTO registerUser(UserCreateDTO userCreateDTO);
    UserDTO getUserByEmail(String email);
    boolean existsByEmail(String email);

    void updateLastLogin(String email);
}