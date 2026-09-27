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
import hms.util.ManageRecordsHelper;
import hms.util.RecordsHelperAppointment;
import hms.util.RecordsHelperAsset;
import hms.util.RecordsHelperConsultation;
import hms.util.RecordsHelperInsurance;
import hms.util.ManagerMethods;
import hms.util.DoctorMethods;
import hms.util.Session;

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
    private final boolean assetTable;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final boolean rosterTable;
    private final boolean consultationTable;
    private final boolean prescriptionTable;
    private final boolean labRequestTable;
    private final boolean medicalRecordTable;
    private final boolean billTable;
    private final boolean feedbackTable;
    private final boolean patientAppointmentTable;
    private final DefaultTableModel tableModel;
    private final JTable recordsTable;
    private final ManageRecordsHelper recordHelper;
    private List<String> records = new ArrayList<>();
    private final ManagerMethods managerMethods;
    private final DoctorMethods doctorMethods;

    public ManageRecordsPanel(String title, String fileName) {
        this.fileName = fileName;
        this.managerMethods = new ManagerMethods(this, fileName);
        this.doctorMethods = new DoctorMethods();
        
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
        
        //Yong Jun Hong - Patient
        medicalRecordTable = "medical_records.txt".equalsIgnoreCase(fileName);
        billTable = "bills.txt".equalsIgnoreCase(fileName);
        feedbackTable = "feedback_records.txt".equalsIgnoreCase(fileName);
        patientAppointmentTable = "appointments.txt".equalsIgnoreCase(fileName);
       
        
        tableModel = new DefaultTableModel(
                departmentTable
                ? new String[]{"DEPTARTMENT_ID", "DEPTARTMENT_NAME", "HEAD_MANAGER_NAME", "DESCRIPTION"}
                : appointmentTable
                ? new String[]{"APPOINTMENT_ID", "PATIENT_NAME", "DOCTOR_NAME", "DATE", "TIME", "STATUS"}
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
                : patientAppointmentTable
                ? new String[]{"APPOINTMENT_ID", "PATIENT_USERNAME", "DOCTOR", "DATE", "TIME", "STATUS"}
                : medicalRecordTable
                ? new String[]{"RECORD_ID", "PATIENT_ID", "DOCTOR_ID", "DATE", "VITALS", "DIAGNOSIS_NOTES", "PRESCRIPTION"}
                : billTable
                ? new String[]{"BILL_ID", "PATIENT_ID", "AMOUNT", "SERVICES", "DATE", "STATUS"}
                : feedbackTable
                ? new String[]{"FEEDBACK_ID", "RECORD_ID", "PATIENT_ID", "DOCTOR_ID", "RATING", "COMMENTS"}
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
        boolean isPatient = currentUser != null && currentUser.getRole() == Role.PATIENT;
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN_STAFF;
        
        JButton approveLabRequestBtn = new JButton("Approve Request");
        approveLabRequestBtn.addActionListener(e -> updateSelectedLabRequestStatus("APPROVED"));
        
        JButton denyLabRequestBtn = new JButton("Deny Request");
        denyLabRequestBtn.addActionListener(e -> updateSelectedLabRequestStatus("DENIED"));

        JButton markLabCompletedBtn = new JButton("Mark Completed");
        markLabCompletedBtn.addActionListener(e -> markLabRequestCompleted());

        
        //button adding 
        actions.add(refreshButton);
        //role-dependent button adding
        if (isPatient) {
            if (appointmentTable || patientAppointmentTable) {
                actions.add(addButton);
                actions.add(cancelAppointmentButton);
            } else if (feedbackTable) {
                actions.add(addButton);
            }
        } else if (isAdmin) {
            // Admins have custom management buttons for lab requests, standard controls for other tables
            if (labRequestTable) {
                actions.add(approveLabRequestBtn);
                actions.add(denyLabRequestBtn);
                actions.add(markLabCompletedBtn);
                actions.add(editButton);
            } else {
                actions.add(addButton);
                actions.add(editButton);
                actions.add(deleteButton);
            }
        } else if (!isDoctor){
            actions.add(addButton);
            actions.add(editButton);
            actions.add(deleteButton);
        }
        //has two rows since its a bit too long
        if (appointmentTable) {
            if(isAdmin){
            JPanel appointmentActions = new JPanel();
            appointmentActions.setLayout(new BoxLayout(appointmentActions, BoxLayout.Y_AXIS));
            
            JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JPanel recordActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
         
            searchActions.add(new JLabel("Search Doctor:"));
            searchActions.add(doctorSearchBox);
            searchActions.add(markCompletedButton);
            searchActions.add(cancelAppointmentButton);
            recordActions.add(refreshButton);
            recordActions.add(addButton);
            recordActions.add(editButton);
            recordActions.add(deleteButton);
            
            appointmentActions.add(searchActions);
            appointmentActions.add(recordActions);
            actions = appointmentActions;
        }
        }

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);


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
        if (!appointmentTable && !patientAppointmentTable) {
            return;
        }

        int viewRow = recordsTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an appointment first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String actionText = newStatus.equals("CANCELLED") ? "cancel this appointment?" : "mark as completed";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + actionText,
                "Confirm Update",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return; 
        }

        int modelRow = recordsTable.convertRowIndexToModel(viewRow);
        String appointmentId = recordsTable.getModel().getValueAt(modelRow, 0).toString().trim();

        records = recordHelper.getRecords();
        int recordIndex = -1;

        for (int i = 0; i < records.size(); i++) {
            String record = records.get(i);
            if (record == null || record.trim().isEmpty()) continue;
            
            String[] parts = record.split("\\|");
            if (parts.length > 0 && parts[0].trim().equals(appointmentId)) {
                recordIndex = i;
                break;
            }
        }

        if (recordIndex == -1) {
            JOptionPane.showMessageDialog(this, 
                    "Appointment record not found in the database.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] parts = records.get(recordIndex).split("\\|", -1);
        
        if (parts.length >= 6) {
            parts[5] = newStatus;
            records.set(recordIndex, String.join("|", parts));
            writeRecords(records, "The appointment status could not be updated.");
        } else {
            JOptionPane.showMessageDialog(this, 
                    "The record format is invalid.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a record first.",
                    "No Record Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = recordsTable.convertRowIndexToModel(viewRow);

        // Get the ID from the selected table row
        String selectedId = recordsTable
                .getModel()
                .getValueAt(modelRow, 0)
                .toString()
                .trim();

        /*
         * Read the latest records directly from the file.
         * Do not use the in-memory records list here because
         * the table may be filtered or the list may be outdated.
         */
        List<String> latestRecords = FileManager.readLines(fileName);

        if (latestRecords == null || latestRecords.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No records were found.",
                    "Edit Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String originalRecord = null;

        /*
         * Skip the header at index 0.
         */
        for (int i = 1; i < latestRecords.size(); i++) {

            String record = latestRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = ManageRecordsHelper.splitRecord(record);

            if (parts.length > 0
                    && parts[0].trim().equals(selectedId)) {

                originalRecord = record;
                break;
            }
        }

        if (originalRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected record could not be found.",
                    "Edit Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String updatedRecord = departmentTable
                ? managerMethods.editDepartmentRecord(originalRecord)
                : appointmentTable
                ? RecordsHelperAppointment.editAppointmentRecord(
                        this,
                        originalRecord
                )
                : insuranceTable
                ? RecordsHelperInsurance.editInsuranceRecord(
                        this,
                        originalRecord
                )
                : consultationRateTable
                ? RecordsHelperConsultation.editConsultationRateRecord(
                        this,
                        originalRecord
                )
                : assetTable
                ? RecordsHelperAsset.editAssetRecord(
                        this,
                        originalRecord
                )
                : rosterTable
                ? managerMethods.editRosterRecord(originalRecord)
                : consultationTable
                ? doctorMethods.editVitalSignRecord(
                        this,
                        originalRecord,
                        fileName
                )
                : prescriptionTable
                ? doctorMethods.editPrescriptionRecord(
                        this,
                        originalRecord,
                        fileName
                )
                : labRequestTable
                ? doctorMethods.editLabRequestRecord(
                        this,
                        originalRecord,
                        fileName
                )
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
            JOptionPane.showMessageDialog(
                    this,
                    "Enter correct record.",
                    "Invalid Record",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        /*
         * Find the record again in the latest file data
         * and replace it.
         */
        int recordIndex = -1;

        for (int i = 1; i < latestRecords.size(); i++) {

            String record = latestRecords.get(i);

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
            JOptionPane.showMessageDialog(
                    this,
                    "The selected record could not be updated.",
                    "Edit Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        latestRecords.set(recordIndex, normalizedRecord);

        /*
         * Save directly using the latest file contents.
         */
        FileManager.writeAllLines(
                fileName,
                latestRecords
        );

        /*
         * Reload records and refresh the JTable.
         */
        refreshTable();
    }

    private String[] splitRecord(String record) {
        return ManageRecordsHelper.splitRecord(record);
    }


    public void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);;
        field.setBackground(Color.LIGHT_GRAY);
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
        refreshTable();
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
            String record =
                    DoctorMethods.addVitalSignRecord(
                            this,
                            fileName
                    );

            if (record != null) {
                FileManager.appendLine(
                        fileName,
                        record
                );

                refreshTable();
            }

            return;
        }
        if (prescriptionTable) {
            String record =
                    DoctorMethods.addPrescriptionRecord(
                            this,
                            fileName
                    );

            if (record != null) {
                FileManager.appendLine(
                        fileName,
                        record
                );

                refreshTable();
            }

            return;
        }
        if (labRequestTable) {
            String record =
                    DoctorMethods.addLabRequestRecord(
                            this,
                            fileName
                    );

            if (record != null) {
                FileManager.appendLine(
                        fileName,
                        record
                );

                refreshTable();
            }

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
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a record first.",
                    "No Record Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = recordsTable.convertRowIndexToModel(viewRow);

        String selectedId = recordsTable
                .getModel()
                .getValueAt(modelRow, 0)
                .toString()
                .trim();

        // Read the latest records directly from the file
        List<String> latestRecords = FileManager.readLines(fileName);

        if (latestRecords == null || latestRecords.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No records were found.",
                    "Delete Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String originalRecord = null;
        int recordIndex = -1;

        // Skip header
        for (int i = 1; i < latestRecords.size(); i++) {

            String record = latestRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length > 0
                    && parts[0].trim().equals(selectedId)) {

                originalRecord = record;
                recordIndex = i;
                break;
            }
        }

        if (originalRecord == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected record could not be found.",
                    "Delete Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Only the manager who manages the roster can delete it
        if (rosterTable) {

            User loggedInManager = Session.getCurrentUser();

            if (loggedInManager == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "No user is currently logged in.",
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(originalRecord);

            if (parts.length < 7) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid roster record.",
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            String managedBy = parts[2].trim();

            if (!managedBy.equalsIgnoreCase(
                    loggedInManager.getFullName().trim())) {

                JOptionPane.showMessageDialog(
                        this,
                        "You can only delete rosters managed by the current signed-in user.",
                        "Access Denied",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        latestRecords.remove(recordIndex);

        FileManager.writeAllLines(
                fileName,
                latestRecords
        );

        refreshTable();
    }

    public boolean hasIllegalChars(String... values) {
        return ManageRecordsHelper.hasIllegalChars(values);
    }
    
    public JTable getRecordsTable() {
        return recordsTable;
    }
    
    public void addBackButton(Runnable action) {
        JButton backButton = new JButton("Back");

        backButton.addActionListener(e -> action.run());

        JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backPanel.setOpaque(false);
        backPanel.add(backButton);

        add(backPanel, BorderLayout.SOUTH);

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
    } else if (appointmentTable || patientAppointmentTable) {
            recordsTable.setAutoCreateRowSorter(false);
            // Apply the sorter to column 5 (Status column for appointments)
            ManageRecordsHelper.applyStatusSorter(recordsTable, 5);
            
            // Force the STATUS column (index 5) to sort ASCENDING on refresh
            if (recordsTable.getRowSorter() != null) {
                recordsTable.getRowSorter().setSortKeys(
                    java.util.List.of(new javax.swing.RowSorter.SortKey(5, javax.swing.SortOrder.ASCENDING))
                );
            }
        } else {
        recordsTable.setAutoCreateRowSorter(true);
    }
    User currentUser = Session.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
            if (recordsTable.getRowSorter() instanceof javax.swing.table.TableRowSorter) {
                @SuppressWarnings("unchecked")
                javax.swing.table.TableRowSorter<DefaultTableModel> sorter = 
                    (javax.swing.table.TableRowSorter<DefaultTableModel>) recordsTable.getRowSorter();
                
                String regex = "(?i)\\b(" + java.util.regex.Pattern.quote(currentUser.getUserId()) + "|" 
                               + java.util.regex.Pattern.quote(currentUser.getFullName()) + ")\\b";
                sorter.setRowFilter(javax.swing.RowFilter.regexFilter(regex));
            }
        }
    }
}