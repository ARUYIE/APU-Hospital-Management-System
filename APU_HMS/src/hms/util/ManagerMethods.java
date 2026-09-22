package hms.util;

import hms.gui.panels.ManageRecordsPanel;
import hms.role.Role;
import hms.role.User;
import hms.util.FileManager;
import hms.util.IDGenerator;
import hms.util.ManageRecordsHelper;
import hms.util.UserRepository;

import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

public class ManagerMethods {

    private final ManageRecordsPanel panel;
    private final String fileName;

    public ManagerMethods(ManageRecordsPanel panel, String fileName) {
        this.panel = panel;
        this.fileName = fileName;
    }

    // Roster Methods
    public void addRosterRecord() {
        List<String> assignments = FileManager.readLines("doctor_manager_assignments.txt");

        List<String> doctorIds = new ArrayList<>();
        List<String> managerIds = new ArrayList<>();

        for (String assignment : assignments) {
            if (assignment == null || assignment.trim().isEmpty()) {
                continue;
            }

            String[] parts = assignment.split("\\|");

            if (parts.length >= 2) {
                doctorIds.add(parts[0].trim());
                managerIds.add(parts[1].trim());
            }
        }

        List<User> users = UserRepository.loadAll();

        List<User> doctors = users.stream()
                .filter(user -> user.getRole() == Role.DOCTOR)
                .toList();

        JComboBox<String> doctorCombo = new JComboBox<>();

        for (String doctorId : doctorIds) {
            for (User doctor : doctors) {
                if (doctor.getUserId().equals(doctorId)) {
                    doctorCombo.addItem(doctor.getFullName());
                    break;
                }
            }
        }

        JComboBox<String> departmentCombo = new JComboBox<>();

        List<String> departments = FileManager.readLines("department.txt");

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

        // Date selector
        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);

        JSpinner.DateEditor dateEditor =
                new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");

        dateSpinner.setEditor(dateEditor);

        // Shift selector
        JComboBox<String> shiftCombo = new JComboBox<>();

        shiftCombo.addItem("Morning");
        shiftCombo.addItem("Afternoon");
        shiftCombo.addItem("Night");

        // Status selector
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
            return;
        }

        String selectedDoctorName =
                (String) doctorCombo.getSelectedItem();

        String department =
                (String) departmentCombo.getSelectedItem();

        Date selectedDate =
                (Date) dateSpinner.getValue();

        String date = new SimpleDateFormat(
                "yyyy-MM-dd"
        ).format(selectedDate);

        String shift =
                (String) shiftCombo.getSelectedItem();

        String status =
                (String) statusCombo.getSelectedItem();

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

            return;
        }

        // Find selected doctor's ID
        String selectedDoctorId = "";

        for (User doctor : doctors) {
            if (doctor.getFullName().equals(selectedDoctorName)) {
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

            return;
        }

        // Find manager assigned to selected doctor
        String managerId = "";

        for (int i = 0; i < doctorIds.size(); i++) {
            if (doctorIds.get(i).equals(selectedDoctorId)) {
                managerId = managerIds.get(i);
                break;
            }
        }

        if (managerId.isEmpty()) {
            JOptionPane.showMessageDialog(
                    panel,
                    "The selected doctor does not have a manager assignment.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // Find manager name
        String managerName = "";

        for (User user : users) {
            if (user.getUserId().equals(managerId)) {
                managerName = user.getFullName();
                break;
            }
        }

        if (managerName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    panel,
                    "The assigned manager could not be found.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // Generate roster ID
        String rosterId = IDGenerator.next("R", fileName);

        // Create roster record
        String normalizedRecord = String.join("|",
                rosterId,
                selectedDoctorName,
                managerName,
                department,
                date,
                shift,
                status
        );

        // Verify save
        FileManager.appendLine(fileName, normalizedRecord);

        List<String> linesAfterSave = FileManager.readLines(fileName);

        boolean saved = !linesAfterSave.isEmpty()
                && linesAfterSave.get(linesAfterSave.size() - 1)
                        .equals(normalizedRecord);

        if (!saved) {
            JOptionPane.showMessageDialog(
                    panel,
                    "The roster could not be saved.",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        panel.refreshTable();
    }
    
    
    public String editRosterRecord(String record) {
        String[] parts = record.split("\\|");

        if (parts.length < 7) {
            return null;
        }

        String rosterId = parts[0].trim();
        String doctorName = parts[1].trim();
        String managerName = parts[2].trim();
        String currentDepartment = parts[3].trim();
        String currentDate = parts[4].trim();
        String currentShift = parts[5].trim();
        String currentStatus = parts[6].trim();

        // Department
        JComboBox<String> departmentCombo = new JComboBox<>();

        List<String> departments = FileManager.readLines("department.txt");

        for (int i = 1; i < departments.size(); i++) {
            String department = departments.get(i);

            if (department == null || department.trim().isEmpty()) {
                continue;
            }

            String[] departmentParts = department.split("\\|");

            if (departmentParts.length >= 2) {
                departmentCombo.addItem(departmentParts[1].trim());
            }
        }

        departmentCombo.setSelectedItem(currentDepartment);

        // Date
        java.util.Date parsedDate;

        try {
            parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .parse(currentDate);
        } catch (java.text.ParseException e) {
            parsedDate = new java.util.Date();
        }

        SpinnerDateModel dateModel = new SpinnerDateModel(
                parsedDate,
                null,
                null,
                java.util.Calendar.DAY_OF_MONTH
        );

        JSpinner dateSpinner = new JSpinner(dateModel);

        JSpinner.DateEditor dateEditor =
                new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");

        dateSpinner.setEditor(dateEditor);

        // Shift
        JComboBox<String> shiftCombo = new JComboBox<>();
        shiftCombo.addItem("Morning");
        shiftCombo.addItem("Afternoon");
        shiftCombo.addItem("Night");
        shiftCombo.setSelectedItem(currentShift);

        // Status
        JComboBox<String> statusCombo = new JComboBox<>();
        statusCombo.addItem("Scheduled");
        statusCombo.addItem("Completed");
        statusCombo.addItem("Cancelled");
        statusCombo.setSelectedItem(currentStatus);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));

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
                "Edit Roster",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String department =
                (String) departmentCombo.getSelectedItem();

        java.util.Date selectedDate =
                (java.util.Date) dateSpinner.getValue();

        if (selectedDate == null) {
            JOptionPane.showMessageDialog(
                    panel,
                    "Please select a date.",
                    "Invalid Roster",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        String date = new java.text.SimpleDateFormat("yyyy-MM-dd")
                .format(selectedDate);

        String shift =
                (String) shiftCombo.getSelectedItem();

        String status =
                (String) statusCombo.getSelectedItem();

        if (department == null || department.isEmpty()
                || shift == null || shift.isEmpty()
                || status == null || status.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please fill in all roster fields.",
                    "Invalid Roster",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }
        
        return String.join("|",
                rosterId,
                doctorName,
                managerName,
                department,
                date,
                shift,
                status
        );
    }

    
    // Report Methods
    public void populateReportTable(ReportData reportData) {
        String currentPeriod = java.time.YearMonth.now().toString();

        List<String> reportRecords = FileManager.readLines(fileName);

        boolean currentMonthExists = false;

        for (String record : reportRecords) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = ManageRecordsHelper.splitRecord(record);

            if (parts.length >= 6
                    && currentPeriod.equals(parts[0].trim())) {
                currentMonthExists = true;
                break;
            }
        }

        if (!currentMonthExists) {
            String newRecord = String.join("|",
                    currentPeriod,
                    String.valueOf(reportData.getTotalPatients()),
                    String.valueOf(reportData.getAppointments()),
                    String.valueOf(reportData.getCompleted()),
                    String.valueOf(reportData.getCancelled()),
                    String.valueOf(reportData.getTotalRevenue())
            );

            FileManager.appendLine(fileName, newRecord);
        }
        
        panel.refreshTable();
    }
}