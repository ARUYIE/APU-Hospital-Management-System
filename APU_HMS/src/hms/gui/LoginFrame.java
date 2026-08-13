package hms.gui;

import hms.role.User;
import hms.util.Session;
import hms.util.UserRepository;
import hms.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

//Login screen shown when the application starts.

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginFrame() {
        super("APU Medical Centre - Hospital Management System - Login");
        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Hospital Management System");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        root.add(title, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        root.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        root.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        root.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        root.add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register New Account");

        loginButton.addActionListener(this::handleLogin);
        registerButton.addActionListener((ActionEvent e) -> {
            new RegisterFrame().setVisible(true);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        root.add(buttonPanel, gbc);

        // Pressing Enter in the password field triggers login
        passwordField.addActionListener(this::handleLogin);

        setContentPane(root);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (!Validator.isNonEmpty(username) || !Validator.isNonEmpty(password)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = UserRepository.authenticate(username, password);
        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Session.login(user);
        new DashboardFrame().setVisible(true);
        dispose();
    }
}
