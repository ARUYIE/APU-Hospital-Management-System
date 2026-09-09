package hms.gui.panels;

import hms.role.Role;
import hms.role.User;
import hms.util.FileManager;
import hms.util.UserRepository;
import hms.util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;


public class ManageUserPanel extends JPanel {

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{"ID", "Name", "Username", "Role", "Email", "Phone"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // read-only, editing happens through the Edit Selected dialog
                }
            };

    private final JTable userTable = new JTable(tableModel);
    private final JComboBox<String> roleFilterBox = new JComboBox<>();
    private List<User> filteredUsers = new ArrayList<>();
    private String headerLine;
    private boolean headerPresent;
    private boolean initialized;

    public ManageUserPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        userTable.setAutoCreateRowSorter(true);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(buildTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        populateRoleFilterOptions();
        refreshTable();
    }

    private JComponent buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());

        JLabel title = new JLabel("All Registered Users");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.add(new JLabel("Filter by Role:"));
        controls.add(roleFilterBox);

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> openEditDialog());
        controls.add(editButton);

        JButton registerButton = new JButton("+ Register New User");
        registerButton.addActionListener(e -> openRegisterFrame());
        controls.add(registerButton);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(controls, BorderLayout.EAST);
        return topBar;
    }

    private void populateRoleFilterOptions() {
        roleFilterBox.addItem("All Roles");
        for (Role role : Role.values()) {
            roleFilterBox.addItem(role.getDisplayName());
        }
        roleFilterBox.addActionListener(e -> refreshTable());
    }

    private void openRegisterFrame() {
        hms.gui.RegisterFrame registerFrame = new hms.gui.RegisterFrame();

        registerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                refreshTable();
            }
        });

        registerFrame.setVisible(true);
    }

    private void openEditDialog() {
        int viewRow = userTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user in the table first.",
                    "No User Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = userTable.convertRowIndexToModel(viewRow);
        User selectedUser = filteredUsers.get(modelRow);

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Edit User", Dialog.ModalityType.APPLICATION_MODAL);

        JTextField fullNameField = new JTextField(selectedUser.getFullName(), 20);
        JTextField emailField = new JTextField(selectedUser.getEmail(), 20);
        JTextField phoneField = new JTextField(selectedUser.getPhone(), 20);
        JPasswordField passwordField = new JPasswordField(20);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        addReadOnlyRow(form, gbc, row++, "User ID:", selectedUser.getUserId());
        addReadOnlyRow(form, gbc, row++, "Username:", selectedUser.getUsername());
        addReadOnlyRow(form, gbc, row++, "Role:", selectedUser.getRole().getDisplayName());
        addEditableRow(form, gbc, row++, "Full Name:", fullNameField);
        addEditableRow(form, gbc, row++, "Email:", emailField);
        addEditableRow(form, gbc, row++, "Phone:", phoneField);
        addEditableRow(form, gbc, row++, "New Password (leave blank to keep current):", passwordField);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        root.add(buildTitle("Edit User"), BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);

        JButton saveButton = new JButton("Save Changes");
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String newPassword = new String(passwordField.getPassword());

            StringBuilder errors = new StringBuilder();
            if (!Validator.isNonEmpty(fullName)) errors.append("- Full name is required.\n");
            if (!Validator.isValidEmail(email)) errors.append("- A valid email is required.\n");
            if (!Validator.isValidPhone(phone)) errors.append("- A valid phone number is required.\n");
            if (!newPassword.isEmpty() && !Validator.isValidPassword(newPassword)) {
                errors.append("- New password must be at least 6 characters.\n");
            }

            if (errors.length() > 0) {
                JOptionPane.showMessageDialog(dialog, errors.toString(), "Please fix the following", JOptionPane.WARNING_MESSAGE);
                return;
            }

            selectedUser.setFullName(fullName);
            selectedUser.setEmail(email);
            selectedUser.setPhone(phone);
            if (!newPassword.isEmpty()) {
                selectedUser.setPassword(newPassword);
            }

            UserRepository.update(selectedUser);
            passwordField.setText("");
            JOptionPane.showMessageDialog(dialog, "User updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            dialog.dispose();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(saveButton);
        footer.add(cancelButton);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private JComponent buildTitle(String header) {
        JLabel title = new JLabel(header);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        return title;
    }


    private static void addReadOnlyRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN));
        gbc.gridx = 1;
        panel.add(valueLabel, gbc);
    }

    private static void addEditableRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void refreshTable() {
        List<String> rawLines = FileManager.readLines("users.txt");
        if (!initialized) {
            headerPresent = !rawLines.isEmpty() && isHeaderLine(rawLines.get(0));
            initialized = true;
        }
        if (headerPresent && !rawLines.isEmpty()) {
            headerLine = rawLines.remove(0);
        }

        String selectedFilter = (String) roleFilterBox.getSelectedItem();

        filteredUsers = new ArrayList<>();
        for (User u : UserRepository.loadAll()) {
            boolean matchesFilter = selectedFilter == null
                    || selectedFilter.equals("All Roles")
                    || selectedFilter.equals(u.getRole().getDisplayName());
            if (matchesFilter) {
                filteredUsers.add(u);
            }
        }

        tableModel.setRowCount(0);
        for (User u : filteredUsers) {
            tableModel.addRow(new Object[]{
                    u.getUserId(), u.getFullName(), u.getUsername(),
                    u.getRole().getDisplayName(), u.getEmail(), u.getPhone()
            });
        }
    }

    private boolean isHeaderLine(String line) {
        if (line == null) {
            return false;
        }
        String upper = line.trim().toUpperCase();
        return upper.startsWith("ID") && upper.contains("ROLE") && upper.contains("EMAIL");
    }
}