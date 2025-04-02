package lk.ijse.poweralert.service.impl;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lk.ijse.poweralert.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
@Primary
public class TwilioSmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsServiceImpl.class);

    // E.164 phone number validation pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    // Cache for SMS templates
    private final Map<String, String> templateCache = new HashMap<>();

    @PostConstruct
    public void init() {
        if (smsEnabled && !accountSid.isEmpty() && !authToken.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio SMS service initialized successfully");

                // Initialize template cache
                initTemplateCache();
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio client", e);
                // Disable SMS if initialization fails
                smsEnabled = false;
            }
        } else {
            logger.info("SMS service is disabled or not fully configured");
        }
    }

    /**
     * Initialize the template cache with common templates
     */
    private void initTemplateCache() {
        try {
            // Add default fallback templates first
            templateCache.put("outage.new", "{0} outage scheduled in {1} from {2} to {3}. Reason: {4}");
            templateCache.put("outage.update", "{0} outage in {1} status updated to {2}. Estimated end time: {3}");
            templateCache.put("outage.cancelled", "{0} outage in {1} scheduled for {2} has been cancelled");
            templateCache.put("outage.restored", "{0} services in {1} have been restored");

            // Then try to load from resource bundles
            try {
                // Load the templates from the resource bundle
                ResourceBundle bundle = ResourceBundle.getBundle("messages");
                logger.info("Default resource bundle loaded successfully");

                // Only override if found in the bundle
                if (bundle.containsKey("outage.new")) {
                    templateCache.put("outage.new", bundle.getString("outage.new"));
                    logger.info("Loaded English template for outage.new");
                }
                if (bundle.containsKey("outage.update")) {
                    templateCache.put("outage.update", bundle.getString("outage.update"));
                    logger.info("Loaded English template for outage.update");
                }
                if (bundle.containsKey("outage.cancelled")) {
                    templateCache.put("outage.cancelled", bundle.getString("outage.cancelled"));
                    logger.info("Loaded English template for outage.cancelled");
                }
                if (bundle.containsKey("outage.restored")) {
                    templateCache.put("outage.restored", bundle.getString("outage.restored"));
                    logger.info("Loaded English template for outage.restored");
                }
            } catch (Exception e) {
                logger.warn("Failed to load default templates from resource bundle: {}", e.getMessage());
                // Continue with fallbacks already set
            }

            // Try to load Sinhala templates
            try {
                ResourceBundle bundleSi = ResourceBundle.getBundle("messages_si");
                logger.info("Sinhala resource bundle loaded successfully");

                // Load Sinhala templates
                if (bundleSi.containsKey("outage.new")) {
                    String template = bundleSi.getString("outage.new");
                    templateCache.put("outage.new_si", template);
                    logger.info("Loaded Sinhala template for outage.new: {}", template);
                } else {
                    logger.warn("Sinhala bundle missing key: outage.new");
                }

                if (bundleSi.containsKey("outage.update")) {
                    String template = bundleSi.getString("outage.update");
                    templateCache.put("outage.update_si", template);
                    logger.info("Loaded Sinhala template for outage.update");
                } else {
                    logger.warn("Sinhala bundle missing key: outage.update");
                }

                if (bundleSi.containsKey("outage.cancelled")) {
                    String template = bundleSi.getString("outage.cancelled");
                    templateCache.put("outage.cancelled_si", template);
                    logger.info("Loaded Sinhala template for outage.cancelled");
                } else {
                    logger.warn("Sinhala bundle missing key: outage.cancelled");
                }

                if (bundleSi.containsKey("outage.restored")) {
                    String template = bundleSi.getString("outage.restored");
                    templateCache.put("outage.restored_si", template);
                    logger.info("Loaded Sinhala template for outage.restored");
                } else {
                    logger.warn("Sinhala bundle missing key: outage.restored");
                }

            } catch (Exception e) {
                logger.warn("Failed to load Sinhala templates: {}", e.getMessage());
            }

            // Try to load Tamil templates
            try {
                ResourceBundle bundleTa = ResourceBundle.getBundle("messages_ta");
                logger.info("Tamil resource bundle loaded successfully");

                // Load Tamil templates
                if (bundleTa.containsKey("outage.new")) {
                    String template = bundleTa.getString("outage.new");
                    templateCache.put("outage.new_ta", template);
                    logger.info("Loaded Tamil template for outage.new: {}", template);
                } else {
                    logger.warn("Tamil bundle missing key: outage.new");
                }

                if (bundleTa.containsKey("outage.update")) {
                    String template = bundleTa.getString("outage.update");
                    templateCache.put("outage.update_ta", template);
                    logger.info("Loaded Tamil template for outage.update");
                } else {
                    logger.warn("Tamil bundle missing key: outage.update");
                }

                if (bundleTa.containsKey("outage.cancelled")) {
                    String template = bundleTa.getString("outage.cancelled");
                    templateCache.put("outage.cancelled_ta", template);
                    logger.info("Loaded Tamil template for outage.cancelled");
                } else {
                    logger.warn("Tamil bundle missing key: outage.cancelled");
                }

                if (bundleTa.containsKey("outage.restored")) {
                    String template = bundleTa.getString("outage.restored");
                    templateCache.put("outage.restored_ta", template);
                    logger.info("Loaded Tamil template for outage.restored");
                } else {
                    logger.warn("Tamil bundle missing key: outage.restored");
                }

            } catch (Exception e) {
                logger.warn("Failed to load Tamil templates: {}", e.getMessage());
            }

            // Log all templates in cache
            logger.info("SMS template cache initialized with {} templates", templateCache.size());
            for (Map.Entry<String, String> entry : templateCache.entrySet()) {
                logger.debug("Template key: {}", entry.getKey());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize SMS template cache", e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendSms(String phoneNumber, String messageContent) {
        if (!smsEnabled) {
            logger.info("SMS sending is disabled. Would have sent to: {}, message: {}", phoneNumber, messageContent);
            return CompletableFuture.completedFuture(true);
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            logger.error("Invalid phone number format: {}", phoneNumber);
            return CompletableFuture.completedFuture(false);
        }

        try {
            logger.info("Sending SMS to: {}", phoneNumber);

            // Format the phone number to E.164 standard
            String formattedPhoneNumber = formatPhoneNumber(phoneNumber);

            Message message = Message.creator(
                            new PhoneNumber(formattedPhoneNumber),
                            new PhoneNumber(twilioPhoneNumber),
                            messageContent)
                    .create();

            logger.info("SMS sent successfully, SID: {}", message.getSid());
            return CompletableFuture.completedFuture(true);
        } catch (ApiException e) {
            logger.error("Twilio API error sending SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send an SMS using a template
     *
     * @param phoneNumber The recipient's phone number
     * @param templateKey The template key (e.g., "outage.new", "outage.update")
     * @param params The parameters to substitute in the template
     * @param language The language code ("en", "si", "ta")
     * @return CompletableFuture indicating success or failure
     */
    @Async
    public CompletableFuture<Boolean> sendTemplatedSms(String phoneNumber, String templateKey,
                                                       String[] params, String language) {
        if (!smsEnabled) {
            logger.info("SMS sending is disabled. Would have sent templated SMS to: {}", phoneNumber);
            return CompletableFuture.completedFuture(true);
        }

        try {
            // Normalize language to lowercase for consistency
            String normalizedLanguage = language != null ? language.toLowerCase() : "en";

            // Build template cache key
            String templateCacheKey = templateKey;

            // For non-English languages, append language code to template key
            if (!"en".equals(normalizedLanguage)) {
                templateCacheKey = templateKey + "_" + normalizedLanguage;
                logger.info("Using language-specific template key: {}", templateCacheKey);
            }

            // Get the template content
            String templateContent = templateCache.get(templateCacheKey);

            // If not found, fall back to default English template
            if (templateContent == null) {
                logger.info("No template found for key: {}, falling back to default", templateCacheKey);
                templateContent = templateCache.get(templateKey);
            }

            // If still no template, create a generic message
            if (templateContent == null) {
                logger.warn("No template found for key: {} or {}, using fallback message",
                        templateCacheKey, templateKey);

                StringBuilder fallback = new StringBuilder("PowerAlert: ");
                if (params != null && params.length > 0) {
                    fallback.append(String.join(" ", params));
                } else {
                    fallback.append("You have a new notification.");
                }

                return sendSms(phoneNumber, fallback.toString());
            }

            // Log the template being used
            logger.info("Using template for key '{}': '{}'", templateCacheKey, templateContent);

            // Replace parameters in template
            String finalMessage = templateContent;
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    // Replace {0}, {1}, etc. with parameter values
                    String placeholder = "{" + i + "}";
                    String paramValue = params[i] != null ? params[i] : "";
                    finalMessage = finalMessage.replace(placeholder, paramValue);
                }
            }

            logger.info("Sending SMS in language '{}' with message: {}", normalizedLanguage, finalMessage);

            // Send the formatted message
            return sendSms(phoneNumber, finalMessage);
        } catch (Exception e) {
            logger.error("Error sending templated SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Format a phone number to E.164 standard (+1234567890)
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }

        // Remove any non-digit characters except the leading plus sign
        String cleanNumber = phoneNumber.trim();
        if (cleanNumber.startsWith("+")) {
            cleanNumber = "+" + cleanNumber.substring(1).replaceAll("[^0-9]", "");
        } else {
            cleanNumber = cleanNumber.replaceAll("[^0-9]", "");
            // Add + if not present
            if (!cleanNumber.startsWith("+")) {
                cleanNumber = "+" + cleanNumber;
            }
        }

        return cleanNumber;
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Format the number and then check if it matches E.164 format
        String formattedNumber = formatPhoneNumber(phoneNumber);
        return formattedNumber != null && PHONE_PATTERN.matcher(formattedNumber).matches();
    }
}