package hms.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Asset-specific record forms and persistence operations for record panels. */
public final class RecordsHelperAsset {

    private RecordsHelperAsset() {
    }

    public static String editAssetRecord(Component comp, String record) {
        String[] parts = ManageRecordsHelper.splitRecord(record);
        if (parts.length < 6) {
            return null;
        }

        JTextField assetIdField = new JTextField(parts[0].trim());
        setUneditable(assetIdField);

        JComboBox<String> roomTypeCombo = createAssetTypeCombo();
        roomTypeCombo.setSelectedItem(normalizeAssetType(parts[1].trim()));

        JTextField roomNameField = new JTextField(parts[2].trim());
        JComboBox<String> locationCombo = new JComboBox<>(
                new String[]{"1st Floor", "2nd Floor", "3rd Floor"}
        );

        locationCombo.setSelectedItem(parts[3].trim());

        JComboBox<String> statusField =
                new JComboBox<>(new String[]{"AVAILABLE", "OCCUPIED"});
        statusField.setSelectedItem(parts[4].trim());

        JTextField reservedByField = new JTextField(parts[5].trim());
        setUneditable(reservedByField);

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));

        form.add(new JLabel("ASSET_ID:"));
        form.add(assetIdField);

        form.add(new JLabel("ROOM_TYPE:"));
        form.add(roomTypeCombo);

        form.add(new JLabel("ROOM_NAME:"));
        form.add(roomNameField);

        form.add(new JLabel("LOCATION:"));
        form.add(locationCombo);

        form.add(new JLabel("STATUS:"));
        form.add(statusField);

        form.add(new JLabel("RESERVED BY:"));
        form.add(reservedByField);

        int choice = JOptionPane.showConfirmDialog(
                comp,
                form,
                "Edit Asset",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        return String.join("|",
                assetIdField.getText().trim(),
                ((String) roomTypeCombo.getSelectedItem()).trim(),
                roomNameField.getText().trim(),
                locationCombo.getSelectedItem().toString().trim(),
                statusField.getSelectedItem().toString().trim(),
                reservedByField.getText().trim()
        );
    }


    public static void addAssetRecord(
            Component comp,
            String fileName,
            Runnable refreshAction) {

        JComboBox<String> roomTypeCombo = createAssetTypeCombo();
        JTextField roomNameField = new JTextField();
        JTextField locationField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"AVAILABLE", "UNAVAILABLE"});
        JTextField reservedByField = new JTextField();
        JComboBox<String> locationCombo = new JComboBox<>(
            new String[]{"1st Floor", "2nd Floor", "3rd Floor"}
        );

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));

        form.add(new JLabel("ROOM_TYPE:"));
        form.add(roomTypeCombo);

        form.add(new JLabel("ROOM_NAME:"));
        form.add(roomNameField);

        form.add(new JLabel("LOCATION:"));
        form.add(locationCombo);

        int choice = JOptionPane.showConfirmDialog(
                comp,
                form,
                "Add Ward / Clinic",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String assetId = IDGenerator.next("ASSET", fileName);
        String roomType =
                ((String) roomTypeCombo.getSelectedItem()).trim();
        String roomName = roomNameField.getText().trim();
        String location = locationCombo.getSelectedItem().toString().trim();

        if (roomType.isEmpty()
                || roomName.isEmpty()
                || location.isEmpty()) {

            showWarning(
                    comp,
                    "Room type, room name, and location are required."
            );
            return;
        }

        if (ManageRecordsHelper.hasIllegalChars(
                assetId,
                roomType,
                roomName,
                location)) {

            showWarning(
                    comp,
                    "Fields cannot contain the '|' character or line breaks."
            );
            return;
        }

        // New assets are always available and unreserved.
        String status = "AVAILABLE";
        String reservedBy = "";

        FileManager.appendLine(
                fileName,
                String.join("|",
                        assetId,
                        roomType,
                        roomName,
                        location,
                        status,
                        reservedBy
                )
        );

        refreshAction.run();
    }


    public static void addAssetRow(
            DefaultTableModel tableModel,
            String line,
            JComboBox<String> assetSearchBox) {

        String[] parts = ManageRecordsHelper.splitRecord(line);

        if (parts.length < 6) {
            return;
        }

        String selectedAssetType =
                (String) assetSearchBox.getSelectedItem();

        String roomType = parts[1].trim();

        if (selectedAssetType != null
                && !"All Room Types".equals(selectedAssetType)
                && !roomType.equals(selectedAssetType)) {

            return;
        }

        tableModel.addRow(new Object[]{
                parts[0].trim(),
                roomType,
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim()
        });
    }


    public static JComboBox<String> createAssetTypeCombo() {
        JComboBox<String> roomTypeCombo = new JComboBox<>();

        for (AssetType assetType : AssetType.values()) {
            roomTypeCombo.addItem(assetType.name());
        }

        return roomTypeCombo;
    }

    private static String normalizeAssetType(String value) {
        return AssetType.fromString(value).name();
    }


    private static void showWarning(Component comp, String message) {
        JOptionPane.showMessageDialog(comp, message,
                "Invalid Asset", JOptionPane.WARNING_MESSAGE);
    }
    private static void setUneditable(JTextField field) {
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(Color.LIGHT_GRAY);
    }
}