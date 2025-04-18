package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.info("Loading user by username/email: {}", usernameOrEmail);

        // Try to find by username first
        Optional<User> userOptional = userRepository.findByUsername(usernameOrEmail);

        // If not found by username, try by email
        if(userOptional.isEmpty()) {
            logger.debug("User not found by username, trying email...");
            userOptional = userRepository.findByEmail(usernameOrEmail);
        }

        User user = userOptional.orElseThrow(() -> {
            logger.error("User not found with username/email: {}", usernameOrEmail);
            return new UsernameNotFoundException("User not found with username/email: " + usernameOrEmail);
        });

        // Check if user is active
        if (!user.isActive()) {
            logger.error("User account is inactive: {}", usernameOrEmail);
            throw new UsernameNotFoundException("User account is inactive: " + usernameOrEmail);
        }

        logger.info("User found with role: {}", user.getRole());

        // Create Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}