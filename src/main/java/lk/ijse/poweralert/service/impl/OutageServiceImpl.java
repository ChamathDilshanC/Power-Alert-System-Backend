package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.OutageCreateDTO;
import lk.ijse.poweralert.dto.OutageDTO;
import lk.ijse.poweralert.dto.OutageUpdateDTO;
import lk.ijse.poweralert.entity.*;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.repository.AreaRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.repository.OutageUpdateRepository;
import lk.ijse.poweralert.repository.UtilityProviderRepository;
import lk.ijse.poweralert.service.NotificationService;
import lk.ijse.poweralert.service.OutageService;
import lk.ijse.poweralert.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OutageServiceImpl implements OutageService {

    private static final Logger logger = LoggerFactory.getLogger(OutageServiceImpl.class);

    private final OutageRepository outageRepository;
    private final OutageUpdateRepository outageUpdateRepository;
    private final AreaRepository areaRepository;
    private final UtilityProviderRepository utilityProviderRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @Autowired
    public OutageServiceImpl(
            OutageRepository outageRepository,
            OutageUpdateRepository outageUpdateRepository,
            AreaRepository areaRepository,
            UtilityProviderRepository utilityProviderRepository,
            UserService userService,
            NotificationService notificationService,
            ModelMapper modelMapper) {
        this.outageRepository = outageRepository;
        this.outageUpdateRepository = outageUpdateRepository;
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

        // Trigger notifications to affected users
        try {
            notificationService.sendOutageNotifications(savedOutage);
        } catch (Exception e) {
            logger.error("Error sending outage notifications: {}", e.getMessage(), e);
            // We continue even if notifications fail
        }

        // Map to DTO and return
        return convertToDTO(savedOutage);
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

        // Save updated outage
        Outage updatedOutage = outageRepository.save(outage);
        logger.info("Outage updated with ID: {}", updatedOutage.getId());

        // Trigger update notifications to affected users
        try {
            notificationService.sendOutageUpdateNotifications(updatedOutage);
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
            outage.setStatus(update.getNewStatus());

            // If status changed to COMPLETED, set actual end time
            if (update.getNewStatus() == OutageStatus.COMPLETED) {
                outage.setActualEndTime(LocalDateTime.now());
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

        // Trigger cancellation notifications to affected users
        try {
            notificationService.sendOutageCancellationNotifications(cancelledOutage);
        } catch (Exception e) {
            logger.error("Error sending outage cancellation notifications: {}", e.getMessage(), e);
            // We continue even if notifications fail
        }

        // Map to DTO and return
        return convertToDTO(cancelledOutage);
    }

    @Override
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