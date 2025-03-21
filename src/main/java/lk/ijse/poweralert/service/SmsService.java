package lk.ijse.poweralert.service;

public interface SmsService {

    /** Send an SMS message to the specified phone number    */
    boolean sendSms(String phoneNumber, String message);

    /** Check if a phone number is valid for receiving SMS   */
    boolean isValidPhoneNumber(String phoneNumber);
}