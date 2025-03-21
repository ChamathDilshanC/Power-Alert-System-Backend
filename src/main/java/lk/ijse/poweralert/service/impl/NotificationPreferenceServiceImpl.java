package lk.ijse.poweralert.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lk.ijse.poweralert.dto.NotificationPreferenceDTO;
import lk.ijse.poweralert.entity.NotificationPreference;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.NotificationPreferenceRepository;
import lk.ijse.poweralert.service.NotificationPreferenceService;
import lk.ijse.poweralert.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceServiceImpl.class);

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDTO> getCurrentUserPreferences() {
        logger.info("Fetching notification preferences for current user");

        // Get current user
        User user = getCurrentUser();

        // Get preferences for the user
        List<NotificationPreference> preferences = notificationPreferenceRepository.findByUser(user);

        return preferences.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationPreferenceDTO addPreference(NotificationPreferenceDTO preferenceDTO) {
        logger.info("Adding new notification preference");

        // Get current user
        User user = getCurrentUser();

        // Create new preference entity
        NotificationPreference preference = new NotificationPreference();
        preference.setUser(user);
        preference.setOutageType(preferenceDTO.getOutageType());
        preference.setChannelType(preferenceDTO.getChannelType());
        preference.setEnabled(preferenceDTO.isEnabled());
        preference.setAdvanceNoticeMinutes(preferenceDTO.getAdvanceNoticeMinutes());
        preference.setReceiveUpdates(preferenceDTO.isReceiveUpdates());
        preference.setReceiveRestoration(preferenceDTO.isReceiveRestoration());

        // Save preference
        NotificationPreference savedPreference = notificationPreferenceRepository.save(preference);
        logger.info("Notification preference saved with ID: {}", savedPreference.getId());

        return convertToDTO(savedPreference);
    }

    @Override
    @Transactional
    public NotificationPreferenceDTO updatePreference(NotificationPreferenceDTO preferenceDTO) {
        logger.info("Updating notification preference with ID: {}", preferenceDTO.getId());

        // Get current user
        User user = getCurrentUser();

        // Find existing preference
        NotificationPreference preference = notificationPreferenceRepository.findById(preferenceDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException("Notification preference not found with ID: " + preferenceDTO.getId()));

        // Verify ownership
        if (!preference.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to update this notification preference");
        }

        // Update fields
        preference.setOutageType(preferenceDTO.getOutageType());
        preference.setChannelType(preferenceDTO.getChannelType());
        preference.setEnabled(preferenceDTO.isEnabled());
        preference.setAdvanceNoticeMinutes(preferenceDTO.getAdvanceNoticeMinutes());
        preference.setReceiveUpdates(preferenceDTO.isReceiveUpdates());
        preference.setReceiveRestoration(preferenceDTO.isReceiveRestoration());

        // Save updated preference
        NotificationPreference updatedPreference = notificationPreferenceRepository.save(preference);
        logger.info("Notification preference updated with ID: {}", updatedPreference.getId());

        return convertToDTO(updatedPreference);
    }

    @Override
    @Transactional
    public boolean deletePreference(Long id) {
        logger.info("Deleting notification preference with ID: {}", id);

        // Get current user
        User user = getCurrentUser();

        // Find existing preference
        NotificationPreference preference = notificationPreferenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification preference not found with ID: " + id));

        // Verify ownership
        if (!preference.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this notification preference");
        }

        // Delete preference
        notificationPreferenceRepository.delete(preference);
        logger.info("Notification preference deleted with ID: {}", id);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceDTO getPreferenceById(Long id) {
        logger.info("Fetching notification preference with ID: {}", id);

        // Get current user
        User user = getCurrentUser();

        // Find preference
        NotificationPreference preference = notificationPreferenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification preference not found with ID: " + id));

        // Verify ownership
        if (!preference.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to access this notification preference");
        }

        return convertToDTO(preference);
    }

    /**
     * Get the current logged-in user
     * @return the user entity
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserEntityByUsername(username);
    }

    /**
     * Convert NotificationPreference entity to DTO
     * @param preference the entity to convert
     * @return the DTO
     */
    private NotificationPreferenceDTO convertToDTO(NotificationPreference preference) {
        return modelMapper.map(preference, NotificationPreferenceDTO.class);
    }
}