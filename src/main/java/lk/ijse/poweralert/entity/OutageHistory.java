package lk.ijse.poweralert.entity;

import jakarta.persistence.*;

import lk.ijse.poweralert.enums.AppEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "outage_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppEnums.OutageType type;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "outage_count", nullable = false)
    private int outageCount;

    @Column(name = "total_outage_hours", nullable = false)
    private double totalOutageHours;

    @Column(name = "average_restoration_time", nullable = false)
    private double averageRestorationTime;
}