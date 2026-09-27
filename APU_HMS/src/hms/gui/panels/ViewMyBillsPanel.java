package hms.gui.panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import hms.role.User;
import hms.util.FileManager;
import hms.util.ManageRecordsHelper;
import hms.util.Session;

public class ViewMyBillsPanel extends JPanel {

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"BILL_ID", "AMOUNT", "SERVICES", "DATE", "STATUS"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable billsTable = new JTable(tableModel);

    public ViewMyBillsPanel(String title) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        billsTable.setAutoCreateRowSorter(true);
        billsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(billsTable), BorderLayout.CENTER);

        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String patientId = currentUser.getUserId() != null ? currentUser.getUserId().trim() : "";
        String patientUsername = currentUser.getUsername() != null ? currentUser.getUsername().trim() : "";
        String patientName = currentUser.getFullName() != null ? currentUser.getFullName().trim() : "";

        List<String> billLines = FileManager.readLines("bills.txt");
        for (int i = 1; i < billLines.size(); i++) {
            String line = billLines.get(i);
            if (line == null || line.trim().isEmpty()) continue;

            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length >= 6) {
                String billId = parts[0].trim();
                String billPatientId = parts[1].trim();
                String amount = parts[2].trim();
                String services = parts[3].trim();
                String date = parts[4].trim();
                String status = parts[5].trim();

                // Check against User ID, Username, or Full Name to ensure a match
                if (billPatientId.equalsIgnoreCase(patientId) 
                        || billPatientId.equalsIgnoreCase(patientUsername)
                        || billPatientId.equalsIgnoreCase(patientName)) {
                    
                    tableModel.addRow(new Object[]{
                        billId,
                        amount,
                        services,
                        date,
                        status
                    });
                }
            }
        }
    }
}