package hms.util;

import javax.swing.*;
import java.awt.*;

public final class BackgroundPatternUtil {
    public static final Color DASHBOARD_BLUE = new Color(216, 229, 244);

    private BackgroundPatternUtil() {
    }

    public static JPanel createPatternPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                paintPattern(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setOpaque(true);
        panel.setBackground(DASHBOARD_BLUE);
        return panel;
    }

    public static void paintPattern(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spacing = 28;
        int loopSize = 16;
        Color loopColor = new Color(120, 148, 188, 48);

        for (int y = -spacing; y < height + spacing; y += spacing) {
            for (int x = -spacing; x < width + spacing; x += spacing) {
                int cx = x + spacing / 2;
                int cy = y + spacing / 2;
                g2.setColor(loopColor);
                g2.draw(new java.awt.geom.Arc2D.Double(
                        cx - loopSize / 2.0, cy - loopSize / 2.0,
                        loopSize, loopSize, 20, 260, java.awt.geom.Arc2D.OPEN));
                g2.draw(new java.awt.geom.Arc2D.Double(
                        cx - loopSize / 4.0, cy - loopSize / 4.0,
                        loopSize / 2.0, loopSize / 2.0, 215, 220, java.awt.geom.Arc2D.OPEN));
            }
        }
    }
}
