package hms.gui.panels;

import javax.swing.*;
import java.awt.*;

//this is here as a placeholder
//update DashboardFrame.showPanelFor() to use it instead of this placeholder.
 
public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String featureName) {
        setLayout(new GridBagLayout());
        JLabel label = new JLabel("<html><center>" + featureName
                + "<br><br><i>Oh? Its empty...</i></center></html>");
        label.setFont(label.getFont().deriveFont(15f));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label);
    }
}
