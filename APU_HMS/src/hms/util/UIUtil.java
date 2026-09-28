package hms.util;

import java.awt.Color;
import java.awt.Dimension;
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
            
            // Logic variables
            private boolean toggleFrame = false;
            private int timerTicks = 0;
            
            // Current visual state
            private double currentOffset = 0.0;
            private double currentRotation = 0.0;

            // Target for continuous rotation
            private double targetRotation = 0.0;

            {
                Timer animationTimer = new Timer(16, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        timerTicks++;
                        
                        // Every ~3 seconds (180 ticks)
                        if (timerTicks >= 180) {
                            toggleFrame = !toggleFrame;
                            
                            // Instead of flipping back to 0, add another 180 degrees (Math.PI)
                            // so it completes the circle in the same direction.
                            targetRotation += Math.PI; 
                            
                            timerTicks = 0; 
                        }

                        // Offset still bounces between 5.0 and 0.0
                        double targetOffset = toggleFrame ? 5.0 : 0.0;

                        // Smoothly move 8% of the remaining distance every frame
                        currentOffset += (targetOffset - currentOffset) * 0.08;
                        currentRotation += (targetRotation - currentRotation) * 0.08;
                        
                        repaint();
                    }
                });
                animationTimer.start();
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                paintPattern(g2, getWidth(), getHeight(), currentOffset, currentRotation);
                g2.dispose();
            }
        };
        panel.setOpaque(true);
        panel.setBackground(DASHBOARD_BLUE);
        
        panel.setPreferredSize(new Dimension(800, 600)); 
        
        return panel;
    }

    public static void paintPattern(Graphics2D g2, int width, int height, double offset, double rotationRadians) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spacing = 28;
        int loopSize = 16;
        Color loopColor = new Color(120, 148, 188, 55);

        AffineTransform oldTransform = g2.getTransform();

        for (double y = -spacing + offset; y < height + spacing; y += spacing) {
            for (double x = -spacing + offset; x < width + spacing; x += spacing) {
                double cx = x + spacing / 2.0;
                double cy = y + spacing / 2.0;

                AffineTransform saveTransform = g2.getTransform();

                // Rotate ONLY this individual element around its exact center (cx, cy)
                g2.rotate(rotationRadians, cx, cy);

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