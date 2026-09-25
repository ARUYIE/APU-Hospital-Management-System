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
import java.awt.*;

/** Shared data and table operations for record-management panels. */
public final class ManageRecordsHelper {

    private final String fileName;
    private boolean patientAppointmentTable;
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

        JComboBox<String> departmentCombo = new JComboBox<>(departments.toArray(String[]::new));
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

        
        } else if ((patientAppointmentTable || appointmentTable) && parts.length >= 6) {
            String doctorName = findName(parts[2].trim());
            String selectedDoctor = doctorSearchBox != null ? (String) doctorSearchBox.getSelectedItem() : null;

            if (selectedDoctor != null
                    && !selectedDoctor.equals("All Doctors")
                    && !selectedDoctor.equals("Doctor Name")
                    && !doctorName.equalsIgnoreCase(selectedDoctor)) {
                return;
            }

            tableModel.addRow(new Object[]{
                parts[0].trim(),           
                findName(parts[1].trim()), 
                doctorName,                
                parts[3].trim(),           
                parts[4].trim(),           
                parts[5].trim()            
            });

       
        } else if (assetTable) {
            RecordsHelperAsset.addAssetRow(tableModel, line, assetSearchBox);

        
        } else if (insuranceTable) {
            RecordsHelperInsurance.addInsuranceRow(tableModel, line);

       
        } else if (consultationRateTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim()
            });

        
        } else if (reportTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim()
            });

       
        } else if (consultationTable && parts.length >= 9) {
            String patientId = parts[1].trim();

            
            User currentUser = Session.getCurrentUser();
            if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
                if (!patientId.equalsIgnoreCase(currentUser.getUserId())
                        && !patientId.equalsIgnoreCase(currentUser.getUsername())) {
                    return; 
                }
            }

            tableModel.addRow(new Object[]{
                parts[0].trim(),           
                findName(parts[1].trim()), 
                findName(parts[2].trim()), 
                parts[3].trim(),           
                parts[4].trim(),           
                parts[5].trim(),           
                parts[6].trim(),           
                parts[7].trim(),           
                parts[8].trim()            
            });

       
        } else if (prescriptionTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim(), parts[7].trim()
            });

       
        } else if (labRequestTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim(), parts[7].trim()
            });

        
        } else if (rosterTable && parts.length >= 7) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim()
            });

        } else {
            if (parts.length > 1) {
                Object[] rowData = new Object[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    rowData[i] = parts[i].trim();
                }
                tableModel.addRow(rowData);
            } else {
                tableModel.addRow(new Object[]{
                    tableModel.getRowCount() + 1,
                    line
                });
            }
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

    
    
  private void addPatientTableRow(String line) {
        String[] parts = splitRecord(line);

        // Check for appointment table records
        if (appointmentTable) {
            // If the record was split into multiple pipe parts
            if (parts.length >= 6) {
                String doctorName = findName(parts[2].trim());
                String selectedDoctor = doctorSearchBox != null ? (String) doctorSearchBox.getSelectedItem() : null;

                if (selectedDoctor != null
                        && !selectedDoctor.equals("All Doctors")
                        && !selectedDoctor.equals("Doctor Name")
                        && !doctorName.equalsIgnoreCase(selectedDoctor)) {
                    return;
                }

                // Add each value to its own separate column
                tableModel.addRow(new Object[]{
                    parts[0].trim(),           // APPOINTMENT_ID box
                    findName(parts[1].trim()), // PATIENT_USERNAME box (who booked)
                    doctorName,                // DOCTOR box (doctor booked)
                    parts[3].trim(),           // DATE box
                    parts[4].trim(),           // TIME box
                    parts[5].trim()            // STATUS box
                });
                return;
            }
        }

        // Department Table
        if (departmentTable && parts.length >= 4) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                findName(parts[3].trim()),
                parts[2].trim()
            });
            return;
        }

        // Asset Table
        if (assetTable) {
            RecordsHelperAsset.addAssetRow(tableModel, line, assetSearchBox);
            return;
        }

        // Insurance Table
        if (insuranceTable) {
            RecordsHelperInsurance.addInsuranceRow(tableModel, line);
            return;
        }

        // Consultation Rates
        if (consultationRateTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim()
            });
            return;
        }

        // Reports
        if (reportTable && parts.length >= 6) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim()
            });
            return;
        }

        // Vital Signs / Consultation
        if (consultationTable && parts.length >= 9) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim(), parts[7].trim(), parts[8].trim()
            });
            return;
        }

        // Prescriptions
        if (prescriptionTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim(), parts[7].trim()
            });
            return;
        }

        // Lab Requests
        if (labRequestTable && parts.length >= 8) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim(), parts[7].trim()
            });
            return;
        }

        // Roster Table
        if (rosterTable && parts.length >= 7) {
            tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim(),
                parts[6].trim()
            });
            return;
        }

        // Fallback: If line contains '|', split and populate as many cells as possible
        if (parts.length > 1) {
            Object[] rowData = new Object[parts.length];
            for (int i = 0; i < parts.length; i++) {
                rowData[i] = parts[i].trim();
            }
            tableModel.addRow(rowData);
        } else {
            tableModel.addRow(new Object[]{
                tableModel.getRowCount() + 1,
                line
            });
        }
    }

}

