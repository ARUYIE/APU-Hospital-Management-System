package hms.gui.panels;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import hms.role.Role;
import hms.role.User;
import hms.util.FileManager;
import hms.util.IDGenerator;
import hms.util.ManageRecordsHelper;
import hms.util.ManagerMethods;
import hms.util.RecordsHelperAppointment;
import hms.util.RecordsHelperAsset;
import hms.util.RecordsHelperConsultation;
import hms.util.RecordsHelperInsurance;
import hms.util.Session;
import hms.util.UserRepository;
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
        
        // Low Kai Lun - Doctor
        consultationTable = "vital_signs.txt".equalsIgnoreCase(fileName);
        prescriptionTable = "prescriptions.txt".equalsIgnoreCase(fileName);
        labRequestTable = "lab_requests.txt".equalsIgnoreCase(fileName);
        
        tableModel = new DefaultTableModel(
                departmentTable
                ? new String[]{"DEPTARTMENT_ID", "DEPTARTMENT_NAME", "HEAD_MANAGER_NAME", "DESCRIPTION"}
                : appointmentTable
                ? new String[]{"APPOINTMENT_ID", "PATIENT_NAME", "DOCTOR_NAME", "DATE", "TIME", "STATUS", "SERVICE_TYPE"}
                : assetTable
                ? new String[]{"ASSET_ID", "ROOM_TYPE", "ROOM_NAME", "LOCATION", "STATUS", "RESERVED_BY"}
                : insuranceTable
                ? new String[]{"INSURANCE_ID", "PROVIDER_NAME", "COVERAGE_RATE", "COVERAGE_PERCENTAGE", "STATUS", "CONTACT_INFO", "EFFECTIVE_DATE"}
                : consultationRateTable
                ? new String[]{"SPECIALTY", "BASE_RATE", "MIN_RATE", "MAX_RATE", "CURRENCY", "EFFECTIVE_DATE"}
                : rosterTable
                ? new String[]{"ROSTER_ID", "DOCTOR_NAME", "MANAGED_BY", "DEPARTMENT", "DATE", "SHIFT", "STATUS"}
                : consultationTable
                ? new String[]{"VITAL_SIGN_ID", "PATIENT_NAME", "DOCTOR_NAME", "CONSULTATION_ID", "BP", "HEART_RATE", "TEMPERATURE", "DATE", "NOTES"}
                : prescriptionTable
                ? new String[]{"PRESCRIPTION_ID", "PATIENT_NAME", "DOCTOR_NAME", "MEDICATION", "DOSAGE", "DURATION", "DATE_ISSUED", "STATUS"}
                : labRequestTable       
                ? new String[]{"REQUEST_ID", "PATIENT_NAME", "DOCTOR_NAME", "TEST_TYPE", "ROOM_ID", "DATE_REQUESTED", "DATE_COMPLETED", "STATUS"}
                : new String[]{"#", "Record"}, 0) {
                   
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
        recordsTable = new JTable(tableModel);
        recordHelper = new ManageRecordsHelper(fileName, tableModel, doctorSearchBox, assetSearchBox, consultationTable, prescriptionTable, labRequestTable);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        String AddLabel = (assetTable)
            ? "Add Room"
            : (appointmentTable)
            ? "Add Appointment"
            : (insuranceTable)
            ? "Add Insurance"
            : (consultationRateTable)
            ? "Add Consultation Rate"
            : (rosterTable)
            ? "Add Roster"
            : (consultationTable)
            ? "Add Consultation"
            : (prescriptionTable)
            ? "Add Prescription"
            : (labRequestTable)
            ? "Add Lab Request"
            : (departmentTable)
            ? "Add Department"
            : "Add Record";
        JButton addButton = new JButton(AddLabel);
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
        } 
        User currentUser = Session.getCurrentUser();
        boolean isDoctor = currentUser != null && currentUser.getRole() == Role.DOCTOR;
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN_STAFF;
        
        JButton approveLabRequestBtn = new JButton("Approve Request");
        approveLabRequestBtn.addActionListener(e -> updateSelectedLabRequestStatus("APPROVED"));
        
        JButton denyLabRequestBtn = new JButton("Deny Request");
        denyLabRequestBtn.addActionListener(e -> updateSelectedLabRequestStatus("DENIED"));

        JButton markLabCompletedBtn = new JButton("Mark Completed");
        markLabCompletedBtn.addActionListener(e -> markLabRequestCompleted());

        
        actions.add(refreshButton);
            if (labRequestTable) {
                if (isDoctor) {
                actions.add(addButton);
                actions.add(editButton);
                actions.add(deleteButton);
                } else if (isAdmin) {
                    actions.add(approveLabRequestBtn);
                    actions.add(denyLabRequestBtn);
                    actions.add(markLabCompletedBtn);
                    actions.add(editButton); 
                } else if (assetTable) {
                    actions.add(assetSearchBox);
                }

            } else {
                actions.add(addButton);
                actions.add(editButton);
                actions.add(deleteButton);
            }


        //has two rows since its a bit too long
        // if (assetTable) {
        //     JPanel wardActions = new JPanel();
        //     wardActions.setLayout(new BoxLayout(wardActions, BoxLayout.Y_AXIS));

        //     JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        //     searchActions.add(new JLabel("Search Wards/Clinics:"));
        //     searchActions.add(assetSearchBox);
        //     searchActions.add(reserveButton);
        //     searchActions.add(finishButton);

        //     JPanel recordActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        //     recordActions.add(refreshButton);
        //     recordActions.add(addButton);
        //     recordActions.add(editButton);
        //     recordActions.add(deleteButton);

        //     wardActions.add(searchActions);
        //     wardActions.add(recordActions);
        //     actions = wardActions;
        // }
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


        recordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // if (labRequestTable) {
        //     recordsTable.setAutoCreateRowSorter(false);
        //     ManageRecordsHelper.applyStatusSorter(recordsTable, 7);
        // } else  {
        //     recordsTable.setAutoCreateRowSorter(true);
        // }

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

    private void updateSelectedLabRequestStatus(String newStatus) {
        if (!labRequestTable) {
            return;
        }

        int viewRow = recordsTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a request first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }


        int modelRow = recordsTable.convertRowIndexToModel(viewRow);
        String record = records.get(modelRow);
        String[] parts = splitRecord(record);

        if (newStatus.equals("DENIED")) {
        parts[6] =java.time.LocalDate.now().toString();

        }

        if (parts.length >= 8) {
            if(parts[7].equals("COMPLETED")){
                JOptionPane.showMessageDialog(this,
                    "This reservation is Already Completed",
                    "Already Completed", JOptionPane.WARNING_MESSAGE);
                return;
            }else{
            parts[7] = newStatus;
            String updatedRecord = String.join("|", parts);

            List<String> updatedLines = new ArrayList<>(records);
            updatedLines.set(modelRow, updatedRecord);

            writeRecords(updatedLines, "The status could not be updated.");
            }
        }
        refreshTable();
    }

    private void markLabRequestCompleted() {
    if (!labRequestTable) {
        return;
    }

    int viewRow = recordsTable.getSelectedRow();
    if (viewRow == -1) {
        JOptionPane.showMessageDialog(this,
                "Please select a request first.",
                "No Record Selected", JOptionPane.WARNING_MESSAGE);
        return;
    }

    int modelRow = recordsTable.convertRowIndexToModel(viewRow);
    String record = records.get(modelRow);
    String[] parts = splitRecord(record);

    if (parts.length < 8) {
        return;
    }

    String dateCompleted = JOptionPane.showInputDialog(
            this,
            "Enter the date completed (YYYY-MM-DD):",
            java.time.LocalDate.now().toString()
    );

    if (dateCompleted == null) {
        return; // User clicked Cancel
    }

    dateCompleted = dateCompleted.trim();

    try {
        java.time.LocalDate.parse(dateCompleted);
    } catch (java.time.format.DateTimeParseException ex) {
        JOptionPane.showMessageDialog(this,
                "Date must be in YYYY-MM-DD format.",
                "Invalid Date", JOptionPane.WARNING_MESSAGE);
        return;
    }

    parts[6] = dateCompleted; // DATE_COMPLETED
    parts[7] = "COMPLETED";   // STATUS
    String updatedRecord = String.join("|", parts);

    List<String> updatedLines = new ArrayList<>(records);
    updatedLines.set(modelRow, updatedRecord);

    writeRecords(updatedLines, "The record could not be updated.");
    refreshTable();
}
    private void editSelectedRecord() {
        int viewRow = recordsTable.getSelectedRow();

        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a record first.",
                    "No Record Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = recordsTable.convertRowIndexToModel(viewRow);

        // Get the ID from the selected table row
        String selectedId = recordsTable
                .getModel()
                .getValueAt(modelRow, 0)
                .toString()
                .trim();

        // Find the actual record in the records list using its ID
        int recordIndex = -1;

        for (int i = 0; i < records.size(); i++) {
            String record = records.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = ManageRecordsHelper.splitRecord(record);

            if (parts.length > 0
                    && parts[0].trim().equals(selectedId)) {
                recordIndex = i;
                break;
            }
        }

        if (recordIndex == -1) {
            JOptionPane.showMessageDialog(this,
                    "The selected record could not be found.",
                    "Edit Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String originalRecord = records.get(recordIndex);

        String updatedRecord = departmentTable
                ? managerMethods.editDepartmentRecord(originalRecord)
                : appointmentTable
                ? RecordsHelperAppointment.editAppointmentRecord(this, originalRecord)
                : insuranceTable
                ? RecordsHelperInsurance.editInsuranceRecord(this, originalRecord)
                : consultationRateTable
                ? RecordsHelperConsultation.editConsultationRateRecord(this, originalRecord)
                : assetTable
                ? RecordsHelperAsset.editAssetRecord(this, originalRecord)
                : rosterTable
                ? managerMethods.editRosterRecord(originalRecord)
                : consultationTable
                ? editVitalSignRecord(originalRecord)
                : prescriptionTable
                ? editPrescriptionRecord(originalRecord)
                : labRequestTable
                ? editLabRequestRecord(originalRecord)
                : (String) JOptionPane.showInputDialog(
                        this,
                        "Edit record:",
                        "Edit Record",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        originalRecord
                );

        if (updatedRecord == null) {
            return;
        }

        String normalizedRecord = updatedRecord.trim();

        if (!isValidRecord(normalizedRecord)) {
            JOptionPane.showMessageDialog(this,
                    "Enter correct record.",
                    "Invalid Record",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Update the actual record in the current records list
        records.set(recordIndex, normalizedRecord);

        // Save the current records list
        writeRecords(
                records,
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
    List<String> consultationRateLines = FileManager.readLines("consultation_rates.txt");
    for (int lineIndex = 0; lineIndex < consultationRateLines.size(); lineIndex++) {
        String rateLine = consultationRateLines.get(lineIndex);
        if (lineIndex == 0 && rateLine.trim().startsWith("SPECIALTY")) {
            continue; // 跳过表头行
        }
        String[] rateParts = rateLine.split("\\|", -1);
        if (rateParts.length >= 1 && !rateParts[0].trim().isEmpty()) {
            specialties.add(rateParts[0].trim());
        }
    }
    JComboBox<String> consultationIdCombo = new JComboBox<>(specialties.toArray(new String[0]));
    consultationIdCombo.setSelectedItem(consultationId);
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

    int selectedPatientIndex = patientCombo.getSelectedIndex();
    String updatedPatientId = selectedPatientIndex >= 0
            ? patients.get(selectedPatientIndex).getUserId()
            : patientId;

    String updatedConsultationId = (String) consultationIdCombo.getSelectedItem();
    String updatedBp = bpField.getText().trim();
    String updatedHeartRate = heartRateField.getText().trim();
    String updatedTemperature = temperatureField.getText().trim();
    String updatedDate = dateField.getText().trim();
    String updatedNotes = notesField.getText().trim();

    if (updatedBp.isEmpty()
            || updatedHeartRate.isEmpty()
            || updatedTemperature.isEmpty()
            || updatedDate.isEmpty()
            || updatedNotes.isEmpty()) {

        JOptionPane.showMessageDialog(
                this,
                "BP, heart rate, temperature, date and notes are required.",
                "Invalid Vital Sign Record",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

    if (hasIllegalChars(updatedConsultationId, updatedBp,
            updatedHeartRate, updatedTemperature, updatedDate, updatedNotes)) {
        JOptionPane.showMessageDialog(
                this,
                "Fields cannot contain the '|' character or line breaks.",
                "Invalid Vital Sign Record",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

    if (hasVitalSignOnDate(updatedPatientId, updatedDate, vitalSignId)) {
        JOptionPane.showMessageDialog(
                this,
                "This patient already has a vital sign record for " + updatedDate + ". Only one record per day is allowed.",
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

    int selectedPatientIndex = patientCombo.getSelectedIndex();
    String updatedPatientId = selectedPatientIndex >= 0
            ? patients.get(selectedPatientIndex).getUserId()
            : patientId;

    String updatedMedication = medicationField.getText().trim();
    String updatedDosage = (String) dosageCombo.getSelectedItem();
    String updatedDuration = (String) durationCombo.getSelectedItem();
    String updatedDateIssued = dateIssuedField.getText().trim();

    if (updatedMedication.isEmpty() || updatedDateIssued.isEmpty()) {
        JOptionPane.showMessageDialog(
                this,
                "Medication and date issued are required.",
                "Invalid Prescription",
                JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    
    if (hasIllegalChars(updatedMedication, updatedDosage, updatedDuration, updatedDateIssued)) {
        JOptionPane.showMessageDialog(
                    this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
                );
            return null;
        }
    
    if (hasPrescriptionForMedication(updatedPatientId, updatedMedication, prescriptionId)) {
    JOptionPane.showMessageDialog(
                    this,
                    "This patient already has a prescription for " + updatedMedication + ".",
                    "Duplicate Medication",
                    JOptionPane.WARNING_MESSAGE
                );
            return null;
        }

    return String.join("|",
            prescriptionIdField.getText().trim(),
            updatedPatientId,
            doctorId,
            updatedMedication,
            updatedDosage,
            updatedDuration,
            updatedDateIssued,
            (String) statusCombo.getSelectedItem()
        );
    }
    
    private String editLabRequestRecord(String record) {
    String[] parts = splitRecord(record);

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
    boolean isDoctor = currentUser != null && currentUser.getRole() == Role.DOCTOR;

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
            "Blood Test", "X-Ray", "MRI", "CT Scan", "Ultrasound", "Other Specialized Imaging"});
    testTypeCombo.setSelectedItem(testType);
    if (!isDoctor) {
        testTypeCombo.setEnabled(false);
    }

    List<String> roomIds = new ArrayList<>();
    JComboBox<String> roomCombo = new JComboBox<>();
    roomCombo.addItem("No room needed");
    boolean currentRoomStillListed = false;
    for (String assetLine : FileManager.readLines("hospital_assets.txt")) {
        String[] assetParts = assetLine.split("\\|", -1);
        if (assetParts.length < 5) {
            continue;
        }
        String assetId = assetParts[0].trim();
        String assetRoomType = assetParts[1].trim();
        String assetRoomName = assetParts[2].trim();
        String assetStatus = assetParts[4].trim();

        boolean isThisRequestsCurrentRoom = assetId.equals(roomId);
        boolean isEligibleRoomType = "IMAGING_ROOM".equalsIgnoreCase(assetRoomType)
                || "LAB".equalsIgnoreCase(assetRoomType)
                || "OPERATION_THEATRE".equalsIgnoreCase(assetRoomType);
        boolean isAvailableEligibleRoom = isEligibleRoomType && "AVAILABLE".equalsIgnoreCase(assetStatus);

        if (isAvailableEligibleRoom || isThisRequestsCurrentRoom) {
            roomIds.add(assetId);
            roomCombo.addItem(assetRoomName + " (" + assetId + ")");
            if (isThisRequestsCurrentRoom) {
                currentRoomStillListed = true;
            }
        }
    }
    if (!roomId.isEmpty() && currentRoomStillListed) {
        roomCombo.setSelectedIndex(roomIds.indexOf(roomId) + 1);
    }
    if (!isDoctor) {
        roomCombo.setEnabled(false);
    }

    JTextField dateRequestedField = new JTextField(dateRequested);
    if (!isDoctor) {
        setUneditable(dateRequestedField);
    }

    JComboBox<String> statusCombo = new JComboBox<>(new String[]{"PENDING", "APPROVED", "DENIED", "IN_PROGRESS", "COMPLETED"});
    statusCombo.setSelectedItem(status);
    if (isDoctor) {
        statusCombo.setEnabled(false);
    }

    JTextField dateCompletedField = new JTextField(dateCompleted);
    if (isDoctor) {
        setUneditable(dateCompletedField);
    }

    JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));

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

    form.add(new JLabel(isDoctor ? "DATE_REQUESTED:" : "DATE_REQUESTED (set by doctor):"));
    form.add(dateRequestedField);

    form.add(new JLabel(isDoctor ? "DATE_COMPLETED (set by admin):" : "DATE_COMPLETED:"));
    form.add(dateCompletedField);

    form.add(new JLabel(isDoctor ? "STATUS (set by admin):" : "STATUS:"));
    form.add(statusCombo);

    int choice = JOptionPane.showConfirmDialog(
            this,
            form,
            "Edit Lab / Imaging Request",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
    );

    if (choice != JOptionPane.OK_OPTION) {
        return null;
    }

    int selectedPatientIndex = patientCombo.getSelectedIndex();
    String updatedPatientId = (isDoctor && selectedPatientIndex >= 0)
            ? patients.get(selectedPatientIndex).getUserId()
            : patientId;

    String updatedTestType = isDoctor ? (String) testTypeCombo.getSelectedItem() : testType;
    String updatedDateRequested = isDoctor ? dateRequestedField.getText().trim() : dateRequested;


    int selectedRoomIndex = roomCombo.getSelectedIndex();
    String updatedRoomId = isDoctor
            ? (selectedRoomIndex > 0 ? roomIds.get(selectedRoomIndex - 1) : "")
            : roomId;

if (hasLabRequestForRoom(updatedPatientId, updatedRoomId, updatedDateRequested, requestId)) {
        String roomName = ManageRecordsHelper.findAssetType(updatedRoomId);

        JOptionPane.showMessageDialog(
                this,
                "This patient already has a request for room " + roomName + " on " + updatedDateRequested + ".",
                "Duplicate Request",
                JOptionPane.WARNING_MESSAGE
        );
        return null;
    }

    String updatedStatus = !isDoctor ? (String) statusCombo.getSelectedItem() : status;
    String updatedDateCompleted = !isDoctor ? dateCompletedField.getText().trim() : dateCompleted;

    if (isDoctor) {
        if (updatedDateRequested.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
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
                    this,
                    "Date requested must be in YYYY-MM-DD format.",
                    "Invalid Request",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    if (hasIllegalChars(updatedTestType, updatedRoomId, updatedDateRequested,
            updatedDateCompleted, updatedStatus)) {
        JOptionPane.showMessageDialog(
                this,
                "Fields cannot contain the '|' character or line breaks.",
                "Invalid Request",
                JOptionPane.WARNING_MESSAGE
        );
        return null;
    }
    refreshTable();
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
        setupTableSorter();
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
        if (labRequestTable) {
        addLabRequestRecord();
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
    
    private boolean hasVitalSignOnDate(String patientId, String date, String excludeVitalSignId) {
    for (String existingRecord : records) {
        String[] existingParts = splitRecord(existingRecord);
        if (existingParts.length < 8) {
            continue;
            }
            String existingId = existingParts[0].trim();
            String existingPatientId = existingParts[1].trim();
            String existingDate = existingParts[7].trim();

        if (excludeVitalSignId != null && existingId.equals(excludeVitalSignId)) {
                continue; 
            }

        if (existingPatientId.equals(patientId) && existingDate.equals(date)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasPrescriptionOnDate(String patientId, String date, String excludePrescriptionId) {
    for (String existingRecord : records) {
        String[] existingParts = splitRecord(existingRecord);
        if (existingParts.length < 8) {
            continue;
            }
        String existingId = existingParts[0].trim();
        String existingPatientId = existingParts[1].trim();
        String existingDate = existingParts[6].trim();

        if (excludePrescriptionId != null && existingId.equals(excludePrescriptionId)) {
            continue; 
            }

        if (existingPatientId.equals(patientId) && existingDate.equals(date)) {
            return true;
            }
        }
        return false;
    }
    
    private boolean hasPrescriptionForMedication(String patientId, String medication, String excludePrescriptionId) {
    for (String existingRecord : records) {
        String[] existingParts = splitRecord(existingRecord);
        if (existingParts.length < 8) {
            continue;
            }
        String existingId = existingParts[0].trim();
        String existingPatientId = existingParts[1].trim();
        String existingMedication = existingParts[3].trim();

        if (excludePrescriptionId != null && existingId.equals(excludePrescriptionId)) {
            continue; 
            }

        if (existingPatientId.equals(patientId) && existingMedication.equalsIgnoreCase(medication)) {
            return true;
            }
        }
        return false;
    }
    
    private boolean hasLabRequestForRoom(String patientId, String roomId, String dateRequested, String excludeRequestId) {
        if (roomId == null || roomId.isEmpty()) {
            return false;
        }
        for (String existingRecord : records) {
            String[] existingParts = splitRecord(existingRecord);
            if (existingParts.length < 8) {
                continue;
            }
            String existingId = existingParts[0].trim();
            String existingPatientId = existingParts[1].trim();
            String existingRoomId = existingParts[4].trim();
            String existingDateRequested = existingParts[5].trim();

            if (excludeRequestId != null && existingId.equals(excludeRequestId)) {
                continue;
            }

            if (existingPatientId.equals(patientId) && existingRoomId.equals(roomId) && existingDateRequested.equals(dateRequested)) {
                return true;
            }
        }
        return false;
    }
        
    private void addVitalSignRecord() {
        User currentDoctor = Session.getCurrentUser();

        List<User> patients = UserRepository.loadAll().stream()
                .filter(user -> user.getRole() == Role.PATIENT)
                .toList();

        if (patients.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "There are no patients to log vitals for.",
                    "Cannot Add Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JComboBox<String> patientCombo = new JComboBox<>();
        for (User patient : patients) {
            patientCombo.addItem(patient.getFullName());
        }

        JTextField doctorIdField = new JTextField(currentDoctor.getFullName());
        setUneditable(doctorIdField);
        List<String> specialties = new ArrayList<>();
        List<String> consultationRateLines = FileManager.readLines("consultation_rates.txt");
        for (int lineIndex = 0; lineIndex < consultationRateLines.size(); lineIndex++) {
            String rateLine = consultationRateLines.get(lineIndex);
            if (lineIndex == 0 && rateLine.trim().startsWith("SPECIALTY")) {
                continue; 
            }
            String[] rateParts = rateLine.split("\\|", -1);
            if (rateParts.length >= 1 && !rateParts[0].trim().isEmpty()) {
                specialties.add(rateParts[0].trim());
            }
        }
        if (specialties.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No specialties are configured yet. Ask an Admin to add one under Consultation Rates first.",
                    "Cannot Add Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        JComboBox<String> consultationIdCombo = new JComboBox<>(specialties.toArray(new String[0]));
        JTextField bpField = new JTextField();
        JTextField heartRateField = new JTextField();
        JTextField temperatureField = new JTextField();
        JTextField dateField = new JTextField(java.time.LocalDate.now().toString());
        JTextField notesField = new JTextField();

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));

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

        String consultationId = (String) consultationIdCombo.getSelectedItem();
        String bp = bpField.getText().trim();
        String heartRate = heartRateField.getText().trim();
        String temperature = temperatureField.getText().trim();
        String date = dateField.getText().trim();
        String notes = notesField.getText().trim();

        if (consultationId.isEmpty()
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

        if (hasIllegalChars(consultationId, bp, heartRate, temperature, date, notes)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Vital Sign Record",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

        String patientId = patients.get(patientCombo.getSelectedIndex()).getUserId();

        if (hasVitalSignOnDate(patientId, date, null)) {
            JOptionPane.showMessageDialog(
                    this,
                    "This patient already has a vital sign record for " + date + ". Only one record per day is allowed.",
                    "Duplicate Record",
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

    List<User> patients = UserRepository.loadAll().stream()
            .filter(user -> user.getRole() == Role.PATIENT)
            .toList();

    if (patients.isEmpty()) {
        JOptionPane.showMessageDialog(
                this,
                "There are no patients to prescribe medication for.",
                "Cannot Add Prescription",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    JComboBox<String> patientCombo = new JComboBox<>();
    for (User patient : patients) {
        patientCombo.addItem(patient.getFullName());
    }

    JTextField doctorIdField = new JTextField(currentDoctor.getFullName());
    setUneditable(doctorIdField);
    JTextField medicationField = new JTextField();
    JComboBox<String> dosageCombo = new JComboBox<>(new String[]{"100mg", "200mg", "300mg", "400mg", "500mg"});
    JComboBox<String> durationCombo = new JComboBox<>(new String[]{
        "1 day", "2 days", "3 days", "4 days", "5 days", "6 days", "7 days"});
    JTextField dateIssuedField = new JTextField(java.time.LocalDate.now().toString());
    JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "COMPLETED", "CANCELLED"});

    JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));

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

    String medication = medicationField.getText().trim();
    String dosage = (String) dosageCombo.getSelectedItem();
    String duration = (String) durationCombo.getSelectedItem();
    String dateIssued = dateIssuedField.getText().trim();
    String status = (String) statusCombo.getSelectedItem();

    if (medication.isEmpty() || dateIssued.isEmpty()) {
        JOptionPane.showMessageDialog(
                this,
                "Medication and date issued are required.",
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

    if (hasIllegalChars(medication, dosage, duration, dateIssued)) {
        JOptionPane.showMessageDialog(
                    this,
                    "Fields cannot contain the '|' character or line breaks.",
                    "Invalid Prescription",
                    JOptionPane.WARNING_MESSAGE
                );
            return;
        }
    
    String patientId = patients.get(patientCombo.getSelectedIndex()).getUserId();
    
    if (hasPrescriptionForMedication(patientId, medication, null)) {
    JOptionPane.showMessageDialog(
                    this,
                    "This patient already has a prescription for " + medication + ".",
                    "Duplicate Medication",
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
    
    
    private void addLabRequestRecord() {
    User currentDoctor = Session.getCurrentUser();

    List<User> patients = UserRepository.loadAll().stream()
            .filter(user -> user.getRole() == Role.PATIENT)
            .toList();

    if (patients.isEmpty()) {
        JOptionPane.showMessageDialog(
                this,
                "There are no patients to request tests for.",
                "Cannot Add Request",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    JComboBox<String> patientCombo = new JComboBox<>();
    for (User patient : patients) {
        patientCombo.addItem(patient.getFullName());
    }

    JTextField doctorIdField = new JTextField(currentDoctor.getFullName());
    setUneditable(doctorIdField);
    JComboBox<String> testTypeCombo = new JComboBox<>(new String[]{
            "Blood Test", "X-Ray", "MRI", "CT Scan", "Ultrasound", "Other Specialized Imaging"});

    List<String> roomIds = new ArrayList<>();
    JComboBox<String> roomCombo = new JComboBox<>();
    roomCombo.addItem("No room needed");
    for (String assetLine : FileManager.readLines("hospital_assets.txt")) {
        String[] assetParts = assetLine.split("\\|", -1);
        if (assetParts.length < 5) {
            continue;
        }
        String assetId = assetParts[0].trim();
        String assetRoomType = assetParts[1].trim();
        String assetRoomName = assetParts[2].trim();
        String assetStatus = assetParts[4].trim();

        boolean isEligibleRoomType = "IMAGING_ROOM".equalsIgnoreCase(assetRoomType)
                || "LAB".equalsIgnoreCase(assetRoomType)
                || "OPERATION_THEATRE".equalsIgnoreCase(assetRoomType);

        if (isEligibleRoomType && "AVAILABLE".equalsIgnoreCase(assetStatus)) {
            roomIds.add(assetId);
            roomCombo.addItem(assetRoomName + " (" + assetId + ")");
        }
    }

    JTextField statusField = new JTextField("PENDING");
    setUneditable(statusField);
    JTextField dateRequestedField = new JTextField(java.time.LocalDate.now().toString());

    JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));

    form.add(new JLabel("PATIENT:"));
    form.add(patientCombo);

    form.add(new JLabel("DOCTOR:"));
    form.add(doctorIdField);

    form.add(new JLabel("TEST_TYPE:"));
    form.add(testTypeCombo);

    form.add(new JLabel("ROOM (if needed):"));
    form.add(roomCombo);

    form.add(new JLabel("STATUS:"));
    form.add(statusField);

    form.add(new JLabel("DATE_REQUESTED (YYYY-MM-DD):"));
    form.add(dateRequestedField);

    int choice = JOptionPane.showConfirmDialog(
            this,
            form,
            "Request Lab Test / Imaging",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
    );

    if (choice != JOptionPane.OK_OPTION) {
        return;
    }

    String testType = (String) testTypeCombo.getSelectedItem();
    String dateRequested = dateRequestedField.getText().trim();

    if (dateRequested.isEmpty()) {
        JOptionPane.showMessageDialog(
                this,
                "Date requested is required.",
                "Invalid Request",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    try {
        java.time.LocalDate.parse(dateRequested);
    } catch (java.time.format.DateTimeParseException exception) {
        JOptionPane.showMessageDialog(
                this,
                "Date requested must be in YYYY-MM-DD format.",
                "Invalid Request",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    int selectedRoomIndex = roomCombo.getSelectedIndex();
    String roomId = selectedRoomIndex > 0 ? roomIds.get(selectedRoomIndex - 1) : "";

    if (hasIllegalChars(testType, dateRequested, roomId)) {
        JOptionPane.showMessageDialog(
                this,
                "Fields cannot contain the '|' character or line breaks.",
                "Invalid Request",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }
    String patientId = patients.get(patientCombo.getSelectedIndex()).getUserId();

    // Check for duplicate lab requests using the raw roomId
    if (hasLabRequestForRoom(patientId, roomId, dateRequested, null)) {
        String roomName = ManageRecordsHelper.findAssetType(roomId);

        JOptionPane.showMessageDialog(
                this,
                "This patient already has a request for room " + roomName + " on " + dateRequested + ".",
                "Duplicate Request",
                JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    FileManager.appendLine(
        fileName,
        String.join("|",
                IDGenerator.next("LR", fileName),
                patientId,
                currentDoctor.getUserId(),
                testType,
                roomId, 
                dateRequested,
                "",
                "PENDING"
        )
    );

    refreshTable();
}
  
    private String findName(String userId) {
        return ManageRecordsHelper.findName(userId);
    }

    public void addBackButton(Runnable onBack) {
        JButton backButton = new JButton("Back");
        backButton.setBackground(Color.BLACK);
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setOpaque(true);
        backButton.setBorderPainted(false);
        backButton.addActionListener(e -> onBack.run());
        
        BorderLayout layout = (BorderLayout) getLayout();
        JPanel topBar = (JPanel) layout.getLayoutComponent(BorderLayout.NORTH);
        
        if (topBar != null) {
            java.awt.Component actions = ((BorderLayout) topBar.getLayout()).getLayoutComponent(BorderLayout.EAST);
            if (actions != null) {
                topBar.remove(actions);
                
                JPanel newActionsPanel = new JPanel(new BorderLayout(15, 0));
                newActionsPanel.add(actions, BorderLayout.CENTER);
                
                JPanel backBtnPanel = new JPanel(new BorderLayout());
                backBtnPanel.add(backButton, BorderLayout.NORTH); // Align top
                newActionsPanel.add(backBtnPanel, BorderLayout.EAST);
                
                topBar.add(newActionsPanel, BorderLayout.EAST);
                topBar.revalidate();
                topBar.repaint();
            }
        }
    }
    private void setupTableSorter() {
    if (labRequestTable) {
        recordsTable.setAutoCreateRowSorter(false);
        ManageRecordsHelper.applyStatusSorter(recordsTable, 7);
        
        // Force the STATUS column (index 7) to sort ASCENDING on refresh
        if (recordsTable.getRowSorter() != null) {
            recordsTable.getRowSorter().setSortKeys(
                java.util.List.of(new javax.swing.RowSorter.SortKey(7, javax.swing.SortOrder.ASCENDING))
            );
        }
    } else {
        recordsTable.setAutoCreateRowSorter(true);
    }
    }
}
    
