package hms.gui;

import hms.role.User;
import hms.util.Session;

import javax.swing.*;
import java.awt.*;

/**
 * Main window shown after a successful login. The menu on the left is built
 * from user.getMenuOptions() - since that method is overridden differently by
 * each User subclass (polymorphism), this single class automatically shows
 * the right features for whichever of the 4 roles logged in, with no
 * if/else chain needed here.
 * <p>
 * Teammates: each menu item currently opens a placeholder panel from
 * hms.gui.panels. Replace the placeholder construction in showPanelFor()
 * with your real panel class as you build each feature out.
 */
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

        showWelcomePanel(user);
    }

    private JComponent buildHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel welcome = new JLabel("Welcome, " + user.getFullName()
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

        JScrollPane scrollPane = new JScrollPane(menuList);
        scrollPane.setPreferredSize(new Dimension(260, 0));
        return scrollPane;
    }

    private void showWelcomePanel(User user) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel("Select an item from the menu to get started.");
        label.setFont(label.getFont().deriveFont(14f));
        panel.add(label);
        setContent(panel);
    }

    /**
     * Routes a selected menu label to its feature panel.
     * Currently every feature is a labeled placeholder - swap each case's
     * panel for the real implementation as your team builds it out.
     */
    private void showPanelFor(String menuLabel) {
        // TODO teammates: add a case per feature as you build each panel,
        // e.g. case "Manage Wards/Clinics": setContent(new hms.gui.panels.WardPanel()); break;
        // See README.md for the recommended pattern: model -> repository -> panel.
        setContent(new hms.gui.panels.PlaceholderPanel(menuLabel));
    }

    private void setContent(JComponent component) {
        contentArea.removeAll();
        contentArea.add(component, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
}
