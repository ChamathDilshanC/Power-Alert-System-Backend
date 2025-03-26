package lk.ijse.poweralert.service;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending WhatsApp messages
 */
public interface WhatsAppService {
    CompletableFuture<Boolean> sendWhatsAppMessage(String phoneNumber, String message);
    CompletableFuture<Boolean> sendTemplateMessage(String phoneNumber, String templateName, String[] parameters);
}