package hms.util;

import hms.role.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Low Kai Lun - Doctor: backs the "View My Patients" menu item. Read-only -
 * built from the current doctor's own appointment history in bookings.txt,
 * so there is nothing here for the doctor to add/edit/delete.
 */
public class MyPatientsPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable patientsTable;

    public MyPatientsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel("View My Patients");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);
        topBar.add(refreshButton, BorderLayout.EAST);

        tableModel = new DefaultTableModel(
                new String[]{"PATIENT_NAME", "LAST_APPOINTMENT_DATE", "LAST_STATUS", "TOTAL_APPOINTMENTS"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        patientsTable = new JTable(tableModel);
        patientsTable.setAutoCreateRowSorter(true);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(patientsTable), BorderLayout.CENTER);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        User currentDoctor = Session.getCurrentUser();
        if (currentDoctor == null) {
            return;
        }

        // bookings.txt: APPOINTMENT_ID|PATIENT_ID|DOCTOR_ID|DATE|TIME|STATUS|NOTES
        List<String> bookings = FileManager.readLines("bookings.txt");
        Map<String, Object[]> summaryByPatientId = new LinkedHashMap<>();

        for (String line : bookings) {
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length < 6) {
                continue;
            }
            String patientId = parts[1].trim();
            String doctorId = parts[2].trim();
            String date = parts[3].trim();
            String status = parts[5].trim();

            if (!doctorId.equals(currentDoctor.getUserId())) {
                continue;
            }

            Object[] existing = summaryByPatientId.get(patientId);
            if (existing == null) {
                summaryByPatientId.put(patientId, new Object[]{date, status, 1});
                continue;
            }

            String existingLatestDate = (String) existing[0];
            int updatedCount = (int) existing[2] + 1;
            // YYYY-MM-DD strings compare correctly as plain strings
            if (date.compareTo(existingLatestDate) >= 0) {
                summaryByPatientId.put(patientId, new Object[]{date, status, updatedCount});
            } else {
                summaryByPatientId.put(patientId, new Object[]{existingLatestDate, existing[1], updatedCount});
            }
        }

        for (Map.Entry<String, Object[]> entry : summaryByPatientId.entrySet()) {
            String patientName = ManageRecordsHelper.findName(entry.getKey());
            Object[] summary = entry.getValue();
            tableModel.addRow(new Object[]{patientName, summary[0], summary[1], summary[2]});
        }
    }
}