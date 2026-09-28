package hms.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class ReportCharts extends JPanel {

    private static final Color[] CHART_COLORS = {
        new Color(66, 133, 244),
        new Color(52, 168, 83),
        new Color(251, 188, 5),
        new Color(234, 67, 53),
        new Color(156, 39, 176),
        new Color(0, 150, 136),
        new Color(255, 112, 67),
        new Color(121, 85, 72)
    };

    private final String selectedMonth;

    private final JPanel revenueChartPanel;
    private final JPanel statusChartPanel;
    private final JPanel doctorWorkloadChartPanel;
    private final JPanel weeklyVolumeChartPanel;

    public ReportCharts(String title) {

        selectedMonth =
                java.time.YearMonth.now().toString();

        String monthDisplay =
                java.time.YearMonth.now()
                        .getMonth()
                        .getDisplayName(
                                java.time.format.TextStyle.FULL,
                                java.util.Locale.ENGLISH
                        );

        setLayout(
                new BorderLayout(10, 10)
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        15, 15, 15, 15
                )
        );

        JLabel heading =
                new JLabel(title);

        heading.setFont(
                heading.getFont().deriveFont(
                        Font.BOLD,
                        16f
                )
        );

        add(
                heading,
                BorderLayout.NORTH
        );

        revenueChartPanel =
                createChartPanel(
                        "Monthly Revenue - " + monthDisplay
                );

        statusChartPanel =
                createChartPanel(
                        "Total Appointments - " + monthDisplay
                );

        doctorWorkloadChartPanel =
                createChartPanel(
                        "Doctor Workload - " + monthDisplay
                );

        weeklyVolumeChartPanel =
                createChartPanel(
                        "Weekly Appointment Volume - " + monthDisplay
                );

        JPanel allCharts =
                new JPanel(
                        new GridLayout(
                                2,
                                2,
                                10,
                                10
                        )
                );

        allCharts.add(
                revenueChartPanel
        );

        allCharts.add(
                statusChartPanel
        );

        allCharts.add(
                doctorWorkloadChartPanel
        );

        allCharts.add(
                weeklyVolumeChartPanel
        );

        add(
                allCharts,
                BorderLayout.CENTER
        );

        loadCharts();
    }

    private JPanel createChartPanel(
            String title) {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        title
                )
        );

        panel.setPreferredSize(
                new Dimension(
                        350,
                        180
                )
        );

        return panel;
    }

    private void loadCharts() {

        ReportData data =
                new ReportData(selectedMonth);


        revenueChartPanel.add(
                new RevenueBarChart(data),
                BorderLayout.CENTER
        );

        /*
         * -----------------------------------------------------
         * COMPLETED VS CANCELLED
         * -----------------------------------------------------
         */

        statusChartPanel.add(
                new StatusPieChart(data),
                BorderLayout.CENTER
        );


        doctorWorkloadChartPanel.add(
                new DoctorWorkloadBarChart(data),
                BorderLayout.CENTER
        );


        weeklyVolumeChartPanel.add(
                new WeeklyVolumeBarChart(data),
                BorderLayout.CENTER
        );

        revalidate();
        repaint();
    }

    private static class RevenueBarChart
            extends JPanel {

        private final ReportData data;

        public RevenueBarChart(
                ReportData data) {

            this.data = data;

            setPreferredSize(
                    new Dimension(
                            350,
                            180
                    )
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Map<String, Double> values =
                    data.getMonthlyRevenue();

            if (values.isEmpty()) {

                drawNoData(
                        g2,
                        getWidth(),
                        getHeight()
                );

                g2.dispose();
                return;
            }

            List<Map.Entry<String, Double>>
                    entries =
                    new ArrayList<>(
                            values.entrySet()
                    );

            entries.sort(
                    Map.Entry.comparingByKey()
            );

            int left = 40;
            int right = 15;
            int top = 15;
            int bottom = 40;

            int chartWidth =
                    getWidth()
                    - left
                    - right;

            int chartHeight =
                    getHeight()
                    - top
                    - bottom;

            double maximum =
                    entries.stream()
                            .mapToDouble(
                                    Map.Entry::getValue
                            )
                            .max()
                            .orElse(1.0);

            if (maximum <= 0) {
                maximum = 1.0;
            }

            int slotWidth =
                    chartWidth
                    / entries.size();

            int barWidth =
                    Math.max(
                            10,
                            slotWidth - 10
                    );

            DateTimeFormatter monthFormatter =
                    DateTimeFormatter.ofPattern(
                            "MMM"
                    );

            for (int i = 0;
                    i < entries.size();
                    i++) {

                Map.Entry<String, Double> entry =
                        entries.get(i);

                double value =
                        entry.getValue();

                int barHeight =
                        (int)
                        ((value / maximum)
                        * chartHeight);

                int x =
                        left
                        + i * slotWidth;

                int y =
                        top
                        + chartHeight
                        - barHeight;

                g2.setColor(
                        CHART_COLORS[
                                i % CHART_COLORS.length
                        ]
                );

                g2.fillRect(
                        x,
                        y,
                        barWidth,
                        barHeight
                );

                g2.setColor(
                        Color.DARK_GRAY
                );

                g2.setFont(
                        g2.getFont().deriveFont(
                                9f
                        )
                );

                String valueLabel =
                        String.format(
                                "RM%.0f",
                                value
                        );

                int valueWidth =
                        g2.getFontMetrics()
                                .stringWidth(
                                        valueLabel
                                );

                g2.drawString(
                        valueLabel,
                        x
                        + (barWidth - valueWidth)
                        / 2,
                        Math.max(
                                12,
                                y - 4
                        )
                );

                String monthLabel;

                try {

                    LocalDate firstDay =
                            LocalDate.parse(
                                    entry.getKey()
                                    + "-01"
                            );

                    monthLabel =
                            firstDay.format(
                                    monthFormatter
                            );

                } catch (Exception e) {

                    monthLabel =
                            entry.getKey();
                }

                int labelWidth =
                        g2.getFontMetrics()
                                .stringWidth(
                                        monthLabel
                                );

                g2.drawString(
                        monthLabel,
                        x
                        + (barWidth - labelWidth)
                        / 2,
                        top
                        + chartHeight
                        + 20
                );
            }

            g2.setStroke(
                    new BasicStroke(1f)
            );

            g2.drawLine(
                    left,
                    top + chartHeight,
                    getWidth() - right,
                    top + chartHeight
            );

            g2.dispose();
        }
    }


    private static class StatusPieChart
            extends JPanel {

        private final ReportData data;

        public StatusPieChart(
                ReportData data) {

            this.data = data;

            setPreferredSize(
                    new Dimension(
                            350,
                            180
                    )
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Map<String, Integer> values =
                    data.getAppointmentStatus();

            int completed =
                    values.getOrDefault(
                            "COMPLETED",
                            0
                    );

            int cancelled =
                    values.getOrDefault(
                            "CANCELLED",
                            0
                    );

            int total =
                    completed + cancelled;

            if (total == 0) {

                drawNoData(
                        g2,
                        getWidth(),
                        getHeight()
                );

                g2.dispose();
                return;
            }

            double completedPercentage =
                    (double) completed
                    / total
                    * 100.0;

            double cancelledPercentage =
                    (double) cancelled
                    / total
                    * 100.0;

            int diameter =
                    Math.min(
                            getHeight() - 25,
                            145
                    );

            int x = 15;

            int y =
                    (getHeight() - diameter) / 2;

            double completedAngle =
                    360.0
                    * completedPercentage
                    / 100.0;

            g2.setColor(
                    CHART_COLORS[0]
            );

            g2.fillArc(
                    x,
                    y,
                    diameter,
                    diameter,
                    0,
                    (int) Math.round(
                            completedAngle
                    )
            );

            g2.setColor(
                    CHART_COLORS[3]
            );

            g2.fillArc(
                    x,
                    y,
                    diameter,
                    diameter,
                    (int) Math.round(
                            completedAngle
                    ),
                    (int) Math.round(
                            360 - completedAngle
                    )
            );


            int legendX =
                    x + diameter + 25;

            int legendY =
                    y + 35;

            g2.setFont(
                    g2.getFont().deriveFont(
                            Font.PLAIN,
                            10f
                    )
            );

            g2.setColor(
                    CHART_COLORS[0]
            );

            g2.fillRect(
                    legendX,
                    legendY - 9,
                    10,
                    10
            );

            g2.setColor(
                    Color.DARK_GRAY
            );

            g2.drawString(
                    "Completed",
                    legendX + 16,
                    legendY
            );

            g2.drawString(
                    completed
                    + " appointments",
                    legendX + 16,
                    legendY + 16
            );

            g2.drawString(
                    String.format(
                            "%.1f%%",
                            completedPercentage
                    ),
                    legendX + 16,
                    legendY + 32
            );

            legendY += 70;

            g2.setColor(
                    CHART_COLORS[3]
            );

            g2.fillRect(
                    legendX,
                    legendY - 9,
                    10,
                    10
            );

            g2.setColor(
                    Color.DARK_GRAY
            );

            g2.drawString(
                    "Cancelled",
                    legendX + 16,
                    legendY
            );

            g2.drawString(
                    cancelled
                    + " appointments",
                    legendX + 16,
                    legendY + 16
            );

            g2.drawString(
                    String.format(
                            "%.1f%%",
                            cancelledPercentage
                    ),
                    legendX + 16,
                    legendY + 32
            );

            g2.dispose();
        }
    }

    private static class DoctorWorkloadBarChart
            extends JPanel {

        private final ReportData data;

        public DoctorWorkloadBarChart(
                ReportData data) {

            this.data = data;

            setPreferredSize(
                    new Dimension(
                            350,
                            180
                    )
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Map<String, Integer> values =
                    data.getDoctorWorkload();

            if (values.isEmpty()) {

                drawNoData(
                        g2,
                        getWidth(),
                        getHeight()
                );

                g2.dispose();
                return;
            }

            List<Map.Entry<String, Integer>>
                    entries =
                    new ArrayList<>(
                            values.entrySet()
                    );

            entries.sort(
                    Map.Entry.<String, Integer>
                            comparingByValue()
                            .reversed()
            );

            int left = 45;
            int right = 15;
            int top = 15;
            int bottom = 45;

            int chartWidth =
                    getWidth()
                    - left
                    - right;

            int chartHeight =
                    getHeight()
                    - top
                    - bottom;

            int maximum =
                    entries.stream()
                            .mapToInt(
                                    Map.Entry::getValue
                            )
                            .max()
                            .orElse(1);

            int slotWidth =
                    chartWidth
                    / entries.size();

            int barWidth =
                    Math.max(
                            10,
                            slotWidth - 10
                    );

            for (int i = 0;
                    i < entries.size();
                    i++) {

                Map.Entry<String, Integer> entry =
                        entries.get(i);

                int value =
                        entry.getValue();

                int barHeight =
                        (int)
                        ((double) value
                        / maximum
                        * chartHeight);

                int x =
                        left
                        + i * slotWidth;

                int y =
                        top
                        + chartHeight
                        - barHeight;

                g2.setColor(
                        CHART_COLORS[
                                i % CHART_COLORS.length
                        ]
                );

                g2.fillRect(
                        x,
                        y,
                        barWidth,
                        barHeight
                );

                g2.setColor(
                        Color.DARK_GRAY
                );

                g2.drawString(
                        String.valueOf(value),
                        x + 3,
                        Math.max(
                                12,
                                y - 4
                        )
                );

                String doctorName =
                        entry.getKey();

                if (doctorName.length() > 12) {

                    doctorName =
                            doctorName.substring(
                                    0,
                                    11
                            )
                            + ".";
                }

                int labelWidth =
                        g2.getFontMetrics()
                                .stringWidth(
                                        doctorName
                                );

                g2.drawString(
                        doctorName,
                        x
                        + (barWidth - labelWidth)
                        / 2,
                        top
                        + chartHeight
                        + 20
                );
            }

            g2.drawLine(
                    left,
                    top + chartHeight,
                    getWidth() - right,
                    top + chartHeight
            );

            g2.dispose();
        }
    }

    private static class WeeklyVolumeBarChart
            extends JPanel {

        private final ReportData data;

        public WeeklyVolumeBarChart(
                ReportData data) {

            this.data = data;

            setPreferredSize(
                    new Dimension(
                            350,
                            180
                    )
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Map<LocalDate, Integer> values =
                    data.getWeeklyAppointmentVolume();

            if (values.isEmpty()) {

                drawNoData(
                        g2,
                        getWidth(),
                        getHeight()
                );

                g2.dispose();
                return;
            }

            List<Map.Entry<LocalDate, Integer>>
                    entries =
                    new ArrayList<>(
                            values.entrySet()
                    );

            entries.sort(
                    Map.Entry.comparingByKey()
            );

            int left = 40;
            int right = 15;
            int top = 15;
            int bottom = 40;

            int chartWidth =
                    getWidth()
                    - left
                    - right;

            int chartHeight =
                    getHeight()
                    - top
                    - bottom;

            int maximum =
                    entries.stream()
                            .mapToInt(
                                    Map.Entry::getValue
                            )
                            .max()
                            .orElse(1);

            int slotWidth =
                    chartWidth
                    / entries.size();

            int barWidth =
                    Math.max(
                            20,
                            slotWidth - 15
                    );

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "MMM d"
                    );

            for (int i = 0;
                    i < entries.size();
                    i++) {

                Map.Entry<LocalDate, Integer>
                        entry =
                        entries.get(i);

                LocalDate sunday =
                        entry.getKey();

                int value =
                        entry.getValue();

                int barHeight =
                        (int)
                        ((double) value
                        / maximum
                        * chartHeight);

                int x =
                        left
                        + i * slotWidth;

                int y =
                        top
                        + chartHeight
                        - barHeight;

                g2.setColor(
                        CHART_COLORS[
                                i % CHART_COLORS.length
                        ]
                );

                g2.fillRect(
                        x,
                        y,
                        barWidth,
                        barHeight
                );

                g2.setColor(
                        Color.DARK_GRAY
                );

                /*
                 * Appointment count.
                 */
                g2.drawString(
                        String.valueOf(value),
                        x + 3,
                        Math.max(
                                12,
                                y - 4
                        )
                );

                String label =
                        sunday.format(
                                formatter
                        );

                int labelWidth =
                        g2.getFontMetrics()
                                .stringWidth(
                                        label
                                );

                g2.drawString(
                        label,
                        x
                        + (barWidth - labelWidth)
                        / 2,
                        top
                        + chartHeight
                        + 20
                );
            }

            g2.drawLine(
                    left,
                    top + chartHeight,
                    getWidth() - right,
                    top + chartHeight
            );

            g2.dispose();
        }
    }
    
    private static void drawNoData(
            Graphics2D g2,
            int width,
            int height) {

        g2.setColor(
                Color.GRAY
        );

        String message =
                "No data available";

        int textWidth =
                g2.getFontMetrics()
                        .stringWidth(message);

        g2.drawString(
                message,
                (width - textWidth) / 2,
                height / 2
        );
    }
}