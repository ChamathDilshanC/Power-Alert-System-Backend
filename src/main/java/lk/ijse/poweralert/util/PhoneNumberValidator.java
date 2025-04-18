package lk.ijse.poweralert.util;

import java.util.regex.Pattern;

public class PhoneNumberValidator {

    private static final Pattern SRI_LANKA_MOBILE_PATTERN =
            Pattern.compile("^\\+94[0-9]{9}$");

    public static boolean isValidSriLankanMobile(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        return SRI_LANKA_MOBILE_PATTERN.matcher(phoneNumber).matches();
    }

    public static String formatPhoneNumber(String phoneNumber) {
        // Remove any spaces, hyphens, or parentheses
        String cleaned = phoneNumber.replaceAll("[\\s\\-()]", "");

        // Ensure it starts with +94
        if (!cleaned.startsWith("+94")) {
            if (cleaned.startsWith("94")) {
                cleaned = "+" + cleaned;
            } else if (cleaned.startsWith("0")) {
                cleaned = "+94" + cleaned.substring(1);
            } else {
                cleaned = "+94" + cleaned;
            }
        }

        return cleaned;
    }
}