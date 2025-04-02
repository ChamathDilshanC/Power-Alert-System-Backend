package lk.ijse.poweralert.config;

import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums.Role;
import lk.ijse.poweralert.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.email:alerts.poweralert@gmail.com}")
    private String adminEmail;

    @Value("${admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user exists
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhoneNumber("+94771234567");
            admin.setRole(Role.ADMIN);
            admin.setPreferredLanguage("en");
            admin.setActive(true);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);

            System.out.println("Initial admin user created with email: " + adminEmail);
        }
    }
}