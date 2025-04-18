package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find all messages ordered by sent time
    List<Message> findAllByOrderBySentAtAsc();

    // Find latest messages limited by count
    List<Message> findTop100ByOrderBySentAtDesc();
}