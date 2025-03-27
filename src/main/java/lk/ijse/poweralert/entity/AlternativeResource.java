package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lk.ijse.poweralert.enums.AppEnums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alternative_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "operating_hours")
    private String operatingHours;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // Add these new fields for image storage
    @Lob
    @Column(name = "image_data", columnDefinition = "MEDIUMBLOB")
    private byte[] imageData;

    @Column(name = "image_content_type")
    private String imageContentType;

    @Column(name = "image_name")
    private String imageName;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;
}