package lk.ijse.poweralert.controller;

import jakarta.validation.Valid;
import lk.ijse.poweralert.dto.CommunityFeedbackDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.CommunityFeedbackService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CommunityFeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(CommunityFeedbackController.class);

    @Autowired
    private CommunityFeedbackService communityFeedbackService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all feedback for a specific outage
     */
    @GetMapping("/outages/{outageId}/feedback")
    public ResponseEntity<ResponseDTO> getOutageFeedback(@PathVariable Long outageId) {
        try {
            logger.debug("Fetching feedback for outage ID: {}", outageId);

            List<CommunityFeedbackDTO> feedbackList = communityFeedbackService.getFeedbackByOutageId(outageId);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Feedback retrieved successfully");
            responseDTO.setData(feedbackList);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving feedback for outage ID {}: {}", outageId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Submit feedback for an outage
     * Requires user authentication
     */
    @PostMapping("/outages/{outageId}/feedback")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> submitFeedback(
            @PathVariable Long outageId,
            @Valid @RequestBody CommunityFeedbackDTO feedbackDTO) {
        try {
            logger.debug("Submitting feedback for outage ID: {}", outageId);

            // Set the outage ID from the path parameter
            feedbackDTO.setOutageId(outageId);

            CommunityFeedbackDTO savedFeedback = communityFeedbackService.submitFeedback(feedbackDTO);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Feedback submitted successfully");
            responseDTO.setData(savedFeedback);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error submitting feedback for outage ID {}: {}", outageId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all feedback submitted by the current user
     * Requires user authentication
     */
    @GetMapping("/user/feedback")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> getUserFeedback() {
        try {
            logger.debug("Fetching feedback for current user");

            List<CommunityFeedbackDTO> feedbackList = communityFeedbackService.getFeedbackByCurrentUser();

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("User feedback retrieved successfully");
            responseDTO.setData(feedbackList);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving feedback for current user: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}