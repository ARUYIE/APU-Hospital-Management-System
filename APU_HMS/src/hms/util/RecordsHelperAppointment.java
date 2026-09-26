package hms.util;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import hms.role.Role;
import hms.role.User;
import java.util.ArrayList;

public final class RecordsHelperAppointment {
    private RecordsHelperAppointment() {
    }

    public static String editAppointmentRecord(Component comp, String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);
        if (parts.length < 7) {
            return null;
        }

        String appointmentId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String date = parts[3].trim();
        String time = parts[4].trim();
        String status = parts[5].trim();
        String serviceType = parts[6].trim();

        JTextField appointmentIdField = new JTextField(appointmentId);
        setUneditable(appointmentIdField);

        JTextField patientIdField = new JTextField(patientId);
        JTextField doctorIdField = new JTextField(doctorId);
        JTextField dateField = new JTextField(date);
        JTextField timeField = new JTextField(time);

        JComboBox<String> statusField = new JComboBox<>(
                new String[]{"SCHEDULED", "COMPLETED", "CANCELLED"}
        );

        statusField.setSelectedItem(status);

        // Load existing departments
        List<String> departmentRecords = FileManager.readLines("department.txt");
        List<String> departments = new ArrayList<>();

        for (int i = 1; i < departmentRecords.size(); i++) {
            String departmentRecord = departmentRecords.get(i);

            if (departmentRecord == null || departmentRecord.trim().isEmpty()) {
                continue;
            }

            String[] departmentParts =
                    ManageRecordsHelper.splitRecord(departmentRecord);

            if (departmentParts.length < 2) {
                continue;
            }

            String departmentName = departmentParts[1].trim();

            if (!departmentName.isEmpty()) {
                departments.add(departmentName);
            }
        }

        JComboBox<String> serviceTypeField =
                new JComboBox<>(departments.toArray(new String[0]));

        if (departments.contains(serviceType)) {
            serviceTypeField.setSelectedItem(serviceType);
        }

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));

        form.add(new JLabel("APPOINTMENT_ID:"));
        form.add(appointmentIdField);

        form.add(new JLabel("PATIENT_ID:"));
        form.add(patientIdField);

        form.add(new JLabel("DOCTOR_ID:"));
        form.add(doctorIdField);

        form.add(new JLabel("DATE:"));
        form.add(dateField);

        form.add(new JLabel("TIME:"));
        form.add(timeField);

        form.add(new JLabel("STATUS:"));
        form.add(statusField);

        form.add(new JLabel("SERVICE_TYPE:"));
        form.add(serviceTypeField);

        int choice = JOptionPane.showConfirmDialog(
                comp,
                form,
                "Edit Appointment",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String selectedServiceType =
                serviceTypeField.getSelectedItem() == null
                        ? ""
                        : serviceTypeField.getSelectedItem().toString().trim();

        return String.join("|",
                appointmentIdField.getText().trim(),
                patientIdField.getText().trim(),
                doctorIdField.getText().trim(),
                dateField.getText().trim(),
                timeField.getText().trim(),
                statusField.getSelectedItem().toString().trim(),
                selectedServiceType
        );
    }


    public static void addAppointmentRecord(Component comp, String fileName,
            List<String> records, Runnable refreshAction) {

        List<User> users = UserRepository.loadAll();

        List<User> patients = users.stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        List<User> doctors = users.stream()
                .filter(user -> user.getRole() == Role.DOCTOR)
                .toList();

        if (patients.isEmpty() || doctors.isEmpty()) {
            JOptionPane.showMessageDialog(
                    comp,
                    "At least one patient and one doctor must exist before adding an appointment.",
                    "Cannot Add Appointment",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JComboBox<String> patientCombo = new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(patient.getFullName());
        }

        JComboBox<String> doctorCombo = new JComboBox<>();

        for (User doctor : doctors) {
            doctorCombo.addItem(doctor.getFullName());
        }

        // Load existing departments
        List<String> departmentRecords = FileManager.readLines("department.txt");
        List<String> departments = new ArrayList<>();

        for (int i = 1; i < departmentRecords.size(); i++) {
            String departmentRecord = departmentRecords.get(i);

            if (departmentRecord == null || departmentRecord.trim().isEmpty()) {
                continue;
            }

            String[] departmentParts =
                    ManageRecordsHelper.splitRecord(departmentRecord);

            if (departmentParts.length < 2) {
                continue;
            }

            String departmentName = departmentParts[1].trim();

            if (!departmentName.isEmpty()) {
                departments.add(departmentName);
            }
        }

        if (departments.isEmpty()) {
            JOptionPane.showMessageDialog(
                    comp,
                    "At least one department must exist before adding an appointment.",
                    "Cannot Add Appointment",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JComboBox<String> serviceTypeCombo =
                new JComboBox<>(departments.toArray(new String[0]));

        JTextField dateField =
                new JTextField(java.time.LocalDate.now().toString());

        JTextField timeField = new JTextField("09:00");

        JComboBox<String> statusCombo = new JComboBox<>(
                new String[]{"SCHEDULED", "COMPLETED", "CANCELLED"}
        );

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));

        form.add(new JLabel("PATIENT_NAME:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR_NAME:"));
        form.add(doctorCombo);

        form.add(new JLabel("DATE (YYYY-MM-DD):"));
        form.add(dateField);

        form.add(new JLabel("TIME (HH:mm):"));
        form.add(timeField);

        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        form.add(new JLabel("SERVICE_TYPE:"));
        form.add(serviceTypeCombo);

        int choice = JOptionPane.showConfirmDialog(
                comp,
                form,
                "Add Appointment",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String appointmentId = IDGenerator.next("B", fileName);
        String date = dateField.getText().trim();
        String time = timeField.getText().trim();

        String status = (String) statusCombo.getSelectedItem();

        String serviceType = serviceTypeCombo.getSelectedItem() == null
                ? ""
                : serviceTypeCombo.getSelectedItem().toString().trim();

        if (ManageRecordsHelper.hasIllegalChars(
                appointmentId,
                date,
                time,
                serviceType)) {

            JOptionPane.showMessageDialog(
                    comp,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Appointment",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(
                    comp,
                    "Date must be in YYYY-MM-DD format.",
                    "Invalid Appointment",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalTime.parse(
                    time,
                    DateTimeFormatter.ofPattern("HH:mm")
            );
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(
                    comp,
                    "Time must be in HH:mm format (e.g. 09:30).",
                    "Invalid Appointment",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int patientIndex = patientCombo.getSelectedIndex();
        int doctorIndex = doctorCombo.getSelectedIndex();

        if (patientIndex < 0 || doctorIndex < 0) {
            return;
        }

        String patientId =
                patients.get(patientIndex).getUserId();

        String doctorId =
                doctors.get(doctorIndex).getUserId();

        // Prevent double-booking the same doctor at the same date/time
        for (String record : records) {

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length >= 6
                    && parts[2].trim().equals(doctorId)
                    && parts[3].trim().equals(date)
                    && parts[4].trim().equals(time)
                    && !"CANCELLED".equalsIgnoreCase(parts[5].trim())) {

                JOptionPane.showMessageDialog(
                        comp,
                        "This doctor already has an appointment at that date and time.",
                        "Scheduling Conflict",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        FileManager.appendLine(
                fileName,
                String.join("|",
                        appointmentId,
                        patientId,
                        doctorId,
                        date,
                        time,
                        status,
                        serviceType
                )
        );

        refreshAction.run();
    }

    public void addAppointmentRow(DefaultTableModel tableModel, String line,JComboBox<String> doctorSearchBox) {
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length < 7) {
                return;
            }
            
            String doctorName = ManageRecordsHelper.findName(parts[2].trim());
            String selectedDoctor = (String) doctorSearchBox.getSelectedItem();
            if (selectedDoctor != null && !selectedDoctor.equals("All Doctors") && !selectedDoctor.equals("Doctor Name")) {
                if (!doctorName.equals(selectedDoctor)) {
                    return;
                }
            }

            tableModel.addRow(new Object[]{
                    parts[0].trim(),
                    ManageRecordsHelper.findName(parts[1].trim()),
                    doctorName,
                    parts[3].trim(),
                    parts[4].trim(),
                    parts[5].trim(),
                    parts[6].trim()
            });
        }
    
    public static List<String> updateAppointmentStatus(Component comp, List<String> records,
            String appointmentId, String newStatus) {
        List<String> updatedLines = new java.util.ArrayList<>();
        boolean found = false;
        for (String record : records) {
            String[] parts = ManageRecordsHelper.splitRecord(record);
            if (parts.length >= 7 && parts[0].trim().equals(appointmentId)) {
                found = true;
                parts[5] = newStatus;
                updatedLines.add(String.join("|", parts));
            } else {
                updatedLines.add(record);
            }
        }

        if (!found) {
            JOptionPane.showMessageDialog(comp,
                    "The selected appointment could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return updatedLines;
    }

    public static void populateDoctorSearchBox(JComboBox<String> doctorSearchBox) {
        doctorSearchBox.addItem("All Doctors");
        for (User user : UserRepository.loadAll()) {
            if (user.getRole() == Role.DOCTOR) {
                doctorSearchBox.addItem(user.getFullName());
            }
        }
    }

    private static void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(Color.LIGHT_GRAY);
    }
}
