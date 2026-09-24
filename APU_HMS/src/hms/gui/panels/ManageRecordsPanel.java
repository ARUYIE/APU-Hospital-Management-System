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
import hms.util.Session;
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
    private final JComboBox<String> assetSearchBox = new JComboBox<>(new String[]{"All Room Types"});
    private final JComboBox<String> reportSearchBox = new JComboBox<>();
    private final boolean assetTable;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final boolean rosterTable;
    private final boolean reportTable;
    private final boolean consultationTable;
    private final boolean prescriptionTable;
    private final boolean labRequestTable;
    private final DefaultTableModel tableModel;
    private final JTable recordsTable;
    private final ManageRecordsHelper recordHelper;
    private List<String> records = new ArrayList<>();
    private final ManagerMethods managerMethods;

    public ManageRecordsPanel(String title, String fileName) {
        this.fileName = fileName;
        this.managerMethods = new ManagerMethods(this, fileName);
        
        // Tan Rui En - Admin
        assetTable = "hospital_assets.txt".equalsIgnoreCase(fileName);
        appointmentTable = "bookings.txt".equalsIgnoreCase(fileName);
        insuranceTable = "insurance_networks.txt".equalsIgnoreCase(fileName);
        consultationRateTable = "consultation_rates.txt".equalsIgnoreCase(fileName);
        
        // Wong Willard - Medical Manager
        departmentTable = "department.txt".equalsIgnoreCase(fileName);
        rosterTable = "roster.txt".equalsIgnoreCase(fileName);
        reportTable = "report.txt".equalsIgnoreCase(fileName);
        
        // Low Kai Lun - Doctor
        consultationTable = "vital_signs.txt".equalsIgnoreCase(fileName);
        prescriptionTable = "prescriptions.txt".equalsIgnoreCase(fileName);
        labRequestTable = "lab_requests.txt".equalsIgnoreCase(fileName);
        
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
                : consultationTable
                ? new String[]{"VITAL_SIGN_ID", "PATIENT_ID", "DOCTOR_ID", "CONSULTATION_ID", "BP", "HEART_RATE", "TEMPERATURE", "DATE", "NOTES"}
                : prescriptionTable
                ? new String[]{"PRESCRIPTION_ID", "PATIENT_ID", "DOCTOR_ID", "MEDICATION", "DOSAGE", "DURATION", "DATE_ISSUED", "STATUS"}
                : labRequestTable       
                ? new String[]{"REQUEST_ID", "PATIENT_ID", "DOCTOR_ID", "TEST_TYPE", "STATUS", "DATE_REQUESTED", "DATE_COMPLETED", "RESULT"}
                : new String[]{"#", "Record"}, 0) {
                   
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
        recordsTable = new JTable(tableModel);
        recordHelper = new ManageRecordsHelper(fileName, tableModel, doctorSearchBox, assetSearchBox, false, false, false);

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

        JButton markCompletedButton = new JButton("Mark Completed");
        markCompletedButton.addActionListener(e -> updateSelectedAppointmentStatus("COMPLETED"));

        JButton cancelAppointmentButton = new JButton("Cancel Appointment");
        cancelAppointmentButton.addActionListener(e -> updateSelectedAppointmentStatus("CANCELLED"));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        
        if (assetTable) {
            assetSearchBox.setToolTipText("Filter by room type");
            assetSearchBox.addActionListener(e -> refreshTable());
        } else if(appointmentTable){
            RecordsHelperAppointment.populateDoctorSearchBox(doctorSearchBox);
            doctorSearchBox.addActionListener(e -> refreshTable());
        } else if(reportTable){
            actions.add(new JLabel("Filter:"));
            actions.add(reportSearchBox);
            setupReportFilter();
            managerMethods.populateReportTable();
        }
        if (!reportTable){  // Only exclude report table because no need function button
            actions.add(refreshButton);
            actions.add(addButton);
            actions.add(editButton);
            actions.add(deleteButton);
        }


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
        if (appointmentTable) {
            JPanel appointmentActions = new JPanel();
            appointmentActions.setLayout(new BoxLayout(appointmentActions, BoxLayout.Y_AXIS));

            JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            searchActions.add(new JLabel("Search Doctor:"));
            searchActions.add(doctorSearchBox);
            searchActions.add(markCompletedButton);
            searchActions.add(cancelAppointmentButton);

            JPanel recordActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            recordActions.add(refreshButton);
            recordActions.add(addButton);
            recordActions.add(editButton);
            recordActions.add(deleteButton);

            appointmentActions.add(searchActions);
            appointmentActions.add(recordActions);
            actions = appointmentActions;
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

    private void updateSelectedAppointmentStatus(String newStatus) {
        if (!appointmentTable) {
            return;
        }

        String appointmentId = getSelectedAssetId();
        if (appointmentId == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an appointment first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> updatedLines = RecordsHelperAppointment.updateAppointmentStatus(this, records, appointmentId, newStatus);
        if (updatedLines == null) {
            return;
        }

        writeRecords(updatedLines, "The appointment status could not be updated.");
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
                ? managerMethods.editDepartmentRecord(records.get(modelRow))
                : appointmentTable
                ? RecordsHelperAppointment.editAppointmentRecord(this, records.get(modelRow))
                : insuranceTable
                ? RecordsHelperInsurance.editInsuranceRecord(this, records.get(modelRow))
                : consultationRateTable
                ? RecordsHelperConsultation.editConsultationRateRecord(this, records.get(modelRow))
                : assetTable
                ? RecordsHelperAsset.editAssetRecord(this, records.get(modelRow))
                : rosterTable
                ? managerMethods.editRosterRecord(records.get(modelRow))
                : consultationTable
                ? editVitalSignRecord(records.get(modelRow))
                : prescriptionTable
                ? editPrescriptionRecord(records.get(modelRow))
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

        writeRecords(
                updatedLines,
                "The record could not be updated."
        );
    }

    private String[] splitRecord(String record) {
        return ManageRecordsHelper.splitRecord(record);
    }


    public void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);;
        field.setBackground(Color.LIGHT_GRAY);
    }
    
    private String editVitalSignRecord(String record) {
        String[] parts = splitRecord(record);

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

        String notes;
        if (parts.length >= 9) {
            notes = parts[8].trim();
        } else {
            notes = "";
        }

        JTextField vitalSignIdField = new JTextField(vitalSignId);
        setUneditable(vitalSignIdField);

        JTextField patientIdField = new JTextField(patientId);
        JTextField doctorIdField = new JTextField(doctorId);
        JTextField consultationIdField = new JTextField(consultationId);
        JTextField bpField = new JTextField(bp);
        JTextField heartRateField = new JTextField(heartRate);
        JTextField temperatureField = new JTextField(temperature);
        JTextField dateField = new JTextField(date);
        JTextField notesField = new JTextField(notes);

        JPanel form = new JPanel(new GridLayout(9, 2, 8, 8));

        form.add(new JLabel("VITAL_SIGN_ID:"));
        form.add(vitalSignIdField);

        form.add(new JLabel("PATIENT_ID:"));
        form.add(patientIdField);

        form.add(new JLabel("DOCTOR_ID:"));
        form.add(doctorIdField);

        form.add(new JLabel("CONSULTATION_ID:"));
        form.add(consultationIdField);

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
                this,
                form,
                "Edit Vital Sign Record",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
            }

        String updatedPatientId = patientIdField.getText().trim();
        String updatedConsultationId = consultationIdField.getText().trim();
        String updatedBp = bpField.getText().trim();
        String updatedHeartRate = heartRateField.getText().trim();
        String updatedTemperature = temperatureField.getText().trim();
        String updatedDate = dateField.getText().trim();
        String updatedNotes = notesField.getText().trim();

        if (updatedPatientId.isEmpty()
                || updatedBp.isEmpty()
                || updatedHeartRate.isEmpty()
                || updatedTemperature.isEmpty()
                || updatedDate.isEmpty()
                || updatedNotes.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Patient ID, BP, heart rate, temperature, date and notes are required.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
                );
            return null;
            }

        if (hasIllegalChars(updatedPatientId, updatedConsultationId, updatedBp,
                updatedHeartRate, updatedTemperature, updatedDate, updatedNotes)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
                );
            return null;
            }

        return String.join("|",
            vitalSignIdField.getText().trim(),
            updatedPatientId,
            doctorIdField.getText().trim(),
            updatedConsultationId,
            updatedBp,
            updatedHeartRate,
            updatedTemperature,
            updatedDate,
            updatedNotes
        );
    }
    
    private String editPrescriptionRecord(String record) {
        String[] parts = splitRecord(record);

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

        JTextField prescriptionIdField = new JTextField(prescriptionId);
        setUneditable(prescriptionIdField);

        JTextField patientIdField = new JTextField(patientId);
        JTextField doctorIdField = new JTextField(doctorId);
        setUneditable(doctorIdField);
        JTextField medicationField = new JTextField(medication);
        JComboBox<String> dosageCombo = new JComboBox<>(new String[]{"100mg", "200mg", "300mg", "400mg", "500mg"});
        dosageCombo.setSelectedItem(dosage);
        JComboBox<String> durationCombo = new JComboBox<>(new String[]{
            "1 day", "2 days", "3 days", "4 days", "5 days", "6 days", "7 days"});
        durationCombo.setSelectedItem(duration);
        JTextField dateIssuedField = new JTextField(dateIssued);
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "COMPLETED", "CANCELLED"});
        statusCombo.setSelectedItem(status);

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));

        form.add(new JLabel("PRESCRIPTION_ID:"));
        form.add(prescriptionIdField);

        form.add(new JLabel("PATIENT_ID:"));
        form.add(patientIdField);

        form.add(new JLabel("DOCTOR_ID:"));
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

        int choice = JOptionPane.showConfirmDialog(
            this,
            form,
            "Edit Prescription",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String updatedPatientId = patientIdField.getText().trim();
        String updatedMedication = medicationField.getText().trim();
        String updatedDosage = (String) dosageCombo.getSelectedItem();
        String updatedDuration = (String) durationCombo.getSelectedItem();
        String updatedDateIssued = dateIssuedField.getText().trim();

        if (updatedPatientId.isEmpty() || updatedMedication.isEmpty() || updatedDosage.isEmpty()
                || updatedDuration.isEmpty() || updatedDateIssued.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Patient ID, medication, dosage, duration and date issued are required.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
        if (hasIllegalChars(updatedPatientId, updatedMedication, updatedDosage, updatedDuration, updatedDateIssued)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        return String.join("|",
                prescriptionIdField.getText().trim(),
                updatedPatientId,
                doctorIdField.getText().trim(),
                updatedMedication,
                updatedDosage,
                updatedDuration,
                updatedDateIssued,
                (String) statusCombo.getSelectedItem()
        );
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

    private void addRecord() {
        if (consultationRateTable) {
            RecordsHelperConsultation.addConsultationRateRecord(this, fileName, this::refreshTable);
            return;
        }
        if (rosterTable) {
            String newRecord = managerMethods.addRosterRecord();

            if (newRecord == null) {
                return;
            }

            List<String> updatedLines = new ArrayList<>(records);
            updatedLines.add(newRecord);

            writeRecords(updatedLines, "The roster could not be added.");
            return;
        }
        
        if (insuranceTable) {
            RecordsHelperInsurance.addInsuranceRecord(this, fileName, this::refreshTable);
            return;
        }
        if (consultationTable) {
            addVitalSignRecord();
            return;
        }
        if (prescriptionTable) {
            addPrescriptionRecord();
            return;
        }
        
        if (departmentTable) {
            String newRecord = managerMethods.addDepartmentRow();

            if (newRecord != null) {

                FileManager.appendLine(
                        fileName,
                        newRecord
                );

                refreshTable();
            }
            
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

    public boolean hasIllegalChars(String... values) {
        return ManageRecordsHelper.hasIllegalChars(values);
    }
    
    public JTable getRecordsTable() {
        return recordsTable;
    }
        
    private void addVitalSignRecord() {
    User currentDoctor = Session.getCurrentUser();

    JTextField patientIdField = new JTextField();
    JTextField doctorIdField = new JTextField(currentDoctor.getUserId());
    setUneditable(doctorIdField);
    JTextField consultationIdField = new JTextField();
    JTextField bpField = new JTextField();
    JTextField heartRateField = new JTextField();
    JTextField temperatureField = new JTextField();
    JTextField dateField = new JTextField(java.time.LocalDate.now().toString());
    JTextField notesField = new JTextField();

    JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));

    form.add(new JLabel("PATIENT_ID:"));
    form.add(patientIdField);

    form.add(new JLabel("DOCTOR_ID:"));
    form.add(doctorIdField);

    form.add(new JLabel("CONSULTATION_ID:"));
    form.add(consultationIdField);

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
            this,
            form,
            "Add Vital Sign Record",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
    );

    if (choice != JOptionPane.OK_OPTION) {
        return;
    }

    String patientId = patientIdField.getText().trim();
    String consultationId = consultationIdField.getText().trim();
    String bp = bpField.getText().trim();
    String heartRate = heartRateField.getText().trim();
    String temperature = temperatureField.getText().trim();
    String date = dateField.getText().trim();
    String notes = notesField.getText().trim();

    if (patientId.isEmpty()
            || consultationId.isEmpty()
            || bp.isEmpty()
            || heartRate.isEmpty()
            || temperature.isEmpty()
            || date.isEmpty()
            || notes.isEmpty()) {

        JOptionPane.showMessageDialog(
                this,
                "All fields, including consultation notes, are required.",
                "Invalid Vital Sign Record",
                JOptionPane.WARNING_MESSAGE
            );
        return;
        }

    if (hasIllegalChars(patientId, consultationId, bp, heartRate, temperature, date, notes)) {
        JOptionPane.showMessageDialog(
                this,
                "Fields cannot contain the '|' character or line breaks.",
                "Invalid Vital Sign Record",
                JOptionPane.WARNING_MESSAGE
            );
        return;
        }

    FileManager.appendLine(
            fileName,
            String.join("|",
                    IDGenerator.next("VS", fileName),
                    patientId,
                    currentDoctor.getUserId(),
                    consultationId,
                    bp,
                    heartRate,
                    temperature,
                    date,
                    notes
                )
        );

    refreshTable();
    }
    
    private void addPrescriptionRecord() {
        User currentDoctor = Session.getCurrentUser();

        JTextField patientIdField = new JTextField();
        JTextField doctorIdField = new JTextField(currentDoctor.getUserId());
        setUneditable(doctorIdField);
        JTextField medicationField = new JTextField();
        JComboBox<String> dosageCombo = new JComboBox<>(new String[]{"100mg", "200mg", "300mg", "400mg", "500mg"});
        JComboBox<String> durationCombo = new JComboBox<>(new String[]{
            "1 day", "2 days", "3 days", "4 days", "5 days", "6 days", "7 days"});
        JTextField dateIssuedField = new JTextField(java.time.LocalDate.now().toString());
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "COMPLETED", "CANCELLED"});

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));

        form.add(new JLabel("PATIENT_ID:"));
        form.add(patientIdField);

        form.add(new JLabel("DOCTOR_ID:"));
        form.add(doctorIdField);

        form.add(new JLabel("MEDICATION:"));
        form.add(medicationField);

        form.add(new JLabel("DOSAGE:"));
        form.add(dosageCombo);

        form.add(new JLabel("DURATION:"));
        form.add(durationCombo);

        form.add(new JLabel("DATE_ISSUED (YYYY-MM-DD):"));
        form.add(dateIssuedField);

        form.add(new JLabel("STATUS:"));
        form.add(statusCombo);

        int choice = JOptionPane.showConfirmDialog(
                this,
                form,
                "Issue Prescription",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String patientId = patientIdField.getText().trim();
        String medication = medicationField.getText().trim();
        String dosage = (String) dosageCombo.getSelectedItem();
        String duration = (String) durationCombo.getSelectedItem();
        String dateIssued = dateIssuedField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();

        if (patientId.isEmpty() || medication.isEmpty() || dosage.isEmpty()
                || duration.isEmpty() || dateIssued.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Patient ID, medication, dosage, duration and date issued are required.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
                );
            return;
            }

        try {
            java.time.LocalDate.parse(dateIssued);
            } catch (java.time.format.DateTimeParseException exception) {
                JOptionPane.showMessageDialog(
                        this,
                        "Date issued must be in YYYY-MM-DD format.",
                        "Invalid Prescription",
                        JOptionPane.WARNING_MESSAGE
                    );
                return;
            }

        if (hasIllegalChars(patientId, medication, dosage, duration, dateIssued)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
                );
            return;
            }

        FileManager.appendLine(
                fileName,
                String.join("|",
                        IDGenerator.next("RX", fileName),
                        patientId,
                        currentDoctor.getUserId(),
                        medication,
                        dosage,
                        duration,
                        dateIssued,
                        status
                    )
            );

        refreshTable();
    }
    
    private void setupReportFilter() {

        reportSearchBox.removeAllItems();

        reportSearchBox.addItem("All Records");
        reportSearchBox.addItem("Latest Records");
        reportSearchBox.addItem("Highest Revenue");

        reportSearchBox.addActionListener(e -> filterReportTable());
    }
    
    private void filterReportTable() {
        if (!reportTable) {
            return;
        }

        String selected =
                (String) reportSearchBox.getSelectedItem();

        if (selected == null) {
            return;
        }

        List<String> reportRecords =
                FileManager.readLines("report.txt");

        List<String> records =
                new ArrayList<>();

        // Skip the first line because it is the header
        for (int i = 1; i < reportRecords.size(); i++) {

            String record = reportRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    record.split("\\|", -1);

            if (parts.length < 6) {
                continue;
            }

            records.add(record);
        }

        // Latest month first
        if (selected.equals("Latest Records")) {

            records.sort((a, b) -> {

                String[] partsA = a.split("\\|", -1);
                String[] partsB = b.split("\\|", -1);

                String monthA = partsA[0].trim();
                String monthB = partsB[0].trim();

                return monthB.compareTo(monthA);
            });
        }

        // Highest revenue first
        else if (selected.equals("Highest Revenue")) {

            records.sort((a, b) -> {

                String[] partsA = a.split("\\|", -1);
                String[] partsB = b.split("\\|", -1);

                double revenueA = parseRevenue(partsA[5]);
                double revenueB = parseRevenue(partsB[5]);

                return Double.compare(revenueB, revenueA);
            });
        }

        // Clear current table
        tableModel.setRowCount(0);

        // Add sorted records to table
        for (String record : records) {

            String[] parts =
                    record.split("\\|", -1);

            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim()
            });
        }
    }
    
    private double parseRevenue(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public void refreshReportFilter(){
        setupReportFilter();
    }

    private String findName(String userId) {
        return ManageRecordsHelper.findName(userId);
    }
}
