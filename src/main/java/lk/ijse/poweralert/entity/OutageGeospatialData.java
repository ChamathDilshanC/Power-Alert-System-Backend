package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity to store geospatial data for outages
 */
@Entity
@Table(name = "outage_geospatial_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutageGeospatialData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "outage_id", nullable = false)
    private Outage outage;

    @Column(name = "geo_json", columnDefinition = "TEXT", nullable = false)
    private String geoJson;

    @Column(name = "center_latitude")
    private Double centerLatitude;

    @Column(name = "center_longitude")
    private Double centerLongitude;

    @Column(name = "bounding_box_north")
    private Double boundingBoxNorth;

    @Column(name = "bounding_box_south")
    private Double boundingBoxSouth;

    @Column(name = "bounding_box_east")
    private Double boundingBoxEast;

    @Column(name = "bounding_box_west")
    private Double boundingBoxWest;

    @Column(name = "affected_population_estimate")
    private Integer affectedPopulationEstimate;

    @Column(name = "static_map_url", columnDefinition = "TEXT")
    private String staticMapUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Pre-persist hook to set timestamps
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Pre-update hook to update timestamp
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}