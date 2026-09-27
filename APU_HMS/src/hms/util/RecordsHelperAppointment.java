package hms.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import hms.role.Role;
import hms.role.User;

public final class RecordsHelperAppointment {
    private RecordsHelperAppointment() {
    }

    public static String editAppointmentRecord(Component comp, String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        String appointmentId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String date = parts[3].trim();
        String time = parts[4].trim();
        String status = parts[5].trim();

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

        return String.join("|",
                appointmentIdField.getText().trim(),
                patientIdField.getText().trim(),
                doctorIdField.getText().trim(),
                dateField.getText().trim(),
                timeField.getText().trim(),
                statusField.getSelectedItem().toString().trim()
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

        JTextField dateField = new JTextField(java.time.LocalDate.now().toString());
        JComboBox<String> timeCombo = new JComboBox<>();
        Runnable updateTimeSlots = () -> {
            timeCombo.removeAllItems();
            String selectedDoctorName = (String) doctorCombo.getSelectedItem();
            String selectedDate = dateField.getText().trim();

            if (selectedDoctorName == null || selectedDate.isEmpty()) return;

            String shiftRange = null;
            List<String> rosterLines = FileManager.readLines("roster.txt");
            for (int i = 1; i < rosterLines.size(); i++) {
                String line = rosterLines.get(i);
                if (line == null || line.trim().isEmpty()) continue;
                String[] parts = ManageRecordsHelper.splitRecord(line);
                if (parts.length >= 6) {
                    String docName = parts[1].trim();
                    String rosterDate = parts[4].trim();
                    String shift = parts[5].trim(); // e.g., "09:30 - 17:00"

                    if ((docName.equalsIgnoreCase(selectedDoctorName) || ManageRecordsHelper.findName(docName).equalsIgnoreCase(selectedDoctorName))
                            && rosterDate.equals(selectedDate)) {
                        shiftRange = shift;
                        break;
                    }
                }
            }

            if (shiftRange == null || !shiftRange.contains("-")) {
                timeCombo.addItem("No shift scheduled");
                return;
            }

            // Parse shift hours
            try {
                String[] times = shiftRange.split("-");
                LocalTime startTime = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime endTime = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("HH:mm"));

                // Find already booked times from records (bookings.txt)
                List<String> bookedTimes = new ArrayList<>();
                for (String rec : records) {
                    String[] p = ManageRecordsHelper.splitRecord(rec);
                    if (p.length >= 6) {
                        String bDoc = p[2].trim();
                        String bDate = p[3].trim();
                        String bTime = p[4].trim();
                        String bStatus = p[5].trim();

                        if ((bDoc.equalsIgnoreCase(selectedDoctorName) || ManageRecordsHelper.findName(bDoc).equalsIgnoreCase(selectedDoctorName))
                                && bDate.equals(selectedDate)
                                && !bStatus.equalsIgnoreCase("CANCELLED")) {
                            bookedTimes.add(bTime);
                        }
                    }
                }

                // Generate 30-minute slots
                LocalTime current = startTime;
                while (current.plusMinutes(30).compareTo(endTime) <= 0) {
                    String slotStr = current.format(DateTimeFormatter.ofPattern("HH:mm"));
                    if (bookedTimes.contains(slotStr)) {
                        timeCombo.addItem(slotStr + " (Booked)");
                    } else {
                        timeCombo.addItem(slotStr);
                    }
                    current = current.plusMinutes(30);
                }

            } catch (Exception ex) {
                timeCombo.addItem("Invalid Shift Format");
            }
        };

        // Custom Renderer to Grey out Booked slots
        timeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && value.toString().contains("(Booked)")) {
                    if (!isSelected) {
                        c.setBackground(Color.LIGHT_GRAY);
                        c.setForeground(Color.DARK_GRAY);
                    }
                    setEnabled(false);
                } else {
                    if (!isSelected) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                    setEnabled(true);
                }
                return c;
            }
        });

        // Trigger updates when doctor or date changes
        doctorCombo.addActionListener(e -> updateTimeSlots.run());
        dateField.addActionListener(e -> updateTimeSlots.run());
        updateTimeSlots.run(); // Initial population

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

        form.add(new JLabel("TIME (30-min slot):"));
        form.add(timeCombo);

        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

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
        
        // Clean up time string if it contains " (Booked)" label
        String rawTimeSelection = (String) timeCombo.getSelectedItem();
        if (rawTimeSelection == null || rawTimeSelection.contains("Booked") || rawTimeSelection.contains("Shift")) {
            JOptionPane.showMessageDialog(comp, "Please select a valid available time slot.", "Invalid Time", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String time = rawTimeSelection.trim();

        String status = (String) statusCombo.getSelectedItem();
        if (ManageRecordsHelper.hasIllegalChars(
                appointmentId,
                date,
                time,
                status)) {

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

        int patientIndex = patientCombo.getSelectedIndex();
        int doctorIndex = doctorCombo.getSelectedIndex();

        if (patientIndex < 0 || doctorIndex < 0) {
            return;
        }

        String patientId = patients.get(patientIndex).getUserId();
        String doctorId = doctors.get(doctorIndex).getUserId();

        // Prevent double-booking the same doctor at the same date/time
        for (String record : records) {
            String[] parts = ManageRecordsHelper.splitRecord(record);
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
                        status
                )
        );

        refreshAction.run();
    }

    public void addAppointmentRow(DefaultTableModel tableModel, String line, JComboBox<String> doctorSearchBox) {
        String[] parts = ManageRecordsHelper.splitRecord(line);
        if (parts.length < 6) {
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
                parts[5].trim()
        });
    }
    
    public static List<String> updateAppointmentStatus(Component comp, List<String> records,
            String appointmentId, String newStatus) {
        List<String> updatedLines = new java.util.ArrayList<>();
        boolean found = false;
        for (String record : records) {
            String[] parts = ManageRecordsHelper.splitRecord(record);
            if (parts.length >= 6 && parts[0].trim().equals(appointmentId)) {
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