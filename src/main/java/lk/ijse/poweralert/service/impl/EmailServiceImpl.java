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
    public void sendEmail(String to, String subject, String content) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent to: {}, subject: {}", to, subject);
            return;
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
            } catch (MailException me) {
                logger.error("JavaMailSender error: {}", me.getMessage(), me);
            }
        } catch (Exception e) {
            logger.error("Exception preparing email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<Boolean> sendTemplateEmail(String to, String subject, String templateName, Object model) {
        return null;
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

            // Ensure the model is a map (if not already)
            Map<String, Object> templateModel = convertToMap(model);

            // Add default values for common template variables if not present
            ensureDefaultTemplateValues(templateModel);

            // First try language-specific template if language is specified
            String templateToUse = templateName;
            if (language != null && !language.equals("en")) {
                templateToUse = templateName + "_" + language;
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
                sendEmail(user.getEmail(), subject, content);
                return CompletableFuture.completedFuture(true);
            } catch (Exception e) {
                logger.error("Failed to process template: {}", e.getMessage(), e);
                // Send fallback email
                sendEmail(user.getEmail(), subject, createSimpleFallbackEmail(user, outage));
                return CompletableFuture.completedFuture(true);
            }
        } catch (Exception e) {
            logger.error("Error sending outage notification email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
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

    @Async
    @Override
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

            // Try to process the template
            try {
                Template template = freemarkerConfig.getTemplate("login-notification.ftl");
                String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
                sendEmail(user.getEmail(), "PowerAlert Security Alert: New Login", content);
                return CompletableFuture.completedFuture(true);
            } catch (Exception e) {
                logger.error("Failed to process login notification template: {}", e.getMessage(), e);

                // Fallback to generate HTML content directly if template fails
                String content = createLoginNotificationHtml(model);
                sendEmail(user.getEmail(), "PowerAlert Security Alert: New Login", content);
                return CompletableFuture.completedFuture(true);
            }
        } catch (Exception e) {
            logger.error("Error sending login notification email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Create HTML content for login notification when template processing fails
     */
    private String createLoginNotificationHtml(Map<String, Object> model) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>PowerAlert Login Notification</title>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Arial,sans-serif;line-height:1.6;color:#333;margin:0;padding:0;background-color:#f5f5f5;}");
        html.append(".container{max-width:600px;margin:0 auto;padding:20px;}");
        html.append(".header{background-color:#4285F4;color:white;padding:20px;text-align:center;border-radius:5px 5px 0 0;}");
        html.append(".content{background-color:white;padding:30px;border-left:1px solid #ddd;border-right:1px solid #ddd;box-shadow:0 2px 5px rgba(0,0,0,0.1);}");
        html.append(".login-info{background-color:#f9f9f9;border:1px solid #eee;border-radius:5px;padding:20px;margin:20px 0;}");
        html.append(".button{display:inline-block;background-color:#4285F4;color:white;text-decoration:none;padding:12px 25px;border-radius:4px;font-weight:bold;margin:15px 0;}");
        html.append(".footer{background-color:#f1f1f1;padding:15px;text-align:center;font-size:12px;color:#666;border-radius:0 0 5px 5px;border:1px solid #ddd;}");
        html.append("</style></head><body><div class=\"container\">");

        // Header
        html.append("<div class=\"header\"><h1>Login Notification</h1></div>");

        // Content
        html.append("<div class=\"content\">");
        html.append("<p>Hello ").append(model.get("username")).append(",</p>");
        html.append("<p>We detected a new login to your PowerAlert account.</p>");

        // Login info box
        html.append("<div class=\"login-info\">");
        html.append("<p><strong>Username:</strong> ").append(model.get("username")).append("</p>");
        html.append("<p><strong>Time:</strong> ").append(model.get("loginTime")).append("</p>");
        html.append("<p><strong>IP Address:</strong> ").append(model.get("ipAddress")).append("</p>");
        html.append("<p><strong>Device:</strong> ").append(model.get("device")).append("</p>");
        html.append("<p><strong>Location:</strong> ").append(model.get("location")).append("</p>");
        html.append("</div>");

        html.append("<p>If this was you, no further action is required. If you didn't login recently, please secure your account immediately by changing your password.</p>");

        // Button
        String accountUrl = (String) model.getOrDefault("accountSecurityUrl", "https://poweralert.lk/account/security");
        html.append("<div style=\"text-align:center;\">");
        html.append("<a href=\"").append(accountUrl).append("\" class=\"button\" style=\"color:white;\">Manage Account Security</a>");
        html.append("</div>");

        html.append("</div>");

        // Footer
        html.append("<div class=\"footer\">");
        html.append("<p>This is an automated message from Power Alert. Please do not reply to this email.</p>");
        html.append("<p>If you did not request this email, please contact support at support@poweralert.lk</p>");
        html.append("<p>&copy; ").append(model.get("year")).append(" Power Alert</p>");
        html.append("</div></div></body></html>");

        return html.toString();
    }
}