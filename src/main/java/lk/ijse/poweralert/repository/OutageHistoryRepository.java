package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.OutageHistory;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OutageHistoryRepository extends JpaRepository<OutageHistory, Long> {

    /** Find outage history by area, type, year and month */
    Optional<OutageHistory> findByAreaIdAndTypeAndYearAndMonth(
            Long areaId, OutageType type, int year, int month);

    /** Find all outage history for a given year and month */
    List<OutageHistory> findByYearAndMonth(int year, int month);

    /** Find all outage history for a given year */
    List<OutageHistory> findByYear(int year);

    /** Find outage history for a specific area */
    List<OutageHistory> findByAreaId(Long areaId, Sort sort);

    /** Find outage history for a specific area and year */
    List<OutageHistory> findByAreaIdAndYear(Long areaId, int year);

    /** Find outage history for a specific area, year and month */
    List<OutageHistory> findByAreaIdAndYearAndMonth(Long areaId, int year, int month);

    /** Find outage history by type */
    List<OutageHistory> findByType(OutageType type, Sort sort);

    /** Find outage history by type and year */
    List<OutageHistory> findByTypeAndYear(OutageType type, int year);

    /** Find outage history by type, year and month */
    List<OutageHistory> findByTypeAndYearAndMonth(OutageType type, int year, int month);

    /** Get top areas with most outages for a given year */
    @Query(value = "SELECT a.id as areaId, a.name as areaName, a.district as district, " +
            "SUM(oh.outage_count) as totalOutages, SUM(oh.total_outage_hours) as totalHours " +
            "FROM outage_history oh " +
            "JOIN areas a ON oh.area_id = a.id " +
            "WHERE oh.year = :year " +
            "GROUP BY a.id, a.name, a.district " +
            "ORDER BY totalOutages DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> findAreasWithMostOutages(@Param("year") int year, @Param("limit") int limit);
}