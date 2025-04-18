package lk.ijse.poweralert.service;

import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending emails
 */
public interface EmailService {

    /**
     * Send an outage update email to a user
     *
     * @param user the user to send the email to
     * @param outage the outage details
     * @param language the user's preferred language
     * @return CompletableFuture containing a boolean indicating success
     */
    @Async
    CompletableFuture<Boolean> sendOutageUpdateEmail(User user, Outage outage, String language);

    /**
     * Send an outage cancellation email to a user
     *
     * @param user the user to send the email to
     * @param outage the outage details
     * @param language the user's preferred language
     * @return CompletableFuture containing a boolean indicating success
     */
    @Async
    CompletableFuture<Boolean> sendOutageCancellationEmail(User user, Outage outage, String language);

    /**
     * Send an outage restoration email to a user
     *
     * @param user the user to send the email to
     * @param outage the outage details
     * @param language the user's preferred language
     * @return CompletableFuture containing a boolean indicating success
     */
    @Async
    CompletableFuture<Boolean> sendOutageRestorationEmail(User user, Outage outage, String language);

    /**
     * Send an email with the given details
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param content the email content (can be plain text or HTML)
     */
    void sendEmail(String to, String subject, String content);

    /**
     * Send an email with template
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param templateName the name of the template to use
     * @param model the model to populate the template with
     * @return CompletableFuture containing a boolean indicating success
     */
    @Async
    CompletableFuture<Boolean> sendTemplateEmail(String to, String subject, String templateName, Object model);

    /**
     * Send an email with template using the specified language
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param templateName the name of the template to use
     * @param model the model to populate the template with
     * @param language the language code (e.g., "en", "si", "ta")
     * @return CompletableFuture containing a boolean indicating success
     */
    @Async
    CompletableFuture<Boolean> sendTemplateEmail(String to, String subject, String templateName, Object model, String language);

    /**
     * Send an email with attachment
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param content the email content
     * @param attachmentFilePath the path to the attachment file
     * @param attachmentFileName the name to display for the attachment
     * @return CompletableFuture containing a boolean indicating success
     */
    CompletableFuture<Boolean> sendEmailWithAttachment(String to, String subject, String content,
                                                       String attachmentFilePath, String attachmentFileName);

    /**
     * Send an outage notification email to a user
     *
     * @param user the user to send the email to
     * @param outage the outage details
     * @param language the user's preferred language
     * @return CompletableFuture containing a boolean indicating success
     */
    CompletableFuture<Boolean> sendOutageNotificationEmail(User user, Outage outage, String language);

    /**
     * Send a login notification email to a user
     *
     * @param user the user to send the email to
     * @param ipAddress the IP address from which login occurred
     * @param device the device/user agent used for login
     * @param location the approximate location based on IP (if available)
     * @return CompletableFuture containing a boolean indicating success
     */
    CompletableFuture<Boolean> sendLoginNotificationEmail(User user, String ipAddress, String device, String location);

    /**
     * Send an email to all active users in the system
     *
     * @param subject the email subject
     * @param content the email content
     * @return CompletableFuture containing the number of successfully sent emails
     */
    CompletableFuture<Integer> sendEmailToAllUsers(String subject, String content);

    /**
     * Send an email to users in a specific area
     *
     * @param areaId the area ID
     * @param subject the email subject
     * @param content the email content
     * @return CompletableFuture containing the number of successfully sent emails
     */
    CompletableFuture<Integer> sendEmailToUsersInArea(Long areaId, String subject, String content);

    /**
     * Get the JavaMailSender instance for testing
     */
    JavaMailSender getMailSender();

    /**
     * Get the from email address
     */
    String getFromEmail();

    /**
     * Test email server connection
     */
    boolean testEmailConnection();

    /**
     * Check if email sending is enabled
     */
    boolean isEnabled();
}