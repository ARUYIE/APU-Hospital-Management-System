package hms.util;

import java.util.List;

public class BillingManager {

    public static void generateBillForAppointment(String aptId) {
        List<String> appointments = FileManager.readLines("bookings.txt");
        List<String> rosterLines = FileManager.readLines("roster.txt");
        List<String> ratesLines = FileManager.readLines("consultation_rates.txt");
        List<String> userLines = FileManager.readLines("users.txt");
        List<String> existingBills = FileManager.readLines("bills.txt");

        // Check to avoid duplicates
        for (String bill : existingBills) {
            String[] bParts = bill.split("\\|", -1);
            if (bParts.length > 0 && bParts[0].trim().equalsIgnoreCase(aptId.replace("B", "BLL"))) {
                return; // Bill already generated
            }
        }

        for (String apt : appointments) {
            String[] aptParts = apt.split("\\|", -1);
            if (aptParts.length >= 6 && aptParts[0].trim().equalsIgnoreCase(aptId)) {
                String patientId = aptParts[1].trim();
                String doctorId = aptParts[2].trim();
                String date = aptParts[3].trim();
                String status = aptParts[5].trim().toUpperCase();

                if (!status.equals("COMPLETED")) {
                    return;
                }

                String patientInsurance = "";
                for (String uLine : userLines) {
                    String[] uParts = uLine.split("\\|", -1);
                    if (uParts.length > 0 && uParts[0].trim().equalsIgnoreCase(patientId)) {
                        patientInsurance = uParts[uParts.length - 1].trim();
                        break;
                    }
                }

                String doctorFullName = ManageRecordsHelper.findName(doctorId);
                String department = "General_Surgery";
                for (String rLine : rosterLines) {
                    String[] rParts = rLine.split("\\|", -1);
                    if (rParts.length >= 4) {
                        String rosterDocName = rParts[1].trim();
                        if (rosterDocName.equalsIgnoreCase(doctorFullName) || rosterDocName.equalsIgnoreCase(doctorId)) {
                            department = rParts[3].trim();
                            break;
                        }
                    }
                }

                double baseRate = 50.00;
                for (String rtLine : ratesLines) {
                    String[] rtParts = rtLine.split("\\|", -1);
                    if (rtParts.length >= 2 && rtParts[0].trim().equalsIgnoreCase(department)) {
                        try {
                            baseRate = Double.parseDouble(rtParts[1].trim());
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    }
                }

                double discount = (!patientInsurance.isEmpty() && !patientInsurance.equalsIgnoreCase("None")) ? baseRate * 0.20 : 0.0;
                double finalAmount = baseRate - discount;

                String billId = aptId.replace("B", "BLL");

                // BILL_ID|BILLPATIENT_ID|AMOUNT|SERVICES|DATE|STATUS
                String newBillRecord = String.join("|",
                        billId,
                        patientId,
                        String.format("%.2f", finalAmount),
                        department,
                        date,
                        "UNPAID"
                );

                FileManager.appendLine("bills.txt", newBillRecord);
                break;
            }
        }
    }
}
