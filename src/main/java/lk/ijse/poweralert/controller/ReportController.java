package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.ReportService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;
    private final ResponseDTO responseDTO;

    @Autowired
    public ReportController(ReportService reportService, ResponseDTO responseDTO) {
        this.reportService = reportService;
        this.responseDTO = responseDTO;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> generateReport(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) String outageType,
            @RequestParam(required = false) Long areaId) {

        try {
            logger.info("Generating report from {} to {} with type {} and area {}",
                    fromDate, toDate, outageType, areaId);

            Map<String, Object> reportData = reportService.generateReport(fromDate, toDate, outageType, areaId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Report generated successfully");
            responseDTO.setData(reportData);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating report: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}