package hms.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

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

        UtilDateModel model = new UtilDateModel();
        try {
            java.util.Date parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(parsedDate);
            model.setDate(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DATE));
            model.setSelected(true);
        } catch (Exception e) {
            model.setSelected(true); // Fallback if parsing fails
        }

        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        JTextField timeField = new JTextField(time);

        JComboBox<String> statusField = new JComboBox<>(
                new String[]{"SCHEDULED", "COMPLETED", "CANCELLED"}
        );

        statusField.setSelectedItem(status);

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));

        form.add(new JLabel("APPOINTMENT_ID:"));
        form.add(appointmentIdField);

        form.add(new JLabel("PATIENT_ID:"));
        form.add(patientIdField);

        form.add(new JLabel("DOCTOR_ID:"));
        form.add(doctorIdField);

        form.add(new JLabel("DATE (YYYY-MM-DD):"));
        form.add(datePicker);

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

        java.util.Date selectedDateUtil = (java.util.Date) datePicker.getModel().getValue();
        String dateStr = selectedDateUtil != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDateUtil)
                : LocalDate.now().toString();

        return String.join("|",
                appointmentIdField.getText().trim(),
                patientIdField.getText().trim(),
                doctorIdField.getText().trim(),
                dateStr,
                timeField.getText().trim(),
                statusField.getSelectedItem().toString().trim()
        );
    }

    public static void addAppointmentRecord(Component comp, String fileName,
            List<String> records, Runnable refreshAction) {

        List<User> users = UserRepository.loadAll();
        User currentUser = Session.getCurrentUser();
        boolean isPatient = (currentUser != null && currentUser.getRole() == Role.PATIENT);

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
        if (isPatient) {
            patientCombo.addItem(currentUser.getFullName());
            patientCombo.setEnabled(false);
            patientCombo.setBackground(Color.LIGHT_GRAY);
        } else {
            for (User patient : patients) {
                patientCombo.addItem(patient.getFullName());
            }
        }

        JComboBox<String> doctorCombo = new JComboBox<>();
        for (User doctor : doctors) {
            doctorCombo.addItem(doctor.getFullName());
        }

        // --- JDatePicker Setup ---
        UtilDateModel model = new UtilDateModel();
        model.setSelected(true);
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        JComboBox<String> timeCombo = new JComboBox<>();
        Runnable updateTimeSlots = () -> {
            timeCombo.removeAllItems();
            String selectedDoctorName = (String) doctorCombo.getSelectedItem();

            java.util.Date selectedDateUtil = (java.util.Date) datePicker.getModel().getValue();
            String selectedDate = selectedDateUtil != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDateUtil) : "";

            if (selectedDoctorName == null) {
                return;
            }

            // --- IGNORING ROSTER DATE: Find the doctor's shift time slots regardless of date ---
            String shiftRange = null;
            List<String> rosterLines = FileManager.readLines("roster.txt");
            for (int i = 1; i < rosterLines.size(); i++) {
                String line = rosterLines.get(i);
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = ManageRecordsHelper.splitRecord(line);
                if (parts.length >= 6) {
                    String docName = parts[1].trim();
                    String shift = parts[5].trim();

                    // Matches doctor name and grabs the shift time range completely ignoring roster date
                    if (docName.equalsIgnoreCase(selectedDoctorName) || ManageRecordsHelper.findName(docName).equalsIgnoreCase(selectedDoctorName)) {
                        shiftRange = shift;
                        break;
                    }
                }
            }

            if (shiftRange == null || !shiftRange.contains("-")) {
                timeCombo.addItem("No shift scheduled");
                return;
            }

            try {
                String[] times = shiftRange.split("-");
                LocalTime startTime = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime endTime = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("HH:mm"));

                List<String> bookedTimes = new ArrayList<>();
                for (String rec : records) {
                    String[] pr = ManageRecordsHelper.splitRecord(rec);
                    if (pr.length >= 6) {
                        String bDoc = pr[2].trim();
                        String bDate = pr[3].trim();
                        String bTime = pr[4].trim();
                        String bStatus = pr[5].trim();

                        // Check booked slots for this specific date and doctor
                        if ((bDoc.equalsIgnoreCase(selectedDoctorName) || ManageRecordsHelper.findName(bDoc).equalsIgnoreCase(selectedDoctorName))
                                && bDate.equals(selectedDate)
                                && !bStatus.equalsIgnoreCase("CANCELLED")) {
                            bookedTimes.add(bTime);
                        }
                    }
                }

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

        doctorCombo.addActionListener(e -> updateTimeSlots.run());
        model.addPropertyChangeListener(e -> {
            if ("value".equals(e.getPropertyName())) {
                updateTimeSlots.run();
            }
        });
        updateTimeSlots.run();

        JComboBox<String> statusCombo = new JComboBox<>(
                new String[]{"SCHEDULED", "COMPLETED", "CANCELLED"}
        );

        if (isPatient) {
            statusCombo.setSelectedItem("SCHEDULED");
            statusCombo.setEnabled(false);
            statusCombo.setBackground(Color.LIGHT_GRAY);
        }

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));

        form.add(new JLabel("PATIENT_NAME:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR_NAME:"));
        form.add(doctorCombo);

        form.add(new JLabel("DATE (YYYY-MM-DD):"));
        form.add(datePicker);

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

        java.util.Date selectedDateUtil = (java.util.Date) datePicker.getModel().getValue();
        String date = selectedDateUtil != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDateUtil) : LocalDate.now().toString();

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

        String patientId;
        if (isPatient) {
            patientId = currentUser.getUserId();
        } else {
            int patientIndex = patientCombo.getSelectedIndex();
            if (patientIndex < 0) {
                return;
            }
            patientId = patients.get(patientIndex).getUserId();
        }

        int doctorIndex = doctorCombo.getSelectedIndex();
        if (doctorIndex < 0) {
            return;
        }
        String doctorId = doctors.get(doctorIndex).getUserId();

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

    public static List<String> updateAppointmentStatus(Component comp, List<String> records, String appointmentId, String newStatus) {
        List<String> updatedLines = new java.util.ArrayList<>();
        boolean found = false;
        for (String record : records) {
            String[] parts = ManageRecordsHelper.splitRecord(record);
            if (parts.length >= 6 && parts[0].trim().equals(appointmentId)) {
                found = true;
                String oldStatus = parts[5].trim();
                parts[5] = newStatus;
                updatedLines.add(String.join("|", parts));

                if ("COMPLETED".equalsIgnoreCase(newStatus) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
                    String patientId = parts[1].trim();
                    String doctorId = parts[2].trim();
                    String appointmentDate = parts[3].trim();
                    BillingManager.generateBillForAppointment(parts[0].trim());
                }
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
