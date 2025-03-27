package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.NotificationPreference;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Find all notification preferences for a specific user
     *
     * @param user the user
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUser(User user);

    /**
     * Find all notification preferences for a user by user ID
     *
     * @param userId the user ID
     * @return list of notification preferences
     */
    List<NotificationPreference> findByUserId(Long userId);

    /**
     * Find notification preferences by user, outage type, and channel type
     */
    List<NotificationPreference> findByUserAndOutageTypeAndChannelType(
            User user, AppEnums.OutageType outageType, AppEnums.NotificationType channelType);
}