package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store device tokens for push notifications
 */
@Entity
@Table(name = "user_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_token", nullable = false, unique = true)
    private String deviceToken;

    @Column(name = "device_type", nullable = false)
    private String deviceType;  // "ANDROID", "IOS", "WEB"

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "fcm_token", unique = true)
    private String fcmToken;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}