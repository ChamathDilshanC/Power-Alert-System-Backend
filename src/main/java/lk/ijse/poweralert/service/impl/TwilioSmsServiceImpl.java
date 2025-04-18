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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private Map<String, String> templateCache = new HashMap<>();

    @PostConstruct
    public void init() {
        if (smsEnabled && !accountSid.isEmpty() && !authToken.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio SMS service initialized successfully");

                // Initialize template cache with UTF-8 encoding support
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

    private void initTemplateCache() {
        templateCache = new ConcurrentHashMap<>();

        // Load default (English) templates
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            logger.info("Default resource bundle loaded successfully");

            loadTemplatesFromBundle(bundle, "");
        } catch (Exception e) {
            logger.error("Failed to load default message templates", e);
        }

        // Load Sinhala templates with UTF-8 encoding
        try {
            ResourceBundle.Control utf8Control = new ResourceBundle.Control() {
                @Override
                public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                                ClassLoader loader, boolean reload)
                        throws IllegalAccessException, InstantiationException, IOException {
                    String resourceName = toBundleName(baseName, locale) + ".properties";
                    InputStream stream = loader.getResourceAsStream(resourceName);
                    if (stream == null) return null;
                    try {
                        return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    } finally {
                        stream.close();
                    }
                }
            };

            ResourceBundle siBundle = ResourceBundle.getBundle("messages", new Locale("si"), utf8Control);
            logger.info("Sinhala resource bundle loaded successfully");

            loadTemplatesFromBundle(siBundle, "_si");
        } catch (Exception e) {
            logger.error("Failed to load Sinhala message templates", e);
        }

        // Load Tamil templates with UTF-8 encoding
        try {
            ResourceBundle.Control utf8Control = new ResourceBundle.Control() {
                @Override
                public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                                ClassLoader loader, boolean reload)
                        throws IllegalAccessException, InstantiationException, IOException {
                    String resourceName = toBundleName(baseName, locale) + ".properties";
                    InputStream stream = loader.getResourceAsStream(resourceName);
                    if (stream == null) return null;
                    try {
                        return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    } finally {
                        stream.close();
                    }
                }
            };

            ResourceBundle taBundle = ResourceBundle.getBundle("messages", new Locale("ta"), utf8Control);
            logger.info("Tamil resource bundle loaded successfully");

            loadTemplatesFromBundle(taBundle, "_ta");
        } catch (Exception e) {
            logger.error("Failed to load Tamil message templates", e);
        }

        logger.info("SMS template cache initialized with {} templates", templateCache.size());

        // Log all templates for debugging
        templateCache.forEach((key, content) -> {
            logger.debug("Template key: {}, content: {}", key, content);
        });
    }

    private void loadTemplatesFromBundle(ResourceBundle bundle, String suffix) {
        String[] templateKeys = {
                "outage.new",
                "outage.update",
                "outage.cancelled",
                "outage.restored"
        };

        for (String key : templateKeys) {
            try {
                String templateContent = bundle.getString(key);
                // Ensure the template content is properly UTF-8 encoded when stored in cache
                byte[] bytes = templateContent.getBytes(StandardCharsets.UTF_8);
                String encodedContent = new String(bytes, StandardCharsets.UTF_8);

                templateCache.put(key + suffix, encodedContent);
                logger.info("Loaded {} template for {}{}",
                        suffix.isEmpty() ? "English" : suffix.substring(1).toUpperCase(),
                        key,
                        encodedContent.isEmpty() ? "" : ": " + encodedContent);
            } catch (MissingResourceException e) {
                logger.warn("Template not found: {}{}", key, suffix);
            }
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

            // Process the message content to interpret any Unicode escape sequences
            String processedMessage = processUnicodeEscapes(messageContent);

            logger.info("Sending SMS with processed message: {}", processedMessage);

            Message message = Message.creator(
                            new PhoneNumber(formattedPhoneNumber),
                            new PhoneNumber(twilioPhoneNumber),
                            processedMessage)
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

    private String processUnicodeEscapes(String input) {
        if (input == null) return null;

        StringBuilder result = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            if (i < input.length() - 5 && input.charAt(i) == '\\' && input.charAt(i+1) == 'u') {
                // Found a Unicode escape sequence
                try {
                    String hexValue = input.substring(i+2, i+6);
                    int codePoint = Integer.parseInt(hexValue, 16);
                    result.append((char)codePoint);
                    i += 6; // Skip past the escape sequence
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // Not a valid escape sequence, just append the backslash
                    result.append(input.charAt(i));
                    i++;
                }
            } else {
                // Regular character
                result.append(input.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
    /**
     * Converts a string to its Unicode escape representation
     * This helps with sending non-Latin characters via SMS
     */
    private String escapeToUnicode(String input) {
        if (input == null) return null;

        StringBuilder b = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c <= 127) {
                // ASCII characters don't need escaping
                b.append(c);
            } else {
                // Convert non-ASCII characters to Unicode escapes
                b.append(String.format("\\u%04X", (int) c));
            }
        }
        return b.toString();
    }

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
            logger.info("Using template content: {}", templateContent);

            // Replace parameters in template with explicit UTF-8 encoding
            String finalMessage = templateContent;
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    // Replace {0}, {1}, etc. with parameter values
                    String placeholder = "{" + i + "}";
                    String paramValue = params[i] != null ? params[i] : "";

                    // Ensure parameter value is properly UTF-8 encoded
                    byte[] paramBytes = paramValue.getBytes(StandardCharsets.UTF_8);
                    String encodedParam = new String(paramBytes, StandardCharsets.UTF_8);

                    finalMessage = finalMessage.replace(placeholder, encodedParam);
                }
            }

            // Ensure the final message is properly UTF-8 encoded
            byte[] finalBytes = finalMessage.getBytes(StandardCharsets.UTF_8);
            String encodedFinalMessage = new String(finalBytes, StandardCharsets.UTF_8);

            logger.info("Sending SMS in language '{}' with message: {}", normalizedLanguage, encodedFinalMessage);

            // Send the formatted message
            return sendSms(phoneNumber, encodedFinalMessage);
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