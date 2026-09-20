package hms.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DoctorManagerAssignmentRepository {

    private static final String FILE_NAME = "doctor_manager_assignments.txt";

    private DoctorManagerAssignmentRepository() {
    }

    public static Map<String, String> loadAll() {
        Map<String, String> assignments = new LinkedHashMap<>();
        for (String line : FileManager.readLines(FILE_NAME)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length >= 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                assignments.put(parts[0].trim(), parts[1].trim());
            }
        }
        return assignments;
    }

    public static void assign(String doctorId, String managerId) {
        Map<String, String> assignments = loadAll();
        assignments.put(doctorId, managerId);
        write(assignments);
    }

    private static void write(Map<String, String> assignments) {
        List<String> lines = assignments.entrySet().stream()
                .map(entry -> entry.getKey() + "|" + entry.getValue())
                .toList();
        FileManager.writeAllLines(FILE_NAME, lines);
    }
}
