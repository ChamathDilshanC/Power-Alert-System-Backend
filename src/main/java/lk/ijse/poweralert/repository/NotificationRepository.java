package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.Notification;
import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Find notifications for a user    */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Find notifications by status */
    List<Notification> findByStatus(NotificationStatus status);

    /** Find failed notifications to retry   */
    List<Notification> findByStatusAndCreatedAtAfter(NotificationStatus status, LocalDateTime createdAfter);

    /** Find notifications for a specific outage */
    List<Notification> findByOutageId(Long outageId);

    /** Find unread notifications for a user */
    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);
}