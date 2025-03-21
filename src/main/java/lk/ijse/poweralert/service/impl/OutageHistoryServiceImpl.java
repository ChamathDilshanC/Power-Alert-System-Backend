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
            double avgRestorationTime = 0;

            for (OutageHistory history : monthHistory) {
                totalCount += history.getOutageCount();
                totalHours += history.getTotalOutageHours();
            }

            if (totalCount > 0) {
                avgRestorationTime = totalHours / totalCount;
            }

            monthData.put("outageCount", totalCount);
            monthData.put("totalOutageHours", totalHours);
            monthData.put("avgRestorationTime", avgRestorationTime);

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

            // Only update history if outage is completed
            if (outage.getStatus() != OutageStatus.COMPLETED) {
                logger.info("Outage is not completed yet, skipping history update");
                return;
            }

            // Calculate outage duration
            LocalDateTime startTime = outage.getStartTime();
            LocalDateTime endTime = outage.getActualEndTime() != null ?
                    outage.getActualEndTime() : LocalDateTime.now();

            // Get month and year from start time
            int year = startTime.getYear();
            int month = startTime.getMonthValue();
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
            logger.info("Updated outage history for area: {}, type: {}, month: {}, year: {}",
                    area.getName(), type, month, year);

        } catch (Exception e) {
            logger.error("Error updating outage history: {}", e.getMessage(), e);
            throw e;
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
}