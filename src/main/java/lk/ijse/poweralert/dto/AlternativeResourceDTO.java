package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeResourceDTO {
    private Long id;

    @NotBlank(message = "Resource name is required")
    private String name;

    private String description;

    @NotNull(message = "Resource type is required")
    private ResourceType type;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private String contactNumber;
    private String operatingHours;
    private boolean isActive = true;

    // Add fields for image metadata but not the binary data itself
    private String imageName;
    private String imageContentType;

    // This field indicates if this resource has an image
    private boolean hasImage;

    @NotNull(message = "Area ID is required")
    private Long areaId;
}