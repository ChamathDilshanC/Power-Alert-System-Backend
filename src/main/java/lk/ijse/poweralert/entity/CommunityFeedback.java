package lk.ijse.poweralert.entity;

import jakarta.persistence.*;
import lk.ijse.poweralert.enums.AppEnums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "outage_id", nullable = false)
    private Outage outage;

    @Column(name = "feedback_text", nullable = false)
    private String feedbackText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType type;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private Double latitude;

    private Double longitude;

}