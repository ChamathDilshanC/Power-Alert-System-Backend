package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    /** Find a device by its token   */
    Optional<UserDevice> findByDeviceToken(String deviceToken);

    /** Find a device by its FCM token   */
    Optional<UserDevice> findByFcmToken(String fcmToken);

    /** Find all active devices for a user   */
    List<UserDevice> findByUserIdAndIsActiveTrue(Long userId);

    /** Deactivate all devices for a user    */
    @Modifying
    @Query("UPDATE UserDevice d SET d.isActive = false WHERE d.user.id = :userId AND d.isActive = true")
    int deactivateAllUserDevices(@Param("userId") Long userId);
}