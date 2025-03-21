package lk.ijse.poweralert.service;

/**
 * Service interface for sending WhatsApp messages
 */
public interface WhatsAppService {

    /** Send a WhatsApp message to the specified phone number    */
    boolean sendWhatsAppMessage(String phoneNumber, String message);

    /** Send a WhatsApp template message */
    boolean sendTemplateMessage(String phoneNumber, String templateName, String[] parameters);
}