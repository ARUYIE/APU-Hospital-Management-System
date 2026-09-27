package hms.util;

import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import hms.role.Role;
import hms.role.User;

public class DoctorMethods {

    public static boolean visibleToCurrentDoctor(String doctorId) {
        User currentUser = Session.getCurrentUser();

        if (currentUser == null
                || currentUser.getRole() != Role.DOCTOR) {
            return true;
        }

        return currentUser.getUserId().equals(doctorId);
    }

    private static String findName(String userId) {
        List<User> users
                = UserRepository.loadAll();

        for (User user : users) {

            if (user.getUserId()
                    .equals(userId)) {

                return user.getFullName();
            }
        }

        return userId;
    }

    public static String editPrescriptionRecord(
            Component parent,
            String record,
            String fileName) {

        String[] parts = ManageRecordsHelper.splitRecord(record);

        if (parts.length < 8) {
            return null;
        }

        String prescriptionId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String medication = parts[3].trim();
        String dosage = parts[4].trim();
        String duration = parts[5].trim();
        String appointmentId = parts[6].trim(); // Appointment ID stored at index 6
        String status = parts[7].trim();

        JTextField prescriptionIdField = new JTextField(prescriptionId);
        setUneditable(prescriptionIdField);

        List<User> patients = UserRepository.loadAll().stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        JComboBox<String> patientCombo = new JComboBox<>();
        for (User patient : patients) {
            patientCombo.addItem(patient.getFullName());
        }
        patientCombo.setSelectedItem(findName(patientId));
        patientCombo.setEnabled(false); // Locked to record patient

        JTextField doctorIdField = new JTextField(findName(doctorId));
        setUneditable(doctorIdField);

        // Load appointments for linking
        List<String> appointmentLines = FileManager.readLines("bookings.txt");
        List<String> appointmentIds = new ArrayList<>();
        List<String> appointmentDisplayItems = new ArrayList<>();
        String preselectedItem = null;

        for (int i = 1; i < appointmentLines.size(); i++) {
            String line = appointmentLines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] aptParts = ManageRecordsHelper.splitRecord(line);
            if (aptParts.length >= 6) {
                String aptId = aptParts[0].trim();
                String patName = ManageRecordsHelper.findName(aptParts[1].trim());
                String docId = aptParts[2].trim();

                if (docId.equalsIgnoreCase(doctorId) || docId.equalsIgnoreCase(findName(doctorId))) {
                    String aptDate = aptParts[3].trim();
                    String aptTime = aptParts[4].trim();

                    appointmentIds.add(aptId);
                    String display = aptId + " - " + patName + " (" + aptDate + " " + aptTime + ")";
                    appointmentDisplayItems.add(display);

                    if (aptId.equals(appointmentId)) {
                        preselectedItem = display;
                    }
                }
            }
        }

        JComboBox<String> appointmentCombo = new JComboBox<>(appointmentDisplayItems.toArray(new String[0]));
        if (preselectedItem != null) {
            appointmentCombo.setSelectedItem(preselectedItem);
        }

        JTextField medicationField = new JTextField(medication);
        JComboBox<String> dosageCombo = new JComboBox<>(new String[]{"100mg", "200mg", "300mg", "400mg", "500mg"});
        dosageCombo.setSelectedItem(dosage);

        JComboBox<String> durationCombo = new JComboBox<>(new String[]{"1 day", "2 days", "3 days", "4 days", "5 days", "6 days", "7 days"});
        durationCombo.setSelectedItem(duration);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "COMPLETED", "CANCELLED"});
        statusCombo.setSelectedItem(status);

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        form.add(new JLabel("PRESCRIPTION_ID:"));
        form.add(prescriptionIdField);
        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);
        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);
        form.add(new JLabel("LINKED APPOINTMENT:"));
        form.add(appointmentCombo);
        form.add(new JLabel("MEDICATION:"));
        form.add(medicationField);
        form.add(new JLabel("DOSAGE:"));
        form.add(dosageCombo);
        form.add(new JLabel("DURATION:"));
        form.add(durationCombo);
        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Edit Prescription",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        int selectedIndex = appointmentCombo.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }

        String updatedAppointmentId = appointmentIds.get(selectedIndex);
        String updatedMedication = medicationField.getText().trim();
        String updatedDosage = (String) dosageCombo.getSelectedItem();
        String updatedDuration = (String) durationCombo.getSelectedItem();
        String updatedStatus = (String) statusCombo.getSelectedItem();

        if (updatedMedication.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Medication is required.", "Invalid Prescription", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return String.join("|",
                prescriptionIdField.getText().trim(),
                patientId,
                doctorId,
                updatedMedication,
                updatedDosage,
                updatedDuration,
                updatedAppointmentId, // Stored at index 6
                updatedStatus
        );
    }

    public static String addPrescriptionRecord(Component parent, String fileName) {
        User currentDoctor = Session.getCurrentUser();

        if (currentDoctor == null) {
            JOptionPane.showMessageDialog(parent, "No doctor is currently logged in.", "Cannot Add Prescription", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        List<String> appointmentLines = FileManager.readLines("bookings.txt");
        List<String> appointmentIds = new ArrayList<>();
        List<String> appointmentDisplayItems = new ArrayList<>();

        for (int i = 1; i < appointmentLines.size(); i++) {
            String line = appointmentLines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length >= 6) {
                String aptId = parts[0].trim();
                String patName = ManageRecordsHelper.findName(parts[1].trim());
                String docId = parts[2].trim();

                if (docId.equalsIgnoreCase(currentDoctor.getUserId()) || docId.equalsIgnoreCase(currentDoctor.getFullName())) {
                    appointmentIds.add(aptId);
                    appointmentDisplayItems.add(aptId + " - " + patName + " (" + parts[3].trim() + " " + parts[4].trim() + ")");
                }
            }
        }

        if (appointmentIds.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No appointments found to tie this prescription to.", "Cannot Add Prescription", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        JComboBox<String> appointmentCombo = new JComboBox<>(appointmentDisplayItems.toArray(new String[0]));
        JTextField doctorIdField = new JTextField(currentDoctor.getFullName());
        setUneditable(doctorIdField);
        JTextField medicationField = new JTextField();
        JComboBox<String> dosageCombo = new JComboBox<>(new String[]{"100mg", "200mg", "300mg", "400mg", "500mg"});
        JComboBox<String> durationCombo = new JComboBox<>(new String[]{"1 day", "2 days", "3 days", "4 days", "5 days", "6 days", "7 days"});
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "COMPLETED", "CANCELLED"});

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("LINKED APPOINTMENT:"));
        form.add(appointmentCombo);
        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);
        form.add(new JLabel("MEDICATION:"));
        form.add(medicationField);
        form.add(new JLabel("DOSAGE:"));
        form.add(dosageCombo);
        form.add(new JLabel("DURATION:"));
        form.add(durationCombo);
        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        int choice = JOptionPane.showConfirmDialog(parent, form, "Issue Prescription", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        int selectedIndex = appointmentCombo.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }

        String appointmentId = appointmentIds.get(selectedIndex);

        // Retrieve patient ID from booking
        String patientId = "";
        for (String line : appointmentLines) {
            String[] p = ManageRecordsHelper.splitRecord(line);
            if (p.length > 0 && p[0].trim().equals(appointmentId)) {
                patientId = p[1].trim();
                break;
            }
        }

        String medication = medicationField.getText().trim();
        String dosage = (String) dosageCombo.getSelectedItem();
        String duration = (String) durationCombo.getSelectedItem();
        String status = (String) statusCombo.getSelectedItem();

        if (medication.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Medication is required.", "Invalid Prescription", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return String.join("|",
                IDGenerator.next("RX", fileName),
                patientId,
                currentDoctor.getUserId(),
                medication,
                dosage,
                duration,
                appointmentId, // Appointment ID stored at index 6
                status
        );
    }

    private static boolean hasIllegalChars(
            String... values) {

        for (String value : values) {

            if (value == null) {
                continue;
            }

            if (value.contains("|")
                    || value.contains("\n")
                    || value.contains("\r")) {

                return true;
            }
        }

        return false;
    }

    private static boolean hasPrescriptionForMedication(
            String fileName,
            String patientId,
            String medication,
            String currentPrescriptionId) {

        List<String> records
                = FileManager.readLines(fileName);

        for (int i = 1; i < records.size(); i++) {

            String record = records.get(i);

            if (record == null
                    || record.trim().isEmpty()) {
                continue;
            }

            String[] parts
                    = ManageRecordsHelper.splitRecord(record);

            if (parts.length < 8) {
                continue;
            }

            String prescriptionId
                    = parts[0].trim();

            String existingPatientId
                    = parts[1].trim();

            String existingMedication
                    = parts[3].trim();

            if (currentPrescriptionId != null
                    && prescriptionId.equals(
                            currentPrescriptionId)) {

                continue;
            }

            if (existingPatientId.equals(patientId)
                    && existingMedication.equalsIgnoreCase(
                            medication)) {

                return true;
            }
        }

        return false;
    }

    public static String editLabRequestRecord(
            Component parent,
            String record,
            String fileName) {

        String[] parts = ManageRecordsHelper.splitRecord(record);

        if (parts.length < 8) {
            return null;
        }

        String requestId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String testType = parts[3].trim();
        String roomId = parts[4].trim();
        String dateRequested = parts[5].trim();
        String dateCompleted = parts[6].trim();
        String status = parts[7].trim();

        User currentUser = Session.getCurrentUser();
        boolean isDoctor = currentUser != null
                && currentUser.getRole() == Role.DOCTOR;

        JTextField requestIdField = new JTextField(requestId);
        setUneditable(requestIdField);

        List<User> patients = UserRepository.loadAll().stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        JComboBox<String> patientCombo = new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(patient.getFullName());
        }

        patientCombo.setSelectedItem(findName(patientId));

        if (!isDoctor) {
            patientCombo.setEnabled(false);
        }

        JTextField doctorIdField = new JTextField(findName(doctorId));
        setUneditable(doctorIdField);

        JComboBox<String> testTypeCombo = new JComboBox<>(new String[]{
            "Blood Test",
            "X-Ray",
            "MRI",
            "CT Scan",
            "Ultrasound",
            "Other Specialized Imaging"
        });

        testTypeCombo.setSelectedItem(testType);

        if (!isDoctor) {
            testTypeCombo.setEnabled(false);
        }

        List<String> roomIds = new ArrayList<>();
        JComboBox<String> roomCombo = new JComboBox<>();

        roomCombo.addItem("No room needed");

        boolean currentRoomStillListed = false;

        List<String> assetLines = FileManager.readLines("hospital_assets.txt");

        for (int i = 1; i < assetLines.size(); i++) {

            String assetLine = assetLines.get(i);

            if (assetLine == null || assetLine.trim().isEmpty()) {
                continue;
            }

            String[] assetParts = assetLine.split("\\|", -1);

            if (assetParts.length < 5) {
                continue;
            }

            String assetId = assetParts[0].trim();
            String assetRoomType = assetParts[1].trim();
            String assetRoomName = assetParts[2].trim();
            String assetStatus = assetParts[4].trim();

            boolean isThisRequestsCurrentRoom
                    = assetId.equals(roomId);

            boolean isEligibleRoomType
                    = "IMAGING_ROOM".equalsIgnoreCase(assetRoomType)
                    || "LAB".equalsIgnoreCase(assetRoomType)
                    || "OPERATION_THEATRE".equalsIgnoreCase(assetRoomType);

            boolean isAvailableEligibleRoom
                    = isEligibleRoomType
                    && "AVAILABLE".equalsIgnoreCase(assetStatus);

            if (isAvailableEligibleRoom || isThisRequestsCurrentRoom) {

                roomIds.add(assetId);

                roomCombo.addItem(
                        assetRoomName + " (" + assetId + ")"
                );

                if (isThisRequestsCurrentRoom) {
                    currentRoomStillListed = true;
                }
            }
        }

        if (!roomId.isEmpty() && currentRoomStillListed) {

            int roomIndex = roomIds.indexOf(roomId);

            if (roomIndex >= 0) {
                roomCombo.setSelectedIndex(roomIndex + 1);
            }
        }

        if (!isDoctor) {
            roomCombo.setEnabled(false);
        }

        JTextField dateRequestedField
                = new JTextField(dateRequested);

        if (!isDoctor) {
            setUneditable(dateRequestedField);
        }

        JComboBox<String> statusCombo = new JComboBox<>(
                new String[]{
                    "PENDING",
                    "IN_PROGRESS",
                    "COMPLETED"
                }
        );

        statusCombo.setSelectedItem(status);

        if (isDoctor) {
            statusCombo.setEnabled(false);
        }

        JTextField dateCompletedField
                = new JTextField(dateCompleted);

        if (isDoctor) {
            setUneditable(dateCompletedField);
        }

        JPanel form = new JPanel(
                new GridLayout(8, 2, 8, 8)
        );

        form.add(new JLabel("REQUEST_ID:"));
        form.add(requestIdField);

        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);

        form.add(new JLabel("TEST_TYPE:"));
        form.add(testTypeCombo);

        form.add(new JLabel("ROOM (if needed):"));
        form.add(roomCombo);

        form.add(new JLabel(
                isDoctor
                        ? "DATE_REQUESTED:"
                        : "DATE_REQUESTED (set by doctor):"
        ));
        form.add(dateRequestedField);

        form.add(new JLabel(
                isDoctor
                        ? "DATE_COMPLETED (set by admin):"
                        : "DATE_COMPLETED:"
        ));
        form.add(dateCompletedField);

        form.add(new JLabel(
                isDoctor
                        ? "STATUS (set by admin):"
                        : "STATUS:"
        ));
        form.add(statusCombo);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Edit Lab / Imaging Request",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        int selectedPatientIndex
                = patientCombo.getSelectedIndex();

        String updatedPatientId
                = (isDoctor && selectedPatientIndex >= 0)
                        ? patients.get(selectedPatientIndex).getUserId()
                        : patientId;

        String updatedTestType
                = isDoctor
                        ? (String) testTypeCombo.getSelectedItem()
                        : testType;

        String updatedDateRequested
                = isDoctor
                        ? dateRequestedField.getText().trim()
                        : dateRequested;

        int selectedRoomIndex
                = roomCombo.getSelectedIndex();

        String updatedRoomId
                = isDoctor
                        ? (selectedRoomIndex > 0
                                ? roomIds.get(selectedRoomIndex - 1)
                                : "")
                        : roomId;

        String updatedStatus
                = !isDoctor
                        ? (String) statusCombo.getSelectedItem()
                        : status;

        String updatedDateCompleted
                = !isDoctor
                        ? dateCompletedField.getText().trim()
                        : dateCompleted;

        if (isDoctor) {

            if (updatedDateRequested.isEmpty()) {

                JOptionPane.showMessageDialog(
                        parent,
                        "Date requested is required.",
                        "Invalid Request",
                        JOptionPane.WARNING_MESSAGE
                );

                return null;
            }

            try {
                java.time.LocalDate.parse(updatedDateRequested);
            } catch (java.time.format.DateTimeParseException exception) {

                JOptionPane.showMessageDialog(
                        parent,
                        "Date requested must be in YYYY-MM-DD format.",
                        "Invalid Request",
                        JOptionPane.WARNING_MESSAGE
                );

                return null;
            }
        }

        if (hasIllegalChars(
                updatedTestType,
                updatedRoomId,
                updatedDateRequested,
                updatedDateCompleted,
                updatedStatus)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Request",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join("|",
                requestIdField.getText().trim(),
                updatedPatientId,
                doctorId,
                updatedTestType,
                updatedRoomId,
                updatedDateRequested,
                updatedDateCompleted,
                updatedStatus
        );
    }

    public static String addLabRequestRecord(
            Component parent,
            String fileName) {

        User currentDoctor = Session.getCurrentUser();

        if (currentDoctor == null) {
            JOptionPane.showMessageDialog(
                    parent,
                    "No doctor is currently logged in.",
                    "Cannot Add Request",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        List<User> patients = UserRepository.loadAll()
                .stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        if (patients.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "There are no patients to request tests for.",
                    "Cannot Add Request",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        JComboBox<String> patientCombo
                = new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(
                    patient.getFullName()
            );
        }

        JTextField doctorIdField
                = new JTextField(
                        currentDoctor.getFullName()
                );

        setUneditable(doctorIdField);

        JComboBox<String> testTypeCombo
                = new JComboBox<>(
                        new String[]{
                            "Blood Test",
                            "X-Ray",
                            "MRI",
                            "CT Scan",
                            "Ultrasound",
                            "Other Specialized Imaging"
                        }
                );

        List<String> roomIds
                = new ArrayList<>();

        JComboBox<String> roomCombo
                = new JComboBox<>();

        roomCombo.addItem("No room needed");

        List<String> assetLines = FileManager.readLines("hospital_assets.txt");

        for (int i = 1; i < assetLines.size(); i++) {

            String assetLine = assetLines.get(i);

            if (assetLine == null || assetLine.trim().isEmpty()) {
                continue;
            }

            String[] assetParts = assetLine.split("\\|", -1);

            if (assetParts.length < 5) {
                continue;
            }

            String assetId = assetParts[0].trim();
            String assetRoomType = assetParts[1].trim();
            String assetRoomName = assetParts[2].trim();
            String assetStatus = assetParts[4].trim();

            boolean isEligibleRoomType
                    = "IMAGING_ROOM".equalsIgnoreCase(assetRoomType)
                    || "LAB".equalsIgnoreCase(assetRoomType)
                    || "OPERATION_THEATRE".equalsIgnoreCase(assetRoomType);

            if (isEligibleRoomType
                    && "AVAILABLE".equalsIgnoreCase(assetStatus)) {

                roomIds.add(assetId);

                roomCombo.addItem(
                        assetRoomName + " (" + assetId + ")"
                );
            }
        }

        JTextField statusField
                = new JTextField("PENDING");

        setUneditable(statusField);

        JTextField dateRequestedField
                = new JTextField(
                        LocalDate.now().toString()
                );

        JPanel form
                = new JPanel(
                        new GridLayout(6, 2, 8, 8)
                );

        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);

        form.add(new JLabel("TEST_TYPE:"));
        form.add(testTypeCombo);

        form.add(
                new JLabel(
                        "ROOM (if imaging needed):"
                )
        );
        form.add(roomCombo);

        form.add(new JLabel("STATUS:"));
        form.add(statusField);

        form.add(
                new JLabel(
                        "DATE_REQUESTED (YYYY-MM-DD):"
                )
        );
        form.add(dateRequestedField);

        int choice
                = JOptionPane.showConfirmDialog(
                        parent,
                        form,
                        "Request Lab Test / Imaging",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String testType
                = (String) testTypeCombo.getSelectedItem();

        String dateRequested
                = dateRequestedField
                        .getText()
                        .trim();

        if (dateRequested.isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Date requested is required.",
                    "Invalid Request",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        try {

            LocalDate.parse(dateRequested);

        } catch (DateTimeParseException exception) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Date requested must be in YYYY-MM-DD format.",
                    "Invalid Request",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        int selectedRoomIndex
                = roomCombo.getSelectedIndex();

        String roomId
                = selectedRoomIndex > 0
                        ? roomIds.get(selectedRoomIndex - 1)
                        : "";

        if (hasIllegalChars(
                testType,
                dateRequested,
                roomId)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Request",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        User selectedPatient
                = patients.get(
                        patientCombo.getSelectedIndex()
                );

        String patientId
                = selectedPatient.getUserId();

        return String.join(
                "|",
                IDGenerator.next(
                        "LR",
                        fileName
                ),
                patientId,
                currentDoctor.getUserId(),
                testType,
                roomId,
                dateRequested,
                "",
                "PENDING"
        );
    }

    public static String editVitalSignRecord(
            Component parent,
            String record,
            String fileName) {

        String[] parts = ManageRecordsHelper.splitRecord(record);

        if (parts.length < 9) {
            return null;
        }

        String vitalSignId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String consultationId = parts[3].trim(); // Holds APPOINTMENT_ID
        String bp = parts[4].trim();
        String heartRate = parts[5].trim();
        String temperature = parts[6].trim();
        String date = parts[7].trim();
        String notes = parts[8].trim();

        User currentDoctor = Session.getCurrentUser();
        JTextField vitalSignIdField = new JTextField(vitalSignId);
        setUneditable(vitalSignIdField);

        List<User> patients = UserRepository.loadAll().stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        JComboBox<String> patientCombo = new JComboBox<>();
        for (User patient : patients) {
            patientCombo.addItem(patient.getFullName());
        }
        patientCombo.setSelectedItem(findName(patientId));
        patientCombo.setEnabled(false); // Locked to record patient

        JTextField doctorIdField = new JTextField(findName(doctorId));
        setUneditable(doctorIdField);

        // Load appointments for linking
        List<String> appointmentLines = FileManager.readLines("bookings.txt");
        List<String> appointmentIds = new ArrayList<>();
        List<String> appointmentDisplayItems = new ArrayList<>();
        String preselectedItem = null;

        for (int i = 1; i < appointmentLines.size(); i++) {
            String line = appointmentLines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] aptParts = ManageRecordsHelper.splitRecord(line);
            if (aptParts.length >= 6) {
                String aptId = aptParts[0].trim();
                String patName = ManageRecordsHelper.findName(aptParts[1].trim());
                String docId = aptParts[2].trim();

                if (docId.equalsIgnoreCase(doctorId) || docId.equalsIgnoreCase(findName(doctorId))) {
                    String aptDate = aptParts[3].trim();
                    String aptTime = aptParts[4].trim();

                    appointmentIds.add(aptId);
                    String display = aptId + " - " + patName + " (" + aptDate + " " + aptTime + ")";
                    appointmentDisplayItems.add(display);

                    if (aptId.equals(consultationId)) {
                        preselectedItem = display;
                    }
                }
            }
        }

        JComboBox<String> appointmentCombo = new JComboBox<>(appointmentDisplayItems.toArray(new String[0]));
        if (preselectedItem != null) {
            appointmentCombo.setSelectedItem(preselectedItem);
        }

        JTextField bpField = new JTextField(bp);
        JTextField heartRateField = new JTextField(heartRate);
        JTextField temperatureField = new JTextField(temperature);
        JTextField dateField = new JTextField(date);
        JTextField notesField = new JTextField(notes);

        JPanel form = new JPanel(new GridLayout(9, 2, 8, 8));
        form.add(new JLabel("VITAL_SIGN_ID:"));
        form.add(vitalSignIdField);
        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);
        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);
        form.add(new JLabel("LINKED APPOINTMENT:"));
        form.add(appointmentCombo);
        form.add(new JLabel("BP:"));
        form.add(bpField);
        form.add(new JLabel("HEART_RATE:"));
        form.add(heartRateField);
        form.add(new JLabel("TEMPERATURE:"));
        form.add(temperatureField);
        form.add(new JLabel("DATE:"));
        form.add(dateField);
        form.add(new JLabel("NOTES:"));
        form.add(notesField);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Edit Vital Sign Record",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        int selectedIndex = appointmentCombo.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }

        String updatedConsultationId = appointmentIds.get(selectedIndex);
        String updatedBp = bpField.getText().trim();
        String updatedHeartRate = heartRateField.getText().trim();
        String updatedTemperature = temperatureField.getText().trim();
        String updatedDate = dateField.getText().trim();
        String updatedNotes = notesField.getText().trim();

        if (updatedBp.isEmpty() || updatedHeartRate.isEmpty() || updatedTemperature.isEmpty() || updatedDate.isEmpty() || updatedNotes.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "All fields are required.", "Invalid Vital Sign Record", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return String.join("|",
                vitalSignIdField.getText().trim(),
                patientId,
                doctorId,
                updatedConsultationId,
                updatedBp,
                updatedHeartRate,
                updatedTemperature,
                updatedDate,
                updatedNotes
        );
    }

    public static String addVitalSignRecord(Component parent, String fileName) {
        User currentDoctor = Session.getCurrentUser();

        if (currentDoctor == null) {
            JOptionPane.showMessageDialog(
                    parent,
                    "No doctor is currently logged in.",
                    "Cannot Add Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        // Load existing appointments to hook CONSULTATION_ID to APPOINTMENT_ID
        List<String> appointmentLines = FileManager.readLines("bookings.txt");
        List<String> appointmentIds = new ArrayList<>();
        List<String> appointmentDisplayItems = new ArrayList<>();

        for (int i = 1; i < appointmentLines.size(); i++) {
            String line = appointmentLines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length >= 6) {
                String aptId = parts[0].trim();
                String patName = ManageRecordsHelper.findName(parts[1].trim());
                String docId = parts[2].trim();

                if (docId.equalsIgnoreCase(currentDoctor.getUserId()) || docId.equalsIgnoreCase(currentDoctor.getFullName())) {
                    String aptDate = parts[3].trim();
                    String aptTime = parts[4].trim();

                    appointmentIds.add(aptId);
                    appointmentDisplayItems.add(aptId + " - " + patName + " (" + aptDate + " " + aptTime + ")");
                }
            }
        }

        if (appointmentIds.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "No valid appointments found for this doctor to link this consultation.",
                    "Cannot Add Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        JComboBox<String> appointmentCombo = new JComboBox<>(appointmentDisplayItems.toArray(new String[0]));
        JTextField bpField = new JTextField();
        JTextField heartRateField = new JTextField();
        JTextField temperatureField = new JTextField();
        JTextField dateField = new JTextField(LocalDate.now().toString());
        JTextField notesField = new JTextField();

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        form.add(new JLabel("LINKED APPOINTMENT:"));
        form.add(appointmentCombo);
        form.add(new JLabel("BP:"));
        form.add(bpField);
        form.add(new JLabel("HEART_RATE:"));
        form.add(heartRateField);
        form.add(new JLabel("TEMPERATURE:"));
        form.add(temperatureField);
        form.add(new JLabel("DATE:"));
        form.add(dateField);
        form.add(new JLabel("NOTES (symptoms / observations / diagnosis):"));
        form.add(notesField);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Add Vital Sign Record",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        int selectedIndex = appointmentCombo.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }

        String consultationId = appointmentIds.get(selectedIndex); // Stores APPOINTMENT_ID

        // Fetch patient ID from the selected booking line
        String selectedAptLine = "";
        for (String line : appointmentLines) {
            String[] p = ManageRecordsHelper.splitRecord(line);
            if (p.length > 0 && p[0].trim().equals(consultationId)) {
                selectedAptLine = line;
                break;
            }
        }

        String[] aptParts = ManageRecordsHelper.splitRecord(selectedAptLine);
        String patientId = aptParts.length >= 2 ? aptParts[1].trim() : "";

        String bp = bpField.getText().trim();
        String heartRate = heartRateField.getText().trim();
        String temperature = temperatureField.getText().trim();
        String date = dateField.getText().trim();
        String notes = notesField.getText().trim();

        if (bp.isEmpty() || heartRate.isEmpty() || temperature.isEmpty() || date.isEmpty() || notes.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "All fields are required.", "Invalid Vital Sign Record", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return String.join("|",
                IDGenerator.next("VS", fileName),
                patientId,
                currentDoctor.getUserId(),
                consultationId,
                bp,
                heartRate,
                temperature,
                date,
                notes
        );
    }

    private static boolean hasVitalSignOnDate(
            String fileName,
            String patientId,
            String date,
            String currentRecordId) {

        List<String> records
                = FileManager.readLines(fileName);

        for (int i = 1; i < records.size(); i++) {

            String record = records.get(i);

            if (record == null
                    || record.trim().isEmpty()) {
                continue;
            }

            String[] parts
                    = ManageRecordsHelper.splitRecord(
                            record
                    );

            if (parts.length < 9) {
                continue;
            }

            String recordId
                    = parts[0].trim();

            String existingPatientId
                    = parts[1].trim();

            String existingDate
                    = parts[7].trim();

            if (currentRecordId != null
                    && recordId.equals(
                            currentRecordId)) {
                continue;
            }

            if (existingPatientId.equals(patientId)
                    && existingDate.equals(date)) {

                return true;
            }
        }

        return false;
    }

    private static void setUneditable(
            JTextField field) {

        field.setEditable(false);
        field.setFocusable(false);
    }
}
