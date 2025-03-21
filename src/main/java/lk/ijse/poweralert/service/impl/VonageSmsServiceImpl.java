package lk.ijse.poweralert.service.impl;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.messages.TextMessage;
import lk.ijse.poweralert.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.regex.Pattern;

@Service
@Primary
public class VonageSmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(VonageSmsServiceImpl.class);

    // E.164 phone number validation pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    @Value("${vonage.api.key}")
    private String apiKey;

    @Value("${vonage.api.secret}")
    private String apiSecret;

    @Value("${vonage.brand.name}")
    private String brandName;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    private VonageClient vonageClient;

    @PostConstruct
    public void init() {
        if (smsEnabled && !apiKey.isEmpty() && !apiSecret.isEmpty()) {
            try {
                vonageClient = VonageClient.builder()
                        .apiKey(apiKey)
                        .apiSecret(apiSecret)
                        .build();
                logger.info("Vonage client initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Vonage client", e);
                // Disable SMS if initialization fails
                smsEnabled = false;
            }
        } else {
            logger.info("SMS service is disabled or not fully configured");
        }
    }

    @Override
    @Async
    public boolean sendSms(String phoneNumber, String messageContent) {
        if (!smsEnabled) {
            logger.info("SMS sending is disabled. Would have sent to: {}, message: {}", phoneNumber, messageContent);
            return true;
        }

        // Format phone number if needed
        String formattedNumber = formatPhoneNumber(phoneNumber);

        if (!isValidPhoneNumber(formattedNumber)) {
            logger.error("Invalid phone number format: {}", formattedNumber);
            return false;
        }

        try {
            logger.info("Sending SMS to: {}", formattedNumber);

            TextMessage message = new TextMessage(
                    brandName,         // From
                    formattedNumber,   // To
                    messageContent     // Message content
            );

            SmsSubmissionResponse response = vonageClient.getSmsClient().submitMessage(message);

            if (response.getMessages() != null && !response.getMessages().isEmpty()) {
                MessageStatus status = response.getMessages().get(0).getStatus();

                if (status == MessageStatus.OK) {
                    logger.info("SMS sent successfully to {} with message ID: {}",
                            formattedNumber, response.getMessages().get(0).getId());
                    return true;
                } else {
                    logger.error("Failed to send SMS to {}, Error: {} - {}",
                            formattedNumber,
                            status,
                            response.getMessages().get(0).getErrorText());
                    return false;
                }
            } else {
                logger.error("Unexpected response from Vonage: No messages in response");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending SMS to {}", formattedNumber, e);
            return false;
        }
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Check if phone number matches E.164 format
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * Format phone number to E.164 format
     */
    private String formatPhoneNumber(String phoneNumber) {
        // Remove any whitespace or special characters
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // If it doesn't start with +, add it
        if (!cleaned.startsWith("+")) {
            // If it starts with 0, replace with +94
            if (cleaned.startsWith("0")) {
                return "+94" + cleaned.substring(1);
            }

            // If it starts with 94, add +
            if (cleaned.startsWith("94")) {
                return "+" + cleaned;
            }

            // Otherwise assume it's a local number and add +94
            return "+94" + cleaned;
        }

        return cleaned;
    }
}