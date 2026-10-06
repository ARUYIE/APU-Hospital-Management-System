package hms.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

/**
 * Consultation-rate forms and persistence operations for record panels.
 */
public final class RecordsHelperConsultation {

    private RecordsHelperConsultation() {
    }

    public static String editConsultationRateRecord(Component parentComponent, String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        JTextField specialtyField = new JTextField(parts[0].trim());
        setUneditable(specialtyField);
        JTextField baseRateField = new JTextField(parts[1].trim());
        JTextField minRateField = new JTextField(parts[2].trim());
        JTextField maxRateField = new JTextField(parts[3].trim());
        JTextField currencyField = new JTextField(parts[4].trim());
        String effectiveDateStr = parts[5].trim();

        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        try {
            java.util.Date parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(effectiveDateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            model.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
            model.setSelected(true);
        } catch (Exception e) {
            model.setSelected(true); // Fallback to current date if parsing fails
        }
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        JPanel form = createForm(specialtyField, baseRateField, minRateField,
                maxRateField, currencyField, datePicker);
        int choice = JOptionPane.showConfirmDialog(parentComponent, form,
                "Edit Consultation Rate", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        if (!ManageRecordsHelper.validRateFields(baseRateField.getText(),
                minRateField.getText(), maxRateField.getText())) {
            showWarning(parentComponent,
                    "Rates must be numeric and satisfy MIN_RATE <= BASE_RATE <= MAX_RATE.");
            return null;
        }
        java.util.Date selectedDateUtil = (java.util.Date) datePicker.getModel().getValue();
        String finalDateStr = selectedDateUtil != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDateUtil)
                : java.time.LocalDate.now().toString();
        return String.join("|", specialtyField.getText().trim(),
                baseRateField.getText().trim(),
                minRateField.getText().trim(),
                maxRateField.getText().trim(),
                currencyField.getText().trim(),
                finalDateStr);
    }

    public static void addConsultationRateRecord(Component parentComponent,
            String fileName, Runnable refreshAction) {
        JTextField specialtyField = new JTextField();
        JTextField baseRateField = new JTextField();
        JTextField minRateField = new JTextField();
        JTextField maxRateField = new JTextField();
        JTextField currencyField = new JTextField("USD");

        UtilDateModel model = new UtilDateModel();
        model.setSelected(true);
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl effectiveDatePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        JPanel form = createForm(specialtyField, baseRateField, minRateField,
                maxRateField, currencyField, effectiveDatePicker);
        int choice = JOptionPane.showConfirmDialog(parentComponent, form,
                "Add Consultation Rate", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        if (specialtyField.getText().trim().isEmpty()
                || !ManageRecordsHelper.validRateFields(baseRateField.getText(),
                        minRateField.getText(), maxRateField.getText())) {
            showWarning(parentComponent,
                    "Enter a specialty and valid rates where MIN_RATE <= BASE_RATE <= MAX_RATE.");
            return;
        }

        java.util.Date selectedDateUtil = (java.util.Date) effectiveDatePicker.getModel().getValue();
        String finalDateStr = selectedDateUtil != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDateUtil)
                : java.time.LocalDate.now().toString();

        FileManager.appendLine(fileName, String.join("|",
                specialtyField.getText().trim(), baseRateField.getText().trim(),
                minRateField.getText().trim(), maxRateField.getText().trim(),
                currencyField.getText().trim(), finalDateStr));
        refreshAction.run();
    }

    private static JPanel createForm(JTextField specialtyField, JTextField baseRateField,
            JTextField minRateField, JTextField maxRateField, JTextField currencyField,
            JComponent effectiveDateField) {
        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("SPECIALTY:"));
        form.add(specialtyField);
        form.add(new JLabel("BASE_RATE:"));
        form.add(baseRateField);
        form.add(new JLabel("MIN_RATE:"));
        form.add(minRateField);
        form.add(new JLabel("MAX_RATE:"));
        form.add(maxRateField);
        form.add(new JLabel("CURRENCY:"));
        form.add(currencyField);
        form.add(new JLabel("EFFECTIVE_DATE:"));
        form.add(effectiveDateField);
        return form;
    }

    private static void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(Color.LIGHT_GRAY);
    }

    private static void showWarning(Component parentComponent, String message) {
        JOptionPane.showMessageDialog(parentComponent, message,
                "Invalid Consultation Rate", JOptionPane.WARNING_MESSAGE);
    }
}
