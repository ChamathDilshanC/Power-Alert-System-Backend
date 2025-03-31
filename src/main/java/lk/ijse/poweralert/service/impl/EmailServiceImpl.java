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
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    @Value("${app.email.from}")
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
    public void sendEmail(String to, String subject, String content) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent to: {}, subject: {}", to, subject);
            return;
        }

        try {
            logger.info("Sending email to: {}, subject: {}", to, subject);

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
            } catch (MailException me) {
                logger.error("JavaMailSender error: {}", me.getMessage(), me);
            }
        } catch (Exception e) {
            logger.error("Exception preparing email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendTemplateEmail(String to, String subject, String templateName, Object model) {
        // Call the overloaded method with default language (English)
        return sendTemplateEmail(to, subject, templateName, model, "en");
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendTemplateEmail(String to, String subject, String templateName, Object model, String language) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent template email to: {}, subject: {}", to, subject);
            return CompletableFuture.completedFuture(true);
        }

        try {
            logger.info("Sending template email to: {}, subject: {}, template: {}, language: {}", to, subject, templateName, language);

            // Ensure the model is a map
            Map<String, Object> templateModel = convertToMap(model);

            // Add default values for common template variables if not present
            ensureDefaultTemplateValues(templateModel, subject);

            // Add portal URL if not present
            if (!templateModel.containsKey("portalUrl")) {
                templateModel.put("portalUrl", "https://poweralert.lk/outages");
            }

            // First try language-specific template if language is specified
            String templateToUse = templateName;
            if (language != null && !language.equals("en")) {
                // Try language-specific template first (e.g., outage-notification_si.ftl)
                templateToUse = templateName.replace(".ftl", "") + "_" + language + ".ftl";
            }

            // Process the template with fallback to default template
            String content;
            try {
                Template template = freemarkerConfig.getTemplate(templateToUse);
                content = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateModel);
            } catch (IOException e) {
                logger.warn("Language-specific template '{}' not found, trying default template", templateToUse);
                try {
                    // Fall back to default template
                    Template defaultTemplate = freemarkerConfig.getTemplate(templateName);
                    content = FreeMarkerTemplateUtils.processTemplateIntoString(defaultTemplate, templateModel);
                } catch (Exception ex) {
                    logger.error("Failed to process default template '{}': {}", templateName, ex.getMessage(), ex);
                    content = createHtmlEmailContent(templateModel);
                }
            } catch (TemplateException e) {
                logger.error("Error processing template '{}': {}", templateToUse, e.getMessage(), e);
                content = createHtmlEmailContent(templateModel);
            }

            // Check if content was generated successfully
            if (content == null || content.trim().isEmpty()) {
                logger.warn("Template processing resulted in empty content, using fallback HTML");
                content = createHtmlEmailContent(templateModel);
            }

            sendEmail(to, subject, content);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            logger.error("Failed to send template email to: {}: {}", to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private void ensureDefaultTemplateValues(Map<String, Object> model, String subject) {
        // Current year
        if (!model.containsKey("year")) {
            model.put("year", Year.now().toString());
        }

        // Use subject as title if no title is provided
        if (!model.containsKey("title") && subject != null) {
            model.put("title", subject);
        }

        // Current date for updatedAt if not set
        if (!model.containsKey("updatedAt")) {
            model.put("updatedAt", LocalDateTime.now().format(DATE_FORMATTER));
        }

        // Add unsubscribe URL if not present
        if (!model.containsKey("unsubscribeUrl")) {
            model.put("unsubscribeUrl", "https://poweralert.lk/preferences/notifications");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object model) {
        if (model instanceof Map) {
            return (Map<String, Object>) model;
        } else {
            // Create a new map
            Map<String, Object> map = new HashMap<>();

            if (model == null) {
                return map;
            }

            // For Outage objects, extract properties
            if (model instanceof Outage) {
                Outage outage = (Outage) model;
                map.put("outageType", outage.getType().toString());
                map.put("status", outage.getStatus().toString());
                map.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

                if (outage.getEstimatedEndTime() != null) {
                    map.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
                }

                if (outage.getReason() != null) {
                    map.put("reason", outage.getReason());
                }

                if (outage.getAffectedArea() != null) {
                    map.put("areaName", outage.getAffectedArea().getName());
                }
            } else {
                // For other objects, put the original object in the map
                map.put("data", model);
            }

            return map;
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendEmailWithAttachment(String to, String subject, String content,
                                                              String attachmentFilePath, String attachmentFileName) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent email with attachment to: {}, subject: {}", to, subject);
            return CompletableFuture.completedFuture(true);
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
                return CompletableFuture.completedFuture(false);
            }

            FileSystemResource fileResource = new FileSystemResource(file);
            helper.addAttachment(attachmentFileName, fileResource);

            mailSender.send(message);
            logger.info("Email with attachment sent successfully to: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (MessagingException e) {
            logger.error("Messaging exception when sending email with attachment: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            logger.error("Failed to send email with attachment to: {}: {}", to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendOutageNotificationEmail(User user, Outage outage, String language) {
        try {
            logger.info("Preparing outage notification email for user: {}", user.getEmail());

            // Create a model with all the required values
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

            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("year", Year.now().toString());

            // Get a localized subject
            String subject = getEmailSubject(outage, language);

            // Send the template email
            return sendTemplateEmail(user.getEmail(), subject, "outage-notification.ftl", model, language);

        } catch (Exception e) {
            logger.error("Error sending outage notification email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendLoginNotificationEmail(User user, String ipAddress, String device, String location) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent login notification to: {}", user.getEmail());
            return CompletableFuture.completedFuture(true);
        }

        try {
            logger.info("Sending login notification email to: {}", user.getEmail());

            // Create the model with the template variables
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("email", user.getEmail());
            model.put("loginTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            model.put("ipAddress", ipAddress != null ? ipAddress : "Unknown");
            model.put("device", device != null ? device : "Unknown");
            model.put("location", location != null ? location : "Unknown");
            model.put("accountSecurityUrl", "https://poweralert.lk/account/security");
            model.put("year", Year.now().toString());
            model.put("unsubscribeUrl", "https://poweralert.lk/preferences/unsubscribe?email=" +
                    java.net.URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8.name()));

            return sendTemplateEmail(
                    user.getEmail(),
                    "PowerAlert Security Alert: New Login",
                    "login-notification.ftl",
                    model,
                    user.getPreferredLanguage()
            );
        } catch (Exception e) {
            logger.error("Error sending login notification email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Integer> sendEmailToAllUsers(String subject, String content) {
        logger.info("Sending email to all active users with subject: {}", subject);

        List<User> activeUsers = userRepository.findByIsActiveTrue();
        AtomicInteger successCount = new AtomicInteger(0);

        activeUsers.forEach(user -> {
            try {
                sendEmail(user.getEmail(), subject, content);
                successCount.incrementAndGet();
            } catch (Exception e) {
                logger.error("Failed to send email to user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        logger.info("Successfully sent emails to {}/{} active users", successCount.get(), activeUsers.size());
        return CompletableFuture.completedFuture(successCount.get());
    }

    @Override
    @Async
    public CompletableFuture<Integer> sendEmailToUsersInArea(Long areaId, String subject, String content) {
        logger.info("Sending email to users in area ID: {} with subject: {}", areaId, subject);

        List<User> usersInArea = userRepository.findUsersByAreaId(areaId);
        AtomicInteger successCount = new AtomicInteger(0);

        usersInArea.forEach(user -> {
            try {
                sendEmail(user.getEmail(), subject, content);
                successCount.incrementAndGet();
            } catch (Exception e) {
                logger.error("Failed to send email to user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        logger.info("Successfully sent emails to {}/{} users in area", successCount.get(), usersInArea.size());
        return CompletableFuture.completedFuture(successCount.get());
    }

    /** Helper method to get email subject with robust error handling */
    private String getEmailSubject(Outage outage, String language) {
        try {
            Locale locale = getLocale(language);
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea().getName();

            ResourceBundle bundle = loadResourceBundle(language);
            try {
                String template = bundle.getString("outage.email.subject");
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

    /**
     * Create a fallback HTML email when template processing fails
     */
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
        content.append("<p>Hello ").append(model.getOrDefault("username", "User")).append(",</p>");

        // Main message
        if (model.containsKey("outageType")) {
            content.append("<p>There is a ").append(model.get("outageType")).append(" outage that affects your area: ");
            content.append("<strong>").append(model.getOrDefault("areaName", "Unknown")).append("</strong></p>");

            // Outage details
            content.append("<div style='margin: 20px 0; background-color: #fff; padding: 15px; border: 1px solid #ddd; border-radius: 5px;'>");
            content.append("<p><strong>Type:</strong> ").append(model.get("outageType")).append("</p>");
            content.append("<p><strong>Area:</strong> ").append(model.getOrDefault("areaName", "Unknown")).append("</p>");
            content.append("<p><strong>Status:</strong> ").append(model.getOrDefault("status", "Unknown")).append("</p>");
            content.append("<p><strong>Start Time:</strong> ").append(model.getOrDefault("startTime", "Unknown")).append("</p>");

            if (model.containsKey("endTime")) {
                content.append("<p><strong>End Time:</strong> ").append(model.get("endTime")).append("</p>");
            }

            if (model.containsKey("reason")) {
                content.append("<p><strong>Reason:</strong> ").append(model.get("reason")).append("</p>");
            }
            content.append("</div>");
        } else {
            content.append("<p>").append(model.getOrDefault("message", "There is an important notification for you.")).append("</p>");
        }

        // Additional info
        content.append("<p>Please plan accordingly. Thank you for your patience.</p>");

        // Button
        if (model.containsKey("portalUrl")) {
            content.append("<div style='text-align: center; margin: 30px 0;'>");
            content.append("<a href='").append(model.get("portalUrl")).append("' class='button' style='color: white;'>");
            content.append("View Details").append("</a>");
            content.append("</div>");
        }

        content.append("</div>");

        // Footer
        content.append("<div class='footer'>");
        content.append("<p>This is an automated message from Power Alert. Please do not reply to this email.</p>");
        content.append("<p>If you need assistance, please contact support@poweralert.lk</p>");

        if (model.containsKey("unsubscribeUrl")) {
            content.append("<p>To manage your notification preferences, ");
            content.append("<a href='").append(model.get("unsubscribeUrl")).append("'>");
            content.append("click here</a></p>");
        }

        content.append("<p>&copy; ").append(model.getOrDefault("year", Year.now().toString())).append(" PowerAlert</p>");
        content.append("</div>");

        content.append("</div></body></html>");
        return content.toString();
    }
}