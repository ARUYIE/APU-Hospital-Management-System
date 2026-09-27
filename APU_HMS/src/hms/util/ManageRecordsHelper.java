package hms.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import hms.role.Role;
import hms.role.User;

/**
 * Shared data and table operations for record-management panels.
 */
public final class ManageRecordsHelper {

    private final String fileName;
    private boolean patientAppointmentTable;
    private final boolean departmentTable;
    private final boolean appointmentTable;
    private final boolean assetTable;
    private final boolean insuranceTable;
    private final boolean consultationRateTable;
    private final boolean rosterTable;
    private final boolean consultationTable;
    private final boolean prescriptionTable;
    private final boolean labRequestTable;
    private final boolean billsTable;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> doctorSearchBox;
    private final JComboBox<String> assetSearchBox;
    private boolean updatingAssetFilter;
    private final List<String> records = new ArrayList<>();
    private String headerLine;

    private final DoctorMethods doctorMethods;

    public ManageRecordsHelper(String fileName, DefaultTableModel tableModel, JComboBox<String> doctorSearchBox, JComboBox<String> assetSearchBox, boolean consultationTable, boolean prescriptionTable, boolean labRequestTable) {
        this.fileName = fileName;
        this.tableModel = tableModel;
        this.doctorSearchBox = doctorSearchBox;
        this.assetSearchBox = assetSearchBox;
        departmentTable = "department.txt".equalsIgnoreCase(fileName);
        appointmentTable = "bookings.txt".equalsIgnoreCase(fileName);
        assetTable = "hospital_assets.txt".equalsIgnoreCase(fileName);
        insuranceTable = "insurance_networks.txt".equalsIgnoreCase(fileName);
        consultationRateTable = "consultation_rates.txt".equalsIgnoreCase(fileName);
        rosterTable = "roster.txt".equalsIgnoreCase(fileName);
        this.consultationTable = "vital_signs.txt".equalsIgnoreCase(fileName);
        this.prescriptionTable = "prescriptions.txt".equalsIgnoreCase(fileName);
        this.labRequestTable = "lab_requests.txt".equalsIgnoreCase(fileName);
        this.billsTable = "bills.txt".equalsIgnoreCase(fileName);
        this.doctorMethods = new DoctorMethods();
    }

    public List<String> getRecords() {
        return new ArrayList<>(records);
    }

    public void refreshTable() {

        List<String> lines = FileManager.readLines(fileName);

        headerLine = null;

        // Always treat the first line as the header
        if (!lines.isEmpty()) {
            headerLine = lines.remove(0);
        }

        records.clear();
        records.addAll(lines);

        refreshAssetFilterOptions();
        tableModel.setRowCount(0);

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            addTableRow(record);
        }
    }

    public boolean appendRecord(String record, String errorMessage) {
        List<String> before = FileManager.readLines(fileName);
        FileManager.appendLine(fileName, record);
        List<String> after = FileManager.readLines(fileName);
        boolean saved = after.size() == before.size() + 1
                && after.get(after.size() - 1).equals(record);
        if (saved) {
            if ("bookings.txt".equalsIgnoreCase(fileName)) {
                String[] parts = splitRecord(record);
                if (parts.length >= 6 && parts[5].trim().equalsIgnoreCase("COMPLETED")) {
                    BillingManager.generateBillForAppointment(parts[0].trim());
                }
            }
            refreshTable();
            return true;
        }
        return false;
    }

    public boolean writeRecords(List<String> updatedRecords, String errorMessage) {
        List<String> linesToWrite = new ArrayList<>();
        if (headerLine != null) {
            linesToWrite.add(headerLine);
        }
        linesToWrite.addAll(updatedRecords);
        FileManager.writeAllLines(fileName, linesToWrite);

        boolean saved = FileManager.readLines(fileName).equals(linesToWrite);
        if (saved) {
            if ("bookings.txt".equalsIgnoreCase(fileName)) {
                for (String record : updatedRecords) {
                    String[] parts = splitRecord(record);
                    if (parts.length >= 6 && parts[5].trim().equalsIgnoreCase("COMPLETED")) {
                        BillingManager.generateBillForAppointment(parts[0].trim());
                    }
                }
            }
            refreshTable();
        }
        return saved;
    }

    public boolean deleteRecord(int modelRow) {
        if (modelRow < 0 || modelRow >= records.size()) {
            return false;
        }
        records.remove(modelRow);
        List<String> linesToWrite = new ArrayList<>();
        if (headerLine != null) {
            linesToWrite.add(headerLine);
        }
        linesToWrite.addAll(records);
        FileManager.writeAllLines(fileName, linesToWrite);
        if (!FileManager.readLines(fileName).equals(linesToWrite)) {
            refreshTable();
            return false;
        }
        refreshTable();
        return true;
    }

    public String getRecord(int modelRow) {
        return modelRow >= 0 && modelRow < records.size() ? records.get(modelRow) : null;
    }

    public void reserveAsset(Component comp, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(comp,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> departments = DepartmentManager.getDepartmentNames();
        if (departments.isEmpty()) {
            JOptionPane.showMessageDialog(comp,
                    "There are no departments available to reserve this asset.",
                    "No Departments Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> departmentCombo = new JComboBox<>(departments.toArray(String[]::new));
        int choice = JOptionPane.showConfirmDialog(comp, departmentCombo,
                "Select Department Reserving This Asset", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String departmentName = (String) departmentCombo.getSelectedItem();
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return;
        }
        if (!"AVAILABLE".equalsIgnoreCase(asset.getStatus())) {
            JOptionPane.showMessageDialog(comp,
                    "This ward/clinic is already in use.",
                    "Reservation Conflict", JOptionPane.WARNING_MESSAGE);
            return;
        }

        asset.setStatus("OCCUPIED");
        asset.setDescription(departmentName.trim());
        if (AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(comp, "Ward/clinic reserved successfully.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(comp,
                    "The reservation could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void finishAsset(java.awt.Component comp, String assetId) {
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            JOptionPane.showMessageDialog(comp,
                    "The selected ward or clinic could not be found.",
                    "Record Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(comp,
                "Mark this ward/clinic as finished and available again?", "Finish Usage",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        asset.setStatus("AVAILABLE");
        asset.setDescription("");
        if (AssetManager.updateAsset(asset)) {
            JOptionPane.showMessageDialog(comp, "Ward/clinic marked as finished.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(comp,
                    "The status update could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAssetFilterOptions() {
        if (!assetTable || updatingAssetFilter) {
            return;
        }

        String selectedAssetType = (String) assetSearchBox.getSelectedItem();
        Set<String> roomTypes = new LinkedHashSet<>();
        for (String record : records) {
            String[] parts = splitRecord(record);
            if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                roomTypes.add(parts[1].trim());
            }
        }

        DefaultComboBoxModel<String> filterModel = new DefaultComboBoxModel<>();
        filterModel.addElement("All Room Types");
        for (String roomType : roomTypes) {
            filterModel.addElement(roomType);
        }

        updatingAssetFilter = true;
        assetSearchBox.setModel(filterModel);
        if (selectedAssetType != null && roomTypes.contains(selectedAssetType)) {
            assetSearchBox.setSelectedItem(selectedAssetType);
        }
        updatingAssetFilter = false;
    }

    private void addTableRow(String line) {
        String[] parts = splitRecord(line);

        if (departmentTable && parts.length >= 4) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                findName(parts[3].trim()),
                parts[2].trim()
            });

        } else if ((patientAppointmentTable || appointmentTable) && parts.length >= 6) {
            String doctorId = parts[2].trim();
            if (!visibleToCurrentDoctor(doctorId)) {
                return;
            }
            //patient can only see their own appointment
            String patientId = parts[1].trim();
            User currentUser = Session.getCurrentUser();
            if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
                if (!patientId.equalsIgnoreCase(currentUser.getUserId())
                        && !patientId.equalsIgnoreCase(currentUser.getUsername())) {
                    return;
                }
            }
            String doctorName = findName(doctorId);
            String selectedDoctor = doctorSearchBox != null ? (String) doctorSearchBox.getSelectedItem() : null;

            if (selectedDoctor != null
                    && !selectedDoctor.equals("All Doctors")
                    && !selectedDoctor.equals("Doctor Name")
                    && !doctorName.equalsIgnoreCase(selectedDoctor)) {
                return;
            }

            String aptId = parts[0].trim();
            String ratingDisplay = "No Review";
            String commentDisplay = "N/A";

            List<String> reviewLines = FileManager.readLines("reviews.txt");
            for (int i = 1; i < reviewLines.size(); i++) {
                String rLine = reviewLines.get(i);
                if (rLine == null || rLine.trim().isEmpty()) {
                    continue;
                }

                String[] rParts = splitRecord(rLine);
                if (rParts.length >= 4 && rParts[1].trim().equalsIgnoreCase(aptId)) {
                    ratingDisplay = rParts[2].trim() + " / 5 ⭐";
                    commentDisplay = rParts[3].trim();
                    break;
                }
            }

            tableModel.addRow(new Object[]{
                aptId,
                findName(parts[1].trim()),
                doctorName,
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                ratingDisplay, // Added Rating column
                commentDisplay // Added Comment column
            });
        } else if (assetTable) {
            RecordsHelperAsset.addAssetRow(tableModel, line, assetSearchBox);

        } else if (insuranceTable) {
            RecordsHelperInsurance.addInsuranceRow(tableModel, line);

        } else if (consultationRateTable && parts.length >= 6) {
            if (parts[0].trim().equalsIgnoreCase("SPECIALTY")) {
                return;
            }

            tableModel.addRow(new Object[]{
                parts[0].trim(), // Specialty
                parts[1].trim(), // Base Rate
                parts[2].trim(), // Min Rate
                parts[3].trim(), // Max Rate
                parts[4].trim(), // Currency
                parts[5].trim() // Effective Date
            });

        } else if (consultationTable && parts.length >= 9) {
            String patientId = parts[1].trim();
            User currentUser = Session.getCurrentUser();

            if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
                if (!patientId.equalsIgnoreCase(currentUser.getUserId())
                        && !patientId.equalsIgnoreCase(currentUser.getUsername())) {
                    return;
                }
            }

            String consultationId = parts[3].trim();
            String appointmentDateTime = consultationId;

            List<String> bookingLines = FileManager.readLines("bookings.txt");
            for (String booking : bookingLines) {
                String[] bParts = splitRecord(booking);
                if (bParts.length >= 5 && bParts[0].trim().equals(consultationId)) {
                    appointmentDateTime = bParts[3].trim() + " " + bParts[4].trim();
                    break;
                }
            }
            tableModel.addRow(new Object[]{
                parts[0].trim(), // Vital Sign ID
                findName(parts[1].trim()), // Patient Name
                findName(parts[2].trim()), // Doctor Name
                appointmentDateTime, // Appointment Date & Time 
                parts[4].trim(), // BP
                parts[5].trim(), // Heart Rate
                parts[6].trim(), // Temperature
                parts[7].trim(), // Date
                parts[8].trim() // Notes
            });
        } else if (prescriptionTable && parts.length >= 8) {
            String patientId = parts[1].trim();

            User currentUser = Session.getCurrentUser();
            if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
                if (!patientId.equalsIgnoreCase(currentUser.getUserId())
                        && !patientId.equalsIgnoreCase(currentUser.getUsername())) {
                    return;
                }
            }

            if (!visibleToCurrentDoctor(parts[2].trim())) {
                return;
            }

            String appointmentId = parts[6].trim();
            String appointmentDateTime = appointmentId;

            List<String> bookingLines = FileManager.readLines("bookings.txt");
            for (String booking : bookingLines) {
                String[] bParts = splitRecord(booking);
                if (bParts.length >= 5 && bParts[0].trim().equals(appointmentId)) {
                    appointmentDateTime = bParts[3].trim() + " " + bParts[4].trim();
                    break;
                }
            }

            tableModel.addRow(new Object[]{
                parts[0].trim(), // Prescription ID
                findName(parts[1].trim()), // Patient Full Name
                findName(parts[2].trim()), // Doctor Full Name
                parts[3].trim(), // Medication
                parts[4].trim(), // Dosage
                parts[5].trim(), // Duration
                appointmentDateTime, // Appointment Date & Time 
                parts[7].trim() // Status
            });

        } else if (labRequestTable && parts.length >= 8) {
            if (!visibleToCurrentDoctor(parts[2].trim())) {
                return;
            }

            String patientId = parts[1].trim();
            User currentUser = Session.getCurrentUser();
            if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
                if (!patientId.equalsIgnoreCase(currentUser.getUserId())
                        && !patientId.equalsIgnoreCase(currentUser.getUsername())) {
                    return;
                }
            }

            String appointmentId = parts[4].trim();
            String appointmentDateTime = appointmentId;

            List<String> bookingLines = FileManager.readLines("bookings.txt");
            for (String booking : bookingLines) {
                String[] bParts = splitRecord(booking);
                if (bParts.length >= 5 && bParts[0].trim().equals(appointmentId)) {
                    appointmentDateTime = bParts[3].trim() + " " + bParts[4].trim();
                    break;
                }
            }

            tableModel.addRow(new Object[]{
                parts[0].trim(), // Request ID
                findName(parts[1].trim()), // Patient Full Name
                findName(parts[2].trim()), // Doctor Full Name
                parts[3].trim(), // Test Type
                appointmentDateTime, // Appointment Date & Time
                parts[5].trim(), // Date Requested
                parts[6].trim(), // Date Completed
                parts[7].trim() // Status
            });

        } else if (rosterTable && parts.length >= 7) {
            tableModel.addRow(new Object[]{
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                parts[6].trim()
            });

        } else if (billsTable && parts.length >= 5) {
            String aptId = parts[0].trim();

            List<String> appointments = FileManager.readLines("bookings.txt");
            List<String> rosterLines = FileManager.readLines("roster.txt");
            List<String> ratesLines = FileManager.readLines("consultation_rates.txt");
            List<String> userLines = FileManager.readLines("users.txt");

            String doctorId = "";
            String date = "";
            String patientId = "";

            for (String apt : appointments) {
                String[] aptParts = splitRecord(apt);
                if (aptParts.length >= 6 && aptParts[0].trim().equalsIgnoreCase(aptId)) {
                    patientId = aptParts[1].trim();
                    doctorId = aptParts[2].trim();
                    date = aptParts[3].trim();
                    break;
                }
            }

            // Check patient insurance
            String patientInsurance = "";
            for (String uLine : userLines) {
                String[] uParts = splitRecord(uLine);
                if (uParts.length > 0 && uParts[0].trim().equalsIgnoreCase(patientId)) {
                    patientInsurance = uParts[uParts.length - 1].trim();
                    break;
                }
            }

            // Find Department from roster.txt
            String doctorFullName = findName(doctorId);
            String department = "General";
            for (String rLine : rosterLines) {
                String[] rParts = splitRecord(rLine);
                if (rParts.length >= 4) {
                    String rosterDocName = rParts[1].trim();
                    if (rosterDocName.equalsIgnoreCase(doctorFullName) || rosterDocName.equalsIgnoreCase(doctorId)) {
                        department = rParts[3].trim();
                        break;
                    }
                }
            }

            // Find Base Rate from consultation_rates.txt
            double baseRate = 50.00;
            for (String rtLine : ratesLines) {
                String[] rtParts = splitRecord(rtLine);
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

            tableModel.addRow(new Object[]{
                aptId,
                date,
                doctorFullName,
                department,
                String.format("$%.2f", baseRate),
                String.format("$%.2f", discount),
                String.format("$%.2f", finalAmount),
                parts[parts.length - 1].trim() // PAID/UNPAID
            });
        } else {
            if (parts.length > 1) {
                Object[] rowData = new Object[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    rowData[i] = parts[i].trim();
                }
                tableModel.addRow(rowData);
            } else {
                tableModel.addRow(new Object[]{
                    tableModel.getRowCount() + 1,
                    line
                });
            }
        }
    }

    private static boolean visibleToCurrentDoctor(String doctorId) {
        User current = Session.getCurrentUser();
        if (current == null || current.getRole() != Role.DOCTOR) {
            return true;
        }
        return doctorId.equalsIgnoreCase(current.getUserId())
                || doctorId.equalsIgnoreCase(current.getFullName());
    }

    public static String[] splitRecord(String record) {
        return record.split("\\|", -1);
    }

    public static String findName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "";
        }
        for (User user : UserRepository.loadAll()) {
            if (userId.equals(user.getUserId())) {
                return user.getFullName();
            }
        }
        return userId;
    }

    public static String findAssetType(String assetId) {
        if (assetId == null || assetId.trim().isEmpty()) {
            return "N/A";
        }

        List<String> assetLines = FileManager.readLines("hospital_assets.txt");
        for (String line : assetLines) {
            String[] parts = splitRecord(line);
            if (parts.length >= 3 && parts[0].trim().equalsIgnoreCase(assetId.trim())) {
                return parts[2].trim();
            }
        }
        return assetId;
    }

    public static boolean validRateFields(String baseRate, String minRate, String maxRate) {
        try {
            double base = Double.parseDouble(baseRate.trim());
            double minimum = Double.parseDouble(minRate.trim());
            double maximum = Double.parseDouble(maxRate.trim());
            return minimum <= base && base <= maximum;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static boolean isValidRecord(String record) {
        return record != null && !record.isEmpty() && !record.contains("\n")
                && !record.contains("\r") && record.indexOf('|') > 0;
    }

    public static boolean hasIllegalChars(String... values) {
        for (String value : values) {
            if (value != null && (value.contains("|") || value.contains("\n") || value.contains("\r"))) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> STATUS_ORDER = Arrays.asList(
            "PENDING",
            "IN_PROGRESS",
            "APPROVED",
            "DENIED",
            "SCHEDULED",
            "CANCELLED",
            "UNPAID",
            "PAID",
            "COMPLETED"
    );

    public static void applyStatusSorter(JTable table, int statusColumnIndex) {
        if (table == null || table.getModel() == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

        Comparator<String> statusComparator = (s1, s2) -> {
            if (s1 == null && s2 == null) {
                return 0;
            }
            if (s1 == null) {
                return 1;
            }
            if (s2 == null) {
                return -1;
            }

            int index1 = STATUS_ORDER.indexOf(s1.trim().toUpperCase());
            int index2 = STATUS_ORDER.indexOf(s2.trim().toUpperCase());

            if (index1 == -1) {
                index1 = Integer.MAX_VALUE;
            }
            if (index2 == -1) {
                index2 = Integer.MAX_VALUE;
            }

            return Integer.compare(index1, index2);
        };

        sorter.setComparator(statusColumnIndex, statusComparator);
        table.setRowSorter(sorter);
    }

    public static void applyStatusColorCoding(JTable table, int statusColumnIndex) {
        if (table == null) {
            return;
        }

        javax.swing.table.TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    int modelRow = jTable.convertRowIndexToModel(row);
                    Object statusObj = jTable.getModel().getValueAt(modelRow, statusColumnIndex);

                    if (statusObj != null) {
                        String status = statusObj.toString().trim().toUpperCase();
                        switch (status) {
                            case "COMPLETED":
                                c.setBackground(java.awt.Color.LIGHT_GRAY);
                                c.setForeground(java.awt.Color.BLACK);
                                break;
                            case "SCHEDULED":
                            case "APPROVED":
                            case "PAID":
                                c.setBackground(new java.awt.Color(220, 248, 220)); // Light Green
                                c.setForeground(java.awt.Color.BLACK);
                                break;
                            case "CANCELLED":
                            case "DENIED":
                            case "UNPAID":
                                c.setBackground(new java.awt.Color(255, 225, 225)); // Light Red/Pink
                                c.setForeground(java.awt.Color.BLACK);
                                break;
                            case "PENDING":
                                c.setBackground(new java.awt.Color(255, 255, 210)); // Light Yellow
                                c.setForeground(java.awt.Color.BLACK);
                                break;
                            case "IN_PROGRESS":
                            default:
                                c.setBackground(java.awt.Color.WHITE);
                                c.setForeground(java.awt.Color.BLACK);
                                break;
                        }
                    } else {
                        c.setBackground(java.awt.Color.WHITE);
                        c.setForeground(java.awt.Color.BLACK);
                    }
                }
                return c;
            }
        });
    }

    public static String showDatePickerDialog(Component parent, String initialDate) {
        JDialog pickerDialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Select Date", Dialog.ModalityType.APPLICATION_MODAL);
        pickerDialog.setLayout(new BorderLayout(8, 8));

        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(initialDate);
        } catch (Exception e) {
            parsedDate = LocalDate.now();
        }

        JTextField yearField = new JTextField(String.valueOf(parsedDate.getYear()), 5);
        JComboBox<Integer> monthCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        monthCombo.setSelectedItem(parsedDate.getMonthValue());
        JComboBox<Integer> dayCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31});
        dayCombo.setSelectedItem(parsedDate.getDayOfMonth());

        panel.add(new JLabel("Year (YYYY):"));
        panel.add(yearField);
        panel.add(new JLabel("Month:"));
        panel.add(monthCombo);
        panel.add(new JLabel("Day:"));
        panel.add(dayCombo);

        final String[] result = {null};
        JButton okButton = new JButton("Select");
        okButton.addActionListener(e -> {
            try {
                int year = Integer.parseInt(yearField.getText().trim());
                int month = (int) monthCombo.getSelectedItem();
                int day = (int) dayCombo.getSelectedItem();
                LocalDate date = LocalDate.of(year, month, day);
                result[0] = date.toString();
                pickerDialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(pickerDialog, "Invalid date values selected.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        pickerDialog.add(panel, BorderLayout.CENTER);
        pickerDialog.add(okButton, BorderLayout.SOUTH);
        pickerDialog.pack();
        pickerDialog.setLocationRelativeTo(parent);
        pickerDialog.setVisible(true);

        return result[0];
    }
}
