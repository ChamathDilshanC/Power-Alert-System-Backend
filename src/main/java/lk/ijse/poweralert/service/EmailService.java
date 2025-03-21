package lk.ijse.poweralert.service;

import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Service interface for sending emails
 */
public interface EmailService {

    /**
     * Send an email with the given details
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param content the email content (can be plain text or HTML)
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendEmail(String to, String subject, String content);

    /**
     * Send an email with template
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param templateName the name of the template to use
     * @param model the model to populate the template with
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendTemplateEmail(String to, String subject, String templateName, Object model);

    /**
     * Send an email with attachment
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param content the email content
     * @param attachmentFilePath the path to the attachment file
     * @param attachmentFileName the name to display for the attachment
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendEmailWithAttachment(String to, String subject, String content,
                                    String attachmentFilePath, String attachmentFileName);

    /**
     * Send an outage notification email to a user
     *
     * @param user the user to send the email to
     * @param outage the outage details
     * @param language the user's preferred language
     * @return true if the email was sent successfully, false otherwise
     */
    boolean sendOutageNotificationEmail(User user, Outage outage, String language);

    /**
     * Send an email to all active users in the system
     *
     * @param subject the email subject
     * @param content the email content
     * @return the number of successfully sent emails
     */
    int sendEmailToAllUsers(String subject, String content);

    /**
     * Send an email to users in a specific area
     *
     * @param areaId the area ID
     * @param subject the email subject
     * @param content the email content
     * @return the number of successfully sent emails
     */
    int sendEmailToUsersInArea(Long areaId, String subject, String content);

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