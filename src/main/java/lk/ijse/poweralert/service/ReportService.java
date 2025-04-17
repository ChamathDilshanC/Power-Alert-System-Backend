package lk.ijse.poweralert.service;

import java.util.Map;

public interface ReportService {
    Map<String, Object> generateReport(String fromDate, String toDate, String outageType, Long areaId);
}