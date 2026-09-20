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
import java.util.Map;


public class ManageUserPanel extends JPanel {

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{"ID", "Name", "Username", "Role", "Email", "Phone", "Medical Manager"}, 0) {
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

    public ManageUserPanel(String title) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        userTable.setAutoCreateRowSorter(true);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(buildTopBar(title), BorderLayout.NORTH);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        populateRoleFilterOptions();
        refreshTable();
    }

    private JComponent buildTopBar(String title) {
        JPanel topBar = new JPanel(new BorderLayout());

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.add(new JLabel("Filter by Role:"));
        controls.add(roleFilterBox);

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> openEditDialog());
        controls.add(editButton);

        JButton assignButton = new JButton("Assign Medical Manager");
        assignButton.addActionListener(e -> openAssignmentDialog());
        controls.add(assignButton);

        JButton registerButton = new JButton("+ Register New User");
        registerButton.addActionListener(e -> openRegisterFrame());
        controls.add(registerButton);

        topBar.add(heading, BorderLayout.WEST);
        topBar.add(controls, BorderLayout.EAST);
        return topBar;
    }

    private void openAssignmentDialog() {
        List<User> doctors = usersWithRole(Role.DOCTOR);
        List<User> managers = usersWithRole(Role.MEDICAL_MANAGER);
        if (doctors.isEmpty() || managers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "At least one doctor and one medical manager are required.",
                    "Cannot Assign Manager", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<User> doctorCombo = new JComboBox<>(doctors.toArray(new User[0]));
        JComboBox<User> managerCombo = new JComboBox<>(managers.toArray(new User[0]));
        Map<String, String> assignments = hms.util.DoctorManagerAssignmentRepository.loadAll();

        doctorCombo.addActionListener(e -> {
            User selectedDoctor = (User) doctorCombo.getSelectedItem();
            String managerId = selectedDoctor == null ? null : assignments.get(selectedDoctor.getUserId());
            selectUserById(managerCombo, managerId);
        });
        selectUserById(managerCombo, assignments.get(doctors.get(0).getUserId()));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Doctor:"));
        form.add(doctorCombo);
        form.add(new JLabel("Medical Manager:"));
        form.add(managerCombo);

        int choice = JOptionPane.showConfirmDialog(this, form,
                "Assign Medical Manager", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        User doctor = (User) doctorCombo.getSelectedItem();
        User manager = (User) managerCombo.getSelectedItem();
        if (doctor == null || manager == null) {
            return;
        }

        hms.util.DoctorManagerAssignmentRepository.assign(
                doctor.getUserId(), manager.getUserId());
        refreshTable();
        JOptionPane.showMessageDialog(this,
                "Medical manager assigned successfully.",
                "Assignment Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private List<User> usersWithRole(Role role) {
        List<User> matchingUsers = new ArrayList<>();
        for (User user : UserRepository.loadAll()) {
            if (user.getRole() == role) {
                matchingUsers.add(user);
            }
        }
        return matchingUsers;
    }

    private void selectUserById(JComboBox<User> comboBox, String userId) {
        if (userId == null) {
            comboBox.setSelectedIndex(0);
            return;
        }
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            if (userId.equals(comboBox.getItemAt(index).getUserId())) {
                comboBox.setSelectedIndex(index);
                return;
            }
        }
        comboBox.setSelectedIndex(0);
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
        root.add(buildheading("Edit User"), BorderLayout.NORTH);
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

    private JComponent buildheading(String header) {
        JLabel heading = new JLabel(header);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        return heading;
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
        Map<String, String> assignments = hms.util.DoctorManagerAssignmentRepository.loadAll();
        Map<String, User> usersById = new java.util.HashMap<>();
        for (User user : UserRepository.loadAll()) {
            usersById.put(user.getUserId(), user);
        }
        for (User u : filteredUsers) {
            User manager = usersById.get(assignments.get(u.getUserId()));
            String managerName = u.getRole() == Role.DOCTOR && manager != null
                ? manager.getFullName()
                : "-";
            tableModel.addRow(new Object[]{
                    u.getUserId(), u.getFullName(), u.getUsername(),
                    u.getRole().getDisplayName(), u.getEmail(), u.getPhone(),
                managerName
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