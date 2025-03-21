package lk.ijse.poweralert.service.impl;

import jakarta.mail.internet.MimeMessage;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public JavaMailSender getMailSender() {
        return mailSender;
    }

    @Override
    public String getFromEmail() {
        return fromEmail;
    }

    @Override
    public boolean isEnabled() {
        return emailEnabled;
    }

    @Override
    public boolean testEmailConnection() {
        try {
            logger.info("Testing email server connection");

            // Create a test message to verify connection
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(fromEmail);
            helper.setSubject("Connection Test");
            helper.setText("Test");

            logger.info("Email server connection test passed");
            return true;
        } catch (Exception e) {
            logger.error("Email server connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async
    public boolean sendEmail(String to, String subject, String content) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent to: {}, subject: {}", to, subject);
            return true;
        }

        try {
            logger.info("Sending email to: {}, subject: {}", to, subject);
            logger.debug("From email: {}", fromEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true indicates HTML content
            helper.setFrom(fromEmail);

            try {
                // Log first 100 chars of content for debugging
                if (logger.isDebugEnabled()) {
                    String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                    logger.debug("Email content preview: {}", preview);
                }

                mailSender.send(message);
                logger.info("Email sent successfully to: {}", to);
                return true;
            } catch (MailException me) {
                logger.error("JavaMailSender error: {}", me.getMessage(), me);
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception preparing email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async
    public boolean sendTemplateEmail(String to, String subject, String templateName, Object model) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent template email to: {}, subject: {}", to, subject);
            return true;
        }

        try {
            logger.info("Sending template email to: {}, subject: {}, template: {}", to, subject, templateName);

            // For this implementation, we'll convert the model to a simple content
            String content = convertModelToContent(model);

            return sendEmail(to, subject, content);
        } catch (Exception e) {
            logger.error("Failed to send template email to: {}", to, e);
            return false;
        }
    }

    private String convertModelToContent(Object model) {
        // This is a simplified conversion. In a real implementation,
        // you might want to use a proper template engine
        if (model instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) model;
            StringBuilder content = new StringBuilder("<html><body>");

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                content.append("<p><strong>").append(entry.getKey()).append(":</strong> ")
                        .append(entry.getValue()).append("</p>");
            }

            content.append("</body></html>");
            return content.toString();
        }

        return model.toString();
    }

    @Override
    @Async
    public boolean sendEmailWithAttachment(String to, String subject, String content,
                                           String attachmentFilePath, String attachmentFileName) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent email with attachment to: {}, subject: {}", to, subject);
            return true;
        }

        try {
            logger.info("Sending email with attachment to: {}, subject: {}", to, subject);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            helper.setFrom(fromEmail);

            FileSystemResource file = new FileSystemResource(new File(attachmentFilePath));
            helper.addAttachment(attachmentFileName, file);

            mailSender.send(message);
            logger.info("Email with attachment sent successfully to: {}", to);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send email with attachment to: {}", to, e);
            return false;
        }
    }

    @Override
    @Async
    public boolean sendOutageNotificationEmail(User user, Outage outage, String language) {
        try {
            logger.info("Preparing outage notification email for user: {}", user.getEmail());

            // Get the appropriate resource bundle for localization
            ResourceBundle bundle = getResourceBundle(language);

            // Create the model with all the data needed for the template
            Map<String, Object> model = new HashMap<>();
            model.put("title", bundle.getString("email.outage.title"));
            model.put("greeting", bundle.getString("email.greeting") + " " + user.getUsername());
            model.put("message", String.format(bundle.getString("email.outage.message"), outage.getType()));

            // Outage details
            model.put("outageTypeLabel", bundle.getString("outage.type"));
            model.put("outageType", outage.getType().toString());
            model.put("areaLabel", bundle.getString("outage.area"));
            model.put("area", outage.getAffectedArea().getName());
            model.put("statusLabel", bundle.getString("outage.status"));
            model.put("status", outage.getStatus().toString());

            // Status labels
            model.put("scheduledLabel", bundle.getString("outage.status.scheduled"));
            model.put("ongoingLabel", bundle.getString("outage.status.ongoing"));
            model.put("completedLabel", bundle.getString("outage.status.completed"));
            model.put("cancelledLabel", bundle.getString("outage.status.cancelled"));

            // Time information
            model.put("startTimeLabel", bundle.getString("outage.startTime"));
            model.put("startTime", outage.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            if (outage.getEstimatedEndTime() != null) {
                model.put("endTimeLabel", bundle.getString("outage.estimatedEndTime"));
                model.put("endTime", outage.getEstimatedEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            if (outage.getReason() != null && !outage.getReason().isEmpty()) {
                model.put("reasonLabel", bundle.getString("outage.reason"));
                model.put("reason", outage.getReason());
            }

            // Additional information
            model.put("additionalInfo", bundle.getString("email.outage.additionalInfo"));

            // Button and links
            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("viewDetailsLabel", bundle.getString("email.viewDetails"));

            // Footer information
            model.put("footerText", bundle.getString("email.footer"));
            model.put("unsubscribeText", bundle.getString("email.unsubscribe"));
            model.put("unsubscribeUrl", "https://poweralert.lk/preferences/notifications?userId=" + user.getId() + "&token=UNSUBSCRIBE_TOKEN");
            model.put("unsubscribeHereLabel", bundle.getString("email.unsubscribeHere"));
            model.put("year", Year.now().toString());

            // Use the template to send the email
            String subject = String.format(bundle.getString("email.outage.subject"),
                    outage.getType(),
                    outage.getAffectedArea().getName());

            // Send using the template (FreeMarker will use outage-notification.ftl)
            return sendTemplateEmail(user.getEmail(), subject, "outage-notification", model);
        } catch (Exception e) {
            logger.error("Error sending outage notification email to {}", user.getEmail(), e);
            return false;
        }
    }
    private String createHtmlEmailContent(Map<String, Object> model) {
        // This is a simplified HTML generation
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #2196F3; color: white; padding: 10px; text-align: center; }");
        content.append(".content { padding: 20px; background-color: #f9f9f9; }");
        content.append(".footer { font-size: 12px; color: #777; text-align: center; margin-top: 20px; }");
        content.append(".button { background-color: #2196F3; color: white; padding: 10px 15px; text-decoration: none; border-radius: 4px; }");
        content.append("</style></head><body><div class='container'>");

        // Header
        content.append("<div class='header'><h2>").append(model.get("title")).append("</h2></div>");

        // Content
        content.append("<div class='content'>");
        content.append("<p>").append(model.get("greeting")).append("</p>");
        content.append("<p>").append(model.get("message")).append("</p>");

        // Outage details
        content.append("<div style='margin: 20px 0;'>");
        content.append("<p><strong>").append(model.get("outageTypeLabel")).append(":</strong> ").append(model.get("outageType")).append("</p>");
        content.append("<p><strong>").append(model.get("areaLabel")).append(":</strong> ").append(model.get("area")).append("</p>");
        content.append("<p><strong>").append(model.get("statusLabel")).append(":</strong> ").append(model.get("status")).append("</p>");
        content.append("<p><strong>").append(model.get("startTimeLabel")).append(":</strong> ").append(model.get("startTime")).append("</p>");

        if (model.containsKey("endTime")) {
            content.append("<p><strong>").append(model.get("endTimeLabel")).append(":</strong> ").append(model.get("endTime")).append("</p>");
        }

        if (model.containsKey("reason")) {
            content.append("<p><strong>").append(model.get("reasonLabel")).append(":</strong> ").append(model.get("reason")).append("</p>");
        }
        content.append("</div>");

        // Button
        content.append("<div style='text-align: center; margin: 30px 0;'>");
        content.append("<a href='").append(model.get("portalUrl")).append("' class='button'>");
        content.append(model.get("viewDetailsLabel")).append("</a>");
        content.append("</div>");

        content.append("<p>").append(model.get("additionalInfo")).append("</p>");
        content.append("</div>");

        // Footer
        content.append("<div class='footer'>");
        content.append("<p>").append(model.get("footerText")).append("</p>");
        content.append("<p>").append(model.get("unsubscribeText")).append(" ");
        content.append("<a href='").append(model.get("unsubscribeUrl")).append("'>");
        content.append(model.get("unsubscribeHereLabel")).append("</a></p>");
        content.append("<p>&copy; ").append(model.get("year")).append(" PowerAlert</p>");
        content.append("</div>");

        content.append("</div></body></html>");
        return content.toString();
    }

    @Override
    public int sendEmailToAllUsers(String subject, String content) {
        logger.info("Sending email to all active users with subject: {}", subject);

        List<User> activeUsers = userRepository.findByIsActiveTrue();
        AtomicInteger successCount = new AtomicInteger(0);

        activeUsers.forEach(user -> {
            try {
                boolean sent = sendEmail(user.getEmail(), subject, content);
                if (sent) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                logger.error("Failed to send email to user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        logger.info("Successfully sent emails to {}/{} active users", successCount.get(), activeUsers.size());
        return successCount.get();
    }

    @Override
    public int sendEmailToUsersInArea(Long areaId, String subject, String content) {
        logger.info("Sending email to users in area ID: {} with subject: {}", areaId, subject);

        List<User> usersInArea = userRepository.findUsersByAreaId(areaId);
        AtomicInteger successCount = new AtomicInteger(0);

        usersInArea.forEach(user -> {
            try {
                boolean sent = sendEmail(user.getEmail(), subject, content);
                if (sent) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                logger.error("Failed to send email to user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        logger.info("Successfully sent emails to {}/{} users in area", successCount.get(), usersInArea.size());
        return successCount.get();
    }

    /**
     * Helper method to get resource bundle for a language
     */
    private ResourceBundle getResourceBundle(String language) {
        Locale locale;

        switch (language.toLowerCase()) {
            case "si":
                locale = new Locale("si", "LK");
                break;
            case "ta":
                locale = new Locale("ta", "LK");
                break;
            default:
                locale = Locale.ENGLISH;
        }

        return ResourceBundle.getBundle("messages", locale);
    }
}