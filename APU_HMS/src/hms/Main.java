package hms;

import hms.gui.LoginFrame;

import javax.swing.*;

/**
 * Entry point for the APU Medical Centre Hospital Management System.
 * <p>
 * The application "runs continuously" in the sense expected of a Swing GUI
 * app: once launched it stays open and responsive on the Event Dispatch
 * Thread (EDT), handling user actions (login, registration, navigating
 * between panels) until the user explicitly closes the window - there is
 * no single pass-through script that exits after one task.
 * <p>
 * Teammates: build your feature panels under hms.gui and wire them into
 * DashboardFrame's menu-handling switch statement. Model classes live under
 * hms.model, and text-file persistence helpers live under hms.util.
 */
public class Main {

    public static void main(String[] args) {
        // Use the OS-native look and feel where available, purely cosmetic.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Non-fatal: fall back to the default cross-platform look and feel.
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // All Swing UI work must happen on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
