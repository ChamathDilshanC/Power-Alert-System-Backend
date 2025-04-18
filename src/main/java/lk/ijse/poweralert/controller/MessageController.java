package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.MessageDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.MessageService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private ResponseDTO responseDTO;

    @Autowired
    private UserRepository userRepository;

    // Get current user ID from authentication
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        // Get username from authentication
        String username = authentication.getName();

        // Get user from repository using username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return user.getId();
    }

    @GetMapping
    public ResponseEntity<ResponseDTO> getAllMessages() {
        try {
            List<MessageDTO> messages = messageService.getRecentMessages(100);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Messages retrieved successfully");
            responseDTO.setData(messages);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving messages: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<ResponseDTO> sendMessage(@RequestBody Map<String, String> payload) {
        try {
            String content = payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                responseDTO.setCode(VarList.Bad_Request);
                responseDTO.setMessage("Message content is required");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }

            Long currentUserId = getCurrentUserId();

            MessageDTO sentMessage = messageService.sendMessage(content, currentUserId);

            responseDTO.setCode(VarList.Created);
            responseDTO.setMessage("Message sent successfully");
            responseDTO.setData(sentMessage);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}