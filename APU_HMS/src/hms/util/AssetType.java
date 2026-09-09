package hms.util;

/**
 * Enum representing different types of hospital assets
 */
public enum AssetType {
    CONSULTATION_ROOM("Consultation Room"),
    INPATIENT_WARD("Inpatient Ward"),
    LAB("Laboratory"),
    IMAGING_ROOM("Imaging/X-Ray Room"),
    OPERATION_THEATRE("Operation Theatre"),
    PHARMACY("Pharmacy"),
    EQUIPMENT("Medical Equipment");

    private final String displayName;

    AssetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AssetType fromString(String value) {
        return AssetType.valueOf(value.trim().toUpperCase());
    }
}
