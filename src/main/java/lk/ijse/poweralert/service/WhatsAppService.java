package lk.ijse.poweralert.service;

import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending WhatsApp messages
 */
public interface WhatsAppService {
    // Add overloaded method with language parameter
    @Async
    CompletableFuture<Boolean> sendTemplateMessage(String phoneNumber, String templateName, String[] parameters, String language);

    CompletableFuture<Boolean> sendWhatsAppMessage(String phoneNumber, String message);
    CompletableFuture<Boolean> sendTemplateMessage(String phoneNumber, String templateName, String[] parameters);
}