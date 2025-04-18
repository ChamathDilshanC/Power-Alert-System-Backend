package lk.ijse.poweralert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateDTO {
    @NotBlank(message = "Message content is required")
    private String content;

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;
}