package hms.role;

/**
 * Abstract base class for every person who can log into the HMS.
 * <p>
 * OOP concepts demonstrated here:
 * - Abstraction: User cannot be instantiated directly; only its subclasses
 *   (AdminStaff, MedicalManager, Doctor, Patient) can be created.
 * - Encapsulation: all fields are private, accessed only via getters/setters.
 * - Inheritance: AdminStaff, MedicalManager, Doctor and Patient extend this class.
 * - Polymorphism: getRole() and getMenuOptions() are overridden differently by
 *   each subclass so the GUI can behave differently per user type.
 */
public abstract class User {

    private String userId;
    private String username;
    private String password; // NOTE: plain text for assignment scope; a real system must hash this.
    private String fullName;
    private String email;
    private String phone;

    protected User(String userId, String username, String password,
                    String fullName, String email, String phone) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    // ---- Abstract methods every subclass must implement (polymorphism) ----

    /** @return the Role enum identifying which of the 4 user types this is. */
    public abstract Role getRole();

    /**
     * @return the list of dashboard menu items this user type is allowed to see.
     * Each subclass decides its own set of features.
     */
    public abstract String[] getMenuOptions();

    // ---- Serialization: text-file storage (pipe-delimited, no DB allowed) ----

    /**
     * Converts this user into a single pipe-delimited line for storage in users.txt.
     * Subclasses append their own extra fields by overriding and calling super.
     */
    public String toFileLine() {
        return String.join("|",
                userId, getRole().name(), username, password, fullName, email, phone);
    }

    // ---- Encapsulated accessors ----

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean checkPassword(String attempt) {
        return password != null && password.equals(attempt);
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole().getDisplayName() + ")";
    }
}
