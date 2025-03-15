package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "outages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Outage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutageType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutageStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area affectedArea;

    @Column(name = "geographical_area_json", columnDefinition = "TEXT")
    private String geographicalAreaJson;

    private String reason;

    @Column(name = "additional_info")
    private String additionalInfo;

    @ManyToOne
    @JoinColumn(name = "utility_provider_id", nullable = false)
    private UtilityProvider utilityProvider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "outage", cascade = CascadeType.ALL)
    private List<OutageUpdate> updates;

    @OneToMany(mappedBy = "outage")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "outage")
    private List<CommunityFeedback> feedbacks;
}