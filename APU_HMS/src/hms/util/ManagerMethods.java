package hms.util;

import hms.gui.panels.ManageRecordsPanel;
import hms.role.Role;
import hms.role.User;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

public class ManagerMethods {

    private final ManageRecordsPanel panel;
    private final String fileName;

    public ManagerMethods(ManageRecordsPanel panel, String fileName) {
        this.panel = panel;
        this.fileName = fileName;
    }

    // Clinical Department Methods
    private boolean departmentNameExists( String departmentName, String currentDeptId) {
        List<String> departmentRecords =
                FileManager.readLines("department.txt");

        // Skip header
        for (int i = 1; i < departmentRecords.size(); i++) {

            String record = departmentRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 4) {
                continue;
            }

            String existingDeptId =
                    parts[0].trim();

            String existingDeptName =
                    parts[1].trim();

            // Ignore the department currently being edited
            if (currentDeptId != null
                    && existingDeptId.equals(currentDeptId)) {
                continue;
            }

            if (existingDeptName.equalsIgnoreCase(
                    departmentName.trim())) {

                return true;
            }
        }

        return false;
    }
    
    public String addDepartmentRow() {
        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();

        List<User> managers =
                UserRepository.loadAll();

        List<User> medicalManagers = managers.stream()
                .filter(user -> user.getRole() == Role.MEDICAL_MANAGER)
                .toList();

        if (medicalManagers.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "No medical managers are available.",
                    "Cannot Add Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        JComboBox<String> managerCombo =
                new JComboBox<>();

        for (User manager : medicalManagers) {
            managerCombo.addItem(
                    manager.getFullName()
            );
        }

        JPanel form =
                new JPanel(
                        new GridLayout(3, 2, 8, 8)
                );

        form.add(new JLabel("DEPT_NAME:"));
        form.add(nameField);

        form.add(new JLabel("HEAD_MANAGER_NAME:"));
        form.add(managerCombo);

        form.add(new JLabel("DESCRIPTION:"));
        form.add(descriptionField);

        int choice = JOptionPane.showConfirmDialog(
                panel,
                form,
                "Add Department",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String deptName =
                nameField.getText().trim();

        String description =
                descriptionField.getText().trim();

        if (deptName.isEmpty()
                || description.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please fill in all department fields.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (panel.hasIllegalChars(
                deptName,
                description)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }
        
        if (departmentNameExists(deptName, null)) {
            JOptionPane.showMessageDialog(
                    panel,
                    "Department already exists.",
                    "Duplicate Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        int selectedIndex =
                managerCombo.getSelectedIndex();

        if (selectedIndex < 0) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please select a medical manager.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        String selectedManagerId =
                medicalManagers
                        .get(selectedIndex)
                        .getUserId();

        String deptId =
                IDGenerator.next("D", fileName);

        return String.join(
                "|",
                deptId,
                deptName,
                description,
                selectedManagerId
        );
    }

    public String editDepartmentRecord(String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);

        if (parts.length < 4) {
            return null;
        }

        String deptId = parts[0].trim();
        String deptName = parts[1].trim();
        String description = parts[2].trim();
        String existingManagerId = parts[3].trim();

        JTextField idField = new JTextField(deptId);
        panel.setUneditable(idField);

        JTextField nameField = new JTextField(deptName);
        JTextField descriptionField = new JTextField(description);

        List<User> managers = UserRepository.loadAll();

        JComboBox<String> managerCombo = new JComboBox<>();

        List<User> headManagers = managers.stream()
                .filter(user -> user.getRole() == Role.MEDICAL_MANAGER)
                .toList();

        int selectedManager = -1;

        for (int index = 0; index < headManagers.size(); index++) {

            User manager = headManagers.get(index);

            managerCombo.addItem(manager.getFullName());

            if (manager.getUserId().equals(existingManagerId)) {
                selectedManager = index;
            }
        }

        if (selectedManager >= 0) {
            managerCombo.setSelectedIndex(selectedManager);
        }

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));

        form.add(new JLabel("DEPT_ID:"));
        form.add(idField);

        form.add(new JLabel("DEPT_NAME:"));
        form.add(nameField);

        form.add(new JLabel("HEAD_MANAGER_NAME:"));
        form.add(managerCombo);

        form.add(new JLabel("DESCRIPTION:"));
        form.add(descriptionField);

        int choice = JOptionPane.showConfirmDialog(
                panel,
                form,
                "Edit Department",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String updatedDeptName =
                nameField.getText().trim();

        String updatedDescription =
                descriptionField.getText().trim();

        if (updatedDeptName.isEmpty()
                || updatedDescription.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Department name and description are required.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        String selectedManagerId = existingManagerId;

        if (managerCombo.getSelectedIndex() >= 0) {

            selectedManagerId =
                    headManagers
                            .get(managerCombo.getSelectedIndex())
                            .getUserId();
        }

        if (panel.hasIllegalChars(
                updatedDeptName,
                updatedDescription)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }
        
        if (departmentNameExists(
                updatedDeptName,
                deptId)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Department already exists.",
                    "Duplicate Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join(
                "|",
                idField.getText().trim(),
                updatedDeptName,
                updatedDescription,
                selectedManagerId
        );
    }
    
    // Roster Methods
    private boolean doctorShiftCheck(
            String doctorName,
            String date,
            String currentRosterId) {

        List<String> rosterRecords =
                FileManager.readLines("roster.txt");

        // Skip header
        for (int i = 1; i < rosterRecords.size(); i++) {

            String record = rosterRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    record.split("\\|", -1);

            if (parts.length < 7) {
                continue;
            }

            String rosterId = parts[0].trim();
            String existingDoctor = parts[1].trim();
            String existingDate = parts[4].trim();

            // Ignore the record currently being edited
            if (currentRosterId != null
                    && rosterId.equals(currentRosterId)) {
                continue;
            }

            if (existingDoctor.equalsIgnoreCase(doctorName)
                    && existingDate.equals(date)) {

                return true;
            }
        }

        return false;
    }

    public String addRosterRecord() {
        // Get the currently logged-in manager
        User loggedInManager = Session.getCurrentUser();

        if (loggedInManager == null) {
            JOptionPane.showMessageDialog(
                    panel,
                    "No user is currently logged in.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return null;
        }

        String loggedInManagerId = loggedInManager.getUserId();

        // Read doctor-manager assignments
        List<String> assignments = FileManager.readLines(
                "doctor_manager_assignments.txt"
        );

        List<String> assignedDoctorIds = new ArrayList<>();

        for (String assignment : assignments) {
            if (assignment == null || assignment.trim().isEmpty()) {
                continue;
            }

            String[] parts = assignment.split("\\|");

            if (parts.length >= 2) {
                String doctorId = parts[0].trim();
                String managerId = parts[1].trim();

                // Only get doctors assigned to the logged-in manager
                if (managerId.equals(loggedInManagerId)) {
                    assignedDoctorIds.add(doctorId);
                }
            }
        }

        // Load users
        List<User> users = UserRepository.loadAll();

        List<User> doctors = users.stream()
                .filter(user -> user.getRole() == Role.DOCTOR)
                .toList();

        // Doctor selection
        JComboBox<String> doctorCombo = new JComboBox<>();

        for (String doctorId : assignedDoctorIds) {
            for (User doctor : doctors) {
                if (doctor.getUserId().equals(doctorId)) {
                    doctorCombo.addItem(doctor.getFullName());
                    break;
                }
            }
        }

        if (doctorCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                    panel,
                    "No doctors are assigned to you.",
                    "Roster Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        // Department selection
        JComboBox<String> departmentCombo = new JComboBox<>();

        List<String> departments = FileManager.readLines(
                "department.txt"
        );

        // Skip the header
        for (int i = 1; i < departments.size(); i++) {
            String department = departments.get(i);

            if (department == null || department.trim().isEmpty()) {
                continue;
            }

            String[] parts = department.split("\\|");

            if (parts.length >= 2) {
                departmentCombo.addItem(parts[1].trim());
            }
        }

        // Date
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);

        JSpinner.DateEditor dateEditor =
                new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");

        dateSpinner.setEditor(dateEditor);

        // Shift
        JComboBox<String> shiftCombo = new JComboBox<>();
        shiftCombo.addItem("Morning");
        shiftCombo.addItem("Afternoon");
        shiftCombo.addItem("Night");

        // Status
        JComboBox<String> statusCombo = new JComboBox<>();
        statusCombo.addItem("Active");
        statusCombo.addItem("Inactive");

        // Form
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));

        form.add(new JLabel("DOCTOR_NAME:"));
        form.add(doctorCombo);

        form.add(new JLabel("DEPARTMENT:"));
        form.add(departmentCombo);

        form.add(new JLabel("DATE:"));
        form.add(dateSpinner);

        form.add(new JLabel("SHIFT:"));
        form.add(shiftCombo);

        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        int choice = JOptionPane.showConfirmDialog(
                panel,
                form,
                "Add Roster",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        // Get selected values
        String selectedDoctorName =
                (String) doctorCombo.getSelectedItem();

        String department =
                (String) departmentCombo.getSelectedItem();

        java.util.Date selectedDate =
                (java.util.Date) dateSpinner.getValue();

        String date = new java.text.SimpleDateFormat(
                "yyyy-MM-dd"
        ).format(selectedDate);

        String shift =
                (String) shiftCombo.getSelectedItem();

        String status =
                (String) statusCombo.getSelectedItem();

        // Validate
        if (selectedDoctorName == null
                || selectedDoctorName.isEmpty()
                || department == null
                || department.isEmpty()
                || selectedDate == null
                || shift == null
                || shift.isEmpty()
                || status == null
                || status.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please fill in all roster fields.",
                    "Invalid Roster",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        // Find selected doctor's ID
        String selectedDoctorId = "";

        for (User doctor : doctors) {
            if (doctor.getFullName().equals(selectedDoctorName)
                    && assignedDoctorIds.contains(doctor.getUserId())) {

                selectedDoctorId = doctor.getUserId();
                break;
            }
        }

        if (selectedDoctorId.isEmpty()) {
            JOptionPane.showMessageDialog(
                    panel,
                    "The selected doctor could not be found.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return null;
        }
        
        if (doctorShiftCheck(
            selectedDoctorName,
            date,
            null)) {

        JOptionPane.showMessageDialog(
                panel,
                "This doctor already has a shift on "
                + date
                + ". A doctor can only have one shift per date.",
                "Scheduling Conflict",
                JOptionPane.WARNING_MESSAGE
        );

        return null;
    }

        // Generate roster ID
        String rosterId = IDGenerator.next("R", fileName);

        // Create the roster record
        String normalizedRecord = String.join("|",
                rosterId,
                selectedDoctorName,
                loggedInManager.getFullName(),
                department,
                date,
                shift,
                status
        );

        // Return the record to ManageRecordsPanel
        return normalizedRecord;
    }   
    
    public String editRosterRecord(String record) {

        String[] parts = record.split("\\|");

        if (parts.length < 7) {
            return null;
        }

        User loggedInManager = Session.getCurrentUser();

        if (loggedInManager == null) {
            JOptionPane.showMessageDialog(
                    panel,
                    "No user is currently logged in.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return null;
        }

        String loggedInManagerId = loggedInManager.getUserId();

        String rosterId = parts[0].trim();
        String doctorName = parts[1].trim();
        String managerName = parts[2].trim();
        String department = parts[3].trim();
        String date = parts[4].trim();
        String shift = parts[5].trim();
        String status = parts[6].trim();

        // Check that the doctor belongs to the logged-in manager
        List<String> assignments = FileManager.readLines(
                "doctor_manager_assignments.txt"
        );

        List<String> assignedDoctorIds = new ArrayList<>();

        for (String assignment : assignments) {

            if (assignment == null || assignment.trim().isEmpty()) {
                continue;
            }

            String[] assignmentParts = assignment.split("\\|");

            if (assignmentParts.length >= 2) {

                String doctorId = assignmentParts[0].trim();
                String managerId = assignmentParts[1].trim();

                if (managerId.equals(loggedInManagerId)) {
                    assignedDoctorIds.add(doctorId);
                }
            }
        }

        List<User> users = UserRepository.loadAll();

        String doctorId = "";

        for (User user : users) {

            if (user.getRole() == Role.DOCTOR
                    && user.getFullName().equals(doctorName)) {

                doctorId = user.getUserId();
                break;
            }
        }

        if (doctorId.isEmpty()
                || !assignedDoctorIds.contains(doctorId)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "You can only edit rosters for doctors assigned to you.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        // Department
        JComboBox<String> departmentField = new JComboBox<>();

        List<String> departments = FileManager.readLines(
                "department.txt"
        );

        for (int i = 1; i < departments.size(); i++) {

            String departmentRecord = departments.get(i);

            if (departmentRecord == null
                    || departmentRecord.trim().isEmpty()) {
                continue;
            }

            String[] departmentParts = departmentRecord.split("\\|");

            if (departmentParts.length >= 2) {
                departmentField.addItem(
                        departmentParts[1].trim()
                );
            }
        }

        departmentField.setSelectedItem(department);

        // Date
        JTextField dateField = new JTextField(date);

        // Shift
        JComboBox<String> shiftField = new JComboBox<>(
                new String[]{
                    "Morning",
                    "Afternoon",
                    "Night"
                }
        );

        shiftField.setSelectedItem(shift);

        // Status
        JComboBox<String> statusField = new JComboBox<>(
                new String[]{
                    "Active",
                    "Inactive"
                }
        );

        statusField.setSelectedItem(status);

        JPanel form = new JPanel(
                new GridLayout(4, 2, 8, 8)
        );

        form.add(new JLabel("DEPARTMENT:"));
        form.add(departmentField);

        form.add(new JLabel("DATE:"));
        form.add(dateField);

        form.add(new JLabel("SHIFT:"));
        form.add(shiftField);

        form.add(new JLabel("STATUS:"));
        form.add(statusField);

        int choice = JOptionPane.showConfirmDialog(
                panel,
                form,
                "Edit Roster",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        if (doctorShiftCheck(
            doctorName,
            dateField.getText().trim(),
            rosterId)) {

        JOptionPane.showMessageDialog(
                panel,
                "This doctor already has a shift on "
                + date
                + ". A doctor can only have one shift per date.",
                "Scheduling Conflict",
                JOptionPane.WARNING_MESSAGE
        );

        return null;
    }
        
        return String.join("|",
                rosterId,
                doctorName,
                managerName,
                departmentField.getSelectedItem().toString().trim(),
                dateField.getText().trim(),
                shiftField.getSelectedItem().toString().trim(),
                statusField.getSelectedItem().toString().trim()
        );
    }

    // Report Methods
    public void populateReportTable() {
        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        List<String> reportRecords =
                FileManager.readLines("report.txt");

        java.util.Set<String> months =
                new java.util.LinkedHashSet<>();

        // Skip the first line because it is the header
        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    record.split("\\|", -1);

            if (parts.length < 7) {
                continue;
            }

            String consultationDate =
                    parts[3].trim();

            if (consultationDate.length() >= 7) {

                String month =
                        consultationDate.substring(0, 7);

                months.add(month);
            }
        }

        for (String month : months) {

            ReportData reportData =
                    new ReportData(month);

            String newRecord = String.join("|",
                    month,
                    String.valueOf(
                            reportData.getTotalPatients()
                    ),
                    String.valueOf(
                            reportData.getAppointments()
                    ),
                    String.valueOf(
                            reportData.getCompleted()
                    ),
                    String.valueOf(
                            reportData.getCancelled()
                    ),
                    String.format(
                            "%.2f",
                            reportData.getTotalRevenue()
                    )
            );

            boolean monthExists = false;

            for (int i = 1; i < reportRecords.size(); i++) {

                String existingRecord =
                        reportRecords.get(i);

                if (existingRecord == null
                        || existingRecord.trim().isEmpty()) {
                    continue;
                }

                String[] parts =
                        existingRecord.split("\\|", -1);

                if (parts.length < 6) {
                    continue;
                }

                if (month.equals(parts[0].trim())) {

                    monthExists = true;

                    // Update existing month
                    reportRecords.set(i, newRecord);
                    break;
                }
            }

            // If this month does not exist, create a new report entry.
            if (!monthExists) {

                reportRecords.add(newRecord);
            }
        }

        FileManager.writeAllLines(
                "report.txt",
                reportRecords
        );
        panel.refreshReportFilter();
        panel.refreshTable();
    }
}