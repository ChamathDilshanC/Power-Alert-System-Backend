package lk.ijse.poweralert.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Add these new fields
    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String province;

    @Column(name = "boundary_json", columnDefinition = "TEXT")
    private String boundaryJson;

    @JsonBackReference
    @ManyToMany(mappedBy = "serviceAreas")
    private List<UtilityProvider> utilityProviders = new ArrayList<>();

    @OneToMany(mappedBy = "affectedArea")
    private List<Outage> outages;

    @OneToMany(mappedBy = "area")
    private List<AlternativeResource> alternativeResources;
    @Column
    private Double latitude;

    @Column
    private Double longitude;
}