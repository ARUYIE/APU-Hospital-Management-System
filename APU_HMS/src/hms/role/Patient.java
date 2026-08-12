package hms.role;

/**
 * Patient: books appointments and views their own assessment results,
 * feedback, prescriptions and bills.
 */
public class Patient extends User {

    private String dateOfBirth; // stored as plain text, e.g. "2001-05-14"
    private String gender;

    public Patient(String userId, String username, String password,
                    String fullName, String email, String phone,
                    String dateOfBirth, String gender) {
        super(userId, username, password, fullName, email, phone);
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }

    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }

    @Override
    public Role getRole() {
        return Role.PATIENT;
    }

    @Override
    public String[] getMenuOptions() {
        return new String[] {
                "Book / View Appointments",
                "View My Assessment Results",
                "View My Bills"
        };
    }

    @Override
    public String toFileLine() {
        return super.toFileLine() + "|" + dateOfBirth + "|" + gender;
    }
}
