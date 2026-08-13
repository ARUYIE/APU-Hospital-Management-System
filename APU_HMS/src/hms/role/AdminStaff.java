package hms.role;

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
            "Profile",
            "Manage Wards/Clinics",
            "Manage Departments/Specialties",
            "Manage User",
            "View Analytical Reports"
        };
    }
}
