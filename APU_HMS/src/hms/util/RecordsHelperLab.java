package hms.util;

import hms.role.Role;
import hms.role.User;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Lab-request-specific record forms and persistence operations for record panels. */
public final class RecordsHelperLab {

    private RecordsHelperLab() {
    }

    public static String editLabRecord(Component comp, String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);
        if (parts.length < 8) {
            return null;
        }

        String existingPatientId = parts[1].trim();
        String doctorId = parts[2].trim();
        String existingAssetId = parts[3].trim();
        List<User> patients = loadPatientsForDoctor(doctorId, existingPatientId);
        if (patients.isEmpty()) {
            showWarning(comp, "This doctor has no patients on record to select from.");
            return null;
        }
        List<Asset> assets = loadSelectableAssets(existingAssetId);
        if (assets.isEmpty()) {
            showWarning(comp, "No lab or imaging rooms are available to select from.");
            return null;
        }

        JTextField requestIdField = new JTextField(parts[0].trim());
        setUneditable(requestIdField);
        JComboBox<String> patientCombo = createPatientCombo(patients, existingPatientId);
        JTextField doctorIdField = new JTextField(doctorId);
        setUneditable(doctorIdField);
        JComboBox<String> assetCombo = createAssetCombo(assets, existingAssetId);
        JComboBox<String> statusCombo = createStatusCombo(parts[4].trim());
        JTextField dateRequestedField = new JTextField(parts[5].trim());
        JTextField dateCompletedField = new JTextField(parts[6].trim());
        JTextField resultField = new JTextField(parts[7].trim());

        JPanel form = createForm(requestIdField, patientCombo, doctorIdField,
                assetCombo, statusCombo, dateRequestedField,
                dateCompletedField, resultField);
        int choice = JOptionPane.showConfirmDialog(comp, form,
                "Edit Lab / Imaging Request", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String patientId = patients.get(patientCombo.getSelectedIndex()).getUserId();
        String assetId = assets.get(assetCombo.getSelectedIndex()).getAssetId();
        String status = (String) statusCombo.getSelectedItem();
        String dateRequested = dateRequestedField.getText().trim();
        String dateCompleted = dateCompletedField.getText().trim();
        String result = resultField.getText().trim();
        if (!isValid(comp, dateRequested, dateCompleted, result)) {
            return null;
        }

        return String.join("|", requestIdField.getText().trim(), patientId,
                doctorIdField.getText().trim(), assetId, status,
                dateRequested, dateCompleted, result);
    }

    public static void addLabRecord(Component comp, String fileName, Runnable refreshAction) {
        User currentDoctor = Session.getCurrentUser();
        List<User> patients = loadPatientsForDoctor(currentDoctor.getUserId(), null);
        if (patients.isEmpty()) {
            showWarning(comp, "You have no patients on record to select from.");
            return;
        }
        List<Asset> assets = loadSelectableAssets(null);
        if (assets.isEmpty()) {
            showWarning(comp, "No lab or imaging rooms are currently available.");
            return;
        }

        JComboBox<String> patientCombo = createPatientCombo(patients, null);
        JTextField doctorIdField = new JTextField(currentDoctor.getUserId());
        setUneditable(doctorIdField);
        JComboBox<String> assetCombo = createAssetCombo(assets, null);
        JTextField statusField = new JTextField("REQUESTED");
        setUneditable(statusField);
        JTextField dateRequestedField = new JTextField(LocalDate.now().toString());
        JTextField dateCompletedField = new JTextField();
        setUneditable(dateCompletedField);
        JTextField resultField = new JTextField();
        setUneditable(resultField);

        JPanel form = createForm(null, patientCombo, doctorIdField, assetCombo,
                statusField, dateRequestedField, dateCompletedField, resultField);
        int choice = JOptionPane.showConfirmDialog(comp, form,
                "Request Lab / Imaging Test", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String patientId = patients.get(patientCombo.getSelectedIndex()).getUserId();
        String assetId = assets.get(assetCombo.getSelectedIndex()).getAssetId();
        String dateRequested = dateRequestedField.getText().trim();
        if (!isValid(comp, dateRequested, "", "")) {
            return;
        }

        String requestLine = String.join("|",
                IDGenerator.next("LR", fileName), patientId,
                currentDoctor.getUserId(), assetId, "REQUESTED",
                dateRequested, "", "");
        FileManager.appendLine(fileName, requestLine);
        markAssetPending(assetId);
        refreshAction.run();
    }

    // Admin approval: reserves the linked asset and moves the request into progress.
    public static List<String> approveLabRequest(Component comp, List<String> records, String requestId) {
        return updateLabRequestStatus(comp, records, requestId, "IN_PROGRESS");
    }

    // Admin denial: cancels the request and releases the asset if it had been reserved.
    public static List<String> denyLabRequest(Component comp, List<String> records, String requestId) {
        return updateLabRequestStatus(comp, records, requestId, "CANCELLED");
    }

    // Entry points for the Wards/Clinics panel: approve/deny the pending request tied to the selected asset.
    public static void approvePendingRequestForAsset(Component comp, String assetId, Runnable refreshAction) {
        updatePendingRequestForAsset(comp, assetId, true, refreshAction);
    }

    public static void denyPendingRequestForAsset(Component comp, String assetId, Runnable refreshAction) {
        updatePendingRequestForAsset(comp, assetId, false, refreshAction);
    }

    private static void updatePendingRequestForAsset(Component comp, String assetId,
            boolean approve, Runnable refreshAction) {
        List<String> lines = FileManager.readLines("lab_requests.txt");
        if (lines.isEmpty()) {
            showWarning(comp, "No lab requests are on record.");
            return;
        }
        String headerLine = lines.remove(0);

        String requestId = null;
        for (String line : lines) {
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length >= 8 && parts[3].trim().equals(assetId)
                    && "REQUESTED".equalsIgnoreCase(parts[4].trim())) {
                requestId = parts[0].trim();
                break;
            }
        }
        if (requestId == null) {
            showWarning(comp, "This room has no pending lab request.");
            return;
        }

        List<String> updatedLines = approve
                ? approveLabRequest(comp, lines, requestId)
                : denyLabRequest(comp, lines, requestId);
        if (updatedLines == null) {
            return;
        }

        List<String> linesToWrite = new ArrayList<>();
        linesToWrite.add(headerLine);
        linesToWrite.addAll(updatedLines);
        FileManager.writeAllLines("lab_requests.txt", linesToWrite);
        refreshAction.run();
    }

    private static List<String> updateLabRequestStatus(Component comp, List<String> records,
            String requestId, String newStatus) {
        List<String> updatedLines = new ArrayList<>();
        boolean found = false;
        String assetId = null;
        String previousStatus = null;
        for (String record : records) {
            String[] parts = ManageRecordsHelper.splitRecord(record);
            if (parts.length >= 8 && parts[0].trim().equals(requestId)) {
                found = true;
                assetId = parts[3].trim();
                previousStatus = parts[4].trim();
                parts[4] = newStatus;
                updatedLines.add(String.join("|", parts));
            } else {
                updatedLines.add(record);
            }
        }

        if (!found) {
            showWarning(comp, "The selected lab request could not be found.");
            return null;
        }

        if ("IN_PROGRESS".equals(newStatus)) {
            if (!reserveLabAsset(comp, assetId)) {
                return null;
            }
        } else if ("IN_PROGRESS".equals(previousStatus)
                || "REQUESTED".equalsIgnoreCase(previousStatus)
                || "PENDING".equalsIgnoreCase(previousStatus)) {
            releaseLabAsset(assetId);
        }

        return updatedLines;
    }

    private static boolean markAssetPending(String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            return false;
        }
        asset.setStatus("PENDING");
        asset.setDescription("Pending Lab Request");
        return AssetManager.updateAsset(asset);
    }

    private static boolean reserveLabAsset(Component comp, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            showWarning(comp, "The linked lab/imaging room could not be found.");
            return false;
        }
        if (!asset.isAvailable()) {
            showWarning(comp, "The linked lab/imaging room is no longer available.");
            return false;
        }
        asset.setStatus("OCCUPIED");
        asset.setDescription("Lab Request");
        return AssetManager.updateAsset(asset);
    }

    private static void releaseLabAsset(String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset != null) {
            asset.setStatus("AVAILABLE");
            asset.setDescription("");
            AssetManager.updateAsset(asset);
        }
    }

    // Lab requests may use a LAB or an IMAGING_ROOM asset; keeps the current asset selectable while editing.
    private static List<Asset> loadSelectableAssets(String mustIncludeAssetId) {
        List<Asset> assets = new ArrayList<>(AssetManager.getAvailableAssets(AssetType.LAB));
        assets.addAll(AssetManager.getAvailableAssets(AssetType.IMAGING_ROOM));
        if (mustIncludeAssetId != null && !mustIncludeAssetId.isEmpty()) {
            boolean alreadyIncluded = false;
            for (Asset asset : assets) {
                if (asset.getAssetId().equals(mustIncludeAssetId)) {
                    alreadyIncluded = true;
                    break;
                }
            }
            if (!alreadyIncluded) {
                Asset current = AssetManager.getAsset(mustIncludeAssetId);
                if (current != null) {
                    assets.add(current);
                }
            }
        }
        return assets;
    }

    private static JComboBox<String> createAssetCombo(List<Asset> assets, String selectedAssetId) {
        JComboBox<String> assetCombo = new JComboBox<>();
        int selectedIndex = -1;
        for (int index = 0; index < assets.size(); index++) {
            Asset asset = assets.get(index);
            assetCombo.addItem(asset.getName() + " (" + asset.getLocation() + ")");
            if (asset.getAssetId().equals(selectedAssetId)) {
                selectedIndex = index;
            }
        }
        if (selectedIndex >= 0) {
            assetCombo.setSelectedIndex(selectedIndex);
        }
        return assetCombo;
    }

    // Restricts selection to patients the doctor has an appointment history with (bookings.txt).
    private static List<User> loadPatientsForDoctor(String doctorId, String mustIncludePatientId) {
        Set<String> patientIds = new LinkedHashSet<>();
        for (String line : FileManager.readLines("bookings.txt")) {
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length < 6) {
                continue;
            }
            if (parts[2].trim().equals(doctorId)) {
                patientIds.add(parts[1].trim());
            }
        }
        if (mustIncludePatientId != null && !mustIncludePatientId.isEmpty()) {
            patientIds.add(mustIncludePatientId);
        }

        List<User> allUsers = UserRepository.loadAll();
        List<User> patients = new ArrayList<>();
        for (String patientId : patientIds) {
            for (User user : allUsers) {
                if (user.getRole() == Role.PATIENT && user.getUserId().equals(patientId)) {
                    patients.add(user);
                    break;
                }
            }
        }
        return patients;
    }

    private static JComboBox<String> createPatientCombo(List<User> patients, String selectedPatientId) {
        JComboBox<String> patientCombo = new JComboBox<>();
        int selectedIndex = -1;
        for (int index = 0; index < patients.size(); index++) {
            User patient = patients.get(index);
            patientCombo.addItem(patient.getFullName());
            if (patient.getUserId().equals(selectedPatientId)) {
                selectedIndex = index;
            }
        }
        if (selectedIndex >= 0) {
            patientCombo.setSelectedIndex(selectedIndex);
        }
        return patientCombo;
    }


    private static JPanel createForm(JTextField requestIdField, JComboBox<String> patientCombo,
            JTextField doctorIdField, JComboBox<String> assetCombo,
            JComponent statusComponent, JTextField dateRequestedField,
            JTextField dateCompletedField, JTextField resultField) {
        JPanel form = new JPanel(new GridLayout(requestIdField == null ? 7 : 8, 2, 8, 8));
        if (requestIdField != null) {
            form.add(new JLabel("REQUEST_ID:"));
            form.add(requestIdField);
        }
        form.add(new JLabel("PATIENT:"));
        form.add(patientCombo);
        form.add(new JLabel("LAB / IMAGING ROOM:"));
        form.add(assetCombo);
        form.add(new JLabel("STATUS:"));
        form.add(statusComponent);
        form.add(new JLabel("DATE_REQUESTED:"));
        form.add(dateRequestedField);
        form.add(new JLabel("DATE_COMPLETED:"));
        form.add(dateCompletedField);
        form.add(new JLabel("RESULT:"));
        form.add(resultField);
        return form;
    }

    private static JComboBox<String> createStatusCombo(String selectedStatus) {
        JComboBox<String> statusCombo = new JComboBox<>(
                new String[]{"REQUESTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"});
        statusCombo.setSelectedItem(selectedStatus);
        return statusCombo;
    }

    private static boolean isValid(Component comp, String dateRequested,
            String dateCompleted, String result) {
        if (dateRequested.isEmpty()) {
            showWarning(comp, "Requested date is required.");
            return false;
        }
        // if (!Validator.isValidDate(dateRequested)
        //         || (!dateCompleted.isEmpty() && !Validator.isValidDate(dateCompleted))) {
        //     showWarning(comp, "Dates must be in YYYY-MM-DD format.");
        //     return false;
        // }
        if (ManageRecordsHelper.hasIllegalChars(dateRequested, dateCompleted, result)) {
            showWarning(comp, "Fields cannot contain the '|' character or line breaks.");
            return false;
        }
        return true;
    }

    private static void showWarning(Component comp, String message) {
        JOptionPane.showMessageDialog(comp, message,
                "Invalid Lab Request", JOptionPane.WARNING_MESSAGE);
    }

    private static void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(Color.LIGHT_GRAY);
    }
}