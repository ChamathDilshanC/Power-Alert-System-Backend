package lk.ijse.poweralert.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class AuthDTO {
    @Email
    private String email;
    private String token;
    private String role;
    private String username;
    private  Long userId;
}