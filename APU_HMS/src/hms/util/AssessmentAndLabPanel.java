package hms.util;
 
import hms.gui.panels.ManageRecordsPanel;
import javax.swing.*;
import java.awt.*;
 
/**
 * Low Kai Lun - Doctor: backs the "Key In Assessment & Lab Results" menu item.
 * The Doctor's menu groups vital signs/consultation notes and lab/imaging
 * requests under one entry, so this just tabs the two existing screens
 * together rather than duplicating their logic.
 */
public class AssessmentAndLabPanel extends JPanel {
 
    public AssessmentAndLabPanel() {
        setLayout(new BorderLayout());
 
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Vital Signs & Consultation Notes",
                new ManageRecordsPanel("Log Patient Vital Signs and Consultation Notes", "vital_signs.txt"));
        tabs.addTab("Lab / Imaging Requests",
                new ManageRecordsPanel("Request Lab Test, X-Ray or Specialized Imaging", "lab_requests.txt"));
 
        add(tabs, BorderLayout.CENTER);
    }
}
