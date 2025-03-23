package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DummyWhatsAppServiceImpl implements WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(DummyWhatsAppServiceImpl.class);

    @Override
    public boolean sendWhatsAppMessage(String phoneNumber, String message) {
        logger.info("DUMMY WhatsApp message would be sent to {}: {}", phoneNumber, message);
        return true;
    }

    @Override
    public boolean sendTemplateMessage(String phoneNumber, String templateName, String[] parameters) {
        logger.info("DUMMY WhatsApp template message would be sent to {}: template={}", phoneNumber, templateName);
        return true;
    }
}