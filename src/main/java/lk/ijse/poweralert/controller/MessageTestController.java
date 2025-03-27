package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/public/test")
@CrossOrigin
public class MessageTestController {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ResponseDTO responseDTO;

    @GetMapping("/messages")
    public ResponseEntity<ResponseDTO> testMessages(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(required = false) String areaName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        // Set default values if not provided
        if (areaName == null) areaName = "Colombo";
        if (startTime == null) startTime = "2023-08-01 14:00";
        if (endTime == null) endTime = "2023-08-01 18:00";

        // Create locale based on language
        Locale locale;
        switch (language.toLowerCase()) {
            case "si":
                locale = new Locale("si", "LK");
                break;
            case "ta":
                locale = new Locale("ta", "LK");
                break;
            default:
                locale = Locale.ENGLISH;
        }

        Map<String, String> messages = new HashMap<>();

        String newOutageMsg = messageSource.getMessage(
                "outage.new",
                new Object[]{"ELECTRICITY", areaName, startTime, endTime, "Scheduled maintenance"},
                locale);
        messages.put("newOutage", newOutageMsg);

        String updateMsg = messageSource.getMessage(
                "outage.update",
                new Object[]{"ELECTRICITY", areaName, "ONGOING", endTime},
                locale);
        messages.put("updateOutage", updateMsg);


        String cancelMsg = messageSource.getMessage(
                "outage.cancelled",
                new Object[]{"ELECTRICITY", areaName, startTime},
                locale);
        messages.put("cancelOutage", cancelMsg);


        String restoreMsg = messageSource.getMessage(
                "outage.restored",
                new Object[]{"ELECTRICITY", areaName},
                locale);
        messages.put("restoreOutage", restoreMsg);

        responseDTO.setCode(200);
        responseDTO.setMessage("Messages retrieved successfully in " + language);
        responseDTO.setData(messages);

        return ResponseEntity.ok(responseDTO);
    }
}