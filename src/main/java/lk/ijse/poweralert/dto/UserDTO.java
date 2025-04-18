package lk.ijse.poweralert.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private boolean active;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
}