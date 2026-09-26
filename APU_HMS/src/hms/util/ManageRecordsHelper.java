package hms.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import hms.role.Role;
import hms.role.User;

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

        refreshAssetFilterOptions();
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

    public void reserveAsset(Component comp, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(comp,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> departments = DepartmentManager.getDepartmentNames();
        if (departments.isEmpty()) {
            JOptionPane.showMessageDialog(comp,
                    "There are no departments available to reserve this asset.",
                    "No Departments Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> departmentCombo = new JComboBox<>(departments.toArray(new String[0]));
        int choice = JOptionPane.showConfirmDialog(comp, departmentCombo,
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
            JOptionPane.showMessageDialog(comp,
                    "This ward/clinic is already in use.",
                    "Reservation Conflict", JOptionPane.WARNING_MESSAGE);
            return;
        }

        asset.setStatus("OCCUPIED");
        asset.setDescription(departmentName.trim());
        if (AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(comp, "Ward/clinic reserved successfully.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(comp,
                    "The reservation could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void finishAsset(java.awt.Component comp, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(comp,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(comp,
                "Mark this ward/clinic as finished and available again?", "Finish Usage",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        asset.setStatus("AVAILABLE");
        asset.setDescription("");
        if (AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(comp, "Ward/clinic marked as finished.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(comp,
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
            tableModel.addRow(new Object[]{parts[0].trim(), findName(parts[1].trim()),
                                                                    doctorName,
                                                                    parts[3].trim(),
                                                                    parts[4].trim(),
                                                                    parts[5].trim(),
                                                                    parts[6].trim()});
        } else if (assetTable) {
            RecordsHelperAsset.addAssetRow(tableModel, line, assetSearchBox);
        } else if (insuranceTable){
            RecordsHelperInsurance.addInsuranceRow(tableModel, line);
        } else if (consultationRateTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{parts[0].trim(),
                                            parts[1].trim(),
                                            parts[2].trim(),
                                            parts[3].trim(),
                                            parts[4].trim(),
                                            parts[5].trim()});
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
            String notes = parts.length >= 9 ? parts[8].trim() : "";
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                findName(parts[1].trim()),
                findName(parts[2].trim()),
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
                findName(parts[1].trim()),
                findName(parts[2].trim()),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim(),
                parts[7].trim()
            });
 
        } else if (labRequestTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), 
                findName(parts[1].trim()),
                findName(parts[2].trim()),
                parts[3].trim(),
                findAssetType(parts[4].trim()),
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

    public static String findAssetType(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return "N/A";
        }
        
        List<String> assetLines = FileManager.readLines("hospital_assets.txt");
        for (String line : assetLines) {
            String[] parts = splitRecord(line);
            if (parts.length >= 3 && parts[0].trim().equalsIgnoreCase(assetId.trim())) {
                return parts[2].trim(); 
            }
        }
        return assetId;
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



    private static final List<String> STATUS_ORDER = Arrays.asList(
        "PENDING", 
        "APPROVED", 
        "DENIED", 
        "COMPLETED"
    );

    public static void applyStatusSorter(JTable table, int statusColumnIndex) {
        if (table == null || table.getModel() == null) return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

        Comparator<String> statusComparator = (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null) return 1;
            if (s2 == null) return -1;

            int index1 = STATUS_ORDER.indexOf(s1.trim().toUpperCase());
            int index2 = STATUS_ORDER.indexOf(s2.trim().toUpperCase());

            if (index1 == -1) index1 = Integer.MAX_VALUE;
            if (index2 == -1) index2 = Integer.MAX_VALUE;

            return Integer.compare(index1, index2);
        };

        sorter.setComparator(statusColumnIndex, statusComparator);
        table.setRowSorter(sorter);
    }
}

