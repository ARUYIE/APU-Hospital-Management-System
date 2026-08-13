package hms.gui.panels;

import hms.role.Role;
import hms.role.User;
import hms.util.Tables;
import hms.util.UserRepository;

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

    // Parallel to the table's rows: filteredUsers.get(modelRow) is the User
    // object behind that row, so "Edit Selected" can find the real object
    // (not just its displayed text) once a row is picked.
    private List<User> filteredUsers = new ArrayList<>();

    public ManageUserPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        userTable.setAutoCreateRowSorter(true); // click any column header (incl. Role) to sort
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

        int modelRow = userTable.convertRowIndexToModel(viewRow); // sorting can reorder view rows
        User selectedUser = filteredUsers.get(modelRow);

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Edit User", Dialog.ModalityType.APPLICATION_MODAL);

       
        Tables tables = new Tables(selectedUser, dialog);
        tables.setOnSaved(this::refreshTable);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        root.add(tables.buildTitle("Edit User"), BorderLayout.NORTH);
        root.add(tables.buildForm(), BorderLayout.CENTER);
        root.add(tables.buildSaveButton("Save Changes", "Cancel"), BorderLayout.SOUTH);
        dialog.setContentPane(root);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private void refreshTable() {
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
}