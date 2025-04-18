package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.CommunityFeedback;
import lk.ijse.poweralert.enums.AppEnums.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityFeedbackRepository extends JpaRepository<CommunityFeedback, Long> {

    /**
     * Find feedback by outage ID, ordered by creation time descending
     * @param outageId the outage ID
     * @return list of feedback for the outage
     */
    List<CommunityFeedback> findByOutageIdOrderByCreatedAtDesc(Long outageId);

    /**
     * Find feedback by user ID, ordered by creation time descending
     * @param userId the user ID
     * @return list of feedback by the user
     */
    List<CommunityFeedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find feedback by outage ID and feedback type
     * @param outageId the outage ID
     * @param type the feedback type
     * @return list of feedback for the outage with the specified type
     */
    List<CommunityFeedback> findByOutageIdAndType(Long outageId, FeedbackType type);

    /**
     * Count feedback by outage ID and feedback type
     * @param outageId the outage ID
     * @param type the feedback type
     * @return the count of feedback with the specified type for the outage
     */
    long countByOutageIdAndType(Long outageId, FeedbackType type);
}