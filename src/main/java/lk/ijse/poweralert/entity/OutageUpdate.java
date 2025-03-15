package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lk.ijse.poweralert.enums.AppEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "outage_updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutageUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "outage_id", nullable = false)
    private Outage outage;

    @Column(name = "update_info", nullable = false)
    private String updateInfo;

    @Column(name = "updated_estimated_end_time")
    private LocalDateTime updatedEstimatedEndTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private AppEnums.OutageStatus newStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
