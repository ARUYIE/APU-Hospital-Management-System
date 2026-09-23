package hms.gui.panels;


import hms.util.FileManager;
import hms.util.IDGenerator;
import hms.util.ManageRecordsHelper;
import hms.util.UserRepository;
import hms.role.Role;
import hms.role.User;

import hms.util.RecordsHelperAsset;
import hms.util.RecordsHelperAppointment;
import hms.util.RecordsHelperInsurance;
import hms.util.RecordsHelperConsultation;
import hms.util.ReportData;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// for wards, department,appontment, consultation rate, insurance
public class ManageRecordsPanel extends JPanel {

    private final String fileName;
    
    private final boolean departmentTable;
    private final boolean appointmentTable;
    private final JComboBox<String> doctorSearchBox = new JComboBox<>();
    private final JComboBox<String> assetSearchBox = new JComboBox<>(new String[]{"All Room Types"});
    private final boolean assetTable;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final boolean rosterTable;
    private final boolean reportTable;

    private final DefaultTableModel tableModel;
    private final JTable recordsTable;
    private final ManageRecordsHelper recordHelper;
    private List<String> records = new ArrayList<>();
    private final ReportData reportData;

    public ManageRecordsPanel(String title, String fileName) {
        this.fileName = fileName;
        reportData = new ReportData();
        
        // Tan Rui En - Admin
        assetTable = "hospital_assets.txt".equalsIgnoreCase(fileName);
        appointmentTable = "bookings.txt".equalsIgnoreCase(fileName);
        insuranceTable = "insurance_networks.txt".equalsIgnoreCase(fileName);
        consultationRateTable = "consultation_rates.txt".equalsIgnoreCase(fileName);
        
        // Wong Willard - Medical Manager
        departmentTable = "department.txt".equalsIgnoreCase(fileName);
        rosterTable = "roster.txt".equalsIgnoreCase(fileName);
        reportTable = "report.txt".equalsIgnoreCase(fileName);
        
        tableModel = new DefaultTableModel(
                departmentTable
                ? new String[]{"DEPTARTMENT_ID", "DEPTARTMENT_NAME", "HEAD_MANAGER_NAME", "DESCRIPTION"}
                : appointmentTable
                ? new String[]{"APPOINTMENT_ID", "PATIENT_NAME", "DOCTOR_NAME", "DATE", "TIME", "STATUS", "NOTES"}
                : assetTable
                ? new String[]{"ASSET_ID", "ROOM_TYPE", "ROOM_NAME", "LOCATION", "STATUS", "RESERVED_BY"}
                : insuranceTable
                ? new String[]{"INSURANCE_ID", "PROVIDER_NAME", "COVERAGE_RATE", "COVERAGE_PERCENTAGE", "STATUS", "CONTACT_INFO", "EFFECTIVE_DATE"}
                : consultationRateTable
                ? new String[]{"SPECIALTY", "BASE_RATE", "MIN_RATE", "MAX_RATE", "CURRENCY", "EFFECTIVE_DATE"}
                : rosterTable
                ? new String[]{"ROSTER_ID", "DOCTOR_ID", "DOCTOR_NAME", "DEPARTMENT", "DATE", "SHIFT", "STATUS"}
                : reportTable
                ? new String[]{"REPORT_PERIOD", "TOTAL_PATIENTS", "APPOINTMENTS", "COMPLETED_APPOINTMENTS", "CANCELLED_APPOINTMENTS", "TOTAL_REVENUE"}
                : new String[]{"#", "Record"}, 0) {
                    
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
        recordsTable = new JTable(tableModel);
        recordHelper = new ManageRecordsHelper(fileName, tableModel, doctorSearchBox, assetSearchBox);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        JButton addButton = new JButton("Add Record");
        addButton.addActionListener(e -> addRecord());

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> editSelectedRecord());

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedRecord());

        JButton reserveButton = new JButton("Reserve Ward");
        reserveButton.addActionListener(e -> reserveSelectedAsset());

        JButton finishButton = new JButton("Finished");
        finishButton.addActionListener(e -> finishSelectedAsset());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (assetTable) {
            assetSearchBox.setToolTipText("Filter by room type");
            assetSearchBox.addActionListener(e -> refreshTable());
        } else if(appointmentTable){
            actions.add(new JLabel("Search Doctor:"));

            RecordsHelperAppointment.populateDoctorSearchBox(doctorSearchBox);
            doctorSearchBox.addActionListener(e -> refreshTable());
        } else if(reportTable){
            populateReportTable();
        }
        actions.add(refreshButton);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);


        //has two rows since its a bit too long
        if (assetTable) {
            JPanel wardActions = new JPanel();
            wardActions.setLayout(new BoxLayout(wardActions, BoxLayout.Y_AXIS));

            JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            searchActions.add(new JLabel("Search Wards/Clinics:"));
            searchActions.add(assetSearchBox);
            searchActions.add(reserveButton);
            searchActions.add(finishButton);

            JPanel recordActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            recordActions.add(refreshButton);
            recordActions.add(addButton);
            recordActions.add(editButton);
            recordActions.add(deleteButton);

            wardActions.add(searchActions);
            wardActions.add(recordActions);
            actions = wardActions;
        }
        
        

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);

        recordsTable.setAutoCreateRowSorter(true);
        recordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(recordsTable), BorderLayout.CENTER);
        refreshTable();
    }

    private String getSelectedAssetId() {
        int viewRow = recordsTable.getSelectedRow();
        if (viewRow == -1) {
            return null;
        }
        int modelRow = recordsTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return null;
        }
        Object val = tableModel.getValueAt(modelRow, 0);
        return val != null ? val.toString().trim() : null;
    }

    private void reserveSelectedAsset() {
        if (!assetTable) {
            return;
        }

        String assetId = getSelectedAssetId();
        if (assetId == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a ward or clinic first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        recordHelper.reserveAsset(this, assetId);
    }

    private void finishSelectedAsset() {
        if (!assetTable) {
            return;
        }

        String assetId = getSelectedAssetId();
        if (assetId == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a ward or clinic first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        recordHelper.finishAsset(this, assetId);
    }

    private void editSelectedRecord() {
        int viewRow = recordsTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a record first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = recordsTable.convertRowIndexToModel(viewRow);
        String updatedRecord = departmentTable
                            ? editDepartmentRecord(records.get(modelRow))
                            : appointmentTable
                            ? RecordsHelperAppointment.editAppointmentRecord(this, records.get(modelRow))
                            : insuranceTable
                            ? RecordsHelperInsurance.editInsuranceRecord(this, records.get(modelRow))
                            : consultationRateTable
                            ? RecordsHelperConsultation.editConsultationRateRecord(this, records.get(modelRow))
                            : assetTable
                            ? RecordsHelperAsset.editAssetRecord(this, records.get(modelRow))
                            : (String) JOptionPane.showInputDialog(this,
                                    "Edit record:", "Edit Record",
                                    JOptionPane.PLAIN_MESSAGE, null, null,
                                    records.get(modelRow));
                                refreshTable();
        if (updatedRecord == null) {
            return;
        }

        String normalizedRecord = updatedRecord.toString().trim();
        if (!isValidRecord(normalizedRecord)) {
            JOptionPane.showMessageDialog(this,
                    "Enter correct record.",
                    "Invalid Record", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> updatedLines = new ArrayList<>(records);
        updatedLines.set(modelRow, normalizedRecord);
        writeRecords(updatedLines, "The record could not be updated.");
    }

    private String[] splitRecord(String record) {
        return ManageRecordsHelper.splitRecord(record);
    }


    private void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);;
        field.setBackground(Color.LIGHT_GRAY);
    }
    private String editDepartmentRecord(String record) {
        String[] parts = splitRecord(record);
        if (parts.length < 4) {
            return null;
        }

        String deptId = parts[0].trim();
        String deptName = parts[1].trim();
        String description = parts[2].trim();
        String existingManagerId = parts[3].trim();

        JTextField idField = new JTextField(deptId);
        setUneditable(idField);
        JTextField nameField = new JTextField(deptName);
        JTextField descriptionField = new JTextField(description);
        List<User> managers = UserRepository.loadAll();
        JComboBox<String> managerCombo = new JComboBox<>();
        int selectedManager = -1;
        for (int index = 0; index < managers.size(); index++) {
            User manager = managers.get(index);
            if (manager.getRole() != Role.MEDICAL_MANAGER) {
                continue;
            }
            managerCombo.addItem(manager.getFullName());
            if (manager.getUserId().equals(existingManagerId)) {
                selectedManager = managerCombo.getItemCount() - 1;
            }
        }
        if (selectedManager >= 0) {
            managerCombo.setSelectedIndex(selectedManager);
        }

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("DEPT_ID:"));
        form.add(idField);
        form.add(new JLabel("DEPT_NAME:"));
        form.add(nameField);
        form.add(new JLabel("HEAD_MANAGER_NAME:"));
        form.add(managerCombo);
        form.add(new JLabel("DESCRIPTION:"));
        form.add(descriptionField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Department", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        List<User> headManagers = managers.stream()
            .filter(user -> user.getRole() == Role.MEDICAL_MANAGER)
            .toList();
        
        String selectedManagerId = existingManagerId;
        if (managerCombo.getSelectedIndex() >= 0) {
            selectedManagerId = headManagers.get(managerCombo.getSelectedIndex()).getUserId();
        }

        return String.join("|", idField.getText().trim(), nameField.getText().trim(),
                descriptionField.getText().trim(), selectedManagerId);
    }

    private boolean isValidRecord(String record) {
        return ManageRecordsHelper.isValidRecord(record);
    }

    private void writeRecords(List<String> updatedRecords, String errorMessage) {
        if (!recordHelper.writeRecords(updatedRecords, errorMessage)) {
            JOptionPane.showMessageDialog(this, errorMessage,
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            refreshTable();
        }
    }

    private void refreshTable() {
        recordHelper.refreshTable();
        records = recordHelper.getRecords();
    }


    private void addDepartmentRow(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 4) {
            return;
        }

        tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                findName(parts[3].trim()),
                parts[2].trim()
        });
    }


    private String findName(String userId) {
        return ManageRecordsHelper.findName(userId);
    }


    private void addRecord() {
        if (consultationRateTable) {
            RecordsHelperConsultation.addConsultationRateRecord(this, fileName, this::refreshTable);
            return;
        }
        if (insuranceTable) {
            RecordsHelperInsurance.addInsuranceRecord(this, fileName, this::refreshTable);
            return;
        }
        if (departmentTable) {
            JTextField nameField = new JTextField();
            JTextField descriptionField = new JTextField();
            List<User> managers = UserRepository.loadAll();
            JComboBox<String> managerCombo = new JComboBox<>();

            for (User manager : managers) {
                if (manager.getRole() == Role.MEDICAL_MANAGER) {
                    managerCombo.addItem(manager.getFullName());
                }
            }

            JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
            form.add(new JLabel("DEPT_NAME:"));
            form.add(nameField);
            form.add(new JLabel("HEAD_MANAGER_NAME:"));
            form.add(managerCombo);
            form.add(new JLabel("DESCRIPTION:"));
            form.add(descriptionField);

            int choice = JOptionPane.showConfirmDialog(this, form,
                    "Add Department", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String deptId = IDGenerator.next("D", fileName);
            String deptName = nameField.getText().trim();
            String description = descriptionField.getText().trim();
            if (deptName.isEmpty() || description.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill in all department fields.",
                        "Invalid Department", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<User> medicalManagers = managers.stream()
                    .filter(user -> user.getRole() == Role.MEDICAL_MANAGER)
                    .toList();
            String selectedManagerId = medicalManagers.get(managerCombo.getSelectedIndex()).getUserId();
            String normalizedRecord = String.join("|", deptId, deptName, description, selectedManagerId);

            List<String> linesBeforeSave = FileManager.readLines(fileName);
            FileManager.appendLine(fileName, normalizedRecord);
            List<String> linesAfterSave = FileManager.readLines(fileName);
            boolean saved = linesAfterSave.size() == linesBeforeSave.size() + 1
                    && linesAfterSave.get(linesAfterSave.size() - 1).equals(normalizedRecord);
            if (!saved) {
                JOptionPane.showMessageDialog(this,
                        "The department could not be saved.",
                        "Save Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            refreshTable();
            return;
        }
        if (appointmentTable) {
            RecordsHelperAppointment.addAppointmentRecord(this, fileName, records, this::refreshTable);
            return;
        }
        if (assetTable) {
            RecordsHelperAsset.addAssetRecord(this, fileName, this::refreshTable);
            return;
        }
        //if not the above tables, will default to doing it via the txt file method
        String record = JOptionPane.showInputDialog(this,
                "Enter the record:\nExample: D001|Cardiology|Dr. Lee|Emergency care",
                "Add Record",
                JOptionPane.PLAIN_MESSAGE);
        if (record == null || record.trim().isEmpty()) {
            return;
        }

        String normalizedRecord = record.trim();
        if (normalizedRecord.contains("\n") || normalizedRecord.contains("\r")
                || normalizedRecord.indexOf('|') <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Enter a single pipe-separated record.",
                    "Invalid Record", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] fields = splitRecord(normalizedRecord);
        if (fields.length < 2) {
            JOptionPane.showMessageDialog(this,
                    "The record must contain at least 2 fields separated by '|'.",
                    "Invalid Record", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> linesBeforeSave = FileManager.readLines(fileName);
        FileManager.appendLine(fileName, normalizedRecord);
        List<String> linesAfterSave = FileManager.readLines(fileName);
        boolean saved = linesAfterSave.size() == linesBeforeSave.size() + 1
            && linesAfterSave.get(linesAfterSave.size() - 1).equals(normalizedRecord);
        if (!saved) {
            JOptionPane.showMessageDialog(this,
                    "The record could not be saved.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
    }


    private void deleteSelectedRecord() {
        int viewRow = recordsTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a record first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = recordsTable.convertRowIndexToModel(viewRow);
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the selected record?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (!recordHelper.deleteRecord(modelRow)) {
            JOptionPane.showMessageDialog(this,
                    "The record could not be deleted.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            refreshTable();
            return;
        }
    }

    private boolean hasIllegalChars(String... values) {
        return ManageRecordsHelper.hasIllegalChars(values);
    }
    
    private void addRosterRecord(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 4) {
            return;
        }

        tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                findName(parts[3].trim()),
                parts[2].trim()
        });
    }
    
    private String editRosterRecord(String record) {
        String[] parts = splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        String rosterId = parts[0].trim();
        String doctorId = parts[1].trim();
        String doctorName = parts[2].trim();
        String departmentName = parts[3].trim();
        String date = parts[4].trim();
        String shift = parts[5].trim();
        String status = parts[6].trim();

        JTextField rosterIdField = new JTextField(rosterId);
        setUneditable(rosterIdField);
        JTextField doctorIdField = new JTextField(doctorId);
        setUneditable(doctorIdField);
        JTextField doctorNameField = new JTextField(doctorName);
        JTextField departmentNameField = new JTextField(departmentName);
        JTextField dateField = new JTextField(date);
        JTextField shiftField = new JTextField(shift);
        JTextField statusField = new JTextField(status);

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("ROSTER_ID:"));
        form.add(rosterIdField);
        form.add(new JLabel("DOCTOR_ID:"));
        form.add(doctorIdField);
        form.add(new JLabel("DOCTOR_NAME:"));
        form.add(doctorNameField);
        form.add(new JLabel("DEPARTMENT:"));
        form.add(departmentNameField);
        form.add(new JLabel("DATE:"));
        form.add(dateField);
        form.add(new JLabel("SHIFT:"));
        form.add(shiftField);
        form.add(new JLabel("STATUS:"));
        form.add(statusField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Insurance", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String updatedRecord = String.join("|",
                rosterIdField.getText().trim(),
                doctorIdField.getText().trim(),
                doctorNameField.getText().trim(),
                departmentNameField.getText().trim(),
                dateField.getText().trim(),
                shiftField.getText().trim(),
                statusField.getText().trim());

        List<String> updatedLines = new ArrayList<>(records);
        int modelRow = recordsTable.convertRowIndexToModel(recordsTable.getSelectedRow());
        updatedLines.set(modelRow, updatedRecord);
        writeRecords(updatedLines, "The insurance record could not be updated.");
        return updatedRecord;
    }
    
    private void populateReportTable() {
        String currentPeriod = java.time.YearMonth.now().toString();

        List<String> reportRecords = FileManager.readLines(fileName);

        boolean currentMonthExists = false;

        for (String record : reportRecords) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = splitRecord(record);

            if (parts.length >= 6
                    && currentPeriod.equals(parts[0].trim())) {
                currentMonthExists = true;
                break;
            }
        }

        if (!currentMonthExists) {
            String newRecord = String.join("|",
                    currentPeriod,
                    String.valueOf(reportData.getTotalPatients()),
                    String.valueOf(reportData.getAppointments()),
                    String.valueOf(reportData.getCompleted()),
                    String.valueOf(reportData.getCancelled()),
                    String.valueOf(reportData.getTotalRevenue())
            );

            FileManager.appendLine(fileName, newRecord);
        }

        refreshTable();
    }
}
