package lk.ijse.poweralert.enums;

public class AppEnums {

    public enum OutageType {
        ELECTRICITY, WATER, GAS
    }

    public enum OutageStatus {
        SCHEDULED, ONGOING, COMPLETED, CANCELLED
    }

    public enum NotificationType {
        SMS, EMAIL, PUSH, WHATSAPP
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, DELIVERED
    }

    public enum Role {
        ADMIN, USER, UTILITY_PROVIDER
    }

    public enum ResourceType {
        WATER_SUPPLY, CHARGING_STATION, GENERATOR
    }

    public enum FeedbackType {
        REPORT_OUTAGE, CONFIRM_OUTAGE, REPORT_RESTORATION
    }

    public enum UtilityType {
        ELECTRICITY, WATER, GAS
    }
}