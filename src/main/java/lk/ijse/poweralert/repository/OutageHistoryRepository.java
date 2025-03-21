package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.OutageHistory;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutageHistoryRepository extends JpaRepository<OutageHistory, Long> {

    /** Find outage history by area, type, year and month */
    Optional<OutageHistory> findByAreaIdAndTypeAndYearAndMonth(
            Long areaId, OutageType type, int year, int month);
}