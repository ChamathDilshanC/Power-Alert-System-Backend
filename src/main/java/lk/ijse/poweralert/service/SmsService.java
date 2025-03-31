package lk.ijse.poweralert.service;

import java.util.concurrent.CompletableFuture;

public interface SmsService {
    CompletableFuture<Boolean> sendSms(String phoneNumber, String messageContent);
    boolean isValidPhoneNumber(String phoneNumber); // This method can stay as boolean since it's not @Async

    /**
     * Send an SMS using a template (default implementation returns unsupported)
     * This allows TwilioSmsServiceImpl to implement it without requiring casting
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