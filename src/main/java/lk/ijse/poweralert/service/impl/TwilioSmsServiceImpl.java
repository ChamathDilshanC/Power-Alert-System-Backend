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

    @PostConstruct
    public void init() {
        if (smsEnabled && !accountSid.isEmpty() && !authToken.isEmpty()) {
            try {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio client", e);
                // Disable SMS if initialization fails
                smsEnabled = false;
            }
        } else {
            logger.info("SMS service is disabled or not fully configured");
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

            Message message = Message.creator(
                            new PhoneNumber(phoneNumber),
                            new PhoneNumber(twilioPhoneNumber),
                            messageContent)
                    .create();

            logger.info("SMS sent successfully, SID: {}", message.getSid());
            return CompletableFuture.completedFuture(true);
        } catch (ApiException e) {
            logger.error("Twilio API error sending SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}", phoneNumber, e);
            return CompletableFuture.completedFuture(false);
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
}