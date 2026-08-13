package hms.util;

import hms.role.Doctor;
import hms.role.Patient;
import hms.role.User;
import java.awt.*;
import javax.swing.*;

//This class is for making tables
//buildForm
//addReadOnlyRow
//addEditableRow
//buildSaveButton
//buildSaveButton("save","cancel") #this is for popup window
//buildTitle("titlename")
//handleSave
public final class Tables {
    
    public final User targetUser;
    private final Window popup;
    private Runnable onSaved;
    
    public final JTextField fullNameField = new JTextField(20);
    public final JTextField emailField = new JTextField(20);
    public final JTextField phoneField = new JTextField(20);
    public final JPasswordField passwordField = new JPasswordField(20);

    // role-specific fields 
    public JTextField specialtyField;
    public JTextField dobField;
    public JComboBox<String> genderBox;

    //normal
    public Tables() {
        this(Session.getCurrentUser(), null);
    }
 
    //for current login user
    public Tables(Window popup) {
        this(Session.getCurrentUser(), popup);
    }
 
    //for table
    public Tables(User targetUser, Window popup) {
        this.targetUser = targetUser;
        this.popup = popup;
    }
 
    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    
    public JComponent buildTitle(String header) {
        JLabel title = new JLabel(header);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        return title;
    }
    
    public JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // read only fields
        addReadOnlyRow(form, gbc, row++, "User ID:", targetUser.getUserId());
        addReadOnlyRow(form, gbc, row++, "Username:", targetUser.getUsername());
        addReadOnlyRow(form, gbc, row++, "Role:", targetUser.getRole().getDisplayName());

        // user editable fields
        fullNameField.setText(targetUser.getFullName());
        emailField.setText(targetUser.getEmail());
        phoneField.setText(targetUser.getPhone());

        addEditableRow(form, gbc, row++, "Full Name:", fullNameField);
        addEditableRow(form, gbc, row++, "Email:", emailField);
        addEditableRow(form, gbc, row++, "Phone:", phoneField);
        addEditableRow(form, gbc, row++, "New Password (leave blank to keep current):", passwordField);

        // role-specific editable fields 
        if (targetUser instanceof Doctor) {
            Doctor doctor = (Doctor) targetUser;
            specialtyField = new JTextField(20);
            specialtyField.setText(doctor.getSpecialty());
            addEditableRow(form, gbc, row++, "Specialty:", specialtyField);
        } else if (targetUser instanceof Patient) {
            Patient patient = (Patient) targetUser;
            dobField = new JTextField(20);
            dobField.setText(patient.getDateOfBirth());
            addEditableRow(form, gbc, row++, "Date of Birth (YYYY-MM-DD):", dobField);

            genderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
            genderBox.setSelectedItem(patient.getGender());
            addEditableRow(form, gbc, row++, "Gender:", genderBox);
        }

        return form;
    }

    public static void addReadOnlyRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN));
        gbc.gridx = 1;
        panel.add(valueLabel, gbc);
    }

    public static void addEditableRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }
    
    public JComponent buildSaveButton() {
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> handleSave());
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.add(saveButton);
        return wrapper;
    }
    
    //save button for popup
    public JComponent buildSaveButton(String save,String cancel) {
        JButton saveButton = new JButton(save);
        JButton cancelButton = new JButton(cancel);
        cancelButton.addActionListener(e -> popup.dispose());
        saveButton.addActionListener(e -> handleSave());
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.add(saveButton);
        wrapper.add(cancelButton);
        return wrapper;
    }

    public void handleSave() {
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
        if (targetUser instanceof Doctor && !Validator.isNonEmpty(specialtyField.getText())) {
            errors.append("- Specialty is required.\n");
        }
        if (targetUser instanceof Patient && !Validator.isNonEmpty(dobField.getText())) {
            errors.append("- Date of birth is required.\n");
        }

        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(null, errors.toString(), 
                    "Please fix the following", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // apply user changes
        targetUser.setFullName(fullName);
        targetUser.setEmail(email);
        targetUser.setPhone(phone);
        if (!newPassword.isEmpty()) {
            targetUser.setPassword(newPassword);
        }
        if (targetUser instanceof Doctor) {
            ((Doctor) targetUser).setSpecialty(specialtyField.getText().trim());
        }
        if (targetUser instanceof Patient) {
            ((Patient) targetUser).setDateOfBirth(dobField.getText().trim());
            ((Patient) targetUser).setGender((String) genderBox.getSelectedItem());
        }

        //Persist to users.txt, then clear the password field back out
        UserRepository.update(targetUser);
        passwordField.setText("");

        JOptionPane.showMessageDialog(null,
                "Your profile has been updated.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        
        if (onSaved != null) onSaved.run(); // tells a table to refresh
        if (popup != null) popup.dispose(); // auto-close the popup, if this form is in one
        
        
    }
    
}
