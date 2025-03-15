package lk.ijse.poweralert.entity;

import jakarta.persistence.*;

import lk.ijse.poweralert.enums.AppEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Entity
@Table(name = "utility_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilityProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppEnums.UtilityType type;

    @OneToMany(mappedBy = "utilityProvider")
    private List<Outage> outages;

    @ManyToMany(mappedBy = "utilityProviders")
    private List<Area> serviceAreas;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "api_key")
    private String apiKey;
}
