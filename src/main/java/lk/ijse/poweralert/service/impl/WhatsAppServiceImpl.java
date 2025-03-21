package lk.ijse.poweralert.service.impl;

import com.vonage.client.VonageClient;
import com.vonage.client.messages.MessageResponse;
import com.vonage.client.messages.MessageStatus;

import lk.ijse.poweralert.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppServiceImpl.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    @Value("${vonage.api.key}")
    private String apiKey;

    @Value("${vonage.api.secret}")
    private String apiSecret;

    @Value("${vonage.whatsapp.number:}")
    private String whatsappNumber;

    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsAppEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private String authToken;

    @PostConstruct
    public void init() {
        if (whatsAppEnabled && !apiKey.isEmpty() && !apiSecret.isEmpty()) {
            try {
                // Generate auth token
                String credentials = apiKey + ":" + apiSecret;
                authToken = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
                logger.info("Vonage WhatsApp service initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize WhatsApp service", e);
                whatsAppEnabled = false;
            }
        } else {
            logger.info("WhatsApp service is disabled or not fully configured");
        }
    }

    @Override
    @Async
    public boolean sendWhatsAppMessage(String phoneNumber, String message) {
        if (!whatsAppEnabled) {
            logger.info("WhatsApp sending is disabled. Would have sent to: {}", phoneNumber);
            return true;
        }

        String formattedNumber = formatPhoneNumber(phoneNumber);

        if (!isValidPhoneNumber(formattedNumber)) {
            logger.error("Invalid phone number format: {}", formattedNumber);
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", "whatsapp:" + whatsappNumber);
            requestBody.put("to", "whatsapp:" + formattedNumber.substring(1)); // Remove the + sign
            requestBody.put("message_type", "text");
            requestBody.put("text", message);
            requestBody.put("channel", "whatsapp");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.nexmo.com/v1/messages", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("WhatsApp message sent successfully to: {}", formattedNumber);
                return true;
            } else {
                logger.error("Failed to send WhatsApp message. Response: {}", response);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending WhatsApp message to {}", formattedNumber, e);
            return false;
        }
    }

    @Override
    public boolean sendTemplateMessage(String phoneNumber, String templateName, String[] parameters) {
        // Similar implementation to sendWhatsAppMessage but with template formatting
        // Would need to adjust the request body format according to Vonage's API docs
        return false; // Placeholder
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && !phoneNumber.isEmpty() &&
                PHONE_PATTERN.matcher(phoneNumber).matches();
    }

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