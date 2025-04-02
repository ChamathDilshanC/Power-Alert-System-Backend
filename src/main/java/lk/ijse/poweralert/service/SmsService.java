package lk.ijse.poweralert.service;

import java.util.concurrent.CompletableFuture;

public interface SmsService {
    /**
     * Send an SMS message directly
     *
     * @param phoneNumber The recipient's phone number
     * @param messageContent The message content
     * @return CompletableFuture indicating success or failure
     */
    CompletableFuture<Boolean> sendSms(String phoneNumber, String messageContent);

    /**
     * Check if a phone number is valid
     *
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    boolean isValidPhoneNumber(String phoneNumber);

    /**
     * Send an SMS using a template
     *
     * @param phoneNumber The recipient's phone number
     * @param templateKey The template key (e.g., "outage.new", "outage.update")
     * @param params The parameters to substitute in the template
     * @param language The language code ("en", "si", "ta")
     * @return CompletableFuture indicating success or failure
     */
    default CompletableFuture<Boolean> sendTemplatedSms(String phoneNumber, String templateKey,
                                                        String[] params, String language) {
        // Default implementation just sends the templateKey as the message
        return sendSms(phoneNumber, "PowerAlert notification: " + templateKey);
    }
}