package hms.util;

import javax.swing.table.DefaultTableModel;

import hms.role.User;

public class PatientAssessment {    
    public void loadUnifiedAssessmentResults(DefaultTableModel tableModel, User currentPatient) {
    tableModel.setRowCount(0); 
    String patientId = currentPatient.getUserId();
    String patientUsername = currentPatient.getUsername();

    // Load Vital Signs / Consultations
    for (String line : FileManager.readLines("vital_signs.txt")) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 9) {
            String recordPatientId = parts[1].trim();
            if (recordPatientId.equalsIgnoreCase(patientId) || recordPatientId.equalsIgnoreCase(patientUsername)) {
                tableModel.addRow(new Object[]{
                    parts[0].trim(), // ID
                    "Vital Sign / Consultation", // Type
                    parts[7].trim(), // Date
                    ManageRecordsHelper.findName(parts[2].trim()), // Doctor Name
                    "BP: " + parts[4] + ", HR: " + parts[5] + ", Temp: " + parts[6] // Summary details
                });
            }
        }
    }

    // Load Prescriptions
    for (String line : FileManager.readLines("prescriptions.txt")) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 8) {
            String recordPatientId = parts[1].trim();
            if (recordPatientId.equalsIgnoreCase(patientId) || recordPatientId.equalsIgnoreCase(patientUsername)) {
                tableModel.addRow(new Object[]{
                    parts[0].trim(),
                    "Prescription",
                    parts[6].trim(), // Date Issued
                    ManageRecordsHelper.findName(parts[2].trim()),
                    "Med: " + parts[3] + " (" + parts[4] + ") - Status: " + parts[7]
                });
            }
        }
    }

    // Load Lab Requests
    for (String line : FileManager.readLines("lab_requests.txt")) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 8) {
            String recordPatientId = parts[1].trim();
            if (recordPatientId.equalsIgnoreCase(patientId) || recordPatientId.equalsIgnoreCase(patientUsername)) {
                tableModel.addRow(new Object[]{
                    parts[0].trim(),
                    "Lab / Imaging",
                    parts[5].trim(), // Date Requested
                    ManageRecordsHelper.findName(parts[2].trim()),
                    "Test: " + parts[3] + " - Status: " + parts[7]
                });
            }
        }
    }
}
}
