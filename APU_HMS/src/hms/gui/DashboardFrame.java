package hms.gui;

import hms.role.User;
import hms.util.BackgroundPatternUtil;
import hms.util.Session;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private final JPanel contentArea = new JPanel(new BorderLayout());

    public DashboardFrame() {
        super("APU Medical Centre HMS - Dashboard");
        User user = Session.getCurrentUser();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BackgroundPatternUtil.DASHBOARD_BLUE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BackgroundPatternUtil.DASHBOARD_BLUE);
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(buildHeader(user), BorderLayout.NORTH);
        root.add(buildMenu(user), BorderLayout.WEST);
        root.add(contentArea, BorderLayout.CENTER);
        setContentPane(root);

        contentArea.setOpaque(false);
        contentArea.setBackground(new Color(0, 0, 0, 0));
    }

    private JComponent buildHeader(User user) {
        JPanel header = BackgroundPatternUtil.createPatternPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(BackgroundPatternUtil.DASHBOARD_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel welcome = new JLabel("Welcome Back, " + user.getFullName()
                + "  (" + user.getRole().getDisplayName() + ")");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 14f));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(Color.WHITE);
        logoutButton.setOpaque(true);
        logoutButton.setBorderPainted(true);
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
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBackground(BackgroundPatternUtil.DASHBOARD_BLUE);

        JList<String> menuList = new JList<>(options);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setFont(menuList.getFont().deriveFont(Font.BOLD, 16f));
        menuList.setBackground(Color.WHITE);
        menuList.setFixedCellHeight(48);
        menuList.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        menuList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
            label.setForeground(Color.BLACK);
            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 195, 215)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            if (isSelected) {
                label.setBackground(new Color(232, 240, 252));
                label.setForeground(new Color(27, 60, 108));
            }
            return label;
            }
        });

        menuList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && menuList.getSelectedValue() != null) {
                showPanelFor(menuList.getSelectedValue());
            }
        });
        
        // shows profile panel on login
        menuList.setSelectedValue("Profile", true);
        
        JScrollPane scrollPane = new JScrollPane(menuList);
        scrollPane.setPreferredSize(new Dimension(320, 0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BackgroundPatternUtil.DASHBOARD_BLUE);
        scrollPane.setBackground(BackgroundPatternUtil.DASHBOARD_BLUE);
        menuPanel.add(scrollPane, BorderLayout.CENTER);
        return menuPanel;
    }

    private void showPanelFor(String menuLabel) {
        switch (menuLabel) {
            case "Profile":
                setContent(new hms.gui.panels.Profile());
                break;
            case "Appointments":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                        "Manage Appointments", "bookings.txt"));
                break;
            case "Users":
                setContent(new hms.gui.panels.ManageUserPanel(
                    "View, Edit And Register Users"));
                break;                
            case "Wards/Clinics":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                        "Manage Wards and Clinics", "hospital_assets.txt"));
                break;
            case "Departments/Specialties":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                        "Manage Departments and Specialties", "department.txt"));
                break;
            case "Consultation Rates":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Configure Consultation Rates", "consultation_rates.txt"));
                break;
            case "Insurances":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                        "Manage Insurance Networks", "insurance_networks.txt"));
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
