package hms.role;

/**
 * Administrative Staff: manage wards/clinics, departments, and user registration.
 */
public class AdminStaff extends User {

    public AdminStaff(String userId, String username, String password,
                       String fullName, String email, String phone) {
        super(userId, username, password, fullName, email, phone);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN_STAFF;
    }

    @Override
    public String[] getMenuOptions() {
        return new String[] {
                "Manage Wards/Clinics",
                "Manage Departments/Specialties",
                "Register New User",
                "View Analytical Reports"
        };
    }
}
