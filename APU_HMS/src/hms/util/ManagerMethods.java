package hms.util;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import hms.gui.panels.ManageRecordsPanel;
import hms.role.Role;
import hms.role.User;

public class ManagerMethods {

    private final ManageRecordsPanel panel;
    private final String fileName;

    public ManagerMethods(ManageRecordsPanel panel, String fileName) {
        this.panel = panel;
        this.fileName = fileName;
    }

    // Clinical Department Methods
    private boolean departmentNameExists(String departmentName, String currentDeptId) {
        List<String> departmentRecords
                = FileManager.readLines("department.txt");

        // Skip header
        for (int i = 1; i < departmentRecords.size(); i++) {

            String record = departmentRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts
                    = ManageRecordsHelper.splitRecord(record);

            if (parts.length < 4) {
                continue;
            }

            String existingDeptId
                    = parts[0].trim();

            String existingDeptName
                    = parts[1].trim();

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

    private boolean managerAlreadyManagesDepartment(
            String managerId,
            String currentDeptId) {

        List<String> lines
                = FileManager.readLines(fileName);

        for (int i = 1; i < lines.size(); i++) {

            String line = lines.get(i);

            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts
                    = ManageRecordsHelper.splitRecord(line);

            if (parts.length < 4) {
                continue;
            }

            String existingDeptId
                    = parts[0].trim();

            String existingManagerId
                    = parts[3].trim();

            // When editing, ignore the current department.
            if (currentDeptId != null
                    && existingDeptId.equals(currentDeptId)) {
                continue;
            }

            if (existingManagerId.equals(managerId)) {
                return true;
            }
        }

        return false;
    }

    public String addDepartmentRow() {
        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();

        List<User> managers
                = UserRepository.loadAll();

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

        // Only include medical managers who are not
        // already assigned to a department.
        List<User> availableManagers = new ArrayList<>();

        for (User manager : medicalManagers) {

            if (!managerAlreadyManagesDepartment(
                    manager.getUserId(),
                    null)) {

                availableManagers.add(manager);
            }
        }

        if (availableManagers.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "All medical managers are already assigned to a department.",
                    "Cannot Add Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        JComboBox<String> managerCombo
                = new JComboBox<>();

        for (User manager : availableManagers) {

            managerCombo.addItem(
                    manager.getFullName()
            );
        }

        JPanel form
                = new JPanel(
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

        String deptName
                = nameField.getText().trim();

        String description
                = descriptionField.getText().trim();

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

        int selectedIndex
                = managerCombo.getSelectedIndex();

        if (selectedIndex < 0) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please select a medical manager.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        User selectedManager
                = availableManagers.get(selectedIndex);

        // Final check before saving.
        if (managerAlreadyManagesDepartment(
                selectedManager.getUserId(),
                null)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "This medical manager is already assigned to a department.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        String selectedManagerId
                = medicalManagers
                        .get(selectedIndex)
                        .getUserId();

        String deptId
                = IDGenerator.next("D", fileName);

        return String.join(
                "|",
                deptId,
                deptName,
                description,
                selectedManagerId
        );
    }

    public String editDepartmentRecord(String record) {
        String[] parts
                = ManageRecordsHelper.splitRecord(record);

        if (parts.length < 4) {
            return null;
        }

        String deptId
                = parts[0].trim();

        String deptName
                = parts[1].trim();

        String description
                = parts[2].trim();

        String existingManagerId
                = parts[3].trim();

        JTextField idField
                = new JTextField(deptId);

        panel.setUneditable(idField);

        JTextField nameField
                = new JTextField(deptName);

        JTextField descriptionField
                = new JTextField(description);

        List<User> managers
                = UserRepository.loadAll();

        List<User> headManagers
                = managers.stream()
                        .filter(user
                                -> user.getRole()
                        == Role.MEDICAL_MANAGER)
                        .toList();

        if (headManagers.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "No medical managers are available.",
                    "Cannot Edit Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        JComboBox<String> managerCombo
                = new JComboBox<>();

        int selectedManager
                = -1;

        // Show ALL medical managers.
        for (int index = 0;
                index < headManagers.size();
                index++) {

            User manager
                    = headManagers.get(index);

            managerCombo.addItem(
                    manager.getFullName()
            );

            if (manager.getUserId()
                    .equals(existingManagerId)) {

                selectedManager = index;
            }
        }

        if (selectedManager >= 0) {
            managerCombo.setSelectedIndex(
                    selectedManager
            );
        }

        JPanel form
                = new JPanel(
                        new GridLayout(4, 2, 8, 8)
                );

        form.add(new JLabel("DEPT_ID:"));
        form.add(idField);

        form.add(new JLabel("DEPT_NAME:"));
        form.add(nameField);

        form.add(new JLabel("HEAD_MANAGER_NAME:"));
        form.add(managerCombo);

        form.add(new JLabel("DESCRIPTION:"));
        form.add(descriptionField);

        int choice
                = JOptionPane.showConfirmDialog(
                        panel,
                        form,
                        "Edit Department",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String updatedDeptName
                = nameField.getText().trim();

        String updatedDescription
                = descriptionField.getText().trim();

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

        int selectedIndex
                = managerCombo.getSelectedIndex();

        if (selectedIndex < 0) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please select a medical manager.",
                    "Invalid Department",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        String selectedManagerId
                = headManagers
                        .get(selectedIndex)
                        .getUserId();

        // Check whether the selected manager
        // is already assigned to another department.
        if (managerAlreadyManagesDepartment(
                selectedManagerId,
                deptId)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "This medical manager is already assigned to another department.",
                    "Manager Already Assigned",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join(
                "|",
                deptId,
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

        List<String> rosterRecords
                = FileManager.readLines("roster.txt");

        // Skip header
        for (int i = 1; i < rosterRecords.size(); i++) {

            String record = rosterRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts
                    = record.split("\\|", -1);

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
        User loggedInManager
                = Session.getCurrentUser();

        if (loggedInManager == null) {

            JOptionPane.showMessageDialog(
                    panel,
                    "No user is currently logged in.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return null;
        }

        String loggedInManagerId
                = loggedInManager.getUserId();

        List<String> assignments
                = FileManager.readLines(
                        "doctor_manager_assignments.txt"
                );

        List<String> assignedDoctorIds
                = new ArrayList<>();

        for (String assignment : assignments) {

            if (assignment == null
                    || assignment.trim().isEmpty()) {
                continue;
            }

            String[] parts
                    = assignment.split("\\|");

            if (parts.length >= 2) {

                String doctorId
                        = parts[0].trim();

                String managerId
                        = parts[1].trim();

                if (managerId.equals(
                        loggedInManagerId)) {

                    assignedDoctorIds.add(
                            doctorId
                    );
                }
            }
        }

        List<User> users
                = UserRepository.loadAll();

        List<User> doctors
                = users.stream()
                        .filter(user
                                -> user.getRole()
                        == Role.DOCTOR)
                        .toList();

        JComboBox<String> doctorCombo
                = new JComboBox<>();

        for (String doctorId
                : assignedDoctorIds) {

            for (User doctor : doctors) {

                if (doctor.getUserId()
                        .equals(doctorId)) {

                    doctorCombo.addItem(
                            doctor.getFullName()
                    );

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

        String department = null;

        List<String> departments
                = FileManager.readLines(
                        "department.txt"
                );

        for (int i = 1;
                i < departments.size();
                i++) {

            String departmentRecord
                    = departments.get(i);

            if (departmentRecord == null
                    || departmentRecord.trim().isEmpty()) {
                continue;
            }

            String[] departmentParts
                    = ManageRecordsHelper.splitRecord(
                            departmentRecord
                    );

            if (departmentParts.length < 4) {
                continue;
            }

            String departmentName
                    = departmentParts[1].trim();

            String managerId
                    = departmentParts[3].trim();

            if (managerId.equals(
                    loggedInManagerId)) {

                department = departmentName;
                break;
            }
        }

        if (department == null
                || department.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "You are not assigned to any department.",
                    "Roster Error",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        JTextField departmentField
                = new JTextField(department);

        panel.setUneditable(
                departmentField
        );

        SpinnerDateModel dateModel
                = new SpinnerDateModel();

        JSpinner dateSpinner
                = new JSpinner(dateModel);

        JSpinner.DateEditor dateEditor
                = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");

        dateSpinner.setEditor(
                dateEditor
        );

        SpinnerDateModel startTimeModel
                = new SpinnerDateModel();

        JSpinner startTimeSpinner
                = new JSpinner(startTimeModel);

        JSpinner.DateEditor startTimeEditor
                = new JSpinner.DateEditor(
                        startTimeSpinner,
                        "HH:mm"
                );

        startTimeSpinner.setEditor(
                startTimeEditor
        );

        SpinnerDateModel endTimeModel
                = new SpinnerDateModel();

        JSpinner endTimeSpinner
                = new JSpinner(endTimeModel);

        JSpinner.DateEditor endTimeEditor
                = new JSpinner.DateEditor(
                        endTimeSpinner,
                        "HH:mm"
                );

        endTimeSpinner.setEditor(
                endTimeEditor
        );

        JComboBox<String> statusCombo
                = new JComboBox<>();

        statusCombo.addItem("Active");
        statusCombo.addItem("Inactive");

        JPanel form
                = new JPanel(
                        new GridLayout(6, 2, 8, 8)
                );

        form.add(
                new JLabel("DOCTOR_NAME:")
        );
        form.add(
                doctorCombo
        );

        form.add(
                new JLabel("DEPARTMENT:")
        );
        form.add(
                departmentField
        );

        form.add(
                new JLabel("DATE:")
        );
        form.add(
                dateSpinner
        );

        form.add(
                new JLabel("SHIFT START:")
        );
        form.add(
                startTimeSpinner
        );

        form.add(
                new JLabel("SHIFT END:")
        );
        form.add(
                endTimeSpinner
        );

        form.add(
                new JLabel("STATUS:")
        );
        form.add(
                statusCombo
        );

        int choice
                = JOptionPane.showConfirmDialog(
                        panel,
                        form,
                        "Add Roster",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String selectedDoctorName
                = (String) doctorCombo.getSelectedItem();

        String selectedDepartment
                = departmentField.getText().trim();

        java.util.Date selectedDate
                = (java.util.Date) dateSpinner.getValue();

        if (selectedDate == null) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please select a date.",
                    "Invalid Roster",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        String date
                = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd"
                ).format(selectedDate);

        java.util.Date selectedStartTime
                = (java.util.Date) startTimeSpinner.getValue();

        java.util.Date selectedEndTime
                = (java.util.Date) endTimeSpinner.getValue();

        java.text.SimpleDateFormat timeFormat
                = new java.text.SimpleDateFormat("HH:mm");

        String startTime
                = timeFormat.format(selectedStartTime);

        String endTime
                = timeFormat.format(selectedEndTime);

        String shift
                = startTime + " - " + endTime;

        String status
                = (String) statusCombo.getSelectedItem();

        if (selectedDoctorName == null
                || selectedDoctorName.isEmpty()
                || selectedDepartment.isEmpty()
                || date.isEmpty()
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

        String selectedDoctorId = "";

        for (User doctor : doctors) {

            if (doctor.getFullName().equals(
                    selectedDoctorName)
                    && assignedDoctorIds.contains(
                            doctor.getUserId())) {

                selectedDoctorId
                        = doctor.getUserId();

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

        String rosterId
                = IDGenerator.next("R", fileName);

        return String.join(
                "|",
                rosterId,
                selectedDoctorName,
                loggedInManager.getFullName(),
                selectedDepartment,
                date,
                shift,
                status
        );
    }

    public String editRosterRecord(String record) {

        String[] parts = record.split("\\|", -1);

        if (parts.length < 7) {
            return null;
        }

        User loggedInManager
                = Session.getCurrentUser();

        if (loggedInManager == null) {

            JOptionPane.showMessageDialog(
                    panel,
                    "No user is currently logged in.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return null;
        }

        String loggedInManagerId
                = loggedInManager.getUserId();

        String rosterId
                = parts[0].trim();

        String doctorName
                = parts[1].trim();

        String managerName
                = parts[2].trim();

        String date
                = parts[4].trim();

        String shift
                = parts[5].trim();

        String status
                = parts[6].trim();

        if (!managerName.equalsIgnoreCase(
                loggedInManager.getFullName().trim())) {

            JOptionPane.showMessageDialog(
                    panel,
                    "You can only edit rosters managed by the current signed-in user.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        String department = null;

        List<String> departments
                = FileManager.readLines(
                        "department.txt"
                );

        for (int i = 1;
                i < departments.size();
                i++) {

            String departmentRecord
                    = departments.get(i);

            if (departmentRecord == null
                    || departmentRecord.trim().isEmpty()) {
                continue;
            }

            String[] departmentParts
                    = ManageRecordsHelper.splitRecord(
                            departmentRecord
                    );

            if (departmentParts.length < 4) {
                continue;
            }

            String departmentName
                    = departmentParts[1].trim();

            String managerId
                    = departmentParts[3].trim();

            if (managerId.equals(
                    loggedInManagerId)) {

                department = departmentName;
                break;
            }
        }

        if (department == null
                || department.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "You are not assigned to any department.",
                    "Roster Error",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        JTextField departmentField
                = new JTextField(department);

        panel.setUneditable(
                departmentField
        );

        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        try {
            java.util.Date parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            model.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            model.setSelected(true);
        } catch (Exception e) {
            model.setSelected(true);
        }
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl effectiveDatePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        SpinnerDateModel startTimeModel
                = new SpinnerDateModel();

        JSpinner startTimeSpinner
                = new JSpinner(startTimeModel);

        JSpinner.DateEditor startTimeEditor
                = new JSpinner.DateEditor(
                        startTimeSpinner,
                        "HH:mm"
                );

        startTimeSpinner.setEditor(
                startTimeEditor
        );

        SpinnerDateModel endTimeModel
                = new SpinnerDateModel();

        JSpinner endTimeSpinner
                = new JSpinner(endTimeModel);

        JSpinner.DateEditor endTimeEditor
                = new JSpinner.DateEditor(
                        endTimeSpinner,
                        "HH:mm"
                );

        endTimeSpinner.setEditor(
                endTimeEditor
        );

        try {

            if (shift.contains(" - ")) {

                String[] shiftParts
                        = shift.split("\\s*-\\s*");

                if (shiftParts.length == 2) {

                    java.text.SimpleDateFormat timeFormat
                            = new java.text.SimpleDateFormat(
                                    "HH:mm"
                            );

                    java.util.Date startTime
                            = timeFormat.parse(
                                    shiftParts[0].trim()
                            );

                    java.util.Date endTime
                            = timeFormat.parse(
                                    shiftParts[1].trim()
                            );

                    startTimeSpinner.setValue(
                            startTime
                    );

                    endTimeSpinner.setValue(
                            endTime
                    );
                }
            }

        } catch (java.text.ParseException e) {

            JOptionPane.showMessageDialog(
                    panel,
                    "The existing shift time is invalid.",
                    "Roster Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return null;
        }

        JComboBox<String> statusField
                = new JComboBox<>(
                        new String[]{
                            "Active",
                            "Inactive"
                        }
                );

        statusField.setSelectedItem(
                status
        );

        JPanel form
                = new JPanel(
                        new GridLayout(5, 2, 8, 8)
                );

        form.add(
                new JLabel("DEPARTMENT:")
        );
        form.add(
                departmentField
        );

        form.add(new JLabel("DATE (YYYY-MM-DD):"));
        form.add(effectiveDatePicker);

        form.add(
                new JLabel("SHIFT START:")
        );
        form.add(
                startTimeSpinner
        );

        form.add(
                new JLabel("SHIFT END:")
        );
        form.add(
                endTimeSpinner
        );

        form.add(
                new JLabel("STATUS:")
        );
        form.add(
                statusField
        );

        int choice
                = JOptionPane.showConfirmDialog(
                        panel,
                        form,
                        "Edit Roster",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        java.util.Date selectedDateUtil = (java.util.Date) effectiveDatePicker.getModel().getValue();
        String updatedDate = selectedDateUtil != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDateUtil)
                : java.time.LocalDate.now().toString();

        java.util.Date selectedStartTime
                = (java.util.Date) startTimeSpinner.getValue();

        java.util.Date selectedEndTime
                = (java.util.Date) endTimeSpinner.getValue();

        if (updatedDate.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Please enter a date.",
                    "Invalid Roster",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (selectedStartTime == null || selectedEndTime == null) {
            JOptionPane.showMessageDialog(
                    panel,
                    "Please select both shift start and end times.",
                    "Invalid Roster",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (!selectedStartTime.before(
                selectedEndTime)) {

            JOptionPane.showMessageDialog(
                    panel,
                    "Shift end time must be later than shift start time.",
                    "Invalid Shift",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        java.text.SimpleDateFormat timeFormat
                = new java.text.SimpleDateFormat(
                        "HH:mm"
                );

        String startTime
                = timeFormat.format(selectedStartTime);

        String endTime
                = timeFormat.format(selectedEndTime);

        String updatedShift
                = startTime + " - " + endTime;

        String updatedStatus
                = statusField
                        .getSelectedItem()
                        .toString()
                        .trim();

        if (doctorShiftCheck(
                doctorName,
                updatedDate,
                rosterId)) {
            JOptionPane.showMessageDialog(
                    panel,
                    "This doctor already has a shift on "
                    + updatedDate
                    + ". A doctor can only have one shift per date.",
                    "Scheduling Conflict",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join(
                "|",
                rosterId,
                doctorName,
                loggedInManager.getFullName(),
                department,
                updatedDate,
                updatedShift,
                updatedStatus
        );
    }
}
