package hms.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class UIUtil {

    public static final Color DASHBOARD_BLUE = new Color(216, 229, 244);

    private UIUtil() {
    }

    public static JPanel createPatternPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            // Toggles between frame 0 and frame 1
            private boolean toggleFrame = false;

            {
                // Timer set to 2000 milliseconds 
                Timer animationTimer = new Timer(3000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toggleFrame = !toggleFrame;
                        // System.out.println("animation frame: " + (toggleFrame ? "2" : "1"));
                        repaint();
                    }
                });
                animationTimer.start();
            }

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
        return panel;
    }

    public static void paintPattern(Graphics2D g2, int width, int height, boolean frame) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spacing = 28;
        int loopSize = 16;
        Color loopColor = new Color(120, 148, 188, 55);

        // Frame 1 (false): 0 offset & 0 degrees. Frame 2 (true): 10px diagonal shift & 25-degree rotation.
        double diagonalOffset = frame ? 5.0 : 0.0;
        double rotationAngle = frame ? Math.toRadians(180.0) : 0.0;

        AffineTransform oldTransform = g2.getTransform();

        for (double y = -spacing + diagonalOffset; y < height + spacing; y += spacing) {
            for (double x = -spacing + diagonalOffset; x < width + spacing; x += spacing) {
                double cx = x + spacing / 2.0;
                double cy = y + spacing / 2.0;

                AffineTransform saveTransform = g2.getTransform();

                // Rotate ONLY this individual element around its exact center (cx, cy)
                g2.rotate(rotationAngle, cx, cy);

                g2.setColor(loopColor);
                g2.draw(new java.awt.geom.Arc2D.Double(
                        cx - loopSize / 2.0, cy - loopSize / 2.0,
                        loopSize, loopSize, 20, 260, java.awt.geom.Arc2D.OPEN));
                g2.draw(new java.awt.geom.Arc2D.Double(
                        cx - loopSize / 4.0, cy - loopSize / 4.0,
                        loopSize / 2.0, loopSize / 2.0, 215, 220, java.awt.geom.Arc2D.OPEN));

                g2.setTransform(saveTransform);
            }
        }

        g2.setTransform(oldTransform);
    }

    public static void styleButton(JButton button, Color backgroundColor) {
        button.setBackground(backgroundColor);
        button.setForeground(Color.black);
        button.setFocusPainted(false);
    }
}
