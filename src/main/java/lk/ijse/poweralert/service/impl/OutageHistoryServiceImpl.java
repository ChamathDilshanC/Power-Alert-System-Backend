package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.OutageHistoryDTO;
import lk.ijse.poweralert.entity.Area;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.OutageHistory;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lk.ijse.poweralert.repository.AreaRepository;
import lk.ijse.poweralert.repository.OutageHistoryRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.service.OutageHistoryService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutageHistoryServiceImpl implements OutageHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(OutageHistoryServiceImpl.class);

    @Autowired
    private OutageHistoryRepository outageHistoryRepository;

    @Autowired
    private OutageRepository outageRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<OutageHistoryDTO> getAllOutageHistory(Integer year, Integer month) {
        logger.info("Fetching all outage history with filters - year: {}, month: {}", year, month);

        List<OutageHistory> historyList;

        if (year != null && month != null) {
            historyList = outageHistoryRepository.findByYearAndMonth(year, month);
        } else if (year != null) {
            historyList = outageHistoryRepository.findByYear(year);
        } else {
            historyList = outageHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "year", "month"));
        }

        return historyList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutageHistoryDTO> getOutageHistoryByArea(Long areaId, Integer year, Integer month) {
        logger.info("Fetching outage history for area ID: {} with filters - year: {}, month: {}", areaId, year, month);

        // Verify area exists
        if (!areaRepository.existsById(areaId)) {
            throw new EntityNotFoundException("Area not found with ID: " + areaId);
        }

        List<OutageHistory> historyList;

        if (year != null && month != null) {
            historyList = outageHistoryRepository.findByAreaIdAndYearAndMonth(areaId, year, month);
        } else if (year != null) {
            historyList = outageHistoryRepository.findByAreaIdAndYear(areaId, year);
        } else {
            historyList = outageHistoryRepository.findByAreaId(areaId, Sort.by(Sort.Direction.DESC, "year", "month"));
        }

        return historyList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutageHistoryDTO> getOutageHistoryByType(OutageType outageType, Integer year, Integer month) {
        logger.info("Fetching outage history for type: {} with filters - year: {}, month: {}", outageType, year, month);

        List<OutageHistory> historyList;

        if (year != null && month != null) {
            historyList = outageHistoryRepository.findByTypeAndYearAndMonth(outageType, year, month);
        } else if (year != null) {
            historyList = outageHistoryRepository.findByTypeAndYear(outageType, year);
        } else {
            historyList = outageHistoryRepository.findByType(outageType, Sort.by(Sort.Direction.DESC, "year", "month"));
        }

        return historyList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getOutageStatistics() {
        logger.info("Generating outage statistics for admin dashboard");

        Map<String, Object> statistics = new HashMap<>();

        // Get current year and month
        int currentYear = LocalDateTime.now().getYear();
        int currentMonth = LocalDateTime.now().getMonthValue();

        // 1. Total outages (completed and ongoing)
        long totalOutages = outageRepository.count();
        statistics.put("totalOutages", totalOutages);

        // 2. Active outages (scheduled and ongoing)
        long activeOutages = outageRepository.countByStatusIn(
                Arrays.asList(OutageStatus.SCHEDULED, OutageStatus.ONGOING));
        statistics.put("activeOutages", activeOutages);

        // 3. Outages by type
        Map<String, Long> outagesByType = new HashMap<>();
        for (OutageType type : OutageType.values()) {
            long count = outageRepository.countByType(type);
            outagesByType.put(type.name(), count);
        }
        statistics.put("outagesByType", outagesByType);

        // 3.5 Get average restoration time
        Double avgRestorationTime = getAverageRestorationTime();
        statistics.put("averageRestorationTime", avgRestorationTime);

        // 4. Recent monthly statistics (last 6 months)
        List<Map<String, Object>> monthlyStats = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            YearMonth ym = YearMonth.of(currentYear, currentMonth).minusMonths(i);
            int year = ym.getYear();
            int month = ym.getMonthValue();

            List<OutageHistory> monthHistory = outageHistoryRepository.findByYearAndMonth(year, month);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("year", year);
            monthData.put("month", month);

            // Calculate totals across all areas
            int totalCount = 0;
            double totalHours = 0;
            double monthAvgRestorationTime = 0;

            for (OutageHistory history : monthHistory) {
                totalCount += history.getOutageCount();
                totalHours += history.getTotalOutageHours();
            }

            if (totalCount > 0) {
                monthAvgRestorationTime = totalHours / totalCount;
            }

            monthData.put("outageCount", totalCount);
            monthData.put("totalOutageHours", totalHours);
            monthData.put("avgRestorationTime", monthAvgRestorationTime);

            monthlyStats.add(monthData);
        }

        statistics.put("monthlyStats", monthlyStats);

        // 5. Top 5 areas with most outages (current year)
        List<Map<String, Object>> topAreas = outageHistoryRepository.findAreasWithMostOutages(currentYear, 5);
        statistics.put("topAreas", topAreas);

        return statistics;
    }

    @Override
    @Transactional
    public void updateOutageHistory(Long outageId) {
        logger.info("Updating outage history for outage ID: {}", outageId);

        try {
            // Fetch the outage
            Outage outage = outageRepository.findById(outageId)
                    .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + outageId));

            // Get month and year from start time
            int year = outage.getStartTime().getYear();
            int month = outage.getStartTime().getMonthValue();
            Area area = outage.getAffectedArea();
            OutageType type = outage.getType();

            // Find existing history record or create new one
            Optional<OutageHistory> historyOptional = outageHistoryRepository
                    .findByAreaIdAndTypeAndYearAndMonth(area.getId(), type, year, month);

            OutageHistory history;
            if (historyOptional.isPresent()) {
                history = historyOptional.get();
            } else {
                history = new OutageHistory();
                history.setArea(area);
                history.setType(type);
                history.setYear(year);
                history.setMonth(month);
                history.setOutageCount(0);
                history.setTotalOutageHours(0);
                history.setAverageRestorationTime(0);
            }

            // Update the history based on outage status
            switch (outage.getStatus()) {
                case SCHEDULED:
                    // For new scheduled outages, increment count but don't add hours yet
                    if (history.getOutageCount() == 0) { // New record
                        history.setOutageCount(1);
                        logger.info("Recording new scheduled outage in history");
                    }
                    break;

                case ONGOING:
                    // For ongoing outages, make sure they're counted
                    if (history.getOutageCount() == 0) { // New record
                        history.setOutageCount(1);
                        logger.info("Recording new ongoing outage in history");
                    }
                    break;

                case COMPLETED:
                    // Calculate duration for completed outages
                    if (outage.getActualEndTime() != null) {
                        double hours = calculateHoursBetween(outage.getStartTime(), outage.getActualEndTime());

                        // Increment count if not already counted
                        if (history.getOutageCount() == 0) {
                            history.setOutageCount(1);
                        }

                        // Add hours to total
                        history.setTotalOutageHours(history.getTotalOutageHours() + hours);

                        // Recalculate average
                        history.setAverageRestorationTime(history.getTotalOutageHours() / history.getOutageCount());

                        logger.info("Recorded completed outage with {} hours in history", hours);
                    }
                    break;

                case CANCELLED:
                    // For cancelled outages, we might want to count them separately
                    // Here we're just ensuring they appear in the history
                    if (history.getOutageCount() == 0) { // New record
                        history.setOutageCount(1);
                        logger.info("Recording cancelled outage in history");
                    }
                    break;

                default:
                    logger.warn("Unknown outage status: {}", outage.getStatus());
                    break;
            }

            // Save the history record
            outageHistoryRepository.save(history);
            logger.info("Updated outage history for area: {}, type: {}, month: {}, year: {}",
                    area.getName(), type, month, year);

        } catch (Exception e) {
            logger.error("Error updating outage history: {}", e.getMessage(), e);
            throw e; // Re-throw to propagate the error
        }
    }

    /**
     * Calculate hours between two LocalDateTime objects
     */
    private double calculateHoursBetween(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        return duration.toSeconds() / 3600.0; // Convert seconds to hours
    }

    /**
     * Convert OutageHistory entity to DTO
     */
    private OutageHistoryDTO convertToDTO(OutageHistory history) {
        OutageHistoryDTO dto = modelMapper.map(history, OutageHistoryDTO.class);
        dto.setAreaId(history.getArea().getId());
        return dto;
    }

    /**
     * Calculate the average restoration time in hours
     * @return Average time in hours
     */
    private Double getAverageRestorationTime() {
        logger.info("Calculating average outage restoration time");

        // Find all completed outages with actual end time
        List<Outage> outages = outageRepository.findByStatusAndActualEndTimeIsNotNull(
                OutageStatus.COMPLETED);

        if (outages.isEmpty()) {
            logger.info("No completed outages found with actual end time");
            return 0.0;
        }

        double totalHours = 0.0;
        for (Outage outage : outages) {
            Duration duration = Duration.between(outage.getStartTime(), outage.getActualEndTime());
            totalHours += duration.toSeconds() / 3600.0;
        }

        double average = totalHours / outages.size();
        logger.info("Calculated average restoration time: {} hours from {} outages",
                average, outages.size());

        return average;
    }
}