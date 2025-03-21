package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums;
import lk.ijse.poweralert.service.EmailService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/emails")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class BulkEmailController {

    private static final Logger logger = LoggerFactory.getLogger(BulkEmailController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResponseDTO responseDTO;

    /** Send email to all active users   */
    @PostMapping("/all")
    public ResponseEntity<ResponseDTO> sendEmailToAllUsers(@Valid @RequestBody EmailRequest request) {
        try {
            logger.info("Sending email to all users with subject: {}", request.getSubject());

            int sentCount = emailService.sendEmailToAllUsers(request.getSubject(), request.getContent());

            Map<String, Object> data = new HashMap<>();
            data.put("emailsSent", sentCount);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Emails sent successfully");
            responseDTO.setData(data);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error sending emails to all users: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Send email to users in a specific area   */
    @PostMapping("/area/{areaId}")
    public ResponseEntity<ResponseDTO> sendEmailToUsersInArea(
            @PathVariable Long areaId,
            @Valid @RequestBody EmailRequest request) {
        try {
            logger.info("Sending email to users in area ID: {} with subject: {}", areaId, request.getSubject());

            int sentCount = emailService.sendEmailToUsersInArea(areaId, request.getSubject(), request.getContent());

            Map<String, Object> data = new HashMap<>();
            data.put("emailsSent", sentCount);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Emails sent to users in area successfully");
            responseDTO.setData(data);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error sending emails to users in area {}: {}", areaId, e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Request body for sending emails  */
    static class EmailRequest {
        @NotBlank(message = "Subject is required")
        private String subject;

        @NotBlank(message = "Content is required")
        private String content;

        // Getters and setters
        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
    @GetMapping("/api/admin/test/email-template")
    public ResponseEntity<ResponseDTO> testEmailTemplate(
            @RequestParam String toEmail,
            @RequestParam(required = false) String language) {

        try {
            User testUser = new User();
            testUser.setId(1L);
            testUser.setUsername("testuser");
            testUser.setEmail(toEmail);


            Outage testOutage = new Outage();
            testOutage.setId(1L);
            testOutage.setType(AppEnums.OutageType.ELECTRICITY);
            testOutage.setStatus(AppEnums.OutageStatus.SCHEDULED);
            testOutage.setStartTime(LocalDateTime.now().plusDays(1));
            testOutage.setEstimatedEndTime(LocalDateTime.now().plusDays(1).plusHours(4));
            testOutage.setReason("Scheduled maintenance");


            Area testArea = new Area();
            testArea.setName("Test Area");
            testOutage.setAffectedArea(testArea);


            boolean sent = emailService.sendOutageNotificationEmail(
                    testUser,
                    testOutage,
                    language != null ? language : "en");

            if (sent) {
                responseDTO.setCode(200);
                responseDTO.setMessage("Test template email sent successfully");
                responseDTO.setData(null);
                return ResponseEntity.ok(responseDTO);
            } else {
                responseDTO.setCode(500);
                responseDTO.setMessage("Failed to send test template email");
                responseDTO.setData(null);
                return ResponseEntity.status(500).body(responseDTO);
            }
        } catch (Exception e) {
            logger.error("Error sending test template email: {}", e.getMessage(), e);
            responseDTO.setCode(500);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return ResponseEntity.status(500).body(responseDTO);
        }
    }
}