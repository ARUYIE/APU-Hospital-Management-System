package hms.util;

import java.awt.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


public final class RecordsHelperInsurance {
    private RecordsHelperInsurance() {
    }

    public static String editInsuranceRecord(Component comp, String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);
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

        int choice = JOptionPane.showConfirmDialog(comp, form,
                "Edit Insurance", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        return String.join("|",
                insuranceIdField.getText().trim(),
                insuranceNameField.getText().trim(),
                coverageRateField.getText().trim(),
                coveragePercentageField.getText().trim(),
                statusField.getText().trim(),
                contactInfoField.getText().trim(),
                effectiveDateField.getText().trim());

    }

    public static void addInsuranceRecord(Component comp, String fileName, Runnable refreshAction) {
        JTextField providerField = new JTextField();
        JTextField coverageRateField = new JTextField();
        JTextField coveragePercentageField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        JTextField contactField = new JTextField();
        JTextField effectiveDateField = new JTextField(java.time.LocalDate.now().toString());

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("PROVIDER_NAME:")); form.add(providerField);
        form.add(new JLabel("COVERAGE_RATE:")); form.add(coverageRateField);
        form.add(new JLabel("COVERAGE_PERCENTAGE:")); form.add(coveragePercentageField);
        form.add(new JLabel("STATUS:")); form.add(statusCombo);
        form.add(new JLabel("CONTACT_INFO:")); form.add(contactField);
        form.add(new JLabel("EFFECTIVE_DATE:")); form.add(effectiveDateField);

        int choice = JOptionPane.showConfirmDialog(comp, form,
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
            JOptionPane.showMessageDialog(comp,
                    "Coverage percentage must be a number from 0 to 100.",
                    "Invalid Insurance Network", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (providerField.getText().trim().isEmpty()
                || coverageRateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(comp,
                    "Insurance ID, provider name, and coverage rate are required.",
                    "Invalid Insurance Network", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileManager.appendLine(fileName, String.join("|",
            IDGenerator.next("INS", fileName), providerField.getText().trim(),
                coverageRateField.getText().trim(), percentage,
                (String) statusCombo.getSelectedItem(), contactField.getText().trim(),
                effectiveDateField.getText().trim()));
        refreshAction.run();
    }

    public static void addInsuranceRow(DefaultTableModel tableModel, String line) {
        String[] parts = ManageRecordsHelper.splitRecord(line);
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
    private static void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(Color.LIGHT_GRAY);
    }
}
