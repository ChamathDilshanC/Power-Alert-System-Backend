package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageDTO {
    private Long id;

    @NotNull(message = "Outage type is required")
    private OutageType type;

    @NotNull(message = "Outage status is required")
    private OutageStatus status;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private LocalDateTime estimatedEndTime;
    private LocalDateTime actualEndTime;

    @NotNull(message = "Affected area is required")
    private AreaDTO affectedArea;

    private String geographicalAreaJson;
    private String reason;
    private String additionalInfo;

    @NotNull(message = "Utility provider is required")
    private UtilityProviderDTO utilityProvider;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OutageUpdateDTO> updates;
}