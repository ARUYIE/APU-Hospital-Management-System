package hms.gui.panels;

import hms.util.Asset;
import hms.util.FileManager;
import hms.util.UserRepository;
import hms.role.Role;
import hms.role.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// for department,appontment, insurance
public class ManageRecordsPanel extends JPanel {

    private final String fileName;
    private final boolean departmentTable;
    private final boolean appointmentTable;
    private final JComboBox<String> doctorSearchBox = new JComboBox<>();
    private final boolean assetTable;
    private final boolean shiftTime;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final DefaultTableModel tableModel;
    private final JTable recordsTable;
    private List<String> records = new ArrayList<>();
    private String headerLine;
    private boolean headerPresent;
    private boolean initialized;

    public ManageRecordsPanel(String title, String fileName) {
        this.fileName = fileName;
        departmentTable = "department.txt".equalsIgnoreCase(fileName);
        assetTable = "hospital_assets.txt".equalsIgnoreCase(fileName);
        appointmentTable = "bookings.txt".equalsIgnoreCase(fileName);
        shiftTime = "shift_time.txt".equalsIgnoreCase(fileName);
        insuranceTable = "insurance_networks.txt".equalsIgnoreCase(fileName);
        consultationRateTable = "consultation_rates.txt".equalsIgnoreCase(fileName);
        tableModel = new DefaultTableModel(
                departmentTable
                ? new String[]{"DEPTARTMENT_ID", "DEPTARTMENT_NAME", "HEAD_MANAGER_NAME", "DESCRIPTION"}
                : appointmentTable
                ? new String[]{"APPOINTMENT_ID", "PATIENT_NAME", "DOCTOR_NAME", "DATE", "TIME", "STATUS", "NOTES"}
                : assetTable
                ? new String[]{"ASSET_ID", "ROOM_TYPE", "ROOM_NAME", "LOCATION", "STATUS", "NOTES"}
                : insuranceTable
                ? new String[]{"INSURANCE_ID", "PROVIDER_NAME", "COVERAGE_RATE", "COVERAGE_PERCENTAGE", "STATUS", "CONTACT_INFO", "EFFECTIVE_DATE"}
                : consultationRateTable
                ? new String[]{"SPECIALTY", "BASE_RATE", "MIN_RATE", "MAX_RATE", "CURRENCY", "EFFECTIVE_DATE"}
                : new String[]{"#", "Record"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
        recordsTable = new JTable(tableModel);

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
            actions.add(reserveButton);
            actions.add(finishButton);
        } else if(appointmentTable){
            actions.add(new JLabel("Search Doctor:"));
            populateDoctorSearchBox();
            actions.add(doctorSearchBox);
        }
        actions.add(refreshButton);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        
        

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
        if (modelRow < 0 || modelRow >= records.size()) {
            return null;
        }
        String[] parts = splitRecord(records.get(modelRow));
        return (parts.length > 0) ? parts[0].trim() : null;
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

        Asset asset = hms.util.AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(this,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.util.List<String> departments = hms.util.DepartmentManager.getDepartmentNames();
        if (departments.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There are no departments available to reserve this asset.",
                    "No Departments Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> departmentCombo = new JComboBox<>(departments.toArray(new String[0]));
        int choice = JOptionPane.showConfirmDialog(this, departmentCombo,
                "Select Department Reserving This Asset",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String departmentName = (String) departmentCombo.getSelectedItem();
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return;
        }

        if (!"AVAILABLE".equalsIgnoreCase(asset.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "This ward/clinic is already in use.",
                    "Reservation Conflict", JOptionPane.WARNING_MESSAGE);
            return;
        }

        asset.setStatus("OCCUPIED");
        if (asset.getDescription() == null || asset.getDescription().trim().isEmpty()) {
            asset.setDescription("Reserved by: " + departmentName.trim());
        } else if (!asset.getDescription().contains(departmentName.trim())) {
            asset.setDescription(asset.getDescription() + " | Reserved by: " + departmentName.trim());
        }

        if (hms.util.AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(this, "Ward/clinic reserved successfully.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this,
                    "The reservation could not be saved.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
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

        Asset asset = hms.util.AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(this,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark this ward/clinic as finished and available again?",
                "Finish Usage",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        asset.setStatus("AVAILABLE");
        if (hms.util.AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(this, "Ward/clinic marked as finished.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this,
                    "The status update could not be saved.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
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
                            : (String) JOptionPane.showInputDialog(this,
                                    "Edit record:", "Edit Record",
                                    JOptionPane.PLAIN_MESSAGE, null, null,
                                    records.get(modelRow));

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
        return record.split("\\|", -1);
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
        String selectedManagerId = selectedManager >= 0
            ? headManagers.get(managerCombo.getSelectedIndex()).getUserId()
            : existingManagerId;
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
        JTextField statusField = new JTextField(status);
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
                statusField.getText().trim(),
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
        String notes = parts.length > 5 ? parts[5].trim() : "";

        JTextField assetIdField = new JTextField(assetId);
        setUneditable(assetIdField);
        JTextField roomTypeField = new JTextField(roomType);
        JTextField roomNameField = new JTextField(roomName);
        JTextField locationField = new JTextField(location);
        JTextField statusField = new JTextField(status);
        JTextField notesField = new JTextField(notes);

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
        form.add(new JLabel("NOTES:"));
        form.add(notesField);

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
                notesField.getText().trim());
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
        try {
            double base = Double.parseDouble(baseRate.trim());
            double minimum = Double.parseDouble(minRate.trim());
            double maximum = Double.parseDouble(maxRate.trim());
            return minimum <= base && base <= maximum;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isValidRecord(String record) {
        return !record.isEmpty()
                && !record.contains("\n")
                && !record.contains("\r")
                && record.indexOf('|') > 0;
    }

    private void writeRecords(List<String> updatedRecords, String errorMessage) {
        List<String> linesToWrite = new ArrayList<>();
        if (headerLine != null) {
            linesToWrite.add(headerLine);
        }
        linesToWrite.addAll(updatedRecords);
        FileManager.writeAllLines(fileName, linesToWrite);
        if (!FileManager.readLines(fileName).equals(linesToWrite)) {
            JOptionPane.showMessageDialog(this, errorMessage,
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            refreshTable();
            return;
        }
        refreshTable();
    }

    private void refreshTable() {
        List<String> lines = FileManager.readLines(fileName);
        if (!initialized) {
            headerPresent = !lines.isEmpty();
            initialized = true;
        }
        headerLine = headerPresent && !lines.isEmpty() ? lines.remove(0) : null;
        records = lines;
        tableModel.setRowCount(0);
        for (int index = 0; index < records.size(); index++) {
            if (departmentTable) {
                addDepartmentRow(records.get(index));
            } else if(appointmentTable) {
                addAppointmentRow(records.get(index));
            } else if(insuranceTable) {
                addInsuranceRow(records.get(index));
            } else if (consultationRateTable) {
                addConsultationRateRow(records.get(index));
            } else if(assetTable) {
                addAssetRow (records.get(index));
            } else {
                tableModel.addRow(new Object[]{index + 1, records.get(index)});
            }
        }
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
        if (userId.isEmpty()) {
            return "";
        }
        for (User user : UserRepository.loadAll()) {
            if (userId.equals(user.getUserId())) {
                return user.getFullName();
            }
        }
        return userId;
    }

    private void addRecord() {
        if (consultationRateTable) {
            addConsultationRateRecord();
            return;
        }
        if (insuranceTable) {
            addInsuranceRecord();
            return;
        }
        if (departmentTable) {
            JTextField idField = new JTextField();
            JTextField nameField = new JTextField();
            JTextField descriptionField = new JTextField();
            List<User> managers = UserRepository.loadAll();
            JComboBox<String> managerCombo = new JComboBox<>();

            for (User manager : managers) {
                if (manager.getRole() == Role.MEDICAL_MANAGER) {
                    managerCombo.addItem(manager.getFullName());
                }
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
                    "Add Department", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String deptId = idField.getText().trim();
            String deptName = nameField.getText().trim();
            String description = descriptionField.getText().trim();
            if (deptId.isEmpty() || deptName.isEmpty() || description.isEmpty()) {
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
        JTextField idField = new JTextField();
        JTextField providerField = new JTextField();
        JTextField coverageRateField = new JTextField();
        JTextField coveragePercentageField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        JTextField contactField = new JTextField();
        JTextField effectiveDateField = new JTextField(java.time.LocalDate.now().toString());

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("INSURANCE_ID:")); form.add(idField);
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

        if (idField.getText().trim().isEmpty() || providerField.getText().trim().isEmpty()
                || coverageRateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Insurance ID, provider name, and coverage rate are required.",
                    "Invalid Insurance Network", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileManager.appendLine(fileName, String.join("|",
                idField.getText().trim(), providerField.getText().trim(),
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

        records.remove(modelRow);
        List<String> linesToWrite = new ArrayList<>();
        if (headerLine != null) {
            linesToWrite.add(headerLine);
        }
        linesToWrite.addAll(records);
        FileManager.writeAllLines(fileName, linesToWrite);
        if (!FileManager.readLines(fileName).equals(linesToWrite)) {
            JOptionPane.showMessageDialog(this,
                    "The record could not be deleted.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            refreshTable();
            return;
        }
        refreshTable();
    }
}