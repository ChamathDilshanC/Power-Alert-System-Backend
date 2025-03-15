package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.NotificationType;
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
public class NotificationPreferenceDTO {
    private Long id;

    @NotNull(message = "Outage type is required")
    private OutageType outageType;

    @NotNull(message = "Notification channel type is required")
    private NotificationType channelType;

    private boolean enabled = true;

    @Min(value = 0, message = "Advance notice minutes cannot be negative")
    private int advanceNoticeMinutes = 60; // 1 hour by default

    private boolean receiveUpdates = true;
    private boolean receiveRestoration = true;
}