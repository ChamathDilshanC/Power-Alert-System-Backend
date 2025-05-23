package lk.ijse.poweralert.controller.test;

import freemarker.template.Template;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lk.ijse.poweralert.job.AdvanceNotificationJob;
import lk.ijse.poweralert.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/admin/test")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResponseDTO responseDTO;

    @Autowired
    private FreeMarkerConfigurer freemarkerConfig;

    @Autowired
    private AdvanceNotificationJob advanceNotificationJob;

    @GetMapping("/email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        try {
            logger.info("Testing email to: {}", to);

            // Send email - void return type
            emailService.sendEmail(
                    to,
                    "PowerAlert Test Email",
                    "<html><body><h1>Test Email</h1><p>This is a test email from PowerAlert.</p></body></html>"
            );

            return ResponseEntity.ok("Email sent successfully to: " + to);
        } catch (Exception e) {
            logger.error("Test email error: ", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/email-diagnostic")
    public ResponseEntity<ResponseDTO> testEmailDiagnostic(
            @RequestParam String to,
            @RequestParam(defaultValue = "Email Diagnostic Test") String subject) {

        try {
            logger.info("Running email diagnostic to: {}", to);

            // Create result object
            StringBuilder result = new StringBuilder();

            // Test connection
            boolean connected = false;
            try {
                connected = emailService.testEmailConnection();
                result.append("Email server connection test: ").append(connected ? "PASSED" : "FAILED").append("\n");
            } catch (Exception e) {
                result.append("Email server connection test exception: ").append(e.getMessage()).append("\n");
            }

            // Send simple test email
            String simpleContent = "<html><body><h1>Test Email</h1>" +
                    "<p>This is a diagnostic test email from PowerAlert.</p>" +
                    "<p>Time: " + LocalDateTime.now() + "</p>" +
                    "</body></html>";

            try {
                // void return type
                emailService.sendEmail(to, subject, simpleContent);
                result.append("Simple email test: SENT\n");
            } catch (Exception e) {
                result.append("Simple email test exception: ").append(e.getMessage()).append("\n");
            }

            // Check configuration
            result.append("\nConfiguration:\n");
            result.append("Email enabled: ").append(emailService.isEnabled()).append("\n");
            result.append("From email: ").append(emailService.getFromEmail()).append("\n");

            responseDTO.setCode(200);
            responseDTO.setMessage("Email diagnostic completed");
            responseDTO.setData(result.toString());
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error during diagnostic: ", e);
            responseDTO.setCode(500);
            responseDTO.setMessage("Error during diagnostic: " + e.getMessage());
            responseDTO.setData(null);
            return ResponseEntity.status(500).body(responseDTO);
        }
    }

    @GetMapping("/test-template")
    public ResponseEntity<String> testTemplate() {
        try {
            logger.info("Testing FreeMarker template processing");

            // Create a test model
            Map<String, Object> model = new HashMap<>();
            model.put("username", "Test User");
            model.put("outageType", "WATER");
            model.put("areaName", "Colombo Central District");
            model.put("status", "SCHEDULED");
            model.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            model.put("endTime", LocalDateTime.now().plusHours(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            model.put("reason", "Scheduled maintenance for pipe repairs");
            model.put("year", Year.now().toString());

            // Try to process template
            try {
                Template template = freemarkerConfig.getConfiguration().getTemplate("outage-notification.ftl");
                String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

                // Log the generated content for debugging
                logger.debug("Generated template content: {}", content);

                // Send a test email with the template - void return type
                emailService.sendEmail(
                        "admin@poweralert.lk",
                        "WATER Outage Alert - Template Test",
                        content
                );

                return ResponseEntity.ok("Template processed successfully and email sent!");
            } catch (Exception e) {
                logger.error("Template processing error: {}", e.getMessage(), e);
                return ResponseEntity.status(500).body("Error processing template: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Test template error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error in test template method: " + e.getMessage());
        }
    }

    @GetMapping("/template-debug")
    public ResponseEntity<ResponseDTO> debugTemplateProcessing() {
        try {
            // Check available templates
            String[] templateNames = {"outage-notification.ftl", "outage-notification", "/outage-notification.ftl", "/templates/outage-notification.ftl"};
            Map<String, Boolean> templateResults = new HashMap<>();

            for (String name : templateNames) {
                try {
                    Template template = freemarkerConfig.getConfiguration().getTemplate(name);
                    templateResults.put(name, true);
                } catch (Exception e) {
                    templateResults.put(name, false);
                    logger.warn("Template not found: {}, Error: {}", name, e.getMessage());
                }
            }

            // Get configuration details
            Map<String, Object> configInfo = new HashMap<>();
            configInfo.put("templateLoaderPath", freemarkerConfig.getConfiguration().getTemplateLoader().toString());
            configInfo.put("defaultEncoding", freemarkerConfig.getConfiguration().getDefaultEncoding());
            configInfo.put("templateResults", templateResults);

            responseDTO.setCode(200);
            responseDTO.setMessage("Template debug information");
            responseDTO.setData(configInfo);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Template debug error: {}", e.getMessage(), e);
            responseDTO.setCode(500);
            responseDTO.setMessage("Error during template debugging: " + e.getMessage());
            responseDTO.setData(null);
            return ResponseEntity.status(500).body(responseDTO);
        }
    }

    @GetMapping("/send-test-template-email")
    public ResponseEntity<String> sendTestTemplateEmail(@RequestParam String to) {
        try {
            // Create a test model
            Map<String, Object> model = new HashMap<>();
            model.put("username", "Test User");
            model.put("outageType", "WATER");
            model.put("areaName", "Colombo Central District");
            model.put("status", "SCHEDULED");
            model.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            model.put("endTime", LocalDateTime.now().plusHours(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            model.put("reason", "Scheduled maintenance for pipe repairs");
            model.put("year", Year.now().toString());

            // Create simple HTML directly without template
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><style>");
            html.append("body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}");
            html.append(".container{max-width:600px;margin:0 auto;padding:20px;}");
            html.append(".header{background:#0066cc;color:white;padding:15px;text-align:center;}");
            html.append(".content{background:#f9f9f9;padding:20px;border:1px solid #ddd;}");
            html.append("</style></head><body><div class='container'>");
            html.append("<div class='header'><h1>").append(model.get("outageType")).append(" Outage Alert</h1></div>");
            html.append("<div class='content'>");
            html.append("<p>Hello ").append(model.get("username")).append(",</p>");
            html.append("<p>This is to inform you about a ").append(model.get("outageType"));
            html.append(" outage affecting your area: ").append(model.get("areaName")).append(".</p>");
            html.append("<div style='background:#fff;border:1px solid #ddd;padding:15px;margin:15px 0;'>");
            html.append("<p><strong>Type:</strong> ").append(model.get("outageType")).append("</p>");
            html.append("<p><strong>Area:</strong> ").append(model.get("areaName")).append("</p>");
            html.append("<p><strong>Status:</strong> ").append(model.get("status")).append("</p>");
            html.append("<p><strong>Start Time:</strong> ").append(model.get("startTime")).append("</p>");
            html.append("<p><strong>End Time:</strong> ").append(model.get("endTime")).append("</p>");
            html.append("<p><strong>Reason:</strong> ").append(model.get("reason")).append("</p>");
            html.append("</div>");
            html.append("<p>Please plan accordingly. We apologize for any inconvenience.</p>");
            html.append("</div>");
            html.append("<div style='text-align:center;padding:10px;background:#eee;font-size:12px;'>");
            html.append("<p>This is an automated message from Power Alert. Please do not reply to this email.</p>");
            html.append("<p>&copy; ").append(model.get("year")).append(" Power Alert</p>");
            html.append("</div></div></body></html>");

            // Send email - void return type
            emailService.sendEmail(to, model.get("outageType") + " Outage Alert - " + model.get("areaName"), html.toString());

            return ResponseEntity.ok("Direct HTML email sent successfully to: " + to);
        } catch (Exception e) {
            logger.error("Error sending test template email: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test-advance-notice")
    public ResponseEntity<ResponseDTO> testAdvanceNoticeEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "ELECTRICITY") String outageType,
            @RequestParam(defaultValue = "24") int hoursUntilStart) {

        try {
            logger.info("Testing advance notice email to: {}", to);

            // Create a test user
            User testUser = new User();
            testUser.setId(1L);
            testUser.setUsername("Test User");
            testUser.setEmail(to);

            // Create a test outage
            Outage testOutage = new Outage();
            testOutage.setId(123L);
            testOutage.setType(OutageType.valueOf(outageType));
            testOutage.setStatus(OutageStatus.SCHEDULED);
            testOutage.setStartTime(LocalDateTime.now().plusHours(hoursUntilStart));
            testOutage.setEstimatedEndTime(LocalDateTime.now().plusHours(hoursUntilStart + 4));
            testOutage.setReason("Scheduled infrastructure maintenance");

            // Create test area
            Area testArea = new Area();
            testArea.setId(10L);
            testArea.setName("Test District");
            testArea.setDistrict("Test District");
            testArea.setProvince("Test Province");
            testOutage.setAffectedArea(testArea);

            // Create template model
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("username", testUser.getUsername());
            templateModel.put("outageType", testOutage.getType().toString());
            templateModel.put("areaName", testOutage.getAffectedArea().getName());
            templateModel.put("status", "UPCOMING");
            templateModel.put("startTime", testOutage.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            templateModel.put("endTime", testOutage.getEstimatedEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            templateModel.put("reason", testOutage.getReason());
            templateModel.put("hoursUntilStart", hoursUntilStart);
            templateModel.put("year", Year.now().toString());
            templateModel.put("portalUrl", "https://poweralert.lk/outages/" + testOutage.getId());

            // Send test email - using CompletableFuture
            String subject = "Advance Notice: " + outageType + " Outage in " + testArea.getName();
            CompletableFuture<Boolean> emailFuture = emailService.sendTemplateEmail(
                    to,
                    subject,
                    "outage-advance-notice",
                    templateModel
            );

            // We can't immediately tell if the email was sent successfully due to async nature
            // So we'll respond with a positive message and log any errors that occur later
            emailFuture.exceptionally(ex -> {
                logger.error("Error sending advance notice email: {}", ex.getMessage());
                return false;
            });

            responseDTO.setCode(200);
            responseDTO.setMessage("Advance notice test email queued successfully");
            responseDTO.setData(templateModel);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error sending advance notice test email: {}", e.getMessage(), e);
            responseDTO.setCode(500);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return ResponseEntity.status(500).body(responseDTO);
        }
    }

    @GetMapping("/trigger-advance-notifications")
    public ResponseEntity<ResponseDTO> triggerAdvanceNotifications() {
        try {
            logger.info("Admin manually triggering advance notifications job");

            advanceNotificationJob.sendAdvanceNotifications();

            responseDTO.setCode(200);
            responseDTO.setMessage("Advance notifications job triggered successfully");
            responseDTO.setData(null);

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error triggering advance notifications job: {}", e.getMessage(), e);

            responseDTO.setCode(500);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return ResponseEntity.status(500).body(responseDTO);
        }
    }
}