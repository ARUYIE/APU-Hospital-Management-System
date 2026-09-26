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
import hms.util.DoctorRosterMethods;
import hms.util.DoctorMethods;

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
    private final DefaultTableModel tableModel;
    private final JTable recordsTable;
    private final ManageRecordsHelper recordHelper;
    private List<String> records = new ArrayList<>();
    private final DoctorRosterMethods rosterMethods;
    private final DoctorMethods doctorMethods;

    public ManageRecordsPanel(String title, String fileName) {
        this.fileName = fileName;
        this.rosterMethods = new DoctorRosterMethods(this, fileName);
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
                ? new String[]{"VITAL_SIGN_ID", "PATIENT_ID", "DOCTOR_NAME", "CONSULTATION_ID", "BP", "HEART_RATE", "TEMPERATURE", "DATE", "NOTES"}
                : prescriptionTable
                ? new String[]{"PRESCRIPTION_ID", "PATIENT_ID", "DOCTOR_NAME", "MEDICATION", "DOSAGE", "DURATION", "DATE_ISSUED", "STATUS"}
                : labRequestTable       
                ? new String[]{"REQUEST_ID", "PATIENT_ID", "DOCTOR_Name", "TEST_TYPE", "ROOM_ID", "DATE_REQUESTED", "DATE_COMPLETED", "STATUS"}
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
        } 
        
        actions.add(refreshButton);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);


        // Has two rows for action buttons
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
                ? rosterMethods.editDepartmentRecord(originalRecord)
                : appointmentTable
                ? RecordsHelperAppointment.editAppointmentRecord(this, originalRecord)
                : insuranceTable
                ? RecordsHelperInsurance.editInsuranceRecord(this, originalRecord)
                : consultationRateTable
                ? RecordsHelperConsultation.editConsultationRateRecord(this, originalRecord)
                : assetTable
                ? RecordsHelperAsset.editAssetRecord(this, originalRecord)
                : rosterTable
                ? rosterMethods.editRosterRecord(originalRecord)
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
            String newRecord = rosterMethods.addRosterRecord();

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
        
        if (departmentTable) {
            String newRecord = rosterMethods.addDepartmentRow();

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
}
