package hms.gui.panels;

import hms.role.User;
import hms.util.Tables;
import hms.util.Session;

import javax.swing.*;
import java.awt.*;

public class Profile extends JPanel {
    Tables tb = new Tables();
    public Profile() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            add(new JLabel("No user is currently logged in."));
            return;
        }

        add(tb.buildTitle("Profile"), BorderLayout.NORTH);
        add(tb.buildForm(), BorderLayout.CENTER);
        add(tb.buildSaveButton(), BorderLayout.SOUTH);
    }
}