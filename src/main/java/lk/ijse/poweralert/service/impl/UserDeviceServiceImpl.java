package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.entity.UserDevice;
import lk.ijse.poweralert.repository.UserDeviceRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.UserDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the UserDeviceService interface
 */
@Service
public class UserDeviceServiceImpl implements UserDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(UserDeviceServiceImpl.class);

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDevice registerDevice(Long userId, String deviceToken, String deviceType, String deviceName, String fcmToken) {
        logger.info("Registering device for user ID: {}, device type: {}", userId, deviceType);

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Check if device token already exists
        userDeviceRepository.findByDeviceToken(deviceToken).ifPresent(device -> {
            logger.info("Device token already exists, updating existing device");
            device.setFcmToken(fcmToken);
            device.setActive(true);
            device.setLastLoginDate(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());
            userDeviceRepository.save(device);
        });

        // Check if FCM token already exists
        userDeviceRepository.findByFcmToken(fcmToken).ifPresent(device -> {
            logger.info("FCM token already exists, updating existing device");
            device.setDeviceToken(deviceToken);
            device.setActive(true);
            device.setLastLoginDate(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());
            userDeviceRepository.save(device);
        });

        // Create new device
        UserDevice userDevice = new UserDevice();
        userDevice.setUser(user);
        userDevice.setDeviceToken(deviceToken);
        userDevice.setDeviceType(deviceType);
        userDevice.setDeviceName(deviceName);
        userDevice.setFcmToken(fcmToken);
        userDevice.setActive(true);
        userDevice.setLastLoginDate(LocalDateTime.now());
        userDevice.setCreatedAt(LocalDateTime.now());
        userDevice.setUpdatedAt(LocalDateTime.now());

        UserDevice savedDevice = userDeviceRepository.save(userDevice);
        logger.info("Device registered with ID: {}", savedDevice.getId());

        return savedDevice;
    }

    @Override
    @Transactional
    public UserDevice updateDevice(Long deviceId, String fcmToken, boolean isActive) {
        logger.info("Updating device ID: {}", deviceId);

        UserDevice userDevice = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with ID: " + deviceId));

        userDevice.setFcmToken(fcmToken);
        userDevice.setActive(isActive);
        userDevice.setLastLoginDate(LocalDateTime.now());
        userDevice.setUpdatedAt(LocalDateTime.now());

        UserDevice updatedDevice = userDeviceRepository.save(userDevice);
        logger.info("Device updated with ID: {}", updatedDevice.getId());

        return updatedDevice;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDevice> getActiveDevices(Long userId) {
        logger.info("Getting active devices for user ID: {}", userId);

        return userDeviceRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDevice getDeviceById(Long deviceId) {
        logger.info("Getting device with ID: {}", deviceId);

        return userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with ID: " + deviceId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDevice getDeviceByToken(String deviceToken) {
        logger.info("Getting device with token: {}", deviceToken);

        return userDeviceRepository.findByDeviceToken(deviceToken)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with token: " + deviceToken));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDevice getDeviceByFcmToken(String fcmToken) {
        logger.info("Getting device with FCM token: {}", fcmToken);

        return userDeviceRepository.findByFcmToken(fcmToken)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with FCM token: " + fcmToken));
    }

    @Override
    @Transactional
    public boolean deactivateDevice(Long deviceId) {
        logger.info("Deactivating device with ID: {}", deviceId);

        UserDevice userDevice = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with ID: " + deviceId));

        userDevice.setActive(false);
        userDevice.setUpdatedAt(LocalDateTime.now());
        userDeviceRepository.save(userDevice);

        return true;
    }

    @Override
    @Transactional
    public int deactivateAllUserDevices(Long userId) {
        logger.info("Deactivating all devices for user ID: {}", userId);

        return userDeviceRepository.deactivateAllUserDevices(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getFcmTokensForUser(Long userId) {
        logger.info("Getting FCM tokens for user ID: {}", userId);

        List<UserDevice> activeDevices = userDeviceRepository.findByUserIdAndIsActiveTrue(userId);

        return activeDevices.stream()
                .map(UserDevice::getFcmToken)
                .filter(token -> token != null && !token.isEmpty())
                .collect(Collectors.toList());
    }
}