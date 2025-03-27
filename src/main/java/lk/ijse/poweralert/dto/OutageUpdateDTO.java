package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
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
public class OutageUpdateDTO {
    private Long id;

    @NotNull(message = "Outage ID is required")
    private Long outageId;

    @NotBlank(message = "Update information is required")
    private String updateInfo;

    private LocalDateTime updatedEstimatedEndTime;
    private String reason;
    private OutageStatus newStatus;
}