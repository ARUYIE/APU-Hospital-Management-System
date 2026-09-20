package hms.gui.panels;

import hms.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Panel for managing hospital assets
 */
public class ManageAssetsPanel extends JPanel {
    
    private JTable assetsTable;
    private DefaultTableModel tableModel;
    private JComboBox<AssetType> typeFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JButton addButton, editButton, deleteButton, viewAllocationsButton;
    private JLabel statsLabel;
    
    public ManageAssetsPanel() {
        initializeUI();
        loadAssets();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // Top panel with filters
        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Filter by Type:"));
        typeFilterCombo = new JComboBox<>(AssetType.values());
        typeFilterCombo.setSelectedItem(null);
        typeFilterCombo.addActionListener(e -> filterAssets());
        filterPanel.add(typeFilterCombo);
        
        filterPanel.add(new JLabel("Filter by Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"ALL", "AVAILABLE", "OCCUPIED", "MAINTENANCE", "OUT_OF_SERVICE"});
        statusFilterCombo.addActionListener(e -> filterAssets());
        filterPanel.add(statusFilterCombo);
        
        add(filterPanel, BorderLayout.NORTH);
        
        // Center panel with table
        String[] columnNames = {"Asset ID", "Type", "Name", "Location", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        assetsTable = new JTable(tableModel);
        assetsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(assetsTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with buttons and stats
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add Asset");
        addButton.addActionListener(e -> addAsset());
        buttonPanel.add(addButton);
        
        editButton = new JButton("Edit Asset");
        editButton.addActionListener(e -> editAsset());
        buttonPanel.add(editButton);
        
        deleteButton = new JButton("Delete Asset");
        deleteButton.addActionListener(e -> deleteAsset());
        buttonPanel.add(deleteButton);
        
        viewAllocationsButton = new JButton("View Allocations");
        viewAllocationsButton.addActionListener(e -> viewAllocations());
        buttonPanel.add(viewAllocationsButton);
        
        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        
        statsLabel = new JLabel("Total Assets: 0");
        bottomPanel.add(statsLabel, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void loadAssets() {
        tableModel.setRowCount(0);
        List<Asset> assets = AssetManager.getAllAssets();
        
        for (Asset asset : assets) {
            tableModel.addRow(new Object[]{
                    asset.getAssetId(),
                    asset.getAssetType().getDisplayName(),
                    asset.getName(),
                    asset.getLocation(),
                    asset.getStatus()
            });
        }
        
        updateStats();
    }
    
    private void filterAssets() {
        tableModel.setRowCount(0);
        List<Asset> assets = AssetManager.getAllAssets();
        
        AssetType selectedType = (AssetType) typeFilterCombo.getSelectedItem();
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();
        
        for (Asset asset : assets) {
            boolean typeMatch = selectedType == null || asset.getAssetType() == selectedType;
            boolean statusMatch = "ALL".equals(selectedStatus) || asset.getStatus().equals(selectedStatus);

            if (typeMatch && statusMatch) {
                tableModel.addRow(new Object[]{
                        asset.getAssetId(),
                        asset.getAssetType().getDisplayName(),
                        asset.getName(),
                        asset.getLocation(),
                        asset.getStatus()
                });
            }
        }
    }
    
    private void addAsset() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Add New Asset");
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Asset Type:"));
        JComboBox<AssetType> typeCombo = new JComboBox<>(AssetType.values());
        panel.add(typeCombo);

        panel.add(new JLabel("Name:"));
        JTextField nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Location:"));
        JTextField locationField = new JTextField();
        panel.add(locationField);

        panel.add(new JLabel("Description:"));
        JTextArea descriptionArea = new JTextArea(3, 20);
        panel.add(new JScrollPane(descriptionArea));
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            try {
                AssetManager.createAsset(
                        (AssetType) typeCombo.getSelectedItem(),
                        nameField.getText(),
                        locationField.getText(),
                        0,
                        "",
                        descriptionArea.getText()
                );
                JOptionPane.showMessageDialog(dialog, "Asset created successfully!");
                dialog.dispose();
                loadAssets();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(saveButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        panel.add(cancelButton);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void editAsset() {
        int selectedRow = assetsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an asset to edit");
            return;
        }
        
        String assetId = (String) tableModel.getValueAt(selectedRow, 0);
        Asset asset = AssetManager.getAsset(assetId);
        
        if (asset != null) {
            JDialog dialog = new JDialog();
            dialog.setTitle("Edit Asset");
            dialog.setSize(500, 400);
            dialog.setLocationRelativeTo(this);
            dialog.setModal(true);
            
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(6, 2, 10, 10));

            panel.add(new JLabel("Asset Type:"));
            JComboBox<AssetType> typeCombo = new JComboBox<>(AssetType.values());
            typeCombo.setSelectedItem(asset.getAssetType());
            panel.add(typeCombo);

            panel.add(new JLabel("Name:"));
            JTextField nameField = new JTextField(asset.getName());
            panel.add(nameField);

            panel.add(new JLabel("Location:"));
            JTextField locationField = new JTextField(asset.getLocation());
            panel.add(locationField);

            panel.add(new JLabel("Status:"));
            JComboBox<String> statusCombo = new JComboBox<>(new String[]{"AVAILABLE", "OCCUPIED", "MAINTENANCE", "OUT_OF_SERVICE"});
            statusCombo.setSelectedItem(asset.getStatus());
            panel.add(statusCombo);

            panel.add(new JLabel("Description:"));
            JTextArea descriptionArea = new JTextArea(asset.getDescription(), 3, 20);
            panel.add(new JScrollPane(descriptionArea));
            
            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(e -> {
                try {
                    asset.setName(nameField.getText());
                    asset.setLocation(locationField.getText());
                    asset.setStatus((String) statusCombo.getSelectedItem());
                    asset.setDescription(descriptionArea.getText());
                    
                    AssetManager.updateAsset(asset);
                    JOptionPane.showMessageDialog(dialog, "Asset updated successfully!");
                    dialog.dispose();
                    loadAssets();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(saveButton);
            
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dialog.dispose());
            panel.add(cancelButton);
            
            dialog.add(panel);
            dialog.setVisible(true);
        }
    }

    private JComboBox<String> createDepartmentComboBox(String selectedDepartment) {
        JComboBox<String> departmentCombo = new JComboBox<>();
        for (String departmentName : DepartmentManager.getDepartmentNames()) {
            departmentCombo.addItem(departmentName);
        }

        if (selectedDepartment != null) {
            departmentCombo.setSelectedItem(selectedDepartment);
        }

        return departmentCombo;
    }
    
    private void deleteAsset() {
        int selectedRow = assetsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an asset to delete");
            return;
        }
        
        String assetId = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this asset?");
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (AssetManager.deleteAsset(assetId)) {
                JOptionPane.showMessageDialog(this, "Asset deleted successfully!");
                loadAssets();
            }
        }
    }
    
    private void viewAllocations() {
        int selectedRow = assetsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an asset to view allocations");
            return;
        }
        
        String assetId = (String) tableModel.getValueAt(selectedRow, 0);
        Asset asset = AssetManager.getAsset(assetId);
        
        JDialog dialog = new JDialog();
        dialog.setTitle("Allocations for " + asset.getName());
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        
        String[] columnNames = {"Allocation ID", "User ID", "Purpose", "Start", "End", "Status"};
        DefaultTableModel allocationModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable allocationsTable = new JTable(allocationModel);
        List<AssetAllocation> allocations = AssetAllocationManager.getAllocationsForAsset(assetId);
        
        for (AssetAllocation alloc : allocations) {
            allocationModel.addRow(new Object[]{
                    alloc.getAllocationId(),
                    alloc.getUserId(),
                    alloc.getPurpose(),
                    alloc.getAllocationStart(),
                    alloc.getAllocationEnd(),
                    alloc.getStatus()
            });
        }
        
        JScrollPane scrollPane = new JScrollPane(allocationsTable);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }
    
    private void updateStats() {
        int total = AssetManager.getAllAssets().size();
        int available = AssetManager.getAvailableCount(null);
        statsLabel.setText(String.format("Total Assets: %d | Available: %d", total, available));
    }
}
