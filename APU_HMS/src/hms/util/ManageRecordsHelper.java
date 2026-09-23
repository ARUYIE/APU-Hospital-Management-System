package hms.util;

import hms.role.Role;
import hms.role.User;
import hms.util.Session;
import hms.util.UserRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared data and table operations for record-management panels. */
public final class ManageRecordsHelper {

    private final String fileName;
    private final boolean departmentTable;
    private final boolean appointmentTable;
    private final boolean assetTable;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final boolean rosterTable;
    private final boolean reportTable;
    private final boolean consultationTable;
    private final boolean prescriptionTable;
    private final boolean labRequestTable;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> doctorSearchBox;
    private final JComboBox<String> assetSearchBox;
    private boolean updatingAssetFilter;
    private final List<String> records = new ArrayList<>();
    private String headerLine;
    private boolean headerPresent;
    private boolean initialized;

    public ManageRecordsHelper(String fileName, DefaultTableModel tableModel, JComboBox<String> doctorSearchBox, JComboBox<String> assetSearchBox, boolean consultationTable, boolean prescriptionTable, boolean labRequestTable) {
        this.fileName = fileName;
        this.tableModel = tableModel;
        this.doctorSearchBox = doctorSearchBox;
        this.assetSearchBox = assetSearchBox;
        departmentTable = "department.txt".equalsIgnoreCase(fileName);
        appointmentTable = "bookings.txt".equalsIgnoreCase(fileName);
        assetTable = "hospital_assets.txt".equalsIgnoreCase(fileName);
        insuranceTable = "insurance_networks.txt".equalsIgnoreCase(fileName);
        consultationRateTable = "consultation_rates.txt".equalsIgnoreCase(fileName);
        rosterTable = "roster.txt".equalsIgnoreCase(fileName);
        reportTable = "report.txt".equalsIgnoreCase(fileName);
        this.consultationTable = "vital_signs.txt".equalsIgnoreCase(fileName);
        this.prescriptionTable = "prescriptions.txt".equalsIgnoreCase(fileName);
        this.labRequestTable = "lab_requests.txt".equalsIgnoreCase(fileName);
    }

    public List<String> getRecords() {
        return new ArrayList<>(records);
    }

    public void refreshTable() {

        List<String> lines = FileManager.readLines(fileName);

        headerLine = null;

        // Always treat the first line as the header
        if (!lines.isEmpty()) {
            headerLine = lines.remove(0);
        }

        records.clear();
        records.addAll(lines);

        tableModel.setRowCount(0);

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            addTableRow(record);
        }
    }

    public boolean appendRecord(String record, String errorMessage) {
        List<String> before = FileManager.readLines(fileName);
        FileManager.appendLine(fileName, record);
        List<String> after = FileManager.readLines(fileName);
        boolean saved = after.size() == before.size() + 1
                && after.get(after.size() - 1).equals(record);
        if (!saved) {
            return false;
        }
        refreshTable();
        return true;
    }

    public boolean writeRecords(List<String> updatedRecords, String errorMessage) {
        List<String> linesToWrite = new ArrayList<>();
        if (headerLine != null) {
            linesToWrite.add(headerLine);
        }
        linesToWrite.addAll(updatedRecords);
        FileManager.writeAllLines(fileName, linesToWrite);
        boolean saved = FileManager.readLines(fileName).equals(linesToWrite);
        if (saved) {
            refreshTable();
        }
        return saved;
    }

    public boolean deleteRecord(int modelRow) {
        if (modelRow < 0 || modelRow >= records.size()) {
            return false;
        }
        records.remove(modelRow);
        List<String> linesToWrite = new ArrayList<>();
        if (headerLine != null) {
            linesToWrite.add(headerLine);
        }
        linesToWrite.addAll(records);
        FileManager.writeAllLines(fileName, linesToWrite);
        if (!FileManager.readLines(fileName).equals(linesToWrite)) {
            refreshTable();
            return false;
        }
        refreshTable();
        return true;
    }

    public String getRecord(int modelRow) {
        return modelRow >= 0 && modelRow < records.size() ? records.get(modelRow) : null;
    }

    public void reserveAsset(java.awt.Component owner, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(owner,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> departments = DepartmentManager.getDepartmentNames();
        if (departments.isEmpty()) {
            JOptionPane.showMessageDialog(owner,
                    "There are no departments available to reserve this asset.",
                    "No Departments Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> departmentCombo = new JComboBox<>(departments.toArray(new String[0]));
        int choice = JOptionPane.showConfirmDialog(owner, departmentCombo,
                "Select Department Reserving This Asset", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String departmentName = (String) departmentCombo.getSelectedItem();
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return;
        }
        if (!"AVAILABLE".equalsIgnoreCase(asset.getStatus())) {
            JOptionPane.showMessageDialog(owner,
                    "This ward/clinic is already in use.",
                    "Reservation Conflict", JOptionPane.WARNING_MESSAGE);
            return;
        }

        asset.setStatus("OCCUPIED");
        asset.setDescription(departmentName.trim());
        if (AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(owner, "Ward/clinic reserved successfully.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(owner,
                    "The reservation could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void finishAsset(java.awt.Component owner, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(owner,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(owner,
                "Mark this ward/clinic as finished and available again?", "Finish Usage",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        asset.setStatus("AVAILABLE");
        asset.setDescription("");
        if (AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(owner, "Ward/clinic marked as finished.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(owner,
                    "The status update could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAssetFilterOptions() {
        if (!assetTable || updatingAssetFilter) {
            return;
        }

        String selectedAssetType = (String) assetSearchBox.getSelectedItem();
        Set<String> roomTypes = new LinkedHashSet<>();
        for (String record : records) {
            String[] parts = splitRecord(record);
            if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                roomTypes.add(parts[1].trim());
            }
        }

        DefaultComboBoxModel<String> filterModel = new DefaultComboBoxModel<>();
        filterModel.addElement("All Room Types");
        for (String roomType : roomTypes) {
            filterModel.addElement(roomType);
        }

        updatingAssetFilter = true;
        assetSearchBox.setModel(filterModel);
        if (selectedAssetType != null && roomTypes.contains(selectedAssetType)) {
            assetSearchBox.setSelectedItem(selectedAssetType);
        }
        updatingAssetFilter = false;
    }

    private void addTableRow(String line) {
        String[] parts = splitRecord(line);

        if (departmentTable && parts.length >= 4) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                findName(parts[3].trim()),
                parts[2].trim()
            });

        } else if (appointmentTable && parts.length >= 7) {
            String doctorName = findName(parts[2].trim());
            String selectedDoctor = (String) doctorSearchBox.getSelectedItem();

            if (selectedDoctor != null
                    && !selectedDoctor.equals("All Doctors")
                    && !selectedDoctor.equals("Doctor Name")
                    && !doctorName.equals(selectedDoctor)) {
                return;
            }

            tableModel.addRow(new Object[]{
                parts[0].trim(),
                findName(parts[1].trim()),
                doctorName,
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim()
            });

        } else if (assetTable && parts.length >= 6) {
            String selectedAssetType = (String) assetSearchBox.getSelectedItem();
            String roomType = parts[1].trim();
            if (selectedAssetType != null && !"All Room Types".equals(selectedAssetType)
                    && !roomType.equals(selectedAssetType)) {
                return;
            }
            tableModel.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim(),
                    parts[3].trim(), parts[4].trim(), parts[5].trim()});
        } else if (insuranceTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts.length > 6 ? parts[6].trim() : ""
            });

        } else if (consultationRateTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim()
            });

        } else if (reportTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim()
            });
           
        } else if (consultationTable && parts.length >= 9) {
            String notes = parts.length >= 10 ? parts[8].trim() : "";
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim(),
                parts[7].trim(),
                notes
            });
 
        } else if (prescriptionTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), 
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim(),
                parts[7].trim()
            });
 
        } else if (labRequestTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim(),
                parts[7].trim()
            });
        
        } else if (rosterTable && parts.length >= 7) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim()
            });
        } else {
            tableModel.addRow(new Object[]{
                tableModel.getRowCount() + 1,
                line
            });
        }
    }

    private static boolean visibleToCurrentDoctor(String doctorId) {
        User current = Session.getCurrentUser();
        if (current == null || current.getRole() != Role.DOCTOR) {
            return true;
        }
        return doctorId.equals(current.getUserId());
    }
    
    public static String[] splitRecord(String record) {
        return record.split("\\|", -1);
    }

    public static String findName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "";
        }
        for (User user : UserRepository.loadAll()) {
            if (userId.equals(user.getUserId())) {
                return user.getFullName();
            }
        }
        return userId;
    }

    public static boolean validRateFields(String baseRate, String minRate, String maxRate) {
        try {
            double base = Double.parseDouble(baseRate.trim());
            double minimum = Double.parseDouble(minRate.trim());
            double maximum = Double.parseDouble(maxRate.trim());
            return minimum <= base && base <= maximum;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
    
    public static boolean isValidRecord(String record) {
        return record != null && !record.isEmpty() && !record.contains("\n")
                && !record.contains("\r") && record.indexOf('|') > 0;
    }

    public static boolean hasIllegalChars(String... values) {
        for (String value : values) {
            if (value != null && (value.contains("|") || value.contains("\n") || value.contains("\r"))) {
                return true;
            }
        }
        return false;
    }
}
