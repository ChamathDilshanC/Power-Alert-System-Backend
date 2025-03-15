package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import lk.ijse.poweralert.enums.AppEnums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long outageId;
    private Long userId;
    private NotificationType type;
    private NotificationStatus status;
    private String content;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
}
