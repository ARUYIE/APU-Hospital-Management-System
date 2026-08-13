package hms.gui.panels;

import hms.role.*;
import hms.util.Session;
import hms.util.UserRepository;
import hms.util.Validator;

import javax.swing.*;
import java.awt.*;

public class Profile extends JPanel {

    private final User currentUser = Session.getCurrentUser();

    private final JTextField fullNameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    // role-specific fields 
    private JTextField specialtyField;
    private JTextField dobField;
    private JComboBox<String> genderBox;

    public Profile() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        if (currentUser == null) {
            add(new JLabel("No user is currently logged in."));
            return;
        }

        add(buildTitle(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildSaveButton(), BorderLayout.SOUTH);
    }

    private JComponent buildTitle() {
        JLabel title = new JLabel("Profile");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        return title;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // read only fields
        addReadOnlyRow(form, gbc, row++, "User ID:", currentUser.getUserId());
        addReadOnlyRow(form, gbc, row++, "Username:", currentUser.getUsername());
        addReadOnlyRow(form, gbc, row++, "Role:", currentUser.getRole().getDisplayName());

        // user editable fields
        fullNameField.setText(currentUser.getFullName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());

        addEditableRow(form, gbc, row++, "Full Name:", fullNameField);
        addEditableRow(form, gbc, row++, "Email:", emailField);
        addEditableRow(form, gbc, row++, "Phone:", phoneField);
        addEditableRow(form, gbc, row++, "New Password (leave blank to keep current):", passwordField);

        // role-specific editable fields 
        if (currentUser instanceof Doctor) {
            Doctor doctor = (Doctor) currentUser;
            specialtyField = new JTextField(20);
            specialtyField.setText(doctor.getSpecialty());
            addEditableRow(form, gbc, row++, "Specialty:", specialtyField);
        } else if (currentUser instanceof Patient) {
            Patient patient = (Patient) currentUser;
            dobField = new JTextField(20);
            dobField.setText(patient.getDateOfBirth());
            addEditableRow(form, gbc, row++, "Date of Birth (YYYY-MM-DD):", dobField);

            genderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
            genderBox.setSelectedItem(patient.getGender());
            addEditableRow(form, gbc, row++, "Gender:", genderBox);
        }

        return form;
    }

    private void addReadOnlyRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN));
        gbc.gridx = 1;
        panel.add(valueLabel, gbc);
    }

    private void addEditableRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private JComponent buildSaveButton() {
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> handleSave());
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.add(saveButton);
        return wrapper;
    }

    private void handleSave() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String newPassword = new String(passwordField.getPassword());

        // input validation
        StringBuilder errors = new StringBuilder();
        if (!Validator.isNonEmpty(fullName)) errors.append("- Full name is required.\n");
        if (!Validator.isValidEmail(email)) errors.append("- A valid email is required.\n");
        if (!Validator.isValidPhone(phone)) errors.append("- A valid phone number is required.\n");

        if (!newPassword.isEmpty() && !Validator.isValidPassword(newPassword)) {
            errors.append("- New password must be at least 6 characters.\n");
        }
        if (currentUser instanceof Doctor && !Validator.isNonEmpty(specialtyField.getText())) {
            errors.append("- Specialty is required.\n");
        }
        if (currentUser instanceof Patient && !Validator.isNonEmpty(dobField.getText())) {
            errors.append("- Date of birth is required.\n");
        }

        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this, errors.toString(),
                    "Please fix the following", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // apply user changes
        currentUser.setFullName(fullName);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        if (!newPassword.isEmpty()) {
            currentUser.setPassword(newPassword);
        }
        if (currentUser instanceof Doctor) {
            ((Doctor) currentUser).setSpecialty(specialtyField.getText().trim());
        }
        if (currentUser instanceof Patient) {
            ((Patient) currentUser).setDateOfBirth(dobField.getText().trim());
            ((Patient) currentUser).setGender((String) genderBox.getSelectedItem());
        }

        // ---- Persist to users.txt, then clear the password field back out ----
        UserRepository.update(currentUser);
        passwordField.setText("");

        JOptionPane.showMessageDialog(this,
                "Your profile has been updated.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}