package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findAllById(Iterable<Long> ids);
}