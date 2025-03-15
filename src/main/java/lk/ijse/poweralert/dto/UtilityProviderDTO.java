package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.UtilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityProviderDTO {
    private Long id;

    @NotBlank(message = "Provider name is required")
    private String name;

    @Email(message = "Email should be valid")
    private String contactEmail;

    private String contactPhone;
    private String website;

    @NotNull(message = "Utility type is required")
    private UtilityType type;
}
