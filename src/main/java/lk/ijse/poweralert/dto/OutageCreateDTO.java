package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageCreateDTO {
    @NotNull(message = "Outage type is required")
    private OutageType type;

    private OutageStatus status = OutageStatus.SCHEDULED;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @Future(message = "Estimated end time must be in the future")
    private LocalDateTime estimatedEndTime;

    @NotNull(message = "Affected area ID is required")
    private Long areaId;

    private String geographicalAreaJson;
    private String reason;
    private String additionalInfo;

    @NotNull(message = "Utility provider ID is required")
    private Long utilityProviderId;
}
