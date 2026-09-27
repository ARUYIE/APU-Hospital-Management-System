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

public class UsersMenuPanel extends JPanel {

    public UsersMenuPanel(DashboardFrame dashboard) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel titleLabel = new JLabel("User Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.black);
        titleLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 0, 30, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);

        JButton btnPatients = new JButton("All Patients");
        UIUtil.styleButton(btnPatients, Color.BLUE);
        btnPatients.setPreferredSize(new Dimension(265, 150));
        btnPatients.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnPatients.addActionListener(e -> {
            ManageUserPanel panel = new ManageUserPanel("All Patients", "PATIENTS");
            panel.addBackButton(() -> dashboard.setContent(new UsersMenuPanel(dashboard)));
            dashboard.setContent(panel);
        });

        JButton btnStaff = new JButton("All Hospital Staff");
        UIUtil.styleButton(btnStaff, Color.RED);
        btnStaff.setPreferredSize(new Dimension(265, 150));
        btnStaff.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnStaff.addActionListener(e -> {
            ManageUserPanel panel = new ManageUserPanel("All Hospital Staff", "STAFF");
            panel.addBackButton(() -> dashboard.setContent(new UsersMenuPanel(dashboard)));
            dashboard.setContent(panel);
        });

        JButton btnAll = new JButton("All Users");
        UIUtil.styleButton(btnAll, Color.DARK_GRAY);
        btnAll.setPreferredSize(new Dimension(540, 100));
        btnAll.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnAll.addActionListener(e -> {
            ManageUserPanel panel = new ManageUserPanel("All Users", "ALL_USERS");
            panel.addBackButton(() -> dashboard.setContent(new UsersMenuPanel(dashboard)));
            dashboard.setContent(panel);
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Row 0, All Patients
        gbc.gridx = 0;
        gbc.gridy = 0;
        buttonPanel.add(btnPatients, gbc);
        // Row 0, All Hospital Staff 
        gbc.gridx = 1;
        gbc.gridy = 0;
        buttonPanel.add(btnStaff, gbc);



        // Row 1, All Users 
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        buttonPanel.add(btnAll, gbc);

        add(buttonPanel, BorderLayout.CENTER);
    }
}