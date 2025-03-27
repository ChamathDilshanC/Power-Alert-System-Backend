package lk.ijse.poweralert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceImageDTO {
    private Long resourceId;
    private String imageName;
    private String contentType;
    private byte[] data;
}