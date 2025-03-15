package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutageHistoryDTO {
    private Long id;

    @NotNull(message = "Area ID is required")
    private Long areaId;

    @NotNull(message = "Outage type is required")
    private OutageType type;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    private int year;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    private int month;

    @Min(value = 0, message = "Outage count cannot be negative")
    private int outageCount;

    @Min(value = 0, message = "Total outage hours cannot be negative")
    private double totalOutageHours;

    @Min(value = 0, message = "Average restoration time cannot be negative")
    private double averageRestorationTime;
}