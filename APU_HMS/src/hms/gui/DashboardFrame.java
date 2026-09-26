package hms.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import hms.role.User;
import hms.util.Session;
import hms.util.UIUtil;

public class DashboardFrame extends JFrame {

    private final JPanel contentArea = new JPanel(new BorderLayout());

    public DashboardFrame() {
        super("APU Medical Centre HMS - Dashboard");
        User user = Session.getCurrentUser();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIUtil.DASHBOARD_BLUE);

        JPanel root = UIUtil.createPatternPanel();
        root.setBackground(UIUtil.DASHBOARD_BLUE);
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
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setOpaque(false);
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
        menuPanel.setBackground(UIUtil.DASHBOARD_BLUE);

        JList<String> menuList = new JList<>(options);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setFont(menuList.getFont().deriveFont(Font.BOLD, 16f));
        menuList.setBackground(Color.WHITE);
        menuList.setFixedCellHeight(48);
        menuList.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // tracks the hovered row so the renderer can enlarge its text
        int[] hoveredIndex = {-1};
        float baseFontSize = 16f;
        float hoverFontSize = 20f;

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
            label.setFont(label.getFont().deriveFont(Font.BOLD,
                index == hoveredIndex[0] ? hoverFontSize : baseFontSize));
            return label;
            }
        });

        menuList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = menuList.locationToIndex(e.getPoint());
                if (index != hoveredIndex[0]) {
                    hoveredIndex[0] = index;
                    menuList.repaint();
                }
            }
        });
        menuList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex[0] = -1;
                menuList.repaint();
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
        scrollPane.getViewport().setBackground(UIUtil.DASHBOARD_BLUE);
        scrollPane.setBackground(UIUtil.DASHBOARD_BLUE);
        menuPanel.add(scrollPane, BorderLayout.CENTER);
        return menuPanel;
    }

    private void showPanelFor(String menuLabel) {
        switch (menuLabel) {
            // Tan Rui En - Admin
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
                setContent(new hms.gui.panels.WardsClinicsMenuPanel(this));
                break;
            case "Consultation Rates":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Configure Consultation Rates", "consultation_rates.txt"));
                break;
            case "Insurances":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                        "Manage Insurance Networks", "insurance_networks.txt"));
                break;
                
            // Wong Willard - Medical Manager
            case "Clinical Departments":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Manage Departments and Specialties", "department.txt"));
                break;                
            case "Doctor Operational Roster":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Manager Doctor Operational Roster", "roster.txt"));
                break;
            case "View Analytical Reports":
                setContent(new hms.util.ReportCharts(
                    "View Hospital Metrics and Revenue Summaries"));
                break;
                
            //Low Kai Lun - Doctor
            case "Patient Vitals & Consultation Notes":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Log Vitals & Consultation Notes", "vital_signs.txt"));
                break;
            case "Prescriptions":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Issue Prescriptions", "prescriptions.txt"));
                break;
            case "Lab & Imaging Requests":
                setContent(new hms.gui.panels.ManageRecordsPanel(
                    "Request Lab Tests / Imaging", "lab_requests.txt"));
                break;
                
            default:
                setContent(new hms.gui.panels.PlaceholderPanel(menuLabel));
        }
    }

    public void setContent(JComponent component) {
        contentArea.removeAll();
        contentArea.add(component, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
}
