package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.NotificationPreference;
import lk.ijse.poweralert.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Find notification preferences by user
     * @param user the user
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUser(User user);

    /**
     * Find notification preferences by user ID
     * @param userId the user ID
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserId(Long userId);

    /**
     * Check if a notification preference exists for the given user and ID
     * @param id the notification preference ID
     * @param userId the user ID
     * @return true if the notification preference exists for the user
     */
    boolean existsByIdAndUserId(Long id, Long userId);
}