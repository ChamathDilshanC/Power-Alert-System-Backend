package lk.ijse.poweralert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {
    private Long id;

    @NotBlank(message = "Area name is required")
    private String name;

    // Add these new fields
    private String city;
    private String postalCode;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Province is required")
    private String province;

    private String boundaryJson;

    // Add JsonIgnore to break the circular reference
    @JsonIgnore
    private List<UtilityProviderDTO> utilityProviders;
}