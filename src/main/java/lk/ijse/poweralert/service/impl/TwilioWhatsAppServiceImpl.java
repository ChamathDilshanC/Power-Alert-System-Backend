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

import java.nio.charset.StandardCharsets;
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
        // Base templates
        templateMap.put("outage.new", "Your {{1}} service will be interrupted in {{2}} from {{3}} to {{4}}. Reason: {{5}}");
        templateMap.put("outage.update", "Update on your {{1}} outage in {{2}}: Status is now {{3}}. Estimated restoration: {{4}}");
        templateMap.put("outage.cancelled", "The scheduled {{1}} outage in {{2}} for {{3}} has been cancelled.");
        templateMap.put("outage.restored", "Good news! {{1}} services in {{2}} have been restored. Thank you for your patience.");

        // Base templates with underscores (for compatibility)
        templateMap.put("outage_new", "Your {{1}} service will be interrupted in {{2}} from {{3}} to {{4}}. Reason: {{5}}");
        templateMap.put("outage_update", "Update on your {{1}} outage in {{2}}: Status is now {{3}}. Estimated restoration: {{4}}");
        templateMap.put("outage_cancelled", "The scheduled {{1}} outage in {{2}} for {{3}} has been cancelled.");
        templateMap.put("outage_restored", "Good news! {{1}} services in {{2}} have been restored. Thank you for your patience.");

        // Sinhala templates
        templateMap.put("outage.new_si", "ඔබගේ {{1}} සේවාව {{2}} හි {{3}} සිට {{4}} දක්වා අත්හිටුවනු ලැබේ. හේතුව: {{5}}");
        templateMap.put("outage.update_si", "{{1}} හි {{2}} විදුලි බිඳවැටීමේ යාවත්කාලීන තොරතුරු: තත්ත්වය {{3}}. අවසන් වන ඇස්තමේන්තු වේලාව: {{4}}");
        templateMap.put("outage.cancelled_si", "{{2}} හි {{3}} සඳහා සැලසුම් කළ {{1}} බිඳවැටීම අවලංගු කර ඇත.");
        templateMap.put("outage.restored_si", "{{2}} හි {{1}} සේවා යථා තත්ත්වයට පත් කර ඇත. ඔබේ ඉවසීම සඳහා ස්තූතියි.");

        // Sinhala templates with underscores (for compatibility)
        templateMap.put("outage_new_si", "ඔබගේ {{1}} සේවාව {{2}} හි {{3}} සිට {{4}} දක්වා අත්හිටුවනු ලැබේ. හේතුව: {{5}}");
        templateMap.put("outage_update_si", "{{1}} හි {{2}} විදුලි බිඳවැටීමේ යාවත්කාලීන තොරතුරු: තත්ත්වය {{3}}. අවසන් වන ඇස්තමේන්තු වේලාව: {{4}}");
        templateMap.put("outage_cancelled_si", "{{2}} හි {{3}} සඳහා සැලසුම් කළ {{1}} බිඳවැටීම අවලංගු කර ඇත.");
        templateMap.put("outage_restored_si", "{{2}} හි {{1}} සේවා යථා තත්ත්වයට පත් කර ඇත. ඔබේ ඉවසීම සඳහා ස්තූතියි.");

        // Tamil templates
        templateMap.put("outage.new_ta", "உங்கள் {{1}} சேவை {{2}} இல் {{3}} முதல் {{4}} வரை தடைப்படும். காரணம்: {{5}}");
        templateMap.put("outage.update_ta", "{{2}} இல் {{1}} சேவை தடை புதுப்பிப்பு: நிலை இப்போது {{3}}. மதிப்பிடப்பட்ட மீட்பு நேரம்: {{4}}");
        templateMap.put("outage.cancelled_ta", "{{2}} இல் {{3}} க்கு திட்டமிடப்பட்ட {{1}} சேவை தடை ரத்து செய்யப்பட்டது.");
        templateMap.put("outage.restored_ta", "நல்ல செய்தி! {{2}} இல் {{1}} சேவைகள் மீட்டமைக்கப்பட்டுள்ளன. உங்கள் பொறுமைக்கு நன்றி.");

        // Tamil templates with underscores (for compatibility)
        templateMap.put("outage_new_ta", "உங்கள் {{1}} சேவை {{2}} இல் {{3}} முதல் {{4}} வரை தடைப்படும். காரணம்: {{5}}");
        templateMap.put("outage_update_ta", "{{2}} இல் {{1}} சேவை தடை புதுப்பிப்பு: நிலை இப்போது {{3}}. மதிப்பிடப்பட்ட மீட்பு நேரம்: {{4}}");
        templateMap.put("outage_cancelled_ta", "{{2}} இல் {{3}} க்கு திட்டமிடப்பட்ட {{1}} சேவை தடை ரத்து செய்யப்பட்டது.");
        templateMap.put("outage_restored_ta", "நல்ல செய்தி! {{2}} இல் {{1}} சேவைகள் மீட்டமைக்கப்பட்டுள்ளன. உங்கள் பொறுமைக்கு நன்றி.");

        // Load additional templates from resource bundles
        loadAllTemplatesFromResourceBundles();
    }

    // Add a new method to load all templates from resource bundles
    private void loadAllTemplatesFromResourceBundles() {
        try {
            loadTemplatesFromBundle("messages", "");
            loadTemplatesFromBundle("messages_si", "_si");
            loadTemplatesFromBundle("messages_ta", "_ta");
        } catch (Exception e) {
            logger.warn("Error loading templates from resource bundles", e);
        }
    }

    private void loadTemplatesFromBundle(String bundleName, String suffix) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
            for (String key : new String[]{"outage.new", "outage.update", "outage.cancelled", "outage.restored"}) {
                if (bundle.containsKey(key)) {
                    String template = bundle.getString(key);
                    // Convert from {0} format to {{0}} format for WhatsApp
                    template = template.replaceAll("\\{(\\d+)\\}", "{{$1}}");
                    templateMap.put(key + suffix, template);
                    logger.info("Loaded template {} from {}", key + suffix, bundleName);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load templates from {}: {}", bundleName, e.getMessage());
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendTemplateMessage(String phoneNumber, String templateName, String[] parameters, String language) {
        if (!whatsappEnabled) {
            logger.info("WhatsApp template sending is disabled. Would have sent to: {}, template: {}, language: {}",
                    phoneNumber, templateName, language);
            return CompletableFuture.completedFuture(true);
        }

        try {
            logger.info("Sending WhatsApp template message to: {}, template: {}, language: {}",
                    phoneNumber, templateName, language);

            // First try language-specific template format (e.g., outage.new_si)
            String langSpecificKey = templateName;
            if (language != null && !language.equals("en")) {
                langSpecificKey = templateName + "_" + language.toLowerCase();
            }

            // Get template content, trying different possible formats
            String templateContent = null;

            // 1. Try with language suffix using dots (e.g., outage.new_si)
            templateContent = getTemplateContent(langSpecificKey, parameters);

            // 2. Try with language suffix using underscores (e.g., outage_new_si)
            if (templateContent == null) {
                templateContent = getTemplateContent(langSpecificKey.replace(".", "_"), parameters);
            }

            // 3. Try the base template with dots (e.g., outage.new)
            if (templateContent == null) {
                templateContent = getTemplateContent(templateName, parameters);
            }

            // 4. Try the base template with underscores (e.g., outage_new)
            if (templateContent == null) {
                templateContent = getTemplateContent(templateName.replace(".", "_"), parameters);
            }

            // If still no template found, use a generic message
            if (templateContent == null) {
                logger.warn("No template found for '{}' in language '{}', using generic message",
                        templateName, language);

                // Create a basic fallback message
                String fallbackMessage = "PowerAlert notification";
                if (parameters != null && parameters.length > 0) {
                    fallbackMessage = "PowerAlert: " + String.join(" ", parameters);
                }

                return sendWhatsAppMessage(phoneNumber, fallbackMessage);
            }

            // Send the template message
            logger.info("Using template content: {}", templateContent);
            return sendWhatsAppMessage(phoneNumber, templateContent);
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp template message to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
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

            // Ensure proper UTF-8 encoding for the message
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            String encodedMessage = new String(messageBytes, StandardCharsets.UTF_8);

            // Create a message with parameters for encoding
            Map<String, String> params = new HashMap<>();
            params.put("To", toNumber);
            params.put("From", fromNumber);
            params.put("Body", encodedMessage);
            params.put("CharacterSet", "UTF-8");

            // Use the parameterized approach for creating the message
            Message messageResult = Message.creator(
                            new PhoneNumber(toNumber),
                            new PhoneNumber(fromNumber),
                            encodedMessage)
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