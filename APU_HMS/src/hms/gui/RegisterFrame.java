package hms.gui;

import hms.role.Patient;
import hms.role.Doctor;
import hms.role.Role;
import hms.role.AdminStaff;
import hms.role.MedicalManager;
import hms.role.User;
import hms.util.UserRepository;
import hms.util.Validator;

import javax.swing.*;
import java.awt.*;

/**
 * Registration form. The role dropdown determines which User subclass gets
 * created (polymorphism in action) and which extra fields are shown -
 * Doctors get a "Specialty" field, Patients get "Date of Birth"/"Gender".
 */
public class RegisterFrame extends JFrame {

    private final JTextField fullNameField = new JTextField(18);
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField phoneField = new JTextField(18);
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());

    // Role-specific fields, only relevant depending on roleBox selection.
    private final JTextField specialtyField = new JTextField(18);   // Doctor
    private final JTextField dobField = new JTextField(18);         // Patient, e.g. YYYY-MM-DD
    private final JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});

    private final JPanel extraFieldsPanel = new JPanel(new CardLayout());

    public RegisterFrame() {
        super("Register New Account");
        buildUI();
        setSize(460, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        addRow(root, gbc, row++, "Role:", roleBox);
        addRow(root, gbc, row++, "Full Name:", fullNameField);
        addRow(root, gbc, row++, "Username:", usernameField);
        addRow(root, gbc, row++, "Password:", passwordField);
        addRow(root, gbc, row++, "Email:", emailField);
        addRow(root, gbc, row++, "Phone:", phoneField);

        // Build the two swappable "extra fields" cards: Doctor and Patient.
        JPanel doctorCard = new JPanel(new GridBagLayout());
        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(5, 5, 5, 5);
        dgbc.fill = GridBagConstraints.HORIZONTAL;
        dgbc.gridx = 0; dgbc.gridy = 0;
        doctorCard.add(new JLabel("Specialty:"), dgbc);
        dgbc.gridx = 1;
        doctorCard.add(specialtyField, dgbc);

        JPanel patientCard = new JPanel(new GridBagLayout());
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.insets = new Insets(5, 5, 5, 5);
        pgbc.fill = GridBagConstraints.HORIZONTAL;
        pgbc.gridx = 0; pgbc.gridy = 0;
        patientCard.add(new JLabel("Date of Birth (YYYY-MM-DD):"), pgbc);
        pgbc.gridx = 1;
        patientCard.add(dobField, pgbc);
        pgbc.gridx = 0; pgbc.gridy = 1;
        patientCard.add(new JLabel("Gender:"), pgbc);
        pgbc.gridx = 1;
        patientCard.add(genderBox, pgbc);

        JPanel blankCard = new JPanel(); // Admin Staff / Medical Manager need no extra fields

        extraFieldsPanel.add(blankCard, Role.ADMIN_STAFF.name());
        extraFieldsPanel.add(blankCard, Role.MEDICAL_MANAGER.name());
        extraFieldsPanel.add(doctorCard, Role.DOCTOR.name());
        extraFieldsPanel.add(patientCard, Role.PATIENT.name());

        roleBox.addActionListener(e -> {
            CardLayout cl = (CardLayout) extraFieldsPanel.getLayout();
            cl.show(extraFieldsPanel, ((Role) roleBox.getSelectedItem()).name());
        });

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        root.add(extraFieldsPanel, gbc);
        row++;

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> handleRegister());
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        root.add(registerButton, gbc);

        setContentPane(root);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        Role role = (Role) roleBox.getSelectedItem();

        // ---- Input validation to avoid logical errors before writing to file ----
        StringBuilder errors = new StringBuilder();
        if (!Validator.isNonEmpty(fullName)) errors.append("- Full name is required.\n");
        if (!Validator.isNonEmpty(username)) errors.append("- Username is required.\n");
        else if (UserRepository.usernameExists(username)) errors.append("- Username is already taken.\n");
        if (!Validator.isValidPassword(password)) errors.append("- Password must be at least 6 characters.\n");
        if (!Validator.isValidEmail(email)) errors.append("- A valid email is required.\n");
        if (!Validator.isValidPhone(phone)) errors.append("- A valid phone number is required.\n");

        String specialty = specialtyField.getText().trim();
        String dob = dobField.getText().trim();
        if (role == Role.DOCTOR && !Validator.isNonEmpty(specialty)) {
            errors.append("- Specialty is required for doctors.\n");
        }
        if (role == Role.PATIENT && !Validator.isNonEmpty(dob)) {
            errors.append("- Date of birth is required for patients.\n");
        }

        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this, errors.toString(),
                    "Please fix the following", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = UserRepository.nextUserId();
        User newUser;
        switch (role) {
            case DOCTOR:
                newUser = new Doctor(userId, username, password, fullName, email, phone, specialty);
                break;
            case PATIENT:
                String gender = (String) genderBox.getSelectedItem();
                newUser = new Patient(userId, username, password, fullName, email, phone, dob, gender);
                break;
            case MEDICAL_MANAGER:
                newUser = new MedicalManager(userId, username, password, fullName, email, phone);
                break;
            case ADMIN_STAFF:
            default:
                newUser = new AdminStaff(userId, username, password, fullName, email, phone);
                break;
        }

        UserRepository.save(newUser);
        JOptionPane.showMessageDialog(this,
                "Registration successful! You can now log in as " + username + ".",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
