package hms.role;

/**
 * The 4 types of end users in the HMS
 */
public enum Role {
    ADMIN_STAFF("Administrative Staff"),
    MEDICAL_MANAGER("Medical Manager"),
    DOCTOR("Doctor"),
    PATIENT("Patient");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // reads user.txt
    public static Role fromString(String value) {
        return Role.valueOf(value.trim().toUpperCase());
    }
}
