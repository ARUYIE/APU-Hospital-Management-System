package hms.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import hms.gui.DashboardFrame;
import hms.util.UIUtil;

public class WardsClinicsMenuPanel extends JPanel {

    public WardsClinicsMenuPanel(DashboardFrame dashboard) {
        setLayout(new BorderLayout());
        setBackground(UIUtil.DASHBOARD_BLUE);

        JLabel titleLabel = new JLabel("Wards and Clinics Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.black);
        titleLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 0, 30, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(UIUtil.DASHBOARD_BLUE);

        JButton btnRequest = new JButton("Wards/Clinics Request");
        UIUtil.styleButton(btnRequest, Color.BLUE);
        btnRequest.setPreferredSize(new Dimension(250, 50));
        btnRequest.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRequest.addActionListener(e -> {
            ManageRecordsPanel panel = new ManageRecordsPanel("Lab & Imaging Requests", "lab_requests.txt");
            panel.addBackButton(() -> dashboard.setContent(new WardsClinicsMenuPanel(dashboard)));
            dashboard.setContent(panel);
        });

        JButton btnAll = new JButton("All Wards/Clinics");
        UIUtil.styleButton(btnAll, Color.BLUE);
        btnAll.setPreferredSize(new Dimension(250, 50));
        btnAll.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnAll.addActionListener(e -> {
            ManageRecordsPanel panel = new ManageRecordsPanel("Manage Wards and Clinics", "hospital_assets.txt");
            panel.addBackButton(() -> dashboard.setContent(new WardsClinicsMenuPanel(dashboard)));
            dashboard.setContent(panel);
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        buttonPanel.add(btnRequest, gbc);

        gbc.gridy = 1;
        buttonPanel.add(btnAll, gbc);

        add(buttonPanel, BorderLayout.CENTER);
    }
}
