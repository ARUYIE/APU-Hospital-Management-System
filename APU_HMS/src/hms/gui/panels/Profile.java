package hms.gui.panels;

import hms.role.Doctor;
import hms.role.Patient;
import hms.role.User;
import hms.util.Session;
import hms.util.UserRepository;
import hms.util.Validator;

import javax.swing.*;
import java.awt.*;

public class Profile extends JPanel {
    public Profile() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            add(new JLabel("No user is currently logged in."));
            return;
        }

        JLabel title = new JLabel("Profile");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JTextField fullNameField = new JTextField(currentUser.getFullName(), 20);
        JTextField emailField = new JTextField(currentUser.getEmail(), 20);
        JTextField phoneField = new JTextField(currentUser.getPhone(), 20);
        JPasswordField passwordField = new JPasswordField(20);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        addReadOnlyRow(form, gbc, row++, "User ID:", currentUser.getUserId());
        addReadOnlyRow(form, gbc, row++, "Username:", currentUser.getUsername());
        addReadOnlyRow(form, gbc, row++, "Role:", currentUser.getRole().getDisplayName());
        addEditableRow(form, gbc, row++, "Full Name:", fullNameField);
        addEditableRow(form, gbc, row++, "Email:", emailField);
        addEditableRow(form, gbc, row++, "Phone:", phoneField);
        addEditableRow(form, gbc, row++, "New Password (leave blank to keep current):", passwordField);

        JButton saveButton = new JButton("Save Changes");
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        form.add(saveButton, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL;

        if (currentUser instanceof Doctor doctor) {
            JTextField specialtyField = new JTextField(doctor.getSpecialty(), 20);
            addEditableRow(form, gbc, row++, "Specialty:", specialtyField);
        } else if (currentUser instanceof Patient patient) {
            JTextField dobField = new JTextField(patient.getDateOfBirth(), 20);
            JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
            genderBox.setSelectedItem(patient.getGender());
            addEditableRow(form, gbc, row++, "Date of Birth (YYYY-MM-DD):", dobField);
            addEditableRow(form, gbc, row++, "Gender:", genderBox);
        }

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
                JOptionPane.showMessageDialog(this, errors.toString(), "Please fix the following", JOptionPane.WARNING_MESSAGE);
                return;
            }

            currentUser.setFullName(fullName);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            if (!newPassword.isEmpty()) {
                currentUser.setPassword(newPassword);
            }

            UserRepository.update(currentUser);
            passwordField.setText("");
            JOptionPane.showMessageDialog(this, "Your profile has been updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
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
}