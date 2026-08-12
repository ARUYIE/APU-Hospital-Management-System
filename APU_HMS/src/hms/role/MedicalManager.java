package hms.role;

/**
 * Medical Manager: designs assessment/check-up types and oversees grading/billing rules,
 * plus analytical reporting across departments.
 */
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
                "Design Assessment/Check-up Types",
                "Medical Grading & Billing Rules",
                "View Analytical Reports"
        };
    }
}
