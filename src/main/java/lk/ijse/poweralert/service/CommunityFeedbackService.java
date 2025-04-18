package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.CommunityFeedbackDTO;

import java.util.List;

public interface CommunityFeedbackService {

    /**
     * Get all feedback for a specific outage
     * @param outageId the outage ID
     * @return list of feedback for the outage
     */
    List<CommunityFeedbackDTO> getFeedbackByOutageId(Long outageId);

    /**
     * Submit feedback for an outage
     * @param feedbackDTO the feedback to submit
     * @return the saved feedback
     */
    CommunityFeedbackDTO submitFeedback(CommunityFeedbackDTO feedbackDTO);

    /**
     * Get all feedback submitted by the current user
     * @return list of feedback by the current user
     */
    List<CommunityFeedbackDTO> getFeedbackByCurrentUser();

    /**
     * Get feedback by ID
     * @param id the feedback ID
     * @return the feedback
     */
    CommunityFeedbackDTO getFeedbackById(Long id);
}