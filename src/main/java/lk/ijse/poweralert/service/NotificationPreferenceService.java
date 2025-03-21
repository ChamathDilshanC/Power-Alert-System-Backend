package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.NotificationPreferenceDTO;

import java.util.List;

public interface NotificationPreferenceService {

    /**
     * Get all notification preferences for the current user
     * @return list of notification preferences
     */
    List<NotificationPreferenceDTO> getCurrentUserPreferences();

    /**
     * Add a new notification preference for the current user
     * @param preferenceDTO the notification preference to add
     * @return the saved notification preference
     */
    NotificationPreferenceDTO addPreference(NotificationPreferenceDTO preferenceDTO);

    /**
     * Update an existing notification preference
     * @param preferenceDTO the notification preference to update
     * @return the updated notification preference
     */
    NotificationPreferenceDTO updatePreference(NotificationPreferenceDTO preferenceDTO);

    /**
     * Delete a notification preference
     * @param id the ID of the notification preference to delete
     * @return true if deletion was successful
     */
    boolean deletePreference(Long id);

    /**
     * Get notification preference by ID
     * @param id the ID of the notification preference
     * @return the notification preference
     */
    NotificationPreferenceDTO getPreferenceById(Long id);
}