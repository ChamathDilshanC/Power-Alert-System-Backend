package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DummySmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(DummySmsServiceImpl.class);

    @Override
    public boolean sendSms(String phoneNumber, String message) {
        logger.info("DUMMY SMS would be sent to {}: {}", phoneNumber, message);
        return true;
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && !phoneNumber.isEmpty();
    }
}