package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.OutageCreateDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.OutageUpdateDTO;
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
import java.util.List;
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
    private OutageHistoryService outageHistoryService;

    @Autowired
    private NotificationEventPublisher eventPublisher;

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

            notificationService.sendOutageUpdateNotifications(outage);
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
            Outage outage = outageRepository.findById(outageId)
                    .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageId));

            notificationService.sendOutageCancellationNotifications(outage);
            logger.info("Cancellation notifications sent successfully for outage ID: {}", outageId);
        } catch (Exception e) {
            logger.error("Error sending cancellation notifications for outage ID {}: {}", outageId, e.getMessage(), e);
            // Don't propagate the exception to prevent transaction rollback
        }
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
     * Convert Outage entity to OutageDTO
     * @param outage the outage entity
     * @return the outage DTO
     */
    private OutageDTO convertToDTO(Outage outage) {
        return modelMapper.map(outage, OutageDTO.class);
    }
}