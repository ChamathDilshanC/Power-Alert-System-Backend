package lk.ijse.poweralert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageGeospatialDTO {
    private Long id;

    @NotNull(message = "Outage ID is required")
    private Long outageId;

    @NotBlank(message = "GeoJSON is required")
    private String geoJson;

    private Double centerLatitude;
    private Double centerLongitude;

    private Double boundingBoxNorth;
    private Double boundingBoxSouth;
    private Double boundingBoxEast;
    private Double boundingBoxWest;

    private Integer affectedPopulationEstimate;
    private String staticMapUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}