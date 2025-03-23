package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.EmailService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);
    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    // In-memory store for password reset tokens
    // In a production environment, this would be stored in a database
    private final Map<String, PasswordResetToken> resetTokens = new HashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Request a password reset token
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseDTO> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            logger.info("Password reset requested for email: {}", request.getEmail());

            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                // Don't reveal that the email doesn't exist for security reasons
                logger.warn("Password reset requested for non-existent email: {}", request.getEmail());
                responseDTO.setCode(VarList.OK);
                responseDTO.setMessage("If your email is registered, you will receive password reset instructions.");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }

            User user = userOptional.get();

            // Generate a secure random token
            String token = generateSecureToken();

            // Store token with expiration time
            resetTokens.put(token, new PasswordResetToken(
                    user.getId(),
                    LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES)
            ));

            // Send password reset email
            sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("If your email is registered, you will receive password reset instructions.");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error in password reset request: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error processing password reset request");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate password reset token
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ResponseDTO> validateResetToken(@RequestParam String token) {
        try {
            logger.info("Validating password reset token");

            PasswordResetToken resetToken = resetTokens.get(token);

            if (resetToken == null || resetToken.isExpired()) {
                logger.warn("Invalid or expired password reset token");
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Invalid or expired password reset token");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Token is valid");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error validating reset token: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error validating token");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            logger.info("Processing password reset");

            // Validate token
            PasswordResetToken resetToken = resetTokens.get(request.getToken());

            if (resetToken == null || resetToken.isExpired()) {
                logger.warn("Invalid or expired password reset token");
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Invalid or expired password reset token");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            // Find user
            Optional<User> userOptional = userRepository.findById(resetToken.getUserId());

            if (userOptional.isEmpty()) {
                logger.warn("User not found for reset token");
                responseDTO.setCode(VarList.Not_Found);
                responseDTO.setMessage("User not found");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Remove used token
            resetTokens.remove(request.getToken());

            logger.info("Password reset successful for user ID: {}", user.getId());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Password has been reset successfully");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error in password reset: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error resetting password");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate a secure random token
     */
    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Send password reset email
     */
    private void sendPasswordResetEmail(String email, String username, String token) {
        String subject = "Reset your PowerAlert password";

        // Create the reset link
        String resetLink = "https://poweralert.lk/reset-password?token=" + token;

        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html lang=\"en\">");
        content.append("<head>");
        content.append("  <meta charset=\"UTF-8\">");
        content.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        content.append("  <title>Reset Your Password</title>");
        content.append("  <style>");
        content.append("    @import url('https://fonts.googleapis.com/css2?family=Google+Sans:wght@400;500;700&family=Roboto:wght@300;400;500&display=swap');");
        content.append("    body { font-family: 'Roboto', sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #202124; line-height: 1.6; }");
        content.append("    .container { max-width: 560px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08); }");
        content.append("    .header { padding: 24px 0; text-align: center; border-bottom: 1px solid #f1f3f4; }");
        content.append("    .header img { width: 32px; height: 32px; margin-bottom: 8px; }");
        content.append("    .content { padding: 40px 32px; }");
        content.append("    h1 { font-family: 'Google Sans', sans-serif; font-size: 22px; font-weight: 500; color: #202124; margin: 0 0 24px; }");
        content.append("    p { font-size: 14px; color: #3c4043; margin: 0 0 16px; }");
        content.append("    .button-container { text-align: center; margin: 32px 0; }");
        content.append("    .button { ");
        content.append("      display: inline-block;");
        content.append("      font-family: 'Google Sans', sans-serif;");
        content.append("      font-size: 14px;");
        content.append("      font-weight: 500;");
        content.append("      color: #ffffff;");
        content.append("      background: linear-gradient(135deg, #6e8efb 0%, #a777e3 100%);");
        content.append("      border-radius: 8px;");
        content.append("      padding: 12px 28px;");
        content.append("      text-decoration: none;");
        content.append("      text-align: center;");
        content.append("      box-shadow: 0 4px 12px rgba(110, 142, 251, 0.3);");
        content.append("      transition: all 0.3s ease;");
        content.append("    }");
        content.append("    .button:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(110, 142, 251, 0.4); }");
        content.append("    .link-text { font-size: 13px; color: #5f6368; margin-top: 16px; word-break: break-all; }");
        content.append("    .footer { padding: 24px 32px; font-size: 12px; color: #5f6368; border-top: 1px solid #f1f3f4; background-color: #fafafa; }");
        content.append("    .footer p { font-size: 12px; color: #5f6368; margin: 8px 0; }");
        content.append("  </style>");
        content.append("</head>");
        content.append("<body>");
        content.append("  <div class=\"container\">");
        content.append("    <div class=\"header\">");
        content.append("      <img src=\"https://i.pinimg.com/736x/28/23/9a/28239abf80f0482f4e052232465f30e8.jpg\" alt=\"PowerAlert Logo\" onerror=\"this.style.display='none'\">");
        content.append("      <div style=\"font-family: 'Google Sans', sans-serif; font-size: 16px; font-weight: 500; color: #6e8efb;\">PowerAlert</div>");
        content.append("    </div>");
        content.append("    <div class=\"content\">");
        content.append("      <h1>Reset your password</h1>");
        content.append("      <p>Hi ").append(username).append(",</p>");
        content.append("      <p>We received a request to reset the password for your PowerAlert account.</p>");
        content.append("      <div class=\"button-container\">");
        content.append("        <a href=\"").append(resetLink).append("\" class=\"button\">Reset password</a>");
        content.append("      </div>");
        content.append("      <p>This link will expire in 30 minutes.</p>");
        content.append("      <p>If you didn't request this change, you can safely ignore this email.</p>");
        content.append("      <p class=\"link-text\">Or copy and paste this URL into your browser:<br>").append(resetLink).append("</p>");
        content.append("    </div>");
        content.append("    <div class=\"footer\">");
        content.append("      <p>This email was sent to verify your identity. If you didn't request a password reset, you can safely ignore this message.</p>");
        content.append("      <p>&copy; 2025 PowerAlert. All rights reserved.</p>");
        content.append("    </div>");
        content.append("  </div>");
        content.append("</body>");
        content.append("</html>");

        try {
            emailService.sendEmail(email, subject, content.toString());
            logger.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            logger.error("Error sending password reset email to {}: {}", email, e.getMessage(), e);
            // Continue execution - don't let email failures stop the process
        }
    }

    /**
     * Password reset token class
     */
    private static class PasswordResetToken {
        private final Long userId;
        private final LocalDateTime expiryDate;

        public PasswordResetToken(Long userId, LocalDateTime expiryDate) {
            this.userId = userId;
            this.expiryDate = expiryDate;
        }

        public Long getUserId() {
            return userId;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryDate);
        }
    }

    /**
     * Request body for forgot password
     */
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * Request body for password reset
     */
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
                message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, one special character, and no whitespace")
        private String newPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}