package hms.gui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import hms.role.Role;
import hms.role.User;
import hms.util.FileManager;
import hms.util.ManageRecordsHelper;
import hms.util.Session;

public class ViewBillsPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable billsTable;
    private final boolean isAdmin;

    public ViewBillsPanel(String title) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        User currentUser = Session.getCurrentUser();
        isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN_STAFF; // Adjust if your staff role has a different enum check

        // Define columns based on role
        if (isAdmin) {
            tableModel = new DefaultTableModel(
                    new String[]{"BILL_ID", "PATIENT", "DATE & TIME", "DOCTOR", "SERVICES / DEPARTMENT", "AMOUNT", "STATUS"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        } else {
            tableModel = new DefaultTableModel(
                    new String[]{"BILL_ID", "DATE & TIME", "DOCTOR", "SERVICES / DEPARTMENT", "AMOUNT", "STATUS"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        }

        billsTable = new JTable(tableModel);
        billsTable.setAutoCreateRowSorter(true);
        billsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);

        // Add action buttons for Admin/Staff view
        if (isAdmin) {
            JButton markPaidButton = new JButton("Mark as Paid");
            markPaidButton.addActionListener(e -> updateBillStatus("PAID"));

            JButton markUnpaidButton = new JButton("Mark as Unpaid");
            markUnpaidButton.addActionListener(e -> updateBillStatus("UNPAID"));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.add(markPaidButton);
            actions.add(markUnpaidButton);
            topBar.add(actions, BorderLayout.EAST);
        }

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

        String currentUserId = currentUser.getUserId();

        List<String> billLines = FileManager.readLines("bills.txt");
        List<String> appointmentLines = FileManager.readLines("bookings.txt");
        List<String> rosterLines = FileManager.readLines("roster.txt");

        for (int i = 1; i < billLines.size(); i++) {
            String line = billLines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length >= 6) {
                String billId = parts[0].trim();
                String patientId = parts[1].trim();
                String amount = parts[2].trim();
                String services = parts[3].trim();
                String date = parts[4].trim();
                String status = parts[5].trim();

                // If user is a patient, filter out other patients' bills
                if (!isAdmin && !patientId.equalsIgnoreCase(currentUserId)) {
                    continue;
                }

                String dateTime = date;
                String doctorName = "N/A";
                String department = services;

                for (String apt : appointmentLines) {
                    String[] aptParts = apt.split("\\|", -1);
                    if (aptParts.length >= 6 && (aptParts[0].trim().equalsIgnoreCase(billId.replace("BLL", "B")) || (aptParts[1].trim().equalsIgnoreCase(patientId) && aptParts[3].trim().equals(date)))) {
                        dateTime = aptParts[3].trim() + " " + aptParts[4].trim();
                        String doctorId = aptParts[2].trim();
                        doctorName = ManageRecordsHelper.findName(doctorId);

                        for (String rLine : rosterLines) {
                            String[] rParts = rLine.split("\\|", -1);
                            if (rParts.length >= 4) {
                                String rosterDocName = rParts[1].trim();
                                if (rosterDocName.equalsIgnoreCase(doctorName) || rosterDocName.equalsIgnoreCase(doctorId)) {
                                    department = rParts[3].trim();
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }

                if (isAdmin) {
                    tableModel.addRow(new Object[]{
                        billId,
                        ManageRecordsHelper.findName(patientId),
                        dateTime,
                        doctorName,
                        department,
                        "$" + amount,
                        status
                    });
                } else {
                    tableModel.addRow(new Object[]{
                        billId,
                        dateTime,
                        doctorName,
                        department,
                        "$" + amount,
                        status
                    });
                }
            }
        }

        // Apply status color coding to the status column index depending on the view layout
        ManageRecordsHelper.applyStatusColorCoding(billsTable, isAdmin ? 6 : 5);
    }

    private void updateBillStatus(String newStatus) {
        int selectedRow = billsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a bill to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = billsTable.convertRowIndexToModel(selectedRow);
        List<String> allRecords = FileManager.readLines("bills.txt");

        // Find matching record by Bill ID
        String targetBillId = tableModel.getValueAt(modelRow, 0).toString();

        for (int i = 1; i < allRecords.size(); i++) {
            String[] parts = allRecords.get(i).split("\\|", -1);
            if (parts.length > 0 && parts[0].trim().equalsIgnoreCase(targetBillId)) {
                parts[parts.length - 1] = newStatus; // Update status
                allRecords.set(i, String.join("|", parts));
                FileManager.writeAllLines("bills.txt", allRecords);
                JOptionPane.showMessageDialog(this, "Bill status successfully changed to " + newStatus + ".");
                refreshTable();
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Could not find bill record to update.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
