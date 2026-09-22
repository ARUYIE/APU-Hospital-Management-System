package hms.role;

public class MedicalManager extends User {

    public MedicalManager(String userId, String username, String password,
                           String fullName, String email, String phone) {
        super(userId, username, password, fullName, email, phone);
    }

    @Override
    public Role getRole() {
        return Role.MEDICAL_MANAGER;
    }

    @Override
    public String[] getMenuOptions() {
        return new String[] {
            "Profile",
            "Clinical Departments",
            "Doctor Operational Roster",
            "View Analytical Reports"
        };
    }
}
