package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.*;
import lk.ijse.poweralert.entity.*;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.event.NotificationEventPublisher;
import lk.ijse.poweralert.repository.*;
import lk.ijse.poweralert.service.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

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
    private OutageHistoryService outageHistoryService;

    @Autowired
    private NotificationEventPublisher eventPublisher;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private  SmsService smsService;

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

        // Update outage history for the new outage
        outageHistoryService.updateOutageHistory(savedOutage.getId());
        logger.info("Outage history updated for new outage ID: {}", savedOutage.getId());

        // Fetch and detach a fresh copy of the outage to prevent lazy loading issues
        Long outageId = savedOutage.getId();

        // Send notifications asynchronously in a separate transaction
        sendNotificationsAsync(outageId);

        // Map to DTO and return
        return convertToDTO(savedOutage);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationsAsync(Long outageId) {
        try {
            logger.info("Sending notifications asynchronously for outage ID: {}", outageId);

            // Fetch a fresh instance of the outage in this new transaction
            Outage outage = outageRepository.findById(outageId)
                    .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageId));

            // Send notifications via the notification service
            notificationService.sendOutageNotifications(outage);

            logger.info("Notifications sent successfully for outage ID: {}", outageId);
        } catch (Exception e) {
            logger.error("Error sending notifications for outage ID {}: {}", outageId, e.getMessage(), e);
            // Don't propagate the exception to prevent transaction rollback
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutageDTO> getAllActiveOutages() {
        logger.info("Fetching all active outages");
        List<Outage> activeOutages = outageRepository.findByStatusIn(
                List.of(OutageStatus.SCHEDULED, OutageStatus.ONGOING));

        return activeOutages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OutageDTO getOutageById(Long id) {
        logger.info("Fetching outage with ID: {}", id);
        Outage outage = outageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + id));

        return convertToDTO(outage);
    }

    @Override
    @Transactional(readOnly = true)
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
                // If status changed to COMPLETED, set actual end time
                if (outageCreateDTO.getStatus() == OutageStatus.COMPLETED) {
                    outage.setActualEndTime(LocalDateTime.now());
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

        // Update outage history
        outageHistoryService.updateOutageHistory(updatedOutage.getId());
        logger.info("Outage history updated for outage ID: {}", updatedOutage.getId());

        // Get ID for notification
        Long outageId = updatedOutage.getId();

        // Send notifications about the update asynchronously
        sendUpdateNotificationsAsync(outageId);

        // Map to DTO and return
        return convertToDTO(updatedOutage);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendUpdateNotificationsAsync(Long outageId) {
        try {
            logger.info("Sending update notifications asynchronously for outage ID: {}", outageId);

            // Fetch a fresh instance of the outage in this new transaction
            Outage outage = outageRepository.findById(outageId)
                    .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageId));

            // Send notifications via notification service
            notificationService.sendOutageUpdateNotifications(outage);

            // Find affected users
            List<User> affectedUsers = findAffectedUsers(outage);
            logger.info("Found {} affected users for outage update ID: {}", affectedUsers.size(), outageId);

            // Send email updates to affected users
            for (User user : affectedUsers) {
                try {
                    // Create the template model
                    Map<String, Object> templateModel = createOutageUpdateTemplateModel(outage, user);

                    // Send the update email
                    String subject = outage.getType() + " Outage Update - " + outage.getAffectedArea().getName();

                    CompletableFuture<Boolean> emailResult = emailService.sendTemplateEmail(
                            user.getEmail(),
                            subject,
                            "outage-update.ftl",
                            templateModel
                    );

                    // If template fails, create simple HTML
                    emailResult.exceptionally(ex -> {
                        logger.warn("Template email failed for user {}, using simplified format", user.getEmail(), ex);
                        // Create simplified HTML
                        String simpleContent = createSimpleUpdateEmail(outage, user);
                        emailService.sendEmail(user.getEmail(), subject, simpleContent);
                        return false;
                    });

                } catch (Exception e) {
                    logger.error("Error sending update email to user {}: {}", user.getEmail(), e.getMessage());
                }
            }

            logger.info("Update notifications sent successfully for outage ID: {}", outageId);
        } catch (Exception e) {
            logger.error("Error sending update notifications for outage ID {}: {}", outageId, e.getMessage(), e);
            // Don't propagate the exception to prevent transaction rollback
        }
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

            // If status changed to COMPLETED, set actual end time
            if (oldStatus != OutageStatus.COMPLETED && update.getNewStatus() == OutageStatus.COMPLETED) {
                outage.setActualEndTime(LocalDateTime.now());
            }

            outageUpdated = true;
        }

        if (outageUpdated) {
            outage.setUpdatedAt(LocalDateTime.now());
            outage = outageRepository.save(outage);
        }

        // Update outage history
        outageHistoryService.updateOutageHistory(outage.getId());
        logger.info("Outage history updated for outage ID: {}", outage.getId());

        // Get ID for notification
        Long outageId = outage.getId();

        // Trigger update notifications to affected users asynchronously
        sendUpdateNotificationsAsync(outageId);

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

        // Update outage history for the cancelled outage
        outageHistoryService.updateOutageHistory(cancelledOutage.getId());
        logger.info("Outage history updated for cancelled outage ID: {}", cancelledOutage.getId());

        // Get ID for notification
        Long outageId = cancelledOutage.getId();

        // Send notifications about cancellation asynchronously
        sendCancellationNotificationsAsync(outageId);

        // Map to DTO and return
        return convertToDTO(cancelledOutage);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendCancellationNotificationsAsync(Long outageId) {
        try {
            logger.info("Sending cancellation notifications asynchronously for outage ID: {}", outageId);

            // Fetch a fresh instance of the outage in this new transaction
            final Outage outage = outageRepository.findById(outageId)
                    .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageId));

            // Send notifications via notification service
            notificationService.sendOutageCancellationNotifications(outage);

            // Find affected users
            List<User> affectedUsers = findAffectedUsers(outage);
            logger.info("Found {} affected users for outage cancellation ID: {}", affectedUsers.size(), outageId);

            // Send notifications to affected users
            for (final User user : affectedUsers) {
                try {
                    // Email notification
                    Map<String, Object> templateModel = createOutageCancellationTemplateModel(outage, user);
                    String subject = outage.getType() + " Outage Cancellation - " + outage.getAffectedArea().getName();
                    CompletableFuture<Boolean> emailResult = emailService.sendTemplateEmail(
                            user.getEmail(),
                            subject,
                            "outage-cancellation.ftl",
                            templateModel
                    );
                    emailResult.exceptionally(ex -> {
                        logger.warn("Cancellation template email failed for user {}, using simplified format", user.getEmail(), ex);
                        String simpleContent = createSimpleCancellationEmail(outage, user);
                        emailService.sendEmail(user.getEmail(), subject, simpleContent);
                        return false;
                    });

                    // SMS notification
                    if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() &&
                            smsService.isValidPhoneNumber(user.getPhoneNumber())) {

                        final String cancellationReason = extractCancellationReason(outage);
                        final String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";

                        String[] smsParams = new String[] {
                                outage.getType().toString(),
                                outage.getAffectedArea().getName(),
                                outage.getStartTime().format(DATE_FORMATTER),
                                cancellationReason
                        };

                        smsService.sendTemplatedSms(user.getPhoneNumber(), "outage.cancelled", smsParams, language);
                    }

                    // WhatsApp notification
                    if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                        final String cancellationReason = extractCancellationReason(outage);
                        final String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";

                        String[] whatsappParams = new String[] {
                                user.getUsername(),
                                outage.getType().toString(),
                                outage.getAffectedArea().getName(),
                                outage.getStartTime().format(DATE_FORMATTER),
                                cancellationReason
                        };

                        whatsAppService.sendTemplateMessage(user.getPhoneNumber(), "outage_cancellation", whatsappParams, language);
                    }

                } catch (Exception e) {
                    logger.error("Error sending cancellation notifications to user {}: {}", user.getEmail(), e.getMessage());
                }
            }

            logger.info("Cancellation notifications sent successfully for outage ID: {}", outageId);
        } catch (Exception e) {
            logger.error("Error sending cancellation notifications for outage ID {}: {}", outageId, e.getMessage(), e);
            // Don't propagate the exception to prevent transaction rollback
        }
    }

    /**
     * Extract cancellation reason from the latest cancellation update
     */
    private String extractCancellationReason(Outage outage) {
        return outage.getUpdates().stream()
                .filter(u -> u.getNewStatus() == OutageStatus.CANCELLED)
                .max(Comparator.comparing(OutageUpdate::getCreatedAt))
                .map(OutageUpdate::getUpdateInfo)
                .filter(info -> info != null && !info.isEmpty())
                .orElse("Administrative decision");
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
     * Helper method to find users affected by an outage
     *
     * @param outage The outage
     * @return List of affected users
     */
    private List<User> findAffectedUsers(Outage outage) {
        // For now, simple implementation that gets all users with addresses in the affected area's district
        return userRepository.findUsersInDistrict(outage.getAffectedArea().getDistrict());
    }

    /**
     * Create model for the new outage template
     */
    private Map<String, Object> createOutageTemplateModel(Outage outage, User user) {
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("outageType", outage.getType().toString());
        model.put("areaName", outage.getAffectedArea().getName());
        model.put("status", outage.getStatus().toString());
        model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

        if (outage.getEstimatedEndTime() != null) {
            model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            model.put("reason", outage.getReason());
        } else {
            model.put("reason", "Scheduled maintenance");
        }

        if (outage.getAdditionalInfo() != null && !outage.getAdditionalInfo().isEmpty()) {
            model.put("additionalInfo", outage.getAdditionalInfo());
        }

        // Add portal URL for viewing details
        model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());

        return model;
    }

    /**
     * Create model for the outage update template
     */
    private Map<String, Object> createOutageUpdateTemplateModel(Outage outage, User user) {
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("outageType", outage.getType().toString());
        model.put("areaName", outage.getAffectedArea().getName());
        model.put("status", outage.getStatus().toString());
        model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));
        model.put("updatedAt", outage.getUpdatedAt().format(DATE_FORMATTER));

        if (outage.getEstimatedEndTime() != null) {
            model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            model.put("reason", outage.getReason());
        }

        // Get latest update info
        outage.getUpdates().stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .ifPresent(update -> {
                    model.put("updateInfo", update.getUpdateInfo());
                    if (update.getReason() != null && !update.getReason().isEmpty()) {
                        model.put("updateReason", update.getReason());
                    }
                });

        // Add portal URL for viewing details
        model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());

        return model;
    }

    /**
     * Create model for outage cancellation template
     */
    private Map<String, Object> createOutageCancellationTemplateModel(Outage outage, User user) {
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("outageType", outage.getType().toString());
        model.put("areaName", outage.getAffectedArea().getName());
        model.put("status", outage.getStatus().toString());
        model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));
        model.put("cancelledAt", outage.getUpdatedAt().format(DATE_FORMATTER));

        // Get cancellation reason
        outage.getUpdates().stream()
                .filter(u -> u.getNewStatus() == OutageStatus.CANCELLED)
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .ifPresent(update -> {
                    if (update.getUpdateInfo() != null && !update.getUpdateInfo().isEmpty()) {
                        model.put("cancellationReason", update.getUpdateInfo());
                    }
                });

        return model;
    }

    /**
     * Create simple HTML email for outage update when template fails
     */
    private String createSimpleUpdateEmail(Outage outage, User user) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body>");
        html.append("<h2>").append(outage.getType()).append(" Outage Update</h2>");
        html.append("<p>Hello ").append(user.getUsername()).append(",</p>");
        html.append("<p>We have an update regarding the ").append(outage.getType())
                .append(" outage affecting ").append(outage.getAffectedArea().getName()).append("</p>");

        html.append("<p><strong>Status:</strong> ").append(outage.getStatus()).append("</p>");

        if (outage.getEstimatedEndTime() != null) {
            html.append("<p><strong>Estimated End Time:</strong> ")
                    .append(outage.getEstimatedEndTime().format(DATE_FORMATTER)).append("</p>");
        }

        // Get update information
        outage.getUpdates().stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .ifPresent(update -> {
                    html.append("<p><strong>Update:</strong> ").append(update.getUpdateInfo()).append("</p>");
                });

        html.append("<p>You can view more details at <a href=\"https://poweralert.lk/outages/")
                .append(outage.getId()).append("\">our portal</a>.</p>");

        html.append("<p>Thank you for your patience.</p>");
        html.append("<p>Power Alert System</p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Create simple HTML email for outage cancellation when template fails
     */
    private String createSimpleCancellationEmail(Outage outage, User user) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body>");
        html.append("<h2>").append(outage.getType()).append(" Outage Cancellation</h2>");
        html.append("<p>Hello ").append(user.getUsername()).append(",</p>");
        html.append("<p>The previously scheduled ").append(outage.getType())
                .append(" outage for ").append(outage.getAffectedArea().getName())
                .append(" has been <strong>cancelled</strong>.</p>");

        html.append("<p><strong>Original Start Time:</strong> ")
                .append(outage.getStartTime().format(DATE_FORMATTER)).append("</p>");

        // Get cancellation reason if available
        outage.getUpdates().stream()
                .filter(u -> u.getNewStatus() == OutageStatus.CANCELLED)
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .ifPresent(update -> {
                    if (update.getUpdateInfo() != null && !update.getUpdateInfo().isEmpty()) {
                        html.append("<p><strong>Reason for Cancellation:</strong> ")
                                .append(update.getUpdateInfo()).append("</p>");
                    }
                });

        html.append("<p>You may continue to use the services as normal.</p>");
        html.append("<p>Thank you for your understanding.</p>");
        html.append("<p>Power Alert System</p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Convert Outage entity to OutageDTO
     * @param outage the outage entity
     * @return the outage DTO
     */
    private OutageDTO convertToDTO(Outage outage) {
        if (outage == null) {
            return null;
        }

        // Using ModelMapper for basic mapping
        OutageDTO outageDTO = modelMapper.map(outage, OutageDTO.class);

        // If any specific mappings need manual handling, add them here
        // For example, if nested objects need explicit mapping:
        if (outage.getAffectedArea() != null) {
            outageDTO.setAffectedArea(modelMapper.map(outage.getAffectedArea(), AreaDTO.class));
        }

        if (outage.getUtilityProvider() != null) {
            outageDTO.setUtilityProvider(modelMapper.map(outage.getUtilityProvider(), UtilityProviderDTO.class));
        }

        // Map updates if needed
        if (outage.getUpdates() != null) {
            outageDTO.setUpdates(
                    outage.getUpdates().stream()
                            .map(update -> modelMapper.map(update, OutageUpdateDTO.class))
                            .collect(Collectors.toList())
            );
        }

        return outageDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutageDTO> getAllOutages() {
        logger.info("Fetching all outages regardless of status");
        List<Outage> allOutages = outageRepository.findAll();

        return allOutages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

}