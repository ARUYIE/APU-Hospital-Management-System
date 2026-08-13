package hms.gui.panels;

import hms.role.User;
import hms.util.UserRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Shows every registered user in a table, with a "Register New User" button
 * at the top. That button reuses the existing hms.gui.RegisterFrame popup
 * (the same one LoginFrame opens) instead of duplicating the whole form
 * here - one registration form for the entire app, opened from two places.
 * When RegisterFrame closes, this table refreshes to pick up any new user.
 */
public class ManageUserPanel extends JPanel {

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{"ID", "Name", "Username", "Role", "Email", "Phone"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // read-only, editing isn't part of this feature
                }
            };

    public ManageUserPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);

        refreshTable();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("All Registered Users");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JButton registerButton = new JButton("+ Register New User");
        registerButton.addActionListener(e -> openRegisterFrame());

        header.add(title, BorderLayout.WEST);
        header.add(registerButton, BorderLayout.EAST);
        return header;
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

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (User u : UserRepository.loadAll()) {
            tableModel.addRow(new Object[]{
                    u.getUserId(), u.getFullName(), u.getUsername(),
                    u.getRole().getDisplayName(), u.getEmail(), u.getPhone()
            });
        }
    }
}