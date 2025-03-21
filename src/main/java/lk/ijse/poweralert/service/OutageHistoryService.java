package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.OutageHistoryDTO;
import lk.ijse.poweralert.enums.AppEnums.OutageType;

import java.util.List;
import java.util.Map;

public interface OutageHistoryService {

    /**
     * Get all outage history data with optional year and month filters
     */
    List<OutageHistoryDTO> getAllOutageHistory(Integer year, Integer month);

    /**
     * Get outage history for a specific area with optional year and month filters
     */
    List<OutageHistoryDTO> getOutageHistoryByArea(Long areaId, Integer year, Integer month);

    /**
     * Get outage history by type with optional year and month filters
     */
    List<OutageHistoryDTO> getOutageHistoryByType(OutageType outageType, Integer year, Integer month);

    /**
     * Get outage statistics for admin dashboard
     */
    Map<String, Object> getOutageStatistics();

    /**
     * Update outage history when outage is completed
     */
    void updateOutageHistory(Long outageId);
}