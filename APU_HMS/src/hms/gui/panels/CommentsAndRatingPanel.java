package hms.gui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import hms.role.User;
import hms.util.FileManager;
import hms.util.IDGenerator;
import hms.util.ManageRecordsHelper;
import hms.util.Session;

public class CommentsAndRatingPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable recordsTable;

    public CommentsAndRatingPanel(String title) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(heading, BorderLayout.WEST);
        topBar.add(refreshButton, BorderLayout.EAST);

        tableModel = new DefaultTableModel(
                new String[]{"APPOINTMENT_ID", "DATE", "TIME", "DOCTOR", "REVIEW STATUS", "ACTION"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Action button column is clickable
            }
        };

        recordsTable = new JTable(tableModel);
        recordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordsTable.setAutoCreateRowSorter(true);

        // Action Button Renderer & Editor
        recordsTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonCellRenderer());
        recordsTable.getColumnModel().getColumn(5).setCellEditor(new ButtonCellEditor());

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
        List<String> appointments = FileManager.readLines("bookings.txt");
        List<String> reviews = FileManager.readLines("reviews.txt");

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

                // Only show for completed appointments belonging to the logged-in patient
                if (status.equals("COMPLETED") && aptPatientId.equalsIgnoreCase(patientId)) {

                    // Check if review already exists for this appointment
                    String reviewStatus = "Pending Review";
                    String buttonLabel = "Fill Review";

                    for (int r = 1; r < reviews.size(); r++) {
                        String rLine = reviews.get(r);
                        String[] rParts = rLine.split("\\|", -1);
                        if (rParts.length >= 4 && rParts[1].trim().equalsIgnoreCase(aptId)) {
                            reviewStatus = "Rating: " + rParts[2].trim() + " / 5 ⭐";
                            buttonLabel = "Edit Review";
                            break;
                        }
                    }

                    tableModel.addRow(new Object[]{
                        aptId,
                        date,
                        time,
                        ManageRecordsHelper.findName(doctorId),
                        reviewStatus,
                        buttonLabel
                    });
                }
            }
        }
    }

    private void openReviewDialog(int modelRow) {
        String aptId = tableModel.getValueAt(modelRow, 0).toString().trim();
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Check for existing review to prefill if editing
        String existingRating = "5";
        String existingComment = "";
        List<String> reviews = FileManager.readLines("reviews.txt");
        int matchingReviewLineIndex = -1;

        for (int i = 1; i < reviews.size(); i++) {
            String[] p = reviews.get(i).split("\\|", -1);
            if (p.length >= 4 && p[1].trim().equalsIgnoreCase(aptId)) {
                existingRating = p[2].trim();
                existingComment = p[3].trim();
                matchingReviewLineIndex = i;
                break;
            }
        }

        JComboBox<String> ratingCombo = new JComboBox<>(new String[]{"1 - Poor", "2 - Fair", "3 - Good", "4 - Very Good", "5 - Excellent"});
        ratingCombo.setSelectedItem(existingRating + " - " + getRatingText(existingRating));

        JTextArea commentArea = new JTextArea(existingComment, 4, 20);
        commentArea.setLineWrap(true);

        JPanel form = new JPanel(new GridLayout(3, 1, 8, 8));
        form.add(new JLabel("SELECT RATING:"));
        form.add(ratingCombo);
        form.add(new JLabel("COMMENTS / FEEDBACK:"));

        JPanel container = new JPanel(new BorderLayout(5, 5));
        container.add(form, BorderLayout.NORTH);
        container.add(new JScrollPane(commentArea), BorderLayout.CENTER);

        int choice = JOptionPane.showConfirmDialog(this, container, "Appointment Review", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            String selectedRatingStr = ratingCombo.getSelectedItem().toString().substring(0, 1);
            String comment = commentArea.getText().trim().replace("|", "-"); // Prevent pipe delimiter corruption

            if (comment.isEmpty()) {
                comment = "No comment provided.";
            }

            // Format: REVIEW_ID|APPOINTMENT_ID|RATING|COMMENT|PATIENT_ID
            String reviewId = (matchingReviewLineIndex != -1) ? reviews.get(matchingReviewLineIndex).split("\\|", -1)[0] : IDGenerator.next("REV", "reviews.txt");
            String reviewRecord = String.join("|", reviewId, aptId, selectedRatingStr, comment, currentUser.getUserId());

            if (matchingReviewLineIndex != -1) {
                reviews.set(matchingReviewLineIndex, reviewRecord);
            } else {
                reviews.add(reviewRecord);
            }

            FileManager.writeAllLines("reviews.txt", reviews);
            JOptionPane.showMessageDialog(this, "Review saved successfully!");
            refreshTable();
        }
    }

    private String getRatingText(String r) {
        return switch (r) {
            case "1" ->
                "Poor";
            case "2" ->
                "Fair";
            case "3" ->
                "Good";
            case "4" ->
                "Very Good";
            default ->
                "Excellent";
        };
    }

    // --- Custom Button Renderer ---
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

    // --- Custom Button Editor ---
    private class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final JButton button = new JButton();
        private int currentRow;

        public ButtonCellEditor() {
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                int modelRow = recordsTable.convertRowIndexToModel(currentRow);
                openReviewDialog(modelRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            button.setText((value == null) ? "" : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }
}
