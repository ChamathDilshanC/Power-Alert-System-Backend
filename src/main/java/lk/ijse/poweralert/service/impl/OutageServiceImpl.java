package lk.ijse.poweralert.service.impl;

import jakarta.mail.internet.MimeMessage;
import lk.ijse.poweralert.dto.OutageCreateDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.OutageUpdateDTO;
import lk.ijse.poweralert.entity.*;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.repository.*;
import lk.ijse.poweralert.service.EmailService;
import lk.ijse.poweralert.service.NotificationService;
import lk.ijse.poweralert.service.OutageService;
import lk.ijse.poweralert.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OutageServiceImpl implements OutageService {

    private static final Logger logger = LoggerFactory.getLogger(OutageServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OutageRepository outageRepository;
    private final OutageUpdateRepository outageUpdateRepository;
    private final OutageHistoryRepository outageHistoryRepository;
    private final AreaRepository areaRepository;
    private final UtilityProviderRepository utilityProviderRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    public OutageServiceImpl(
            OutageRepository outageRepository,
            OutageUpdateRepository outageUpdateRepository,
            OutageHistoryRepository outageHistoryRepository,
            AreaRepository areaRepository,
            UtilityProviderRepository utilityProviderRepository,
            UserService userService,
            NotificationService notificationService,
            ModelMapper modelMapper) {
        this.outageRepository = outageRepository;
        this.outageUpdateRepository = outageUpdateRepository;
        this.outageHistoryRepository = outageHistoryRepository;
        this.areaRepository = areaRepository;
        this.utilityProviderRepository = utilityProviderRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public OutageDTO createOutage(OutageCreateDTO outageCreateDTO) {
        logger.info("Creating new outage of type: {}", outageCreateDTO.getType());

        // Fetch area entity
        Area area = areaRepository.findById(outageCreateDTO.getAreaId())
                .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + outageCreateDTO.getAreaId()));

        // Fetch utility provider entity
        UtilityProvider utilityProvider = utilityProviderRepository.findById(outageCreateDTO.getUtilityProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + outageCreateDTO.getUtilityProviderId()));

        // Create outage entity
        Outage outage = new Outage();
        outage.setType(outageCreateDTO.getType());
        outage.setStatus(outageCreateDTO.getStatus());
        outage.setStartTime(outageCreateDTO.getStartTime());
        outage.setEstimatedEndTime(outageCreateDTO.getEstimatedEndTime());
        outage.setAffectedArea(area);
        outage.setGeographicalAreaJson(outageCreateDTO.getGeographicalAreaJson());
        outage.setReason(outageCreateDTO.getReason());
        outage.setAdditionalInfo(outageCreateDTO.getAdditionalInfo());
        outage.setUtilityProvider(utilityProvider);
        outage.setCreatedAt(LocalDateTime.now());
        outage.setUpdatedAt(LocalDateTime.now());

        // Save outage
        Outage savedOutage = outageRepository.save(outage);
        logger.info("Outage created with ID: {}", savedOutage.getId());

        // Create initial outage update record
        OutageUpdate initialUpdate = new OutageUpdate();
        initialUpdate.setOutage(savedOutage);
        initialUpdate.setUpdateInfo("Initial outage creation");
        initialUpdate.setNewStatus(savedOutage.getStatus());
        initialUpdate.setUpdatedEstimatedEndTime(savedOutage.getEstimatedEndTime());
        initialUpdate.setReason(savedOutage.getReason());
        initialUpdate.setCreatedAt(LocalDateTime.now());
        outageUpdateRepository.save(initialUpdate);
        logger.info("Initial outage update recorded");

        // Send notifications to affected users
        sendOutageNotificationsToAffectedUsers(savedOutage);

        // Map to DTO and return
        return convertToDTO(savedOutage);
    }

    /**
     * Send notifications to users affected by an outage
     */
    private void sendOutageNotificationsToAffectedUsers(Outage outage) {
        try {
            // Verify email server connection first (optional)
            boolean emailServerConnected = emailService.testEmailConnection();
            if (!emailServerConnected) {
                logger.warn("Email server connection test failed, but will attempt to send emails anyway");
            }

            // Get affected users in the area
            List<User> affectedUsers = userRepository.findUsersByAreaId(outage.getAffectedArea().getId());
            logger.info("Found {} affected users for outage ID: {}", affectedUsers.size(), outage.getId());

            if (affectedUsers.isEmpty()) {
                logger.warn("No affected users found for outage in area ID: {}", outage.getAffectedArea().getId());
                return;
            }

            int emailsSent = 0;
            int emailsFailed = 0;

            // Send notification emails using template
            for (User user : affectedUsers) {
                try {
                    if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                        logger.warn("Skipping notification for user with null or empty email");
                        continue;
                    }

                    logger.info("Sending outage notification to user: {} ({})", user.getUsername(), user.getEmail());

                    // Use the FreeMarker template to send email
                    boolean sent = emailService.sendOutageNotificationEmail(user, outage,
                            user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en");

                    if (sent) {
                        logger.info("Outage notification email sent successfully to: {}", user.getEmail());
                        emailsSent++;
                    } else {
                        logger.warn("Failed to send notification email to: {}", user.getEmail());
                        emailsFailed++;
                    }
                } catch (Exception e) {
                    emailsFailed++;
                    logger.error("Error sending notification email to user {}: {}",
                            user != null ? user.getEmail() : "null", e.getMessage(), e);
                }
            }

            logger.info("Email sending summary: {} sent successfully, {} failed", emailsSent, emailsFailed);

            // Also notify through other channels (SMS, push notifications, etc.)
            try {
                notificationService.sendOutageNotifications(outage);
                logger.info("Notification service completed successfully for outage ID: {}", outage.getId());
            } catch (Exception e) {
                logger.error("Error in notification service for outage ID {}: {}", outage.getId(), e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("Error in main notification process: {}", e.getMessage(), e);
            // Continue even if notifications fail - we don't want to roll back the outage creation
        }
    }

    /**
     * Create simple email content for outage notification
     */
    private String createSimpleEmailContent(User user, Outage outage) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><body>");
        content.append("<h2 style='color:#0066cc;'>Outage Notification</h2>");
        content.append("<p>Dear ").append(user.getUsername()).append(",</p>");
        content.append("<p>We are writing to inform you about a <strong>").append(outage.getType())
                .append("</strong> outage in ").append(outage.getAffectedArea().getName()).append(".</p>");

        content.append("<div style='background-color:#f8f8f8; border-left:4px solid #0066cc; padding:15px; margin:15px 0;'>");
        content.append("<p><strong>Start Time:</strong> ").append(outage.getStartTime().format(DATE_FORMATTER)).append("</p>");

        if (outage.getEstimatedEndTime() != null) {
            content.append("<p><strong>Estimated End Time:</strong> ")
                    .append(outage.getEstimatedEndTime().format(DATE_FORMATTER)).append("</p>");
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            content.append("<p><strong>Reason:</strong> ").append(outage.getReason()).append("</p>");
        }
        content.append("</div>");

        content.append("<p>Thank you for your understanding.</p>");
        content.append("<p>PowerAlert Team</p>");
        content.append("</body></html>");

        return content.toString();
    }

    /**
     * Send email with retry mechanism
     */
    private boolean sendEmailWithRetry(String to, String subject, String content, int maxRetries) {
        int retries = 0;
        boolean sent = false;
        Exception lastException = null;

        while (!sent && retries < maxRetries) {
            try {
                sent = emailService.sendEmail(to, subject, content);
                if (sent) {
                    return true;
                }
                retries++;
                if (retries < maxRetries) {
                    logger.info("Retrying email send to {} (attempt {}/{})", to, retries + 1, maxRetries);
                    Thread.sleep(1000); // Wait 1 second before retry
                }
            } catch (Exception e) {
                lastException = e;
                retries++;
                logger.warn("Email send attempt {} failed: {}", retries, e.getMessage());
                try {
                    Thread.sleep(1000); // Wait 1 second before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (lastException != null) {
            logger.error("All email send attempts failed to {}: {}", to, lastException.getMessage(), lastException);
        }

        return sent;
    }

    @Override
    public List<OutageDTO> getAllActiveOutages() {
        logger.info("Fetching all active outages");
        List<Outage> activeOutages = outageRepository.findByStatusIn(
                List.of(OutageStatus.SCHEDULED, OutageStatus.ONGOING));

        return activeOutages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OutageDTO getOutageById(Long id) {
        logger.info("Fetching outage with ID: {}", id);
        Outage outage = outageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + id));

        return convertToDTO(outage);
    }

    @Override
    public List<OutageDTO> getOutagesByArea(Long areaId) {
        logger.info("Fetching outages for area with ID: {}", areaId);

        // Verify area exists
        if (!areaRepository.existsById(areaId)) {
            throw new EntityNotFoundException("Area not found with ID: " + areaId);
        }

        List<Outage> outages = outageRepository.findByAffectedAreaIdOrderByStartTimeDesc(areaId);

        return outages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OutageDTO updateOutage(Long id, OutageCreateDTO outageCreateDTO) {
        logger.info("Updating outage with ID: {}", id);

        // Fetch existing outage
        Outage outage = outageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + id));

        // Only allow updates if outage is not completed or cancelled
        if (outage.getStatus() == OutageStatus.COMPLETED || outage.getStatus() == OutageStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update outage with status: " + outage.getStatus());
        }

        // Store original values for comparison
        OutageStatus originalStatus = outage.getStatus();
        LocalDateTime originalEstEndTime = outage.getEstimatedEndTime();
        String originalReason = outage.getReason();
        boolean statusChanged = originalStatus != outageCreateDTO.getStatus();
        boolean endTimeChanged = !originalEstEndTime.equals(outageCreateDTO.getEstimatedEndTime());
        boolean reasonChanged = originalReason != null && !originalReason.equals(outageCreateDTO.getReason());

        // Fetch area entity if changed
        if (!outage.getAffectedArea().getId().equals(outageCreateDTO.getAreaId())) {
            Area area = areaRepository.findById(outageCreateDTO.getAreaId())
                    .orElseThrow(() -> new EntityNotFoundException("Area not found with ID: " + outageCreateDTO.getAreaId()));
            outage.setAffectedArea(area);
        }

        // Fetch utility provider entity if changed
        if (!outage.getUtilityProvider().getId().equals(outageCreateDTO.getUtilityProviderId())) {
            UtilityProvider utilityProvider = utilityProviderRepository.findById(outageCreateDTO.getUtilityProviderId())
                    .orElseThrow(() -> new EntityNotFoundException("Utility provider not found with ID: " + outageCreateDTO.getUtilityProviderId()));
            outage.setUtilityProvider(utilityProvider);
        }

        // Update fields
        outage.setType(outageCreateDTO.getType());
        outage.setStatus(outageCreateDTO.getStatus());
        outage.setStartTime(outageCreateDTO.getStartTime());
        outage.setEstimatedEndTime(outageCreateDTO.getEstimatedEndTime());
        outage.setGeographicalAreaJson(outageCreateDTO.getGeographicalAreaJson());
        outage.setReason(outageCreateDTO.getReason());
        outage.setAdditionalInfo(outageCreateDTO.getAdditionalInfo());
        outage.setUpdatedAt(LocalDateTime.now());

        // Create outage update record if significant changes were made
        if (statusChanged || endTimeChanged || reasonChanged) {
            OutageUpdate update = new OutageUpdate();
            update.setOutage(outage);
            update.setUpdateInfo("Outage details updated");

            if (statusChanged) {
                update.setNewStatus(outageCreateDTO.getStatus());
                // If status changed to COMPLETED, set actual end time and update history
                if (outageCreateDTO.getStatus() == OutageStatus.COMPLETED) {
                    outage.setActualEndTime(LocalDateTime.now());
                    updateOutageHistory(outage);
                }
            }

            if (endTimeChanged) {
                update.setUpdatedEstimatedEndTime(outageCreateDTO.getEstimatedEndTime());
            }

            if (reasonChanged) {
                update.setReason(outageCreateDTO.getReason());
            }

            update.setCreatedAt(LocalDateTime.now());
            outageUpdateRepository.save(update);
            logger.info("Outage update recorded for outage ID: {}", outage.getId());
        }

        // Save updated outage
        Outage updatedOutage = outageRepository.save(outage);
        logger.info("Outage updated with ID: {}", updatedOutage.getId());

        // Send notifications about the update
        try {
            notificationService.sendOutageUpdateNotifications(updatedOutage);

            // Send individual email updates to affected users
            List<User> affectedUsers = userRepository.findUsersByAreaId(updatedOutage.getAffectedArea().getId());
            for (User user : affectedUsers) {
                try {
                    if (user != null && user.getEmail() != null) {
                        // Create update email subject and content
                        String subject = "Power Alert: Outage Update for " + updatedOutage.getAffectedArea().getName();
                        String content = createUpdateEmailContent(updatedOutage, user);

                        // Send direct email update using SendGrid
                        boolean emailSent = emailService.sendEmail(user.getEmail(), subject, content);
                        if (emailSent) {
                            logger.info("Outage update email sent successfully to user: {}", user.getEmail());
                        } else {
                            logger.warn("Failed to send outage update email to user: {}", user.getEmail());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error sending outage update email to user {}: {}", user.getEmail(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending outage update notifications: {}", e.getMessage(), e);
            // We continue even if notifications fail
        }

        // Map to DTO and return
        return convertToDTO(updatedOutage);
    }

    @Override
    @Transactional
    public OutageDTO addOutageUpdate(OutageUpdateDTO outageUpdateDTO) {
        logger.info("Adding update to outage with ID: {}", outageUpdateDTO.getOutageId());

        // Fetch existing outage
        Outage outage = outageRepository.findById(outageUpdateDTO.getOutageId())
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageUpdateDTO.getOutageId()));

        // Only allow updates if outage is not completed or cancelled
        if (outage.getStatus() == OutageStatus.COMPLETED || outage.getStatus() == OutageStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update outage with status: " + outage.getStatus());
        }

        // Create outage update
        OutageUpdate update = new OutageUpdate();
        update.setOutage(outage);
        update.setUpdateInfo(outageUpdateDTO.getUpdateInfo());
        update.setUpdatedEstimatedEndTime(outageUpdateDTO.getUpdatedEstimatedEndTime());
        update.setReason(outageUpdateDTO.getReason());
        update.setNewStatus(outageUpdateDTO.getNewStatus());
        update.setCreatedAt(LocalDateTime.now());

        // Save update
        OutageUpdate savedUpdate = outageUpdateRepository.save(update);
        logger.info("Outage update created with ID: {}", savedUpdate.getId());

        // Update outage fields if needed
        boolean outageUpdated = false;

        if (update.getUpdatedEstimatedEndTime() != null) {
            outage.setEstimatedEndTime(update.getUpdatedEstimatedEndTime());
            outageUpdated = true;
        }

        if (update.getNewStatus() != null) {
            OutageStatus oldStatus = outage.getStatus();
            outage.setStatus(update.getNewStatus());

            // If status changed to COMPLETED, set actual end time and update history
            if (oldStatus != OutageStatus.COMPLETED && update.getNewStatus() == OutageStatus.COMPLETED) {
                outage.setActualEndTime(LocalDateTime.now());
                updateOutageHistory(outage);
            }

            outageUpdated = true;
        }

        if (outageUpdated) {
            outage.setUpdatedAt(LocalDateTime.now());
            outage = outageRepository.save(outage);
        }

        // Trigger update notifications to affected users
        try {
            notificationService.sendOutageUpdateNotifications(outage);

            // Send direct notification emails about the update
            List<User> affectedUsers = userRepository.findUsersByAreaId(outage.getAffectedArea().getId());
            for (User user : affectedUsers) {
                if (user != null && user.getEmail() != null) {
                    // Create update email content
                    String subject = "Power Alert: Important Update for " + outage.getType() + " Outage";
                    String content = "<html><body>";
                    content += "<h2>Outage Update</h2>";
                    content += "<p>Dear " + user.getUsername() + ",</p>";
                    content += "<p>There has been an update to the " + outage.getType() + " outage in " + outage.getAffectedArea().getName() + ".</p>";
                    content += "<p><strong>Update:</strong> " + update.getUpdateInfo() + "</p>";

                    if (update.getNewStatus() != null) {
                        content += "<p><strong>New Status:</strong> " + update.getNewStatus() + "</p>";
                    }

                    if (update.getUpdatedEstimatedEndTime() != null) {
                        content += "<p><strong>Updated Estimated End Time:</strong> " +
                                update.getUpdatedEstimatedEndTime().format(DATE_FORMATTER) + "</p>";
                    }

                    if (update.getReason() != null && !update.getReason().isEmpty()) {
                        content += "<p><strong>Reason:</strong> " + update.getReason() + "</p>";
                    }

                    content += "<p>For more details, please visit our portal at <a href='https://poweralert.lk/outages/" +
                            outage.getId() + "'>PowerAlert</a>.</p>";
                    content += "<p>Thank you for your patience.</p>";
                    content += "<p>PowerAlert Team</p>";
                    content += "</body></html>";

                    // Send email using SendGrid
                    emailService.sendEmail(user.getEmail(), subject, content);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending outage update notifications: {}", e.getMessage(), e);
            // We continue even if notifications fail
        }

        // Map to DTO and return
        return convertToDTO(outage);
    }

    @Override
    @Transactional
    public OutageDTO cancelOutage(Long id) {
        logger.info("Cancelling outage with ID: {}", id);

        // Fetch existing outage
        Outage outage = outageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + id));

        // Only allow cancellation if outage is not completed
        if (outage.getStatus() == OutageStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed outage");
        }

        // Update status to CANCELLED
        outage.setStatus(OutageStatus.CANCELLED);
        outage.setUpdatedAt(LocalDateTime.now());

        // Save updated outage
        Outage cancelledOutage = outageRepository.save(outage);
        logger.info("Outage cancelled with ID: {}", cancelledOutage.getId());

        // Create cancellation update
        OutageUpdate update = new OutageUpdate();
        update.setOutage(outage);
        update.setUpdateInfo("Outage cancelled by administrator");
        update.setNewStatus(OutageStatus.CANCELLED);
        update.setCreatedAt(LocalDateTime.now());
        outageUpdateRepository.save(update);
        logger.info("Cancellation record created for outage ID: {}", outage.getId());

        // Send notifications about cancellation
        try {
            notificationService.sendOutageCancellationNotifications(cancelledOutage);

            // Send individual cancellation emails to affected users
            List<User> affectedUsers = userRepository.findUsersByAreaId(cancelledOutage.getAffectedArea().getId());
            for (User user : affectedUsers) {
                try {
                    if (user != null && user.getEmail() != null) {
                        // Create cancellation email content
                        String subject = "Power Alert: Outage Cancellation Notice";
                        String content = "<html><body>";
                        content += "<h2>Outage Cancellation Notice</h2>";
                        content += "<p>Dear " + user.getUsername() + ",</p>";
                        content += "<p>We are pleased to inform you that the scheduled " + cancelledOutage.getType() +
                                " outage in " + cancelledOutage.getAffectedArea().getName() + " has been <strong>cancelled</strong>.</p>";
                        content += "<p>The outage that was scheduled for " +
                                cancelledOutage.getStartTime().format(DATE_FORMATTER) + " will no longer take place.</p>";
                        content += "<p>We apologize for any inconvenience this may have caused.</p>";
                        content += "<p>Thank you for your understanding.</p>";
                        content += "<p>PowerAlert Team</p>";
                        content += "</body></html>";

                        // Send email using SendGrid
                        emailService.sendEmail(user.getEmail(), subject, content);
                    }
                } catch (Exception e) {
                    logger.error("Error sending cancellation email to user {}: {}", user.getEmail(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending outage cancellation notifications: {}", e.getMessage(), e);
            // We continue even if notifications fail
        }

        // Map to DTO and return
        return convertToDTO(cancelledOutage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutageDTO> getOutagesForCurrentUser() {
        logger.info("Fetching outages for current user");

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserEntityByUsername(username);

        // Get user's addresses
        List<Address> addresses = user.getAddresses();

        if (addresses.isEmpty()) {
            logger.warn("User has no registered addresses");
            return List.of();
        }

        // Get outages for user's areas based on address locations
        List<Outage> outages = outageRepository.findOutagesForAddresses(
                addresses,
                List.of(OutageStatus.SCHEDULED, OutageStatus.ONGOING)
        );

        return outages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to create update email content
     */
    private String createUpdateEmailContent(Outage outage, User user) {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><body>");
        content.append("<h2>Outage Update</h2>");
        content.append("<p>Dear ").append(user.getUsername()).append(",</p>");
        content.append("<p>There has been an update to the ").append(outage.getType())
                .append(" outage in ").append(outage.getAffectedArea().getName()).append(".</p>");

        content.append("<p><strong>Current Status:</strong> ").append(outage.getStatus()).append("</p>");
        content.append("<p><strong>Start Time:</strong> ").append(outage.getStartTime().format(DATE_FORMATTER)).append("</p>");

        if (outage.getEstimatedEndTime() != null) {
            content.append("<p><strong>Estimated End Time:</strong> ")
                    .append(outage.getEstimatedEndTime().format(DATE_FORMATTER)).append("</p>");
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            content.append("<p><strong>Reason:</strong> ").append(outage.getReason()).append("</p>");
        }

        content.append("<p>For more details, please visit our portal at <a href='https://poweralert.lk/outages/")
                .append(outage.getId()).append("'>PowerAlert</a>.</p>");
        content.append("<p>Thank you for your patience.</p>");
        content.append("<p>PowerAlert Team</p>");
        content.append("</body></html>");

        return content.toString();
    }

    /**
     * Update the OutageHistory records when an outage is completed
     * @param outage the completed outage
     */
    private void updateOutageHistory(Outage outage) {
        logger.info("Updating outage history for completed outage ID: {}", outage.getId());
        try {
            // Calculate outage duration
            LocalDateTime startTime = outage.getStartTime();
            LocalDateTime endTime = outage.getActualEndTime() != null ?
                    outage.getActualEndTime() : LocalDateTime.now();

            // Get month and year from start time
            int year = startTime.getYear();
            int month = startTime.getMonthValue();

            // Find existing history record or create new one
            Optional<OutageHistory> historyOptional = outageHistoryRepository
                    .findByAreaIdAndTypeAndYearAndMonth(
                            outage.getAffectedArea().getId(),
                            outage.getType(),
                            year,
                            month);

            OutageHistory history;
            if (historyOptional.isPresent()) {
                history = historyOptional.get();
            } else {
                history = new OutageHistory();
                history.setArea(outage.getAffectedArea());
                history.setType(outage.getType());
                history.setYear(year);
                history.setMonth(month);
                history.setOutageCount(0);
                history.setTotalOutageHours(0);
                history.setAverageRestorationTime(0);
            }

            // Calculate duration in hours
            double hours = calculateHoursBetween(startTime, endTime);

            // Update the history record
            history.setOutageCount(history.getOutageCount() + 1);
            history.setTotalOutageHours(history.getTotalOutageHours() + hours);

            // Calculate average restoration time
            if (history.getOutageCount() > 0) {
                history.setAverageRestorationTime(
                        history.getTotalOutageHours() / history.getOutageCount());
            }

            // Save the history record
            outageHistoryRepository.save(history);
            logger.info("Updated outage history for area: {}, month: {}, year: {}",
                    outage.getAffectedArea().getName(), month, year);

        } catch (Exception e) {
            logger.error("Error updating outage history: {}", e.getMessage(), e);
            // Continue even if history update fails
        }
    }

    /**
     * Calculate hours between two LocalDateTime objects
     * @param start the start time
     * @param end the end time
     * @return the duration in hours
     */
    private double calculateHoursBetween(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        return duration.toSeconds() / 3600.0; // Convert seconds to hours
    }

    /**
     * Convert Outage entity to OutageDTO
     * @param outage the outage entity
     * @return the outage DTO
     */
    private OutageDTO convertToDTO(Outage outage) {
        return modelMapper.map(outage, OutageDTO.class);
    }
}