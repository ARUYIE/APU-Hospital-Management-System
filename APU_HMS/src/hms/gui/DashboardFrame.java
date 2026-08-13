package hms.gui;

import hms.role.User;
import hms.util.Session;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private final JPanel contentArea = new JPanel(new BorderLayout());

    public DashboardFrame() {
        super("APU Medical Centre HMS - Dashboard");
        User user = Session.getCurrentUser();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildHeader(user), BorderLayout.NORTH);
        add(buildMenu(user), BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);
    }

    private JComponent buildHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel welcome = new JLabel("Welcome Back, " + user.getFullName()
                + "  (" + user.getRole().getDisplayName() + ")");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 14f));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            Session.logout();
            new LoginFrame().setVisible(true);
            dispose();
        });

        header.add(welcome, BorderLayout.WEST);
        header.add(logoutButton, BorderLayout.EAST);
        return header;
    }

    private JComponent buildMenu(User user) {
        // Built directly from the logged-in user's overridden getMenuOptions() - polymorphism.
        String[] options = user.getMenuOptions();
        JList<String> menuList = new JList<>(options);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setFixedCellHeight(36);
        menuList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        menuList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && menuList.getSelectedValue() != null) {
                showPanelFor(menuList.getSelectedValue());
            }
        });
        
        // shows profile panel on login
        menuList.setSelectedValue("Profile", true);
        
        JScrollPane scrollPane = new JScrollPane(menuList);
        scrollPane.setPreferredSize(new Dimension(260, 0));
        return scrollPane;
    }

    private void showPanelFor(String menuLabel) {
        // TODO: add a case per feature like below as you build each panel,
        switch (menuLabel) {
            case"Profile":
                setContent(new hms.gui.panels.Profile());
                break;
            case "Manage User":
                setContent(new hms.gui.panels.ManageUserPanel());
                break;
                
                
            default:
                setContent(new hms.gui.panels.PlaceholderPanel(menuLabel));
        }
    }

    private void setContent(JComponent component) {
        contentArea.removeAll();
        contentArea.add(component, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
}
