package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private Role role;
    private String preferredLanguage;
    private List<AddressDTO> addresses = new ArrayList<>();
    private List<NotificationPreferenceDTO> notificationPreferences = new ArrayList<>();

    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}