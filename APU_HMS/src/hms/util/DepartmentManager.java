package hms.util;

import java.util.ArrayList;
import java.util.List;

public final class DepartmentManager {

    private static final String DEPARTMENTS_FILE = "department.txt";

    private DepartmentManager() {
    }

    public static List<String> getDepartmentNames() {
        List<String> departmentNames = new ArrayList<>();
        for (String line : FileManager.readLines(DEPARTMENTS_FILE)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length > 1 && !"DEPT_NAME".equalsIgnoreCase(parts[1].trim())
                    && !parts[1].trim().isEmpty()) {
                departmentNames.add(parts[1].trim());
            }
        }
        return departmentNames;
    }
}
