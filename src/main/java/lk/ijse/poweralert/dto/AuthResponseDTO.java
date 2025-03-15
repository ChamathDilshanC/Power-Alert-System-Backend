package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String username;
    private Role role;
    private String email;
    private Long userId;
}