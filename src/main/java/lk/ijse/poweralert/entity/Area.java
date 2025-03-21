package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String province;

    @Column(name = "boundary_json", columnDefinition = "TEXT")
    private String boundaryJson;

    @ManyToMany
    @JoinTable(
            name = "area_utility_providers",
            joinColumns = @JoinColumn(name = "area_id"),
            inverseJoinColumns = @JoinColumn(name = "utility_provider_id")
    )
    private List<UtilityProvider> utilityProviders;

    @OneToMany(mappedBy = "affectedArea")
    private List<Outage> outages;

    @OneToMany(mappedBy = "area")
    private List<AlternativeResource> alternativeResources;
}