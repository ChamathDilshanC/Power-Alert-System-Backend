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
import java.util.ResourceBundle;
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
        // Add default template mappings for outage notifications
        templateMap.put("outage_notification", "Your {{1}} service will be interrupted in {{2}} from {{3}} to {{4}}. Reason: {{5}}");
        templateMap.put("outage_update", "Update on your {{1}} outage in {{2}}: Status is now {{3}}. Estimated restoration: {{4}}");
        templateMap.put("outage_restoration", "Good news! {{1}} services in {{2}} have been restored. Thank you for your patience.");
        templateMap.put("outage_cancellation", "The scheduled {{1}} outage in {{2}} for {{3}} has been cancelled.");

        // Add mappings for outage.new, outage.update, etc. that match message keys in ResourceBundle
        templateMap.put("outage.new", "{{0}} outage scheduled in {{1}} from {{2}} to {{3}}. Reason: {{4}}");
        templateMap.put("outage.update", "{{0}} outage in {{1}} status updated to {{2}}. Estimated end time: {{3}}");
        templateMap.put("outage.cancelled", "{{0}} outage in {{1}} scheduled for {{2}} has been cancelled");
        templateMap.put("outage.restored", "{{0}} services in {{1}} have been restored");

        // Try to load from resource bundles for better localization
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            if (bundle.containsKey("outage.new")) {
                String template = bundle.getString("outage.new");
                // Convert from {0} format to {{0}} format for WhatsApp
                template = template.replaceAll("\\{(\\d+)\\}", "{{$1}}");
                templateMap.put("outage.new", template);
            }
            // Same for other keys
        } catch (Exception e) {
            logger.warn("Could not load templates from resource bundle, using defaults", e);
        }

        // Add language-specific templates
        templateMap.put("outage_notification_si", "ඔබගේ {{2}} ප්‍රදේශයේ {{1}} සේවාව {{3}} සිට {{4}} දක්වා අත්හිටුවනු ලැබේ. හේතුව: {{5}}");
        templateMap.put("outage_notification_ta", "உங்கள் {{2}} பகுதியில் {{1}} சேவை {{3}} முதல் {{4}} வரை தடைப்படும். காரணம்: {{5}}");

        // Add mappings for outage.new_si, outage.new_ta, etc.
        try {
            ResourceBundle bundleSi = ResourceBundle.getBundle("messages_si");
            if (bundleSi.containsKey("outage.new")) {
                String template = bundleSi.getString("outage.new");
                template = template.replaceAll("\\{(\\d+)\\}", "{{$1}}");
                templateMap.put("outage.new_si", template);
            }
        } catch (Exception e) {
            logger.warn("Could not load Sinhala templates", e);
        }

        try {
            ResourceBundle bundleTa = ResourceBundle.getBundle("messages_ta");
            if (bundleTa.containsKey("outage.new")) {
                String template = bundleTa.getString("outage.new");
                template = template.replaceAll("\\{(\\d+)\\}", "{{$1}}");
                templateMap.put("outage.new_ta", template);
            }
        } catch (Exception e) {
            logger.warn("Could not load Tamil templates", e);
        }

        // Add different outage types templates
        templateMap.put("electricity_outage", "⚡ ELECTRICITY OUTAGE: There will be a power outage in {{1}} from {{2}} to {{3}}. Reason: {{4}}");
        templateMap.put("water_outage", "💧 WATER OUTAGE: Water supply will be interrupted in {{1}} from {{2}} to {{3}}. Reason: {{4}}");
        templateMap.put("gas_outage", "🔥 GAS OUTAGE: Gas supply will be interrupted in {{1}} from {{2}} to {{3}}. Reason: {{4}}");
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

            // Validate and format the phone numbers
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                logger.error("Invalid phone number: Empty or null");
                return CompletableFuture.completedFuture(false);
            }

            // Format WhatsApp numbers
            String fromNumber = formatWhatsAppNumber(whatsappNumber);
            String toNumber = formatWhatsAppNumber(phoneNumber);

            if (fromNumber == null || toNumber == null) {
                logger.error("Invalid WhatsApp number format for from: {} or to: {}", whatsappNumber, phoneNumber);
                return CompletableFuture.completedFuture(false);
            }

            Message messageResult = Message.creator(
                            new PhoneNumber(toNumber),
                            new PhoneNumber(fromNumber),
                            message)
                    .create();

            logger.info("WhatsApp message sent successfully, SID: {}", messageResult.getSid());
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
            // First check if we have a language-specific template if the template name contains a language code
            String templateContent = null;

            // Replace dots with underscores if needed (outage.new -> outage_new)
            String lookupName = templateName.replace(".", "_");

            // Try to get the template content
            templateContent = getTemplateContent(lookupName, parameters);

            // If template not found, try fallback to base template without language suffix
            if (templateContent == null && lookupName.contains("_")) {
                // Extract base template name (e.g., "outage_notification" from "outage_notification_si")
                String baseTemplateName = lookupName.substring(0, lookupName.lastIndexOf("_"));
                logger.info("Template '{}' not found, trying base template '{}'", lookupName, baseTemplateName);
                templateContent = getTemplateContent(baseTemplateName, parameters);
            }

            // Try the original name if we still don't have content
            if (templateContent == null && !templateName.equals(lookupName)) {
                templateContent = getTemplateContent(templateName, parameters);
            }

            // If still no template found, use a generic message
            if (templateContent == null) {
                logger.warn("No template found for '{}', using generic message", templateName);

                // Create a basic fallback message
                String fallbackMessage = "PowerAlert notification";
                if (parameters != null && parameters.length > 0) {
                    fallbackMessage = "PowerAlert: " + String.join(" ", parameters);
                }

                return sendWhatsAppMessage(phoneNumber, fallbackMessage);
            }

            // Send the template message
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

        // Remove any existing prefix and whitespace
        String cleanNumber = phoneNumber.replace("whatsapp:", "").trim();

        // Remove any non-digit characters except the leading plus sign
        if (cleanNumber.startsWith("+")) {
            cleanNumber = "+" + cleanNumber.substring(1).replaceAll("[^0-9]", "");
        } else {
            cleanNumber = cleanNumber.replaceAll("[^0-9]", "");
            // Add + if not present
            if (!cleanNumber.startsWith("+")) {
                cleanNumber = "+" + cleanNumber;
            }
        }

        // Ensure the number has at least 10 digits (including country code)
        if (cleanNumber.replaceAll("[^0-9]", "").length() < 10) {
            logger.error("Phone number too short (less than 10 digits): {}", cleanNumber);
            return null;
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
                String paramValue = parameters[i] != null ? parameters[i] : "";
                result = result.replace("{{" + i + "}}", paramValue);
            }
        }

        return result;
    }
}