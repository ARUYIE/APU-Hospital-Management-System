package hms.util;

import hms.role.Patient;
import hms.role.Doctor;
import hms.role.Role;
import hms.role.AdminStaff;
import hms.role.MedicalManager;
import hms.role.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads/writes User objects (and its subclasses) to data/users.txt.
 * Centralizing this here means the GUI never has to know the file format.
 */
public final class UserRepository {

    private static final String FILE_NAME = "users.txt";

    private UserRepository() { }

    /** Parses every line in users.txt back into the correct User subclass. */
    public static List<User> loadAll() {
        List<User> users = new ArrayList<>();
        for (String line : FileManager.readLines(FILE_NAME)) {
            User user = parseLine(line);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    /**
     * Common fields (from User.toFileLine()) are:
     * userId | ROLE | username | password | fullName | email | phone [ | extra fields... ]
     */
    private static User parseLine(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 7) {
            System.err.println("Skipping malformed user line: " + line);
            return null;
        }
        String userId = p[0], roleStr = p[1], username = p[2], password = p[3],
                fullName = p[4], email = p[5], phone = p[6];

        try {
            Role role = Role.fromString(roleStr);
            switch (role) {
                case ADMIN_STAFF:
                    return new AdminStaff(userId, username, password, fullName, email, phone);
                case MEDICAL_MANAGER:
                    return new MedicalManager(userId, username, password, fullName, email, phone);
                case DOCTOR:
                    String specialty = p.length > 7 ? p[7] : "";
                    return new Doctor(userId, username, password, fullName, email, phone, specialty);
                case PATIENT:
                    String dob = p.length > 7 ? p[7] : "";
                    String gender = p.length > 8 ? p[8] : "";
                    return new Patient(userId, username, password, fullName, email, phone, dob, gender);
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Skipping user line with unknown role: " + line);
            return null;
        }
    }

    public static void save(User user) {
        FileManager.appendLine(FILE_NAME, user.toFileLine());
    }

    public static boolean usernameExists(String username) {
        for (User u : loadAll()) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /** @return the matching User if username+password are correct, otherwise null. */
    public static User authenticate(String username, String password) {
        for (User u : loadAll()) {
            if (u.getUsername().equalsIgnoreCase(username) && u.checkPassword(password)) {
                return u;
            }
        }
        return null;
    }

    public static String nextUserId() {
        return IDGenerator.next("U", FILE_NAME);
    }
}
