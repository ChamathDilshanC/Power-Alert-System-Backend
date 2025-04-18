package lk.ijse.poweralert.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import lk.ijse.poweralert.enums.AppEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.ArrayList;
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

    @JsonIgnore
    @OneToMany(mappedBy = "utilityProvider")
    private List<User> users;

    @JsonIgnore
    @OneToMany(mappedBy = "utilityProvider")
    private List<Outage> outages;

    @JsonManagedReference
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "provider_service_areas",
            joinColumns = @JoinColumn(name = "provider_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id")
    )
    private List<Area> serviceAreas = new ArrayList<>();

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "api_key")
    private String apiKey;
}
