package lk.ijse.poweralert.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lk.ijse.poweralert.dto.CommunityFeedbackDTO;
import lk.ijse.poweralert.entity.CommunityFeedback;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.CommunityFeedbackRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.service.CommunityFeedbackService;
import lk.ijse.poweralert.service.UserService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityFeedbackServiceImpl implements CommunityFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(CommunityFeedbackServiceImpl.class);

    @Autowired
    private CommunityFeedbackRepository communityFeedbackRepository;

    @Autowired
    private OutageRepository outageRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityFeedbackDTO> getFeedbackByOutageId(Long outageId) {
        logger.info("Fetching feedback for outage ID: {}", outageId);

        // Verify outage exists
        if (!outageRepository.existsById(outageId)) {
            throw new EntityNotFoundException("Outage not found with ID: " + outageId);
        }

        // Get feedback for outage
        List<CommunityFeedback> feedbackList = communityFeedbackRepository.findByOutageIdOrderByCreatedAtDesc(outageId);

        return feedbackList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommunityFeedbackDTO submitFeedback(CommunityFeedbackDTO feedbackDTO) {
        logger.info("Submitting feedback for outage ID: {}", feedbackDTO.getOutageId());

        // Get current user
        User user = getCurrentUser();

        // Verify outage exists
        Outage outage = outageRepository.findById(feedbackDTO.getOutageId())
                .orElseThrow(() -> new EntityNotFoundException("Outage not found with ID: " + feedbackDTO.getOutageId()));

        // Create feedback entity
        CommunityFeedback feedback = new CommunityFeedback();
        feedback.setUser(user);
        feedback.setOutage(outage);
        feedback.setFeedbackText(feedbackDTO.getFeedbackText());
        feedback.setType(feedbackDTO.getType());
        feedback.setAnonymous(feedbackDTO.isAnonymous());
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setLatitude(feedbackDTO.getLatitude());
        feedback.setLongitude(feedbackDTO.getLongitude());

        // Save feedback
        CommunityFeedback savedFeedback = communityFeedbackRepository.save(feedback);
        logger.info("Feedback saved with ID: {}", savedFeedback.getId());

        return convertToDTO(savedFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityFeedbackDTO> getFeedbackByCurrentUser() {
        logger.info("Fetching feedback for current user");

        // Get current user
        User user = getCurrentUser();

        // Get feedback by user
        List<CommunityFeedback> feedbackList = communityFeedbackRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return feedbackList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityFeedbackDTO getFeedbackById(Long id) {
        logger.info("Fetching feedback with ID: {}", id);

        // Find feedback
        CommunityFeedback feedback = communityFeedbackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Feedback not found with ID: " + id));

        return convertToDTO(feedback);
    }

    /**
     * Get the current logged-in user
     * @return the user entity
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserEntityByUsername(username);
    }

    /**
     * Convert CommunityFeedback entity to DTO
     * @param feedback the entity to convert
     * @return the DTO
     */
    private CommunityFeedbackDTO convertToDTO(CommunityFeedback feedback) {
        CommunityFeedbackDTO dto = modelMapper.map(feedback, CommunityFeedbackDTO.class);

        // Set user ID and outage ID for the DTO
        dto.setUserId(feedback.getUser().getId());
        dto.setOutageId(feedback.getOutage().getId());

        return dto;
    }
}