package hms.gui.panels;

import hms.util.Asset;
import hms.util.FileManager;
import hms.util.IDGenerator;
import hms.util.ManageRecordsHelper;
import hms.util.UserRepository;
import hms.role.Role;
import hms.role.User;
import hms.util.ReportData;
import hms.util.RosterData;
import hms.util.ManagerMethods;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// for wards, department,appontment, consultation rate, insurance
public class ManageRecordsPanel extends JPanel {

    private final String fileName;
    
    private final boolean departmentTable;
    private final boolean appointmentTable;
    private final JComboBox<String> doctorSearchBox = new JComboBox<>();
    private final JTextField assetSearchField = new JTextField(14);
    private final boolean assetTable;
    private final boolean shiftTime;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final boolean rosterTable;
    private final boolean reportTable;
    private final DefaultTableModel tableModel;
    private final JTable recordsTable;
    private final ManageRecordsHelper recordHelper;
    private List<String> records = new ArrayList<>();
    private String headerLine;
    private boolean headerPresent;
    private boolean initialized;
    
    private final ReportData reportData;
    private final ManagerMethods managerMethods;

    public ManageRecordsPanel(String title, String fileName) {
        this.fileName = fileName;
        this.managerMethods = new ManagerMethods(this, fileName);
        reportData = new ReportData();
        
        // Tan Rui En - Admin
        assetTable = "hospital_assets.txt".equalsIgnoreCase(fileName);
        appointmentTable = "bookings.txt".equalsIgnoreCase(fileName);
        shiftTime = "shift_time.txt".equalsIgnoreCase(fileName);
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
                ? new String[]{"ROSTER_ID", "DOCTOR_NAME", "MANAGED_BY", "DEPARTMENT", "DATE", "SHIFT", "STATUS"}
                : reportTable
                ? new String[]{"REPORT_PERIOD", "TOTAL_PATIENTS", "APPOINTMENTS", "COMPLETED_APPOINTMENTS", "CANCELLED_APPOINTMENTS", "TOTAL_REVENUE"}
                : new String[]{"#", "Record"}, 0) {
                    
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
        recordsTable = new JTable(tableModel);
        recordHelper = new ManageRecordsHelper(fileName, tableModel, doctorSearchBox);

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
            actions.add(new JLabel("Search Wards/Clinics:"));
            assetSearchField.setToolTipText("Search by name, type, location, or status");
            assetSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            });
            actions.add(assetSearchField);
            actions.add(reserveButton);
            actions.add(finishButton);
        } else if(appointmentTable){
            actions.add(new JLabel("Search Doctor:"));
            populateDoctorSearchBox();
            actions.add(doctorSearchBox);
        } else if(reportTable){
            managerMethods.populateReportTable(reportData);
        } else if(rosterTable){
            //Load roster
        }
        
        if (!reportTable){  // Only exclude report table because no need function button
            actions.add(refreshButton);
            actions.add(addButton);
            actions.add(editButton);
            actions.add(deleteButton);
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
                ? editAppointmentRecord(records.get(modelRow))
                : insuranceTable
                ? editInsuranceRecord(records.get(modelRow))
                : consultationRateTable
                ? editConsultationRateRecord(records.get(modelRow))
                : assetTable
                ? editAssetRecord(records.get(modelRow))
                : rosterTable
                ? managerMethods.editRosterRecord(records.get(modelRow))
                : (String) JOptionPane.showInputDialog(this,
                        "Edit record:", "Edit Record",
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        records.get(modelRow));

        if (updatedRecord == null) {
            return;
        }

        String normalizedRecord = updatedRecord.trim();

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
    private String editAppointmentRecord(String record) {
        String[] parts = splitRecord(record);
        if (parts.length < 7) {
            return null;
        }

        String appointmentId = parts[0].trim();
        String patientId = parts[1].trim();
        String doctorId = parts[2].trim(); 
        String date = parts[3].trim();
        String time = parts[4].trim();
        String status = parts[5].trim();
        String notes = parts[6].trim();             
        JTextField appointmentIdField = new JTextField(appointmentId);
        setUneditable(appointmentIdField);
        JTextField patientIdField = new JTextField(patientId);
        JTextField doctorIdField = new JTextField(doctorId);
        JTextField dateField = new JTextField(date);
        JTextField timeField = new JTextField(time);
        JComboBox<String> statusField = new JComboBox<>(new String[]{"SCHEDULED", "COMPLETED", "CANCELLED"});
        JTextField notesField = new JTextField(notes);

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
        form.add(new JLabel("NOTES:"));
        form.add(notesField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Appointment", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        return String.join("|",
                appointmentIdField.getText().trim(),
                patientIdField.getText().trim(),
                doctorIdField.getText().trim(),
                dateField.getText().trim(),
                timeField.getText().trim(),
                statusField.getSelectedItem().toString().trim(),
                notesField.getText().trim());
    }


    private void populateDoctorSearchBox() {
        doctorSearchBox.addItem("All Doctors");
        for (User user : UserRepository.loadAll()) {
            if (user.getRole() == Role.DOCTOR) {
                doctorSearchBox.addItem(user.getFullName());
            }
        }
        doctorSearchBox.addActionListener(e -> refreshTable());
    }



    private String editAssetRecord(String record) {
        String[] parts = splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        String assetId = parts[0].trim();
        String roomType = parts[1].trim();
        String roomName = parts[2].trim();
        String location = parts[3].trim();
        String status = parts[4].trim();
        String reservedby = parts.length > 5 ? parts[5].trim() : "";

        JTextField assetIdField = new JTextField(assetId);
        setUneditable(assetIdField);
        JTextField roomTypeField = new JTextField(roomType);
        JTextField roomNameField = new JTextField(roomName);
        JTextField locationField = new JTextField(location);
        JTextField statusField = new JTextField(status);
        JTextField reservedByField = new JTextField(reservedby);

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("ASSET_ID:"));
        form.add(assetIdField);
        form.add(new JLabel("ROOM_TYPE:"));
        form.add(roomTypeField);
        form.add(new JLabel("ROOM_NAME:"));
        form.add(roomNameField);
        form.add(new JLabel("LOCATION:"));
        form.add(locationField);
        form.add(new JLabel("STATUS:"));
        form.add(statusField);
        form.add(new JLabel("RESERVED BY:"));
        form.add(reservedByField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Asset", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        return String.join("|",
                assetIdField.getText().trim(),
                roomTypeField.getText().trim(),
                roomNameField.getText().trim(),
                locationField.getText().trim(),
                statusField.getText().trim(),
                reservedByField.getText().trim());
    }
    private String editInsuranceRecord(String record) {
        String[] parts = splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        String insuranceId = parts[0].trim();
        String insuranceName = parts[1].trim();
        String coverageRate = parts[2].trim();
        String coveragePercentage = parts[3].trim();
        String status = parts[4].trim();
        String contactInfo = parts[5].trim();
        String effectiveDate = parts.length > 6 ? parts[6].trim() : "";

        JTextField insuranceIdField = new JTextField(insuranceId);
        setUneditable(insuranceIdField);
        JTextField insuranceNameField = new JTextField(insuranceName);
        setUneditable(insuranceNameField);
        JTextField coverageRateField = new JTextField(coverageRate);
        JTextField coveragePercentageField = new JTextField(coveragePercentage);
        JTextField statusField = new JTextField(status);
        JTextField contactInfoField = new JTextField(contactInfo);
        JTextField effectiveDateField = new JTextField(effectiveDate);

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("INSURANCE_ID:"));
        form.add(insuranceIdField);
        form.add(new JLabel("INSURANCE_NAME:"));
        form.add(insuranceNameField);
        form.add(new JLabel("COVERAGE_RATE:"));
        form.add(coverageRateField);
        form.add(new JLabel("COVERAGE_PERCENTAGE:"));
        form.add(coveragePercentageField);
        form.add(new JLabel("STATUS:"));
        form.add(statusField);
        form.add(new JLabel("CONTACT_INFO:"));
        form.add(contactInfoField);
        form.add(new JLabel("EFFECTIVE_DATE:"));
        form.add(effectiveDateField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Insurance", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String updatedRecord = String.join("|",
                insuranceIdField.getText().trim(),
                insuranceNameField.getText().trim(),
                coverageRateField.getText().trim(),
                coveragePercentageField.getText().trim(),
                statusField.getText().trim(),
                contactInfoField.getText().trim(),
                effectiveDateField.getText().trim());

        List<String> updatedLines = new ArrayList<>(records);
        int modelRow = recordsTable.convertRowIndexToModel(recordsTable.getSelectedRow());
        updatedLines.set(modelRow, updatedRecord);
        writeRecords(updatedLines, "The insurance record could not be updated.");
        return updatedRecord;
    }

    private String editConsultationRateRecord(String record) {
        String[] parts = splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        JTextField specialtyField = new JTextField(parts[0].trim());
        setUneditable(specialtyField);
        JTextField baseRateField = new JTextField(parts[1].trim());
        JTextField minRateField = new JTextField(parts[2].trim());
        JTextField maxRateField = new JTextField(parts[3].trim());
        JTextField currencyField = new JTextField(parts[4].trim());
        JTextField effectiveDateField = new JTextField(parts[5].trim());

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("SPECIALTY:"));
        form.add(specialtyField);
        form.add(new JLabel("BASE_RATE:"));
        form.add(baseRateField);
        form.add(new JLabel("MIN_RATE:"));
        form.add(minRateField);
        form.add(new JLabel("MAX_RATE:"));
        form.add(maxRateField);
        form.add(new JLabel("CURRENCY:"));
        form.add(currencyField);
        form.add(new JLabel("EFFECTIVE_DATE:"));
        form.add(effectiveDateField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Consultation Rate", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        if (!validRateFields(baseRateField.getText(), minRateField.getText(), maxRateField.getText())) {
            JOptionPane.showMessageDialog(this,
                    "Rates must be numeric and satisfy MIN_RATE <= BASE_RATE <= MAX_RATE.",
                    "Invalid Consultation Rate", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return String.join("|",
                specialtyField.getText().trim(), baseRateField.getText().trim(),
                minRateField.getText().trim(), maxRateField.getText().trim(),
                currencyField.getText().trim(), effectiveDateField.getText().trim());
    }

    private boolean validRateFields(String baseRate, String minRate, String maxRate) {
        return ManageRecordsHelper.validRateFields(baseRate, minRate, maxRate);
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

    public void refreshTable() {
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

    private void addAppointmentRow(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 7) {
            return;
        }
        
        String doctorName = findName(parts[2].trim());
        String selectedDoctor = (String) doctorSearchBox.getSelectedItem();
        if (selectedDoctor != null && !selectedDoctor.equals("All Doctors") && !selectedDoctor.equals("Doctor Name")) {
            if (!doctorName.equals(selectedDoctor)) {
                return;
            }
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
    }
    
    private void addAssetRow(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 6) {
            return;
        }

        tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts.length > 5 ? parts[5].trim() : ""
        });
    }
    private void addInsuranceRow(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 6) {
            return;
        }

        tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts.length > 6 ? parts[6].trim() : ""
        });
    }

    private void addConsultationRateRow(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 6) {
            return;
        }
        tableModel.addRow(new Object[]{
                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                parts[3].trim(), parts[4].trim(), parts[5].trim()
        });
    }
    private String findName(String userId) {
        return ManageRecordsHelper.findName(userId);
    }

    private void addRecord() {
        if (consultationRateTable) {
            addConsultationRateRecord();
            return;
        }
        if (rosterTable) {
            managerMethods.addRosterRecord();
            return;
        }
        
        if (insuranceTable) {
            addInsuranceRecord();
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
            addAppointmentRecord();
            return;
        }
        if (assetTable) {
            addAssetRecord();
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

    private void addInsuranceRecord() {
        JTextField providerField = new JTextField();
        JTextField coverageRateField = new JTextField();
        JTextField coveragePercentageField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        JTextField contactField = new JTextField();
        JTextField effectiveDateField = new JTextField(java.time.LocalDate.now().toString());

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("PROVIDER_NAME:")); form.add(providerField);
        form.add(new JLabel("COVERAGE_RATE:")); form.add(coverageRateField);
        form.add(new JLabel("COVERAGE_PERCENTAGE:")); form.add(coveragePercentageField);
        form.add(new JLabel("STATUS:")); form.add(statusCombo);
        form.add(new JLabel("CONTACT_INFO:")); form.add(contactField);
        form.add(new JLabel("EFFECTIVE_DATE:")); form.add(effectiveDateField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Add Accepted Insurance Network", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String percentage = coveragePercentageField.getText().trim();
        try {
            double numericPercentage = Double.parseDouble(percentage);
            if (numericPercentage < 0 || numericPercentage > 100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this,
                    "Coverage percentage must be a number from 0 to 100.",
                    "Invalid Insurance Network", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (providerField.getText().trim().isEmpty()
                || coverageRateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Insurance ID, provider name, and coverage rate are required.",
                    "Invalid Insurance Network", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileManager.appendLine(fileName, String.join("|",
            IDGenerator.next("INS", fileName), providerField.getText().trim(),
                coverageRateField.getText().trim(), percentage,
                (String) statusCombo.getSelectedItem(), contactField.getText().trim(),
                effectiveDateField.getText().trim()));
        refreshTable();
    }

    private void addConsultationRateRecord() {
        JTextField specialtyField = new JTextField();
        JTextField baseRateField = new JTextField();
        JTextField minRateField = new JTextField();
        JTextField maxRateField = new JTextField();
        JTextField currencyField = new JTextField("USD");
        JTextField effectiveDateField = new JTextField(java.time.LocalDate.now().toString());

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("SPECIALTY:")); form.add(specialtyField);
        form.add(new JLabel("BASE_RATE:")); form.add(baseRateField);
        form.add(new JLabel("MIN_RATE:")); form.add(minRateField);
        form.add(new JLabel("MAX_RATE:")); form.add(maxRateField);
        form.add(new JLabel("CURRENCY:")); form.add(currencyField);
        form.add(new JLabel("EFFECTIVE_DATE:")); form.add(effectiveDateField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Add Consultation Rate", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        if (specialtyField.getText().trim().isEmpty()
                || !validRateFields(baseRateField.getText(), minRateField.getText(), maxRateField.getText())) {
            JOptionPane.showMessageDialog(this,
                    "Enter a specialty and valid rates where MIN_RATE <= BASE_RATE <= MAX_RATE.",
                    "Invalid Consultation Rate", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileManager.appendLine(fileName, String.join("|",
                specialtyField.getText().trim(), baseRateField.getText().trim(),
                minRateField.getText().trim(), maxRateField.getText().trim(),
                currencyField.getText().trim(), effectiveDateField.getText().trim()));
        refreshTable();
    }

     private void addAssetRecord() {
        JTextField roomTypeField = new JTextField();
        JTextField roomNameField = new JTextField();
        JTextField locationField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"AVAILABLE", "OCCUPIED"});
        JTextField reservedByField = new JTextField();
 
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("ROOM_TYPE:")); form.add(roomTypeField);
        form.add(new JLabel("ROOM_NAME:")); form.add(roomNameField);
        form.add(new JLabel("LOCATION:")); form.add(locationField);
        form.add(new JLabel("STATUS:")); form.add(statusCombo);
        form.add(new JLabel("RESERVED BY:")); form.add(reservedByField);
 
        int choice = JOptionPane.showConfirmDialog(this, form,
                "Add Ward / Clinic", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
 
        String assetId = IDGenerator.next("ASSET", fileName);
        String roomType = roomTypeField.getText().trim();
        String roomName = roomNameField.getText().trim();
        String location = locationField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();
        String reservedBy = reservedByField.getText().trim();
 
        if (roomType.isEmpty() || roomName.isEmpty() || location.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Asset ID, room type, room name, and location are required.",
                    "Invalid Asset", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (hasIllegalChars(assetId, roomType, roomName, location, reservedBy)) {
            JOptionPane.showMessageDialog(this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Asset", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        // Keep status and reservation consistent with Reserve / Finished buttons
        if ("AVAILABLE".equals(status)) {
            reservedBy = "";
        } else if (reservedBy.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "An occupied ward/clinic must have a 'Reserved By' value.",
                    "Invalid Asset", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        FileManager.appendLine(fileName, String.join("|",
                assetId, roomType, roomName, location, status, reservedBy));
        refreshTable();
    }
 
    private void addAppointmentRecord() {
        List<User> users = UserRepository.loadAll();
        // NOTE: change Role.PATIENT if your Role enum names it differently
        List<User> patients = users.stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();
        List<User> doctors = users.stream()
                .filter(user -> user.getRole() == Role.DOCTOR)
                .toList();
 
        if (patients.isEmpty() || doctors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "At least one patient and one doctor must exist before adding an appointment.",
                    "Cannot Add Appointment", JOptionPane.WARNING_MESSAGE);
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
        JTextField timeField = new JTextField("09:00");
        // NOTE: adjust these to match the statuses already used in bookings.txt
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"SCHEDULED", "COMPLETED", "CANCELLED"});
        JTextField notesField = new JTextField();
 
        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("PATIENT_NAME:")); form.add(patientCombo);
        form.add(new JLabel("DOCTOR_NAME:")); form.add(doctorCombo);
        form.add(new JLabel("DATE (YYYY-MM-DD):")); form.add(dateField);
        form.add(new JLabel("TIME (HH:mm):")); form.add(timeField);
        form.add(new JLabel("STATUS:")); form.add(statusCombo);
        form.add(new JLabel("NOTES:")); form.add(notesField);
 
        int choice = JOptionPane.showConfirmDialog(this, form,
                "Add Appointment", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
 
        String appointmentId = IDGenerator.next("B", fileName);
        String date = dateField.getText().trim();
        String time = timeField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();
        String notes = notesField.getText().trim();
 
        if (hasIllegalChars(appointmentId, date, time, notes)) {
            JOptionPane.showMessageDialog(this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Appointment", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            java.time.LocalDate.parse(date);
        } catch (java.time.format.DateTimeParseException exception) {
            JOptionPane.showMessageDialog(this,
                    "Date must be in YYYY-MM-DD format.",
                    "Invalid Appointment", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            java.time.LocalTime.parse(time, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } catch (java.time.format.DateTimeParseException exception) {
            JOptionPane.showMessageDialog(this,
                    "Time must be in HH:mm format (e.g. 09:30).",
                    "Invalid Appointment", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        // Combo indexes line up with the filtered lists, so map back to user IDs
        String patientId = patients.get(patientCombo.getSelectedIndex()).getUserId();
        String doctorId = doctors.get(doctorCombo.getSelectedIndex()).getUserId();
 
        // Prevent double-booking the same doctor at the same date/time
        for (String record : records) {
            String[] parts = splitRecord(record);
            if (parts.length >= 6
                    && parts[2].trim().equals(doctorId)
                    && parts[3].trim().equals(date)
                    && parts[4].trim().equals(time)
                    && !"CANCELLED".equalsIgnoreCase(parts[5].trim())) {
                JOptionPane.showMessageDialog(this,
                        "This doctor already has an appointment at that date and time.",
                        "Scheduling Conflict", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
 
        FileManager.appendLine(fileName, String.join("|",
                appointmentId, patientId, doctorId, date, time, status, notes));
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
    
    public JTable getRecordsTable() {
        return recordsTable;
    }
}