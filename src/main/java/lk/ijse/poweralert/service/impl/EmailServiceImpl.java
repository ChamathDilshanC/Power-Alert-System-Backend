package lk.ijse.poweralert.service.impl;

import jakarta.annotation.PostConstruct;
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

    /**
     * Initialize the service and check template availability
     */
    @PostConstruct
    public void init() {
        checkTemplateAvailability();
    }

    /**
     * Check template availability for debugging purposes
     */
    private void checkTemplateAvailability() {
        try {
            logger.info("FreeMarker template base path: {}",
                    freemarkerConfig.getTemplateLoader().toString());

            // List of templates to check
            String[] templates = {
                    "outage-notification.ftl",
                    "outage-notification_si.ftl",
                    "outage-notification_ta.ftl",
                    "outage-update.ftl",
                    "outage-update_si.ftl",
                    "outage-update_ta.ftl",
                    "outage-cancellation.ftl",
                    "outage-cancellation_si.ftl",
                    "outage-cancellation_ta.ftl",
                    "outage-restoration.ftl",
                    "outage-restoration_si.ftl",
                    "outage-restoration_ta.ftl"
            };

            for (String template : templates) {
                try {
                    freemarkerConfig.getTemplate(template);
                    logger.info("Template found: {}", template);
                } catch (IOException e) {
                    logger.warn("Template not found: {} - {}", template, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error checking templates: {}", e.getMessage(), e);
        }
    }

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
    public CompletableFuture<Boolean> sendOutageUpdateEmail(User user, Outage outage, String language) {
        try {
            logger.info("Preparing outage update email for user: {}, language: {}", user.getEmail(), language);

            // Create a model with all the required values
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("outageType", outage.getType().toString());
            model.put("areaName", outage.getAffectedArea().getName());
            model.put("status", outage.getStatus().toString());
            model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));
            model.put("updatedAt", LocalDateTime.now().format(DATE_FORMATTER));
            model.put("language", language); // Add language for template use

            if (outage.getEstimatedEndTime() != null) {
                model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
            }

            if (outage.getReason() != null && !outage.getReason().isEmpty()) {
                model.put("reason", outage.getReason());
            }

            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("year", Year.now().toString());

            // Get a localized subject
            String subject = getLocalizedSubject(outage, "outage.email.update.subject", language);

            // Send the template email
            return sendTemplateEmail(user.getEmail(), subject, "outage-update.ftl", model, language);

        } catch (Exception e) {
            logger.error("Error sending outage update email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendOutageCancellationEmail(User user, Outage outage, String language) {
        try {
            logger.info("Preparing outage cancellation email for user: {}, language: {}", user.getEmail(), language);

            // Create a model with all the required values
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("outageType", outage.getType().toString());
            model.put("areaName", outage.getAffectedArea().getName());
            model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));
            model.put("updatedAt", LocalDateTime.now().format(DATE_FORMATTER));
            model.put("language", language); // Add language for template use

            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("year", Year.now().toString());

            // Get a localized subject
            String subject = getLocalizedSubject(outage, "outage.email.cancel.subject", language);

            // Send the template email
            return sendTemplateEmail(user.getEmail(), subject, "outage-cancellation.ftl", model, language);

        } catch (Exception e) {
            logger.error("Error sending outage cancellation email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendOutageRestorationEmail(User user, Outage outage, String language) {
        try {
            logger.info("Preparing outage restoration email for user: {}, language: {}", user.getEmail(), language);

            // Create a model with all the required values
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("outageType", outage.getType().toString());
            model.put("areaName", outage.getAffectedArea().getName());
            model.put("updatedAt", LocalDateTime.now().format(DATE_FORMATTER));
            model.put("language", language); // Add language for template use

            if (outage.getActualEndTime() != null) {
                model.put("actualEndTime", outage.getActualEndTime().format(DATE_FORMATTER));
            }

            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("year", Year.now().toString());

            // Get a localized subject
            String subject = getLocalizedSubject(outage, "outage.email.restore.subject", language);

            // Send the template email
            return sendTemplateEmail(user.getEmail(), subject, "outage-restoration.ftl", model, language);

        } catch (Exception e) {
            logger.error("Error sending outage restoration email to {}: {}", user.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
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
            logger.info("Sending template email to: {}, subject: {}, template: {}, language: {}",
                    to, subject, templateName, language);

            // Ensure the model is a map
            Map<String, Object> templateModel = convertToMap(model);

            // Add default values for common template variables if not present
            ensureDefaultTemplateValues(templateModel, subject);

            // Add language explicitly to the model
            templateModel.put("language", language);

            // Add portal URL if not present
            if (!templateModel.containsKey("portalUrl")) {
                templateModel.put("portalUrl", "https://poweralert.lk/outages");
            }

            // Build a list of templates to try in order of preference
            List<String> templateCandidates = buildTemplateCandidateList(templateName, language);

            // Try each template until one works
            String templateContent = null;
            String successfulTemplate = null;

            for (String candidate : templateCandidates) {
                try {
                    logger.debug("Attempting to load template: {}", candidate);
                    Template template = freemarkerConfig.getTemplate(candidate); // Without "static/" prefix
                    templateContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateModel);
                    successfulTemplate = candidate;
                    logger.info("Successfully processed template: {}", candidate);
                    break;
                } catch (IOException e) {
                    logger.debug("Template not found: {}", candidate);
                } catch (TemplateException e) {
                    logger.warn("Error processing template {}: {}", candidate, e.getMessage());
                }
            }



            // If no template was found or successfully processed, create a fallback
            if (templateContent == null || templateContent.trim().isEmpty()) {
                logger.warn("No suitable template found or processed for {}. Templates tried: {}",
                        templateName, templateCandidates);
                templateContent = createHtmlEmailContent(templateModel, language);

                // Add debug information to the email if in debug mode
                if (logger.isDebugEnabled()) {
                    templateContent += createDebugInfo(templateName, language, templateCandidates);
                }
            } else {
                logger.info("Using template: {} for language: {}", successfulTemplate, language);
            }

            sendEmail(to, subject, templateContent);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            logger.error("Failed to send template email to {}: {}", to, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @PostConstruct
    public void logTemplateFiles() {
        try {
            File staticDir = new File("I:/Projects/PowerAlert/target/classes/static/");
            if (staticDir.exists() && staticDir.isDirectory()) {
                logger.info("Listing template files in directory: {}", staticDir.getAbsolutePath());
                for (File file : staticDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".ftl")) {
                        logger.info("Found template file: {}", file.getName());
                    }
                }
            } else {
                logger.warn("Static directory not found or is not a directory: {}", staticDir.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Error listing template files: {}", e.getMessage(), e);
        }
    }

    /**
     * Build a list of template candidates to try in order of preference
     */
    private List<String> buildTemplateCandidateList(String templateName, String language) {
        List<String> candidates = new ArrayList<>();
        String baseName = templateName.replace(".ftl", "");

        // If a specific language is requested (not English), try language-specific templates first
        if (language != null && !language.equalsIgnoreCase("en")) {
            // Format: outage-notification_si.ftl
            candidates.add(baseName + "_" + language.toLowerCase() + ".ftl");

            // Format: outage-notification-si.ftl
            candidates.add(baseName + "-" + language.toLowerCase() + ".ftl");

            // Format: si/outage-notification.ftl (in language subdirectory)
            candidates.add(language.toLowerCase() + "/" + templateName);
        }

        // Always try the default template last
        candidates.add(templateName);

        return candidates;
    }

    /**
     * Create debug information for troubleshooting template issues
     */
    private String createDebugInfo(String templateName, String language, List<String> templatesTried) {
        StringBuilder debug = new StringBuilder();
        debug.append("\n<!-- Debug info: -->\n");
        debug.append("<!-- Template name: ").append(templateName).append(" -->\n");
        debug.append("<!-- Language: ").append(language).append(" -->\n");
        debug.append("<!-- Templates tried: ");

        for (String template : templatesTried) {
            debug.append(template).append(", ");
        }

        debug.append(" -->\n");
        return debug.toString();
    }

    /**
     * Ensure default template values are present in the model
     */
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

    /**
     * Convert an object to a map for template processing
     */
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
            logger.info("Preparing outage notification email for user: {}, language: {}",
                    user.getEmail(), language);

            // Create a model with all the required values
            Map<String, Object> model = new HashMap<>();
            model.put("username", user.getUsername());
            model.put("outageType", outage.getType().toString());
            model.put("areaName", outage.getAffectedArea().getName());
            model.put("status", outage.getStatus().toString());
            model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));
            model.put("language", language); // Add language for template use

            if (outage.getEstimatedEndTime() != null) {
                model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
            }

            if (outage.getReason() != null && !outage.getReason().isEmpty()) {
                model.put("reason", outage.getReason());
            }

            model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
            model.put("year", Year.now().toString());

            // Get a localized subject
            String subject = getLocalizedSubject(outage, "outage.email.subject", language);

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
            String language = user.getPreferredLanguage();
            logger.info("Sending login notification email to: {}, language: {}", user.getEmail(), language);

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
            model.put("language", language); // Add language for template use
            model.put("unsubscribeUrl", "https://poweralert.lk/preferences/unsubscribe?email=" +
                    java.net.URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8.name()));

            // Get localized subject from resource bundle
            String subject = "PowerAlert Security Alert: New Login";
            try {
                ResourceBundle bundle = loadResourceBundle(language);
                subject = bundle.getString("login.notification.subject");
            } catch (Exception e) {
                logger.warn("Could not get localized subject for login notification: {}", e.getMessage());
            }

            return sendTemplateEmail(
                    user.getEmail(),
                    subject,
                    "login-notification.ftl",
                    model,
                    language
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
                // Use user's preferred language if available
                String userSubject = subject;
                String userContent = content;

                // If content is from a template, consider translating it
                // This is a simplified approach; in a real app, you might want to use a template for bulk emails too

                sendEmail(user.getEmail(), userSubject, userContent);
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
                // Consider user's preferred language for these emails as well
                String userLanguage = user.getPreferredLanguage();
                String userSubject = subject;

                // Try to get localized subject if possible
                try {
                    ResourceBundle bundle = loadResourceBundle(userLanguage);
                    if (bundle.containsKey("area.notification.subject")) {
                        userSubject = bundle.getString("area.notification.subject");
                    }
                } catch (Exception e) {
                    logger.debug("Could not localize area notification subject: {}", e.getMessage());
                }

                sendEmail(user.getEmail(), userSubject, content);
                successCount.incrementAndGet();
            } catch (Exception e) {
                logger.error("Failed to send email to user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        logger.info("Successfully sent emails to {}/{} users in area", successCount.get(), usersInArea.size());
        return CompletableFuture.completedFuture(successCount.get());
    }

    /**
     * Get subject with localization based on resource bundle
     */
    private String getLocalizedSubject(Outage outage, String subjectKey, String language) {
        try {
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea().getName();

            ResourceBundle bundle = loadResourceBundle(language);
            try {
                String template = bundle.getString(subjectKey);
                return String.format(template, outageType, areaName);
            } catch (MissingResourceException e) {
                logger.warn("Missing resource key '{}' for language '{}', using fallback", subjectKey, language);

                // Fallback subjects based on subject key
                switch (subjectKey) {
                    case "outage.email.subject":
                        return outageType + " Outage Alert - " + areaName;
                    case "outage.email.update.subject":
                        return outageType + " Outage Update - " + areaName;
                    case "outage.email.cancel.subject":
                        return outageType + " Outage Cancellation - " + areaName;
                    case "outage.email.restore.subject":
                        return outageType + " Service Restoration - " + areaName;
                    default:
                        return "PowerAlert Notification - " + areaName;
                }
            }
        } catch (Exception e) {
            logger.warn("Error generating localized subject: {}", e.getMessage());
            // Generic fallback subject
            return "PowerAlert Notification - " + outage.getAffectedArea().getName();
        }
    }

    /**
     * Helper method to get locale from language code
     */
    private Locale getLocale(String language) {
        if (language == null || language.isEmpty()) {
            return Locale.ENGLISH;
        }

        switch (language.toLowerCase()) {
            case "si":
                return new Locale("si", "LK");
            case "ta":
                return new Locale("ta", "LK");
            case "en":
                return Locale.ENGLISH;
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
        try {
            return ResourceBundle.getBundle("messages", locale);
        } catch (MissingResourceException e) {
            logger.warn("Resource bundle for locale {} not found, falling back to English", locale);
            return ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
    }

    /**
     * Create a fallback HTML email when template processing fails
     */
    private String createHtmlEmailContent(Map<String, Object> model, String language) {
        // Get localized strings if possible
        String hello = "Hello";
        String outageTitle = "Utility Outage Notification";
        String outageMessage = "There is an outage that affects your area:";
        String typeLabel = "Type:";
        String areaLabel = "Area:";
        String statusLabel = "Status:";
        String startTimeLabel = "Start Time:";
        String endTimeLabel = "End Time:";
        String reasonLabel = "Reason:";
        String planMessage = "Please plan accordingly. Thank you for your patience.";
        String viewDetails = "View Details";
        String automatedMessage = "This is an automated message from Power Alert. Please do not reply to this email.";
        String needAssistance = "If you need assistance, please contact support@poweralert.lk";
        String managePreferences = "To manage your notification preferences,";
        String clickHere = "click here";

        try {
            // Try to load localized strings from resource bundle
            ResourceBundle bundle = loadResourceBundle(language);

            try { hello = bundle.getString("email.greeting"); } catch (Exception e) {}
            try { outageTitle = bundle.getString("email.outage.title"); } catch (Exception e) {}
            try { outageMessage = bundle.getString("email.outage.message"); } catch (Exception e) {}
            try { typeLabel = bundle.getString("email.label.type"); } catch (Exception e) {}
            try { areaLabel = bundle.getString("email.label.area"); } catch (Exception e) {}
            try { statusLabel = bundle.getString("email.label.status"); } catch (Exception e) {}
            try { startTimeLabel = bundle.getString("email.label.startTime"); } catch (Exception e) {}
            try { endTimeLabel = bundle.getString("email.label.endTime"); } catch (Exception e) {}
            try { reasonLabel = bundle.getString("email.label.reason"); } catch (Exception e) {}
            try { planMessage = bundle.getString("email.message.plan"); } catch (Exception e) {}
            try { viewDetails = bundle.getString("email.button.viewDetails"); } catch (Exception e) {}
            try { automatedMessage = bundle.getString("email.footer.automated"); } catch (Exception e) {}
            try { needAssistance = bundle.getString("email.footer.assistance"); } catch (Exception e) {}
            try { managePreferences = bundle.getString("email.footer.manage"); } catch (Exception e) {}
            try { clickHere = bundle.getString("email.footer.clickHere"); } catch (Exception e) {}

        } catch (Exception e) {
            logger.debug("Could not load localized strings for fallback email: {}", e.getMessage());
        }

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
        content.append("<div class='header'><h2>").append(model.getOrDefault("title", outageTitle)).append("</h2></div>");

        // Content
        content.append("<div class='content'>");
        content.append("<p>").append(hello).append(" ").append(model.getOrDefault("username", "User")).append(",</p>");

        // Main message
        if (model.containsKey("outageType")) {
            content.append("<p>").append(outageMessage).append(" ");
            content.append("<strong>").append(model.getOrDefault("areaName", "Unknown")).append("</strong></p>");

            // Outage details
            content.append("<div style='margin: 20px 0; background-color: #fff; padding: 15px; border: 1px solid #ddd; border-radius: 5px;'>");
            content.append("<p><strong>").append(typeLabel).append("</strong> ").append(model.get("outageType")).append("</p>");
            content.append("<p><strong>").append(areaLabel).append("</strong> ").append(model.getOrDefault("areaName", "Unknown")).append("</p>");
            content.append("<p><strong>").append(statusLabel).append("</strong> ").append(model.getOrDefault("status", "Unknown")).append("</p>");
            content.append("<p><strong>").append(startTimeLabel).append("</strong> ").append(model.getOrDefault("startTime", "Unknown")).append("</p>");

            if (model.containsKey("endTime")) {
                content.append("<p><strong>").append(endTimeLabel).append("</strong> ").append(model.get("endTime")).append("</p>");
            }

            if (model.containsKey("reason")) {
                content.append("<p><strong>").append(reasonLabel).append("</strong> ").append(model.get("reason")).append("</p>");
            }
            content.append("</div>");
        } else {
            content.append("<p>").append(model.getOrDefault("message", "There is an important notification for you.")).append("</p>");
        }

        // Additional info
        content.append("<p>").append(planMessage).append("</p>");

        // Button
        if (model.containsKey("portalUrl")) {
            content.append("<div style='text-align: center; margin: 30px 0;'>");
            content.append("<a href='").append(model.get("portalUrl")).append("' class='button' style='color: white;'>");
            content.append(viewDetails).append("</a>");
            content.append("</div>");
        }

        content.append("</div>");

        // Footer
        content.append("<div class='footer'>");
        content.append("<p>").append(automatedMessage).append("</p>");
        content.append("<p>").append(needAssistance).append("</p>");

        if (model.containsKey("unsubscribeUrl")) {
            content.append("<p>").append(managePreferences).append(" ");
            content.append("<a href='").append(model.get("unsubscribeUrl")).append("'>");
            content.append(clickHere).append("</a></p>");
        }

        content.append("<p>&copy; ").append(model.getOrDefault("year", Year.now().toString())).append(" PowerAlert</p>");
        content.append("</div>");

        content.append("</div></body></html>");
        return content.toString();
    }
}