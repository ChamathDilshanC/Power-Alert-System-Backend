package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/test")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResponseDTO responseDTO;

    @GetMapping("/email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        try {
            logger.info("Testing email to: {}", to);
            boolean result = emailService.sendEmail(
                    to,
                    "PowerAlert Test Email",
                    "<html><body><h1>Test Email</h1><p>This is a test email from PowerAlert.</p></body></html>"
            );

            if (result) {
                return ResponseEntity.ok("Email sent successfully to: " + to);
            } else {
                return ResponseEntity.status(500).body("Failed to send email. Check logs for details.");
            }
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

            boolean simpleSent = false;
            try {
                simpleSent = emailService.sendEmail(to, subject, simpleContent);
                result.append("Simple email test: ").append(simpleSent ? "SENT" : "FAILED").append("\n");
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
}