package lk.ijse.poweralert.service;

import lk.ijse.poweralert.entity.UserDevice;

import java.util.List;

public interface UserDeviceService {

    /**
     * Register a new device for a user
     *
     * @param userId the user ID
     * @param deviceToken the unique device identifier
     * @param deviceType the device type (ANDROID, IOS, WEB)
     * @param deviceName the device name (optional)
     * @param fcmToken the Firebase Cloud Messaging token
     * @return the created user device
     */
    UserDevice registerDevice(Long userId, String deviceToken, String deviceType, String deviceName, String fcmToken);

    /**
     * Update an existing device
     *
     * @param deviceId the device ID
     * @param fcmToken the new FCM token
     * @param isActive whether the device is active
     * @return the updated user device
     */
    UserDevice updateDevice(Long deviceId, String fcmToken, boolean isActive);

    /**
     * Get all active devices for a user
     *
     * @param userId the user ID
     * @return list of active devices
     */
    List<UserDevice> getActiveDevices(Long userId);

    /**
     * Get device by ID
     *
     * @param deviceId the device ID
     * @return the user device
     */
    UserDevice getDeviceById(Long deviceId);

    /**
     * Get device by token
     *
     * @param deviceToken the device token
     * @return the user device
     */
    UserDevice getDeviceByToken(String deviceToken);

    /**
     * Get device by FCM token
     *
     * @param fcmToken the FCM token
     * @return the user device
     */
    UserDevice getDeviceByFcmToken(String fcmToken);

    /**
     * Deactivate a device
     *
     * @param deviceId the device ID
     * @return true if deactivated successfully
     */
    boolean deactivateDevice(Long deviceId);

    /**
     * Deactivate all devices for a user
     *
     * @param userId the user ID
     * @return number of devices deactivated
     */
    int deactivateAllUserDevices(Long userId);

    /**
     * Get FCM tokens for a user
     *
     * @param userId the user ID
     * @return list of FCM tokens
     */
    List<String> getFcmTokensForUser(Long userId);
}