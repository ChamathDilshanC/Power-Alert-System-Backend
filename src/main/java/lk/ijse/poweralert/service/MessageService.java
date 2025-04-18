package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.MessageDTO;
import java.util.List;

public interface MessageService {

    // Get all messages
    List<MessageDTO> getAllMessages();

    // Get recent messages
    List<MessageDTO> getRecentMessages(int limit);

    // Send a message
    MessageDTO sendMessage(String content, Long userId);
}