package hms.util;

import hms.role.Role;
import hms.role.User;
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
        List<User> users =
                UserRepository.loadAll();

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

        String[] parts =
                ManageRecordsHelper.splitRecord(record);

        if (parts.length < 8) {
            return null;
        }

        String prescriptionId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String medication = parts[3].trim();
        String dosage = parts[4].trim();
        String duration = parts[5].trim();
        String dateIssued = parts[6].trim();
        String status = parts[7].trim();

        JTextField prescriptionIdField =
                new JTextField(prescriptionId);

        setUneditable(prescriptionIdField);

        List<User> patients =
                UserRepository.loadAll()
                        .stream()
                        .filter(user ->
                                user.getRole() == Role.PATIENT)
                        .toList();

        JComboBox<String> patientCombo =
                new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(
                    patient.getFullName()
            );
        }

        patientCombo.setSelectedItem(
                findName(patientId)
        );

        JTextField doctorIdField =
                new JTextField(
                        findName(doctorId)
                );

        setUneditable(doctorIdField);

        JTextField medicationField =
                new JTextField(medication);

        JComboBox<String> dosageCombo =
                new JComboBox<>(
                        new String[]{
                            "100mg",
                            "200mg",
                            "300mg",
                            "400mg",
                            "500mg"
                        }
                );

        dosageCombo.setSelectedItem(dosage);

        JComboBox<String> durationCombo =
                new JComboBox<>(
                        new String[]{
                            "1 day",
                            "2 days",
                            "3 days",
                            "4 days",
                            "5 days",
                            "6 days",
                            "7 days"
                        }
                );

        durationCombo.setSelectedItem(duration);

        JTextField dateIssuedField =
                new JTextField(dateIssued);

        JComboBox<String> statusCombo =
                new JComboBox<>(
                        new String[]{
                            "ACTIVE",
                            "COMPLETED",
                            "CANCELLED"
                        }
                );

        statusCombo.setSelectedItem(status);

        JPanel form =
                new JPanel(
                        new GridLayout(8, 2, 8, 8)
                );

        form.add(
                new JLabel("PRESCRIPTION_ID:")
        );
        form.add(prescriptionIdField);

        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);

        form.add(new JLabel("MEDICATION:"));
        form.add(medicationField);

        form.add(new JLabel("DOSAGE:"));
        form.add(dosageCombo);

        form.add(new JLabel("DURATION:"));
        form.add(durationCombo);

        form.add(new JLabel("DATE_ISSUED:"));
        form.add(dateIssuedField);

        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        int choice =
                JOptionPane.showConfirmDialog(
                        parent,
                        form,
                        "Edit Prescription",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        int selectedPatientIndex =
                patientCombo.getSelectedIndex();

        String updatedPatientId =
                selectedPatientIndex >= 0
                ? patients
                        .get(selectedPatientIndex)
                        .getUserId()
                : patientId;

        String updatedMedication =
                medicationField
                        .getText()
                        .trim();

        String updatedDosage =
                (String) dosageCombo
                        .getSelectedItem();

        String updatedDuration =
                (String) durationCombo
                        .getSelectedItem();

        String updatedDateIssued =
                dateIssuedField
                        .getText()
                        .trim();

        String updatedStatus =
                (String) statusCombo
                        .getSelectedItem();

        if (updatedMedication.isEmpty()
                || updatedDateIssued.isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Medication and date issued are required.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (hasIllegalChars(
                updatedMedication,
                updatedDosage,
                updatedDuration,
                updatedDateIssued)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        try {

            LocalDate.parse(updatedDateIssued);

        } catch (DateTimeParseException exception) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Date issued must be in YYYY-MM-DD format.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (hasPrescriptionForMedication(
                fileName,
                updatedPatientId,
                updatedMedication,
                prescriptionId)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "This patient already has a prescription for "
                    + updatedMedication + ".",
                    "Duplicate Medication",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join(
                "|",
                prescriptionIdField
                        .getText()
                        .trim(),
                updatedPatientId,
                doctorId,
                updatedMedication,
                updatedDosage,
                updatedDuration,
                updatedDateIssued,
                updatedStatus
        );
    }
    
    public static String addPrescriptionRecord(Component parent, String fileName) {

        User currentDoctor = Session.getCurrentUser();

        if (currentDoctor == null) {
            JOptionPane.showMessageDialog(
                    parent,
                    "No doctor is currently logged in.",
                    "Cannot Add Prescription",
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
                    "There are no patients to prescribe medication for.",
                    "Cannot Add Prescription",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        JComboBox<String> patientCombo = new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(patient.getFullName());
        }

        JTextField doctorIdField =
                new JTextField(currentDoctor.getFullName());

        setUneditable(doctorIdField);

        JTextField medicationField =
                new JTextField();

        JComboBox<String> dosageCombo =
                new JComboBox<>(
                        new String[]{
                            "100mg",
                            "200mg",
                            "300mg",
                            "400mg",
                            "500mg"
                        }
                );

        JComboBox<String> durationCombo =
                new JComboBox<>(
                        new String[]{
                            "1 day",
                            "2 days",
                            "3 days",
                            "4 days",
                            "5 days",
                            "6 days",
                            "7 days"
                        }
                );

        JTextField dateIssuedField =
                new JTextField(LocalDate.now().toString());

        JComboBox<String> statusCombo =
                new JComboBox<>(
                        new String[]{
                            "ACTIVE",
                            "COMPLETED",
                            "CANCELLED"
                        }
                );

        JPanel form =
                new JPanel(
                        new GridLayout(7, 2, 8, 8)
                );

        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);

        form.add(new JLabel("MEDICATION:"));
        form.add(medicationField);

        form.add(new JLabel("DOSAGE:"));
        form.add(dosageCombo);

        form.add(new JLabel("DURATION:"));
        form.add(durationCombo);

        form.add(
                new JLabel(
                        "DATE_ISSUED (YYYY-MM-DD):"
                )
        );
        form.add(dateIssuedField);

        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Issue Prescription",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String medication =
                medicationField.getText().trim();

        String dosage =
                (String) dosageCombo.getSelectedItem();

        String duration =
                (String) durationCombo.getSelectedItem();

        String dateIssued =
                dateIssuedField.getText().trim();

        String status =
                (String) statusCombo.getSelectedItem();

        if (medication.isEmpty()
                || dateIssued.isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Medication and date issued are required.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        try {

            LocalDate.parse(dateIssued);

        } catch (DateTimeParseException exception) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Date issued must be in YYYY-MM-DD format.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (hasIllegalChars(
                medication,
                dosage,
                duration,
                dateIssued)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        User selectedPatient =
                patients.get(
                        patientCombo.getSelectedIndex()
                );

        String patientId =
                selectedPatient.getUserId();

        if (hasPrescriptionForMedication(
                fileName,
                patientId,
                medication,
                null)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "This patient already has a prescription for "
                    + medication + ".",
                    "Duplicate Medication",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join(
                "|",
                IDGenerator.next("RX", fileName),
                patientId,
                currentDoctor.getUserId(),
                medication,
                dosage,
                duration,
                dateIssued,
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

        List<String> records =
                FileManager.readLines(fileName);

        for (int i = 1; i < records.size(); i++) {

            String record = records.get(i);

            if (record == null
                    || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 8) {
                continue;
            }

            String prescriptionId =
                    parts[0].trim();

            String existingPatientId =
                    parts[1].trim();

            String existingMedication =
                    parts[3].trim();

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

            boolean isThisRequestsCurrentRoom =
                    assetId.equals(roomId);

            boolean isEligibleRoomType =
                    "IMAGING_ROOM".equalsIgnoreCase(assetRoomType)
                    || "LAB".equalsIgnoreCase(assetRoomType)
                    || "OPERATION_THEATRE".equalsIgnoreCase(assetRoomType);

            boolean isAvailableEligibleRoom =
                    isEligibleRoomType
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

        JTextField dateRequestedField =
                new JTextField(dateRequested);

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

        JTextField dateCompletedField =
                new JTextField(dateCompleted);

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

        int selectedPatientIndex =
                patientCombo.getSelectedIndex();

        String updatedPatientId =
                (isDoctor && selectedPatientIndex >= 0)
                        ? patients.get(selectedPatientIndex).getUserId()
                        : patientId;

        String updatedTestType =
                isDoctor
                        ? (String) testTypeCombo.getSelectedItem()
                        : testType;

        String updatedDateRequested =
                isDoctor
                        ? dateRequestedField.getText().trim()
                        : dateRequested;

        int selectedRoomIndex =
                roomCombo.getSelectedIndex();

        String updatedRoomId =
                isDoctor
                        ? (selectedRoomIndex > 0
                                ? roomIds.get(selectedRoomIndex - 1)
                                : "")
                        : roomId;

        String updatedStatus =
                !isDoctor
                        ? (String) statusCombo.getSelectedItem()
                        : status;

        String updatedDateCompleted =
                !isDoctor
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

        JComboBox<String> patientCombo =
                new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(
                    patient.getFullName()
            );
        }

        JTextField doctorIdField =
                new JTextField(
                        currentDoctor.getFullName()
                );

        setUneditable(doctorIdField);

        JComboBox<String> testTypeCombo =
                new JComboBox<>(
                        new String[]{
                            "Blood Test",
                            "X-Ray",
                            "MRI",
                            "CT Scan",
                            "Ultrasound",
                            "Other Specialized Imaging"
                        }
                );

        List<String> roomIds =
                new ArrayList<>();

        JComboBox<String> roomCombo =
                new JComboBox<>();

        roomCombo.addItem("No room needed");

        List<String> assetLines =
                FileManager.readLines(
                        "hospital_assets.txt"
                );

        for (int i = 1; i < assetLines.size(); i++) {

            String assetLine =
                    assetLines.get(i);

            if (assetLine == null
                    || assetLine.trim().isEmpty()) {
                continue;
            }

            String[] assetParts =
                    ManageRecordsHelper.splitRecord(
                            assetLine
                    );

            if (assetParts.length < 5) {
                continue;
            }

            String assetId =
                    assetParts[0].trim();

            String assetRoomType =
                    assetParts[1].trim();

            String assetRoomName =
                    assetParts[2].trim();

            String assetStatus =
                    assetParts[4].trim();

            if ("IMAGING_ROOM".equalsIgnoreCase(
                        assetRoomType)
                    && "AVAILABLE".equalsIgnoreCase(
                        assetStatus)) {

                roomIds.add(assetId);

                roomCombo.addItem(
                        assetRoomName
                        + " ("
                        + assetId
                        + ")"
                );
            }
        }

        JTextField statusField =
                new JTextField("PENDING");

        setUneditable(statusField);

        JTextField dateRequestedField =
                new JTextField(
                        LocalDate.now().toString()
                );

        JPanel form =
                new JPanel(
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

        int choice =
                JOptionPane.showConfirmDialog(
                        parent,
                        form,
                        "Request Lab Test / Imaging",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String testType =
                (String) testTypeCombo.getSelectedItem();

        String dateRequested =
                dateRequestedField
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

        int selectedRoomIndex =
                roomCombo.getSelectedIndex();

        String roomId =
                selectedRoomIndex > 0
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

        User selectedPatient =
                patients.get(
                        patientCombo.getSelectedIndex()
                );

        String patientId =
                selectedPatient.getUserId();

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
        String consultationId = parts[3].trim();
        String bp = parts[4].trim();
        String heartRate = parts[5].trim();
        String temperature = parts[6].trim();
        String date = parts[7].trim();
        String notes = parts[8].trim();

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

        JTextField doctorIdField = new JTextField(findName(doctorId));
        setUneditable(doctorIdField);

        List<String> specialties = new ArrayList<>();

        List<String> consultationRateLines =
                FileManager.readLines("consultation_rates.txt");

        for (int lineIndex = 0;
                lineIndex < consultationRateLines.size();
                lineIndex++) {

            String rateLine = consultationRateLines.get(lineIndex);

            if (rateLine == null || rateLine.trim().isEmpty()) {
                continue;
            }

            if (lineIndex == 0
                    && rateLine.trim().startsWith("SPECIALTY")) {
                continue;
            }

            String[] rateParts =
                    rateLine.split("\\|", -1);

            if (rateParts.length >= 1
                    && !rateParts[0].trim().isEmpty()) {

                specialties.add(rateParts[0].trim());
            }
        }

        JComboBox<String> consultationIdCombo =
                new JComboBox<>(
                        specialties.toArray(new String[0])
                );

        consultationIdCombo.setSelectedItem(consultationId);

        JTextField bpField = new JTextField(bp);
        JTextField heartRateField = new JTextField(heartRate);
        JTextField temperatureField = new JTextField(temperature);
        JTextField dateField = new JTextField(date);
        JTextField notesField = new JTextField(notes);

        JPanel form = new JPanel(
                new GridLayout(9, 2, 8, 8)
        );

        form.add(new JLabel("VITAL_SIGN_ID:"));
        form.add(vitalSignIdField);

        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);

        form.add(new JLabel("CONSULTATION_ID (specialty):"));
        form.add(consultationIdCombo);

        form.add(new JLabel("BP:"));
        form.add(bpField);

        form.add(new JLabel("HEART_RATE:"));
        form.add(heartRateField);

        form.add(new JLabel("TEMPERATURE:"));
        form.add(temperatureField);

        form.add(new JLabel("DATE:"));
        form.add(dateField);

        form.add(new JLabel(
                "NOTES (symptoms / observations / diagnosis):"
        ));
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

        int selectedPatientIndex =
                patientCombo.getSelectedIndex();

        String updatedPatientId =
                selectedPatientIndex >= 0
                        ? patients.get(selectedPatientIndex).getUserId()
                        : patientId;

        String updatedConsultationId =
                (String) consultationIdCombo.getSelectedItem();

        String updatedBp =
                bpField.getText().trim();

        String updatedHeartRate =
                heartRateField.getText().trim();

        String updatedTemperature =
                temperatureField.getText().trim();

        String updatedDate =
                dateField.getText().trim();

        String updatedNotes =
                notesField.getText().trim();

        if (updatedBp.isEmpty()
                || updatedHeartRate.isEmpty()
                || updatedTemperature.isEmpty()
                || updatedDate.isEmpty()
                || updatedNotes.isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "BP, heart rate, temperature, date and notes are required.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (hasIllegalChars(
                updatedConsultationId,
                updatedBp,
                updatedHeartRate,
                updatedTemperature,
                updatedDate,
                updatedNotes)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        try {
            java.time.LocalDate.parse(updatedDate);
        } catch (java.time.format.DateTimeParseException exception) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Date must be in YYYY-MM-DD format.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (hasVitalSignOnDate(
                fileName,
                updatedPatientId,
                updatedDate,
                vitalSignId)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "This patient already has a vital sign record for "
                            + updatedDate
                            + ". Only one record per day is allowed.",
                    "Duplicate Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join("|",
                vitalSignIdField.getText().trim(),
                updatedPatientId,
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

        List<User> patients = UserRepository.loadAll()
                .stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        if (patients.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "There are no patients to log vitals for.",
                    "Cannot Add Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        JComboBox<String> patientCombo =
                new JComboBox<>();

        for (User patient : patients) {
            patientCombo.addItem(
                    patient.getFullName()
            );
        }

        JTextField doctorIdField =
                new JTextField(
                        currentDoctor.getFullName()
                );

        setUneditable(doctorIdField);

        List<String> specialties =
                new ArrayList<>();

        List<String> consultationRateLines =
                FileManager.readLines(
                        "consultation_rates.txt"
                );

        for (int lineIndex = 1;
                lineIndex < consultationRateLines.size();
                lineIndex++) {

            String rateLine =
                    consultationRateLines.get(lineIndex);

            if (rateLine == null
                    || rateLine.trim().isEmpty()) {
                continue;
            }

            String[] rateParts =
                    ManageRecordsHelper.splitRecord(
                            rateLine
                    );

            if (rateParts.length >= 1
                    && !rateParts[0].trim().isEmpty()) {

                specialties.add(
                        rateParts[0].trim()
                );
            }
        }

        if (specialties.isEmpty()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "No specialties are configured yet. "
                    + "Ask an Admin to add one under "
                    + "Consultation Rates first.",
                    "Cannot Add Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        JComboBox<String> consultationIdCombo =
                new JComboBox<>(
                        specialties.toArray(
                                new String[0]
                        )
                );

        JTextField bpField =
                new JTextField();

        JTextField heartRateField =
                new JTextField();

        JTextField temperatureField =
                new JTextField();

        JTextField dateField =
                new JTextField(
                        LocalDate.now().toString()
                );

        JTextField notesField =
                new JTextField();

        JPanel form =
                new JPanel(
                        new GridLayout(8, 2, 8, 8)
                );

        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);

        form.add(new JLabel("DOCTOR:"));
        form.add(doctorIdField);

        form.add(
                new JLabel(
                        "CONSULTATION_ID (specialty):"
                )
        );
        form.add(consultationIdCombo);

        form.add(new JLabel("BP:"));
        form.add(bpField);

        form.add(new JLabel("HEART_RATE:"));
        form.add(heartRateField);

        form.add(new JLabel("TEMPERATURE:"));
        form.add(temperatureField);

        form.add(new JLabel("DATE:"));
        form.add(dateField);

        form.add(
                new JLabel(
                        "NOTES (symptoms / observations / diagnosis):"
                )
        );
        form.add(notesField);

        int choice =
                JOptionPane.showConfirmDialog(
                        parent,
                        form,
                        "Add Vital Sign Record",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String consultationId =
                (String) consultationIdCombo
                        .getSelectedItem();

        String bp =
                bpField.getText().trim();

        String heartRate =
                heartRateField.getText().trim();

        String temperature =
                temperatureField.getText().trim();

        String date =
                dateField.getText().trim();

        String notes =
                notesField.getText().trim();

        if (consultationId == null
                || consultationId.isEmpty()
                || bp.isEmpty()
                || heartRate.isEmpty()
                || temperature.isEmpty()
                || date.isEmpty()
                || notes.isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "All fields, including consultation notes, are required.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        if (hasIllegalChars(
                consultationId,
                bp,
                heartRate,
                temperature,
                date,
                notes)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        try {

            LocalDate.parse(date);

        } catch (DateTimeParseException exception) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Date must be in YYYY-MM-DD format.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        User selectedPatient =
                patients.get(
                        patientCombo.getSelectedIndex()
                );

        String patientId =
                selectedPatient.getUserId();

        if (hasVitalSignOnDate(
                fileName,
                patientId,
                date,
                null)) {

            JOptionPane.showMessageDialog(
                    parent,
                    "This patient already has a vital sign record for "
                    + date
                    + ". Only one record per day is allowed.",
                    "Duplicate Record",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        return String.join(
                "|",
                IDGenerator.next(
                        "VS",
                        fileName
                ),
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

        List<String> records =
                FileManager.readLines(fileName);

        for (int i = 1; i < records.size(); i++) {

            String record = records.get(i);

            if (record == null
                    || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(
                            record
                    );

            if (parts.length < 9) {
                continue;
            }

            String recordId =
                    parts[0].trim();

            String existingPatientId =
                    parts[1].trim();

            String existingDate =
                    parts[7].trim();

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