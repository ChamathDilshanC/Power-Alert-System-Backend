package lk.ijse.poweralert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lk.ijse.poweralert.enums.AppEnums;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRegistrationDTO {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must have at least 8 characters and include uppercase, lowercase, number, and special character")
    private String password;

    @NotBlank(message = "Provider name is required")
    private String name;

    @NotNull(message = "Utility type is required")
    private AppEnums.UtilityType type;

    @Email(message = "Contact email should be valid")
    @NotBlank(message = "Contact email is required")
    private String contactEmail;

    private String contactPhone;
    private String website;
    private List<Long> serviceAreaIds;
}