package lk.ijse.poweralert.entity;

import jakarta.persistence.*;

import lk.ijse.poweralert.enums.AppEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "outage_type", nullable = false)
    private AppEnums.OutageType outageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private AppEnums.NotificationType channelType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "advance_notice_minutes", nullable = false)
    private int advanceNoticeMinutes;

    @Column(name = "receive_updates", nullable = false)
    private boolean receiveUpdates;

    @Column(name = "receive_restoration", nullable = false)
    private boolean receiveRestoration;
}
