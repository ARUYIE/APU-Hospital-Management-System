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
        if (value == null) {
            throw new IllegalArgumentException("Asset type cannot be null");
        }

        String trimmedValue = value.trim();
        String normalizedValue = trimmedValue.toUpperCase().replace(' ', '_');
        for (AssetType assetType : values()) {
            if (assetType.name().equals(normalizedValue)
                    || assetType.displayName.equalsIgnoreCase(trimmedValue)) {
                return assetType;
            }
        }

        throw new IllegalArgumentException("Unknown asset type: " + value);
    }
}
