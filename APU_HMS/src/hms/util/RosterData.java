/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hms.util;

/**
 *
 * @author User
 */
import hms.role.User;
import java.util.List;

public class RosterData {

    private final String assignmentFile = "doctor_manager_assignments.txt";

    public String getDoctorName() {
        String doctorId = getDoctorId();

        if (doctorId.isEmpty()) {
            return "";
        }

        List<User> users = UserRepository.loadAll();

        for (User user : users) {
            if (doctorId.equals(user.getUserId())) {
                return user.getFullName();
            }
        }

        return "";
    }

    public String getManagedBy() {
        String managerId = getManagerId();

        if (managerId.isEmpty()) {
            return "";
        }

        List<User> users = UserRepository.loadAll();

        for (User user : users) {
            if (managerId.equals(user.getUserId())) {
                return user.getFullName();
            }
        }

        return "";
    }

    private String getDoctorId() {
        List<String> lines = FileManager.readLines(assignmentFile);

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|");

            if (parts.length >= 2) {
                return parts[0].trim();
            }
        }

        return "";
    }

    private String getManagerId() {
        List<String> lines = FileManager.readLines(assignmentFile);

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|");

            if (parts.length >= 2) {
                return parts[1].trim();
            }
        }

        return "";
    }
}