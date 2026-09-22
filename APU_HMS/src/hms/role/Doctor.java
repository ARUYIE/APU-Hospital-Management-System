package hms.role;

public class Doctor extends User {

    private String specialty; // e.g. department/specialty name this doctor belongs to

    public Doctor(String userId, String username, String password,
                  String fullName, String email, String phone, String specialty) {
        super(userId, username, password, fullName, email, phone);
        this.specialty = specialty;
    }

    public String getSpecialty() { 
        return specialty; 
    }
    public void setSpecialty(String specialty) { 
        this.specialty = specialty; 
    }

    @Override
    public Role getRole() {
        return Role.DOCTOR;
    }

    @Override
    public String[] getMenuOptions() {
        return new String[] {
            "Profile",
            "Patient Vitals & Consultation Notes",
            "Prescriptions",
            "Lab & Imaging Requests"
        };
    }

    @Override
    public String toFileLine() {
        // Append the extra field to the base line so it round-trips through the text file.
        return super.toFileLine() + "|" + specialty;
    }
}
