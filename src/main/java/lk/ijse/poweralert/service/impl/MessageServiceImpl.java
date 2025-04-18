package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.MessageDTO;
import lk.ijse.poweralert.entity.Message;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.MessageRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getAllMessages() {
        List<Message> messages = messageRepository.findAllByOrderBySentAtAsc();
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getRecentMessages(int limit) {
        List<Message> messages = messageRepository.findTop100ByOrderBySentAtDesc();

        // Reverse to get oldest first
        List<Message> orderedMessages = messages.stream()
                .sorted((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
                .collect(Collectors.toList());

        return orderedMessages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(String content, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setContent(content);
        message.setUser(user);
        message.setSentAt(LocalDateTime.now());
        message.setUsername(user.getUsername());
        message.setUserRole(user.getRole().name());
        // Set the sender_id to match the user_id
        message.setSenderId(userId);

        Message savedMessage = messageRepository.save(message);

        return convertToDTO(savedMessage);
    }

    private MessageDTO convertToDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .userId(message.getUser().getId())
                .username(message.getUsername())
                .userRole(message.getUserRole())
                .sentAt(message.getSentAt())
                .build();
    }
}