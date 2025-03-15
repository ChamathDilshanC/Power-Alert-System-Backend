package lk.ijse.poweralert.dto;

import lk.ijse.poweralert.enums.AppEnums.FeedbackType;
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
public class CommunityFeedbackDTO {
    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Outage ID is required")
    private Long outageId;

    @NotBlank(message = "Feedback text is required")
    private String feedbackText;

    @NotNull(message = "Feedback type is required")
    private FeedbackType type;

    private boolean isAnonymous = false;
    private LocalDateTime createdAt;
    private Double latitude;
    private Double longitude;
}