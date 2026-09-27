package hms.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;

public final class BackgroundPatternUtil {

    public static final Color DASHBOARD_BLUE = new Color(216, 229, 244);

    // Tracks whether we are on frame 0 or frame 1
    private static boolean toggleFrame = false;

    private BackgroundPatternUtil() {
    }

    public static JPanel createPatternPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                paintPattern(g2, getWidth(), getHeight(), toggleFrame);
                g2.dispose();
            }
        };
        panel.setOpaque(true);
        panel.setBackground(DASHBOARD_BLUE);

        // Timer set to 2000 milliseconds (2 seconds)
        Timer animationTimer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFrame = !toggleFrame; // Switch between frame 0 and frame 1
                // System.out.println("animation running");
                panel.repaint();
            }
        });
        animationTimer.start();

        return panel;
    }

    public static void paintPattern(Graphics2D g2, int width, int height, boolean frame) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spacing = 28;
        int loopSize = 16;
        Color loopColor = new Color(120, 148, 188, 55);

        // Shift the starting Y position slightly up or down depending on the current frame
        double yOffset = frame ? 10.0 : 0.0;

        for (double y = -spacing + yOffset; y < height + spacing; y += spacing) {
            for (int x = -spacing; x < width + spacing; x += spacing) {
                double cx = x + spacing / 2.0;
                double cy = y + spacing / 2.0;

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
