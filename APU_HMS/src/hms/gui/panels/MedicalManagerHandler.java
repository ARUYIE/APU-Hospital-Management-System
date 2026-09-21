/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hms.gui.panels;

import hms.util.Asset;
import hms.util.FileManager;
import hms.util.IDGenerator;
import hms.util.ManageRecordsHelper;
import hms.util.UserRepository;
import hms.role.Role;
import hms.role.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class MedicalManagerHandler {
    
    public MedicalManagerHandler(DefaultTableModel tableModel, List<String> records){
        
    }
    
    public void assignDepartment() {
        // Assign department functionality
    }

    public void assignRosterShift() {
        // Assign roster shift functionality
    }

    public void viewHospitalMetrics() {
        // View hospital metrics functionality
    }

    public void viewRevenueSummary() {
        // View revenue summary functionality
    }
    
    private String[] splitRecord(String record) {
        return ManageRecordsHelper.splitRecord(record);
    }
    
    private void addRosterRecord(String line) {
        String[] parts = splitRecord(line);
        if (parts.length < 4) {
            return;
        }

        tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                findName(parts[3].trim()),
                parts[2].trim()
        });
    }
    
    private String editRosterRecord(String record, JTable recordsTable, List<String> records) {
        String[] parts = splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        String rosterId = parts[0].trim();
        String doctorId = parts[1].trim();
        String doctorName = parts[2].trim();
        String departmentName = parts[3].trim();
        String date = parts[4].trim();
        String shift = parts[5].trim();
        String status = parts[6].trim();

        JTextField rosterIdField = new JTextField(rosterId);
        setUneditable(rosterIdField);
        JTextField doctorIdField = new JTextField(doctorId);
        setUneditable(doctorIdField);
        JTextField doctorNameField = new JTextField(doctorName);
        JTextField departmentNameField = new JTextField(departmentName);
        JTextField dateField = new JTextField(date);
        JTextField shiftField = new JTextField(shift);
        JTextField statusField = new JTextField(status);

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("ROSTER_ID:"));
        form.add(rosterIdField);
        form.add(new JLabel("DOCTOR_ID:"));
        form.add(doctorIdField);
        form.add(new JLabel("DOCTOR_NAME:"));
        form.add(doctorNameField);
        form.add(new JLabel("DEPARTMENT:"));
        form.add(departmentNameField);
        form.add(new JLabel("DATE:"));
        form.add(dateField);
        form.add(new JLabel("SHIFT:"));
        form.add(shiftField);
        form.add(new JLabel("STATUS:"));
        form.add(statusField);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Edit Insurance", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String updatedRecord = String.join("|",
                rosterIdField.getText().trim(),
                doctorIdField.getText().trim(),
                doctorNameField.getText().trim(),
                departmentNameField.getText().trim(),
                dateField.getText().trim(),
                shiftField.getText().trim(),
                statusField.getText().trim());

        List<String> updatedLines = new ArrayList<>(records);
        int modelRow = recordsTable.convertRowIndexToModel(recordsTable.getSelectedRow());
        updatedLines.set(modelRow, updatedRecord);
        writeRecords(updatedLines, "The insurance record could not be updated.");
        return updatedRecord;
    }
}
