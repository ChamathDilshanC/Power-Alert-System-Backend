package lk.ijse.poweralert.service;

import java.util.concurrent.CompletableFuture;

public interface SmsService {
    CompletableFuture<Boolean> sendSms(String phoneNumber, String messageContent);
    boolean isValidPhoneNumber(String phoneNumber); // This method can stay as boolean since it's not @Async
}