package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.OutageHistoryDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import lk.ijse.poweralert.service.OutageHistoryService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OutageHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(OutageHistoryController.class);

    @Autowired
    private OutageHistoryService outageHistoryService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all historical outage data
     */
    @GetMapping("/public/outage-history")
    public ResponseEntity<ResponseDTO> getAllOutageHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            logger.info("Fetching all outage history data");
            List<OutageHistoryDTO> historyList = outageHistoryService.getAllOutageHistory(year, month);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outage history retrieved successfully");
            responseDTO.setData(historyList);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving outage history: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get historical outage data for a specific area
     */
    @GetMapping("/public/outage-history/area/{areaId}")
    public ResponseEntity<ResponseDTO> getOutageHistoryByArea(
            @PathVariable Long areaId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            logger.info("Fetching outage history for area ID: {}", areaId);
            List<OutageHistoryDTO> historyList = outageHistoryService.getOutageHistoryByArea(areaId, year, month);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Area outage history retrieved successfully");
            responseDTO.setData(historyList);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving area outage history: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get historical outage data by type
     */
    @GetMapping("/public/outage-history/type/{type}")
    public ResponseEntity<ResponseDTO> getOutageHistoryByType(
            @PathVariable String type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            logger.info("Fetching outage history for type: {}", type);
            OutageType outageType = OutageType.valueOf(type.toUpperCase());
            List<OutageHistoryDTO> historyList = outageHistoryService.getOutageHistoryByType(outageType, year, month);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Type outage history retrieved successfully");
            responseDTO.setData(historyList);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid outage type: {}", type);
            responseDTO.setCode(VarList.Bad_Request);
            responseDTO.setMessage("Invalid outage type: " + type);
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error retrieving type outage history: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get outage statistics for admin dashboard
     */
    @GetMapping("/admin/dashboard/statistics")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> getOutageStatistics() {
        try {
            logger.info("Fetching outage statistics for admin dashboard");
            Map<String, Object> statistics = outageHistoryService.getOutageStatistics();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Outage statistics retrieved successfully");
            responseDTO.setData(statistics);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving outage statistics: {}", e.getMessage(), e);
            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}