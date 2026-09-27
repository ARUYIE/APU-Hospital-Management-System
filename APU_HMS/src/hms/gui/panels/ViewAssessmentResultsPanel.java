package hms.gui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.AbstractCellEditor;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import hms.role.User;
import hms.util.FileManager;
import hms.util.ManageRecordsHelper;
import hms.util.RecordDetailDialog;
import hms.util.Session;

public class ViewAssessmentResultsPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable recordsTable;

    public ViewAssessmentResultsPanel(String title) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        JButton detailsButton = new JButton("View All Details");
        detailsButton.addActionListener(e -> showSelectedRecordDetails());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(refreshButton);
        actions.add(detailsButton);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);

        tableModel = new DefaultTableModel(
                new String[]{"APPOINTMENT_ID", "DATE", "TIME", "DOCTOR", "CONSULTATION NOTES & VITALS", "PRESCRIPTION"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Columns 4 and 5 are interactive/clickable
                return column == 4 || column == 5;
            }
        };

        recordsTable = new JTable(tableModel);
        recordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordsTable.setAutoCreateRowSorter(true);

        // Column 4 Vitals & Notes Editor/Renderer
        recordsTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonCellRenderer());
        recordsTable.getColumnModel().getColumn(4).setCellEditor(new ButtonCellEditor(true));

        // Column 5 Prescription Editor/Renderer
        recordsTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonCellRenderer());
        recordsTable.getColumnModel().getColumn(5).setCellEditor(new ButtonCellEditor(false));

        recordsTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        recordsTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        recordsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        recordsTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        recordsTable.getColumnModel().getColumn(4).setPreferredWidth(220);
        recordsTable.getColumnModel().getColumn(5).setPreferredWidth(220);

        // Sort by Date
        if (recordsTable.getRowSorter() != null) {
            recordsTable.getRowSorter().setSortKeys(
                    java.util.List.of(new javax.swing.RowSorter.SortKey(1, javax.swing.SortOrder.DESCENDING))
            );
        }

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(recordsTable), BorderLayout.CENTER);

        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String patientId = currentUser.getUserId();
        String patientUsername = currentUser.getUsername();

        List<String> appointments = FileManager.readLines("bookings.txt");
        List<String> vitalsList = FileManager.readLines("vital_signs.txt");
        List<String> prescriptionsList = FileManager.readLines("prescriptions.txt");

        for (int i = 1; i < appointments.size(); i++) {
            String aptLine = appointments.get(i);
            if (aptLine == null || aptLine.trim().isEmpty()) {
                continue;
            }

            String[] aptParts = aptLine.split("\\|", -1);
            if (aptParts.length >= 6) {
                String aptId = aptParts[0].trim();
                String aptPatientId = aptParts[1].trim();
                String doctorId = aptParts[2].trim();
                String date = aptParts[3].trim();
                String time = aptParts[4].trim();
                String status = aptParts[5].trim().toUpperCase();

                if (!status.equals("COMPLETED") && !status.equals("SCHEDULED")) {
                    continue;
                }

                if (aptPatientId.equalsIgnoreCase(patientId) || aptPatientId.equalsIgnoreCase(patientUsername)) {

                    // Match Vital Signs by Appointment ID 
                    String vitalsSummary = "No Vitals Recorded";
                    for (int v = 1; v < vitalsList.size(); v++) {
                        String vLine = vitalsList.get(v);
                        String[] vParts = vLine.split("\\|", -1);
                        if (vParts.length >= 9) {
                            String linkedAptId = vParts[3].trim();
                            if (linkedAptId.equalsIgnoreCase(aptId)) {
                                vitalsSummary = "BP: " + vParts[4] + " | Temp: " + vParts[6] + "°C [View Vitals]";
                                break;
                            }
                        }
                    }

                    // Match Prescriptions by Appointment ID
                    String rxSummary = "No Prescription Issued";
                    for (int r = 1; r < prescriptionsList.size(); r++) {
                        String rLine = prescriptionsList.get(r);
                        String[] rParts = rLine.split("\\|", -1);
                        if (rParts.length >= 8) {
                            String linkedAptId = rParts[6].trim();
                            if (linkedAptId.equalsIgnoreCase(aptId)) {
                                rxSummary = rParts[3] + " (" + rParts[4] + ") [View Rx]";
                                break;
                            }
                        }
                    }

                    tableModel.addRow(new Object[]{
                        aptId,
                        date,
                        time,
                        ManageRecordsHelper.findName(doctorId),
                        vitalsSummary,
                        rxSummary
                    });
                }
            }
        }
    }

    private void showSelectedRecordDetails() {
        int viewRow = recordsTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an appointment session first.",
                    "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = recordsTable.convertRowIndexToModel(viewRow);
        showCombinedDetailsForRow(modelRow);
    }

    // Popup showing ONLY Vitals & Consultation Notes
    private void showVitalsDetailsForRow(int modelRow) {
        String aptId = tableModel.getValueAt(modelRow, 0).toString().trim();

        String vitalSignId = "N/A";
        String bp = "N/A";
        String hr = "N/A";
        String temp = "N/A";
        String notes = "No detailed consultation notes or vitals found for this appointment.";

        for (String line : FileManager.readLines("vital_signs.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 9 && p[3].trim().equalsIgnoreCase(aptId)) {
                vitalSignId = p[0];
                bp = p[4];
                hr = p[5] + " bpm";
                temp = p[6] + " °C";
                notes = p[8];
                break;
            }
        }

        String[] labels = {"VITAL_SIGN_ID", "BLOOD_PRESSURE", "HEART_RATE", "TEMPERATURE", "CONSULTATION_NOTES"};
        String[] values = {vitalSignId, bp, hr, temp, notes};

        RecordDetailDialog.showDetails(this, "Consultation Notes & Vitals", labels, values);
    }

    // Popup showing ONLY Prescription Details
    private void showPrescriptionDetailsForRow(int modelRow) {
        String aptId = tableModel.getValueAt(modelRow, 0).toString().trim();
        String appointmentDate = tableModel.getValueAt(modelRow, 1).toString().trim();

        String rxId = "N/A";
        String medication = "N/A";
        String dosage = "N/A";
        String duration = "N/A";
        String dateIssued = appointmentDate;
        String status = "N/A";

        for (String line : FileManager.readLines("prescriptions.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 8 && p[6].trim().equalsIgnoreCase(aptId)) {
                rxId = p[0];
                medication = p[3];
                dosage = p[4];
                duration = p[5];
                dateIssued = appointmentDate;
                status = p[7];
                break;
            }
        }

        String[] labels = {"PRESCRIPTION_ID", "MEDICATION", "DOSAGE", "DURATION", "DATE_ISSUED", "STATUS"};
        String[] values = {rxId, medication, dosage, duration, dateIssued, status};

        RecordDetailDialog.showDetails(this, "Prescription Details", labels, values);
    }

    // Popup showing both combined 
    private void showCombinedDetailsForRow(int modelRow) {
        String aptId = tableModel.getValueAt(modelRow, 0).toString().trim();
        String appointmentDate = tableModel.getValueAt(modelRow, 1).toString().trim();

        String fullVitalsDetails = "No detailed consultation notes or vitals found for this appointment.";
        for (String line : FileManager.readLines("vital_signs.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 9 && p[3].trim().equalsIgnoreCase(aptId)) {
                fullVitalsDetails = "Vital Sign ID: " + p[0]
                        + "\nBlood Pressure: " + p[4]
                        + "\nHeart Rate: " + p[5] + " bpm"
                        + "\nTemperature: " + p[6] + " °C"
                        + "\n\nConsultation Notes:\n" + p[8];
                break;
            }
        }

        String fullPrescriptionDetails = "No prescription records found for this appointment.";
        for (String line : FileManager.readLines("prescriptions.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 8 && p[6].trim().equalsIgnoreCase(aptId)) {
                fullPrescriptionDetails = "Prescription ID: " + p[0]
                        + "\nMedication: " + p[3]
                        + "\nDosage: " + p[4]
                        + "\nDuration: " + p[5]
                        + "\nDate Issued: " + appointmentDate
                        + "\nStatus: " + p[7];
                break;
            }
        }

        String[] labels = {"SESSION DATE", "CONSULTATION NOTES & VITALS", "PRESCRIPTION DETAILS"};
        String[] values = {appointmentDate, fullVitalsDetails, fullPrescriptionDetails};

        RecordDetailDialog.showDetails(this, "Session Assessment Details", labels, values);
    }

    // Custom Renderer 
    private static class ButtonCellRenderer extends JButton implements TableCellRenderer {

        public ButtonCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final JButton button = new JButton();
        private String label;
        private int currentRow;
        private final boolean isVitalsColumn;

        public ButtonCellEditor(boolean isVitalsColumn) {
            this.isVitalsColumn = isVitalsColumn;
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    int modelRow = recordsTable.convertRowIndexToModel(currentRow);
                    if (isVitalsColumn) {
                        showVitalsDetailsForRow(modelRow);
                    } else {
                        showPrescriptionDetailsForRow(modelRow);
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }
    }
}
