package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.UtilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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

    // Replace with a simplified DTO that doesn't reference back to UtilityProviderDTO
    private List<AreaSummaryDTO> serviceAreas;

    // For receiving updates from the frontend
    private List<Long> serviceAreaIds;
}