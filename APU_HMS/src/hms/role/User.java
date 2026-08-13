package hms.role;


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

    public abstract Role getRole();


    public abstract String[] getMenuOptions();


    public String toFileLine() {
        return String.join("，",
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
