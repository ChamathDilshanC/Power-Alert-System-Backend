package lk.ijse.poweralert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaSummaryDTO {
    private Long id;
    private String name;
    private String district;
    private String province;
}