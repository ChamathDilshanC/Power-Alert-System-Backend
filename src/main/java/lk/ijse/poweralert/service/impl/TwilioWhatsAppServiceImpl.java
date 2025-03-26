package lk.ijse.poweralert.service.impl;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lk.ijse.poweralert.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class TwilioWhatsAppServiceImpl implements WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioWhatsAppServiceImpl.class);

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number:}")
    private String whatsappNumber;

    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    // Cache for template message mappings
    private final Map<String, String> templateMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (whatsappEnabled && !accountSid.isEmpty() && !authToken.isEmpty()) {
            try {
                // Initialize Twilio if not already initialized
                if (Twilio.getRestClient() == null) {
                    Twilio.init(accountSid, authToken);
                }
                logger.info("WhatsApp service initialized successfully");

                // Initialize template mappings
                initTemplateMap();
            } catch (Exception e) {
                logger.error("Failed to initialize WhatsApp service", e);
                whatsappEnabled = false;
            }
        } else {
            logger.info("WhatsApp service is disabled or not fully configured");
        }
    }

    private void initTemplateMap() {
        // Add template mappings for outage notifications
        templateMap.put("outage_notification", "Your {{1}} service will be interrupted in {{2}} from {{3}} to {{4}}. Reason: {{5}}");
        templateMap.put("outage_update", "Update on your {{1}} outage in {{2}}: Status is now {{3}}. Estimated restoration: {{4}}");
        templateMap.put("outage_restoration", "Good news! {{1}} services in {{2}} have been restored. Thank you for your patience.");
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendWhatsAppMessage(String phoneNumber, String message) {
        if (!whatsappEnabled) {
            logger.info("WhatsApp sending is disabled. Would have sent to: {}, message: {}", phoneNumber, message);
            return CompletableFuture.completedFuture(true);
        }

        try {
            logger.info("Sending WhatsApp message to: {}", phoneNumber);

            // Format WhatsApp numbers
            String fromNumber = formatWhatsAppNumber(whatsappNumber);
            String toNumber = formatWhatsAppNumber(phoneNumber);

            Message message1 = Message.creator(
                            new PhoneNumber(toNumber),
                            new PhoneNumber(fromNumber),
                            message)
                    .create();

            logger.info("WhatsApp message sent successfully, SID: {}", message1.getSid());
            return CompletableFuture.completedFuture(true);
        } catch (ApiException e) {
            logger.error("Twilio API error sending WhatsApp message to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp message to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendTemplateMessage(String phoneNumber, String templateName, String[] parameters) {
        if (!whatsappEnabled) {
            logger.info("WhatsApp template sending is disabled. Would have sent to: {}, template: {}",
                    phoneNumber, templateName);
            return CompletableFuture.completedFuture(true);
        }

        try {
            String templateContent = getTemplateContent(templateName, parameters);
            if (templateContent == null) {
                logger.error("Template not found: {}", templateName);
                return CompletableFuture.completedFuture(false);
            }

            // Since sendWhatsAppMessage now returns a CompletableFuture<Boolean>,
            // we need to handle it differently
            return sendWhatsAppMessage(phoneNumber, templateContent);
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp template message to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    /**
     * Format phone number for WhatsApp
     * WhatsApp requires numbers in format: whatsapp:+1234567890
     */
    private String formatWhatsAppNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }

        // Remove any existing prefix
        String cleanNumber = phoneNumber.replace("whatsapp:", "").trim();

        // Ensure number starts with +
        if (!cleanNumber.startsWith("+")) {
            cleanNumber = "+" + cleanNumber;
        }

        // Add WhatsApp prefix
        return "whatsapp:" + cleanNumber;
    }

    /**
     * Get template content with parameters applied
     */
    private String getTemplateContent(String templateName, String[] parameters) {
        String template = templateMap.get(templateName);
        if (template == null) {
            return null;
        }

        // Replace parameters in template
        String result = template;
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                result = result.replace("{{" + (i + 1) + "}}", parameters[i]);
            }
        }

        return result;
    }
}