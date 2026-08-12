package hms.gui.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Generic stand-in panel shown for any feature that hasn't been built yet.
 * Teammates: create a real panel class in this package (see WardPanel for a
 * full worked example - list + add form + validation + text-file save), then
 * update DashboardFrame.showPanelFor() to use it instead of this placeholder.
 */
public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String featureName) {
        setLayout(new GridBagLayout());
        JLabel label = new JLabel("<html><center>" + featureName
                + "<br><br><i>(Not implemented yet - see WardPanel for a worked example)</i></center></html>");
        label.setFont(label.getFont().deriveFont(15f));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label);
    }
}
