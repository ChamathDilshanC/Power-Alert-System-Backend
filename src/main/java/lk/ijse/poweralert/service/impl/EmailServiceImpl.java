package lk.ijse.poweralert.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Configuration freemarkerConfig;

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

            // Ensure the model is a map (if not already)
            Map<String, Object> templateModel = convertToMap(model);

            // Add default values for common template variables if not present
            ensureDefaultTemplateValues(templateModel);

            // Try to get the template
            Template template;
            try {
                template = freemarkerConfig.getTemplate(templateName);
            } catch (IOException e) {
                logger.error("Failed to load template '{}': {}", templateName, e.getMessage(), e);
                // Try with .ftl extension if not already included
                if (!templateName.endsWith(".ftl")) {
                    try {
                        template = freemarkerConfig.getTemplate(templateName + ".ftl");
                    } catch (IOException e2) {
                        logger.error("Failed to load template with .ftl extension: {}", e2.getMessage());
                        return sendEmail(to, subject, createHtmlEmailContent(templateModel));
                    }
                } else {
                    return sendEmail(to, subject, createHtmlEmailContent(templateModel));
                }
            }

            // Process the template
            String content;
            try {
                content = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateModel);
            } catch (TemplateException e) {
                logger.error("Error processing template '{}': {}", templateName, e.getMessage(), e);
                return sendEmail(to, subject, createHtmlEmailContent(templateModel));
            }

            // Check if content was generated successfully
            if (content == null || content.trim().isEmpty()) {
                logger.warn("Template processing resulted in empty content, using fallback HTML");
                content = createHtmlEmailContent(templateModel);
            }

            return sendEmail(to, subject, content);
        } catch (Exception e) {
            logger.error("Failed to send template email to: {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    private void ensureDefaultTemplateValues(Map<String, Object> model) {
        if (!model.containsKey("year")) {
            model.put("year", Year.now().toString());
        }
        if (!model.containsKey("title") && model.containsKey("subject")) {
            model.put("title", model.get("subject"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object model) {
        if (model instanceof Map) {
            return (Map<String, Object>) model;
        } else {
            // Create a new map and copy properties from the object
            Map<String, Object> map = new HashMap<>();
            // This is a simplified approach - in a real implementation,
            // you might use reflection to extract properties
            map.put("data", model);
            return map;
        }
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

            // Verify that the attachment file exists
            File file = new File(attachmentFilePath);
            if (!file.exists() || !file.isFile()) {
                logger.error("Attachment file not found or is not a file: {}", attachmentFilePath);
                return false;
            }

            FileSystemResource fileResource = new FileSystemResource(file);
            helper.addAttachment(attachmentFileName, fileResource);

            mailSender.send(message);
            logger.info("Email with attachment sent successfully to: {}", to);
            return true;
        } catch (MessagingException e) {
            logger.error("Messaging exception when sending email with attachment: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Failed to send email with attachment to: {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async
    public boolean sendOutageNotificationEmail(User user, Outage outage, String language) {
        try {
            logger.info("Preparing outage notification email for user: {}", user.getEmail());

            // Create a simple model with direct values
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("outageType", outage.getType().toString());
            model.put("areaName", outage.getAffectedArea().getName());
            model.put("status", outage.getStatus().toString());
            model.put("startTime", outage.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            if (outage.getEstimatedEndTime() != null) {
                model.put("endTime", outage.getEstimatedEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            if (outage.getReason() != null && !outage.getReason().isEmpty()) {
                model.put("reason", outage.getReason());
            }

            model.put("year", Year.now().toString());

            // Simple subject
            String subject = outage.getType() + " Outage Alert - " + outage.getAffectedArea().getName();

            // Process template manually
            try {
                // Try to get template
                Template template = freemarkerConfig.getTemplate("outage-notification.ftl");
                String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
                return sendEmail(user.getEmail(), subject, content);
            } catch (Exception e) {
                logger.error("Failed to process template: {}", e.getMessage(), e);
                // Send fallback email
                return sendEmail(user.getEmail(), subject, createSimpleFallbackEmail(user, outage));
            }
        } catch (Exception e) {
            logger.error("Error sending outage notification email to {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    private String createSimpleFallbackEmail(User user, Outage outage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body>");
        sb.append("<h2>").append(outage.getType()).append(" Outage Alert</h2>");
        sb.append("<p>Hello ").append(user.getUsername()).append(",</p>");
        sb.append("<p>There is a ").append(outage.getType())
                .append(" outage affecting your area: ").append(outage.getAffectedArea().getName()).append("</p>");

        sb.append("<p><strong>Start Time:</strong> ")
                .append(outage.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>");

        if (outage.getEstimatedEndTime() != null) {
            sb.append("<p><strong>Estimated End Time:</strong> ")
                    .append(outage.getEstimatedEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>");
        }

        sb.append("<p><strong>Status:</strong> ").append(outage.getStatus()).append("</p>");

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            sb.append("<p><strong>Reason:</strong> ").append(outage.getReason()).append("</p>");
        }

        sb.append("<p>Please plan accordingly. We apologize for any inconvenience.</p>");
        sb.append("<p>Power Alert System</p>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private void populateTemplateModel(Map<String, Object> model, User user, Outage outage, ResourceBundle bundle) {
        try {
            model.put("title", getLocalizedMessage(bundle, "email.outage.title", "Utility Outage Notification"));
            model.put("greeting", getLocalizedMessage(bundle, "email.greeting", "Hello") + " " + user.getUsername());
            model.put("message", String.format(
                    getLocalizedMessage(bundle, "email.outage.message",
                            "We are writing to inform you about a %s outage that affects your registered address."),
                    outage.getType()));

            // Outage details
            model.put("outageTypeLabel", getLocalizedMessage(bundle, "outage.type", "Type"));
            model.put("outageType", outage.getType().toString());
            model.put("areaLabel", getLocalizedMessage(bundle, "outage.area", "Affected Area"));
            model.put("area", outage.getAffectedArea().getName());
            model.put("statusLabel", getLocalizedMessage(bundle, "outage.status", "Status"));
            model.put("status", outage.getStatus().toString());

            // Status labels
            model.put("scheduledLabel", getLocalizedMessage(bundle, "outage.status.scheduled", "Scheduled"));
            model.put("ongoingLabel", getLocalizedMessage(bundle, "outage.status.ongoing", "Ongoing"));
            model.put("completedLabel", getLocalizedMessage(bundle, "outage.status.completed", "Completed"));
            model.put("cancelledLabel", getLocalizedMessage(bundle, "outage.status.cancelled", "Cancelled"));

            // Time information
            model.put("startTimeLabel", getLocalizedMessage(bundle, "outage.startTime", "Start Time"));
            model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

            if (outage.getEstimatedEndTime() != null) {
                model.put("endTimeLabel", getLocalizedMessage(bundle, "outage.estimatedEndTime", "Estimated End Time"));
                model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
            }

            if (outage.getReason() != null && !outage.getReason().isEmpty()) {
                model.put("reasonLabel", getLocalizedMessage(bundle, "outage.reason", "Reason"));
                model.put("reason", outage.getReason());
            }

            // Additional information
            model.put("additionalInfo", getLocalizedMessage(bundle, "email.outage.additionalInfo",
                    "Please plan accordingly. We apologize for any inconvenience this may cause."));

            // Button and links
            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("viewDetailsLabel", getLocalizedMessage(bundle, "email.viewDetails", "View Details"));

            // Footer information
            model.put("footerText", getLocalizedMessage(bundle, "email.footer",
                    "This is an automated message from Power Alert. Please do not reply to this email."));
            model.put("unsubscribeText", getLocalizedMessage(bundle, "email.unsubscribe",
                    "To update your notification preferences"));
            model.put("unsubscribeUrl", "https://poweralert.lk/preferences/notifications?userId=" + user.getId() + "&token=UNSUBSCRIBE_TOKEN");
            model.put("unsubscribeHereLabel", getLocalizedMessage(bundle, "email.unsubscribeHere", "click here"));
            model.put("year", Year.now().toString());

        } catch (Exception e) {
            logger.warn("Error populating template model: {}", e.getMessage());
            // If there's an error, fall back to default values
            setFallbackModelValues(model, user, outage);
        }
    }

    private String getLocalizedMessage(ResourceBundle bundle, String key, String defaultValue) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }

    private void setFallbackModelValues(Map<String, Object> model, User user, Outage outage) {
        // Populate model with hardcoded values as fallback
        model.put("title", "Utility Outage Notification");
        model.put("greeting", "Hello " + user.getUsername());
        model.put("message", "We are writing to inform you about a " + outage.getType() + " outage that affects your registered address.");

        // Outage details
        model.put("outageTypeLabel", "Type");
        model.put("outageType", outage.getType().toString());
        model.put("areaLabel", "Affected Area");
        model.put("area", outage.getAffectedArea().getName());
        model.put("statusLabel", "Status");
        model.put("status", outage.getStatus().toString());

        // Status labels
        model.put("scheduledLabel", "Scheduled");
        model.put("ongoingLabel", "Ongoing");
        model.put("completedLabel", "Completed");
        model.put("cancelledLabel", "Cancelled");

        // Time information
        model.put("startTimeLabel", "Start Time");
        model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

        if (outage.getEstimatedEndTime() != null) {
            model.put("endTimeLabel", "Estimated End Time");
            model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            model.put("reasonLabel", "Reason");
            model.put("reason", outage.getReason());
        }

        // Additional information
        model.put("additionalInfo", "Please plan accordingly. We apologize for any inconvenience this may cause.");

        // Button and links
        model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
        model.put("viewDetailsLabel", "View Details");

        // Footer information
        model.put("footerText", "This is an automated message from Power Alert. Please do not reply to this email.");
        model.put("unsubscribeText", "To update your notification preferences");
        model.put("unsubscribeUrl", "https://poweralert.lk/preferences/notifications?userId=" + user.getId() + "&token=UNSUBSCRIBE_TOKEN");
        model.put("unsubscribeHereLabel", "click here");
        model.put("year", Year.now().toString());
    }

    private String createHtmlEmailContent(Map<String, Object> model) {
        // This is a simplified HTML generation
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #0066cc; color: white; padding: 10px; text-align: center; }");
        content.append(".content { padding: 20px; background-color: #f9f9f9; }");
        content.append(".footer { font-size: 12px; color: #777; text-align: center; margin-top: 20px; }");
        content.append(".button { display: inline-block; background-color: #0066cc; color: white; padding: 10px 15px; text-decoration: none; border-radius: 4px; }");
        content.append("</style></head><body><div class='container'>");

        // Header
        content.append("<div class='header'><h2>").append(model.getOrDefault("title", "Utility Outage Notification")).append("</h2></div>");

        // Content
        content.append("<div class='content'>");
        content.append("<p>").append(model.getOrDefault("greeting", "Hello")).append("</p>");
        content.append("<p>").append(model.getOrDefault("message", "There is an outage that affects your area.")).append("</p>");

        // Outage details
        content.append("<div style='margin: 20px 0; background-color: #fff; padding: 15px; border: 1px solid #ddd; border-radius: 5px;'>");
        content.append("<p><strong>").append(model.getOrDefault("outageTypeLabel", "Type")).append(":</strong> ");
        content.append("<span style='font-weight: bold; color: #cc0000;'>").append(model.getOrDefault("outageType", "Unknown")).append("</span></p>");

        content.append("<p><strong>").append(model.getOrDefault("areaLabel", "Area")).append(":</strong> ").append(model.getOrDefault("area", "Unknown")).append("</p>");
        content.append("<p><strong>").append(model.getOrDefault("statusLabel", "Status")).append(":</strong> ").append(model.getOrDefault("status", "Unknown")).append("</p>");
        content.append("<p><strong>").append(model.getOrDefault("startTimeLabel", "Start Time")).append(":</strong> ").append(model.getOrDefault("startTime", "Unknown")).append("</p>");

        if (model.containsKey("endTime")) {
            content.append("<p><strong>").append(model.getOrDefault("endTimeLabel", "End Time")).append(":</strong> ").append(model.get("endTime")).append("</p>");
        }

        if (model.containsKey("reason")) {
            content.append("<p><strong>").append(model.getOrDefault("reasonLabel", "Reason")).append(":</strong> ").append(model.get("reason")).append("</p>");
        }
        content.append("</div>");

        // Additional info
        content.append("<p>").append(model.getOrDefault("additionalInfo", "Please plan accordingly.")).append("</p>");

        // Button
        if (model.containsKey("portalUrl")) {
            content.append("<div style='text-align: center; margin: 30px 0;'>");
            content.append("<a href='").append(model.get("portalUrl")).append("' class='button' style='color: white;'>");
            content.append(model.getOrDefault("viewDetailsLabel", "View Details")).append("</a>");
            content.append("</div>");
        }

        content.append("</div>");

        // Footer
        content.append("<div class='footer'>");
        content.append("<p>").append(model.getOrDefault("footerText", "This is an automated message.")).append("</p>");

        if (model.containsKey("unsubscribeUrl")) {
            content.append("<p>").append(model.getOrDefault("unsubscribeText", "To update your preferences")).append(" ");
            content.append("<a href='").append(model.get("unsubscribeUrl")).append("'>");
            content.append(model.getOrDefault("unsubscribeHereLabel", "click here")).append("</a></p>");
        }

        content.append("<p>&copy; ").append(model.getOrDefault("year", Year.now().toString())).append(" PowerAlert</p>");
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

    /** Helper method to get email subject with robust error handling */
    private String getEmailSubject(Outage outage, String language) {
        try {
            Locale locale = getLocale(language);
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea().getName();

            ResourceBundle bundle = loadResourceBundle(language);
            try {
                String template = bundle.getString("email.outage.subject");
                return String.format(template, outageType, areaName);
            } catch (MissingResourceException e) {
                // Fallback to default subject
                return outageType + " Outage Alert - " + areaName;
            }
        } catch (Exception e) {
            logger.warn("Error generating email subject: {}", e.getMessage());
            // Fallback subject if all lookups fail
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea().getName();
            return outageType + " Outage Alert - " + areaName;
        }
    }

    /** Helper method to get locale from language code */
    private Locale getLocale(String language) {
        if (language == null) {
            return Locale.ENGLISH;
        }

        switch (language.toLowerCase()) {
            case "si":
                return new Locale("si", "LK");
            case "ta":
                return new Locale("ta", "LK");
            default:
                return Locale.ENGLISH;
        }
    }

    /**
     * Helper method to load resource bundle with robust error handling
     * @return ResourceBundle or throws MissingResourceException if not found
     */
    private ResourceBundle loadResourceBundle(String language) throws MissingResourceException {
        Locale locale = getLocale(language);
        return ResourceBundle.getBundle("messages", locale);
    }
}