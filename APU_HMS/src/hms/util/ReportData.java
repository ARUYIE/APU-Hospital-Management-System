/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hms.util;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ReportData {

    private final String month;
    
    private int totalPatients;
    private int appointments;
    private int completed;
    private int cancelled;
    private double totalRevenue;

    public ReportData(String month) {
        this.month = month;
        calculateReport(month);
    }

    private void calculateReport(String month) {

        totalPatients = 0;
        appointments = 0;
        completed = 0;
        cancelled = 0;
        totalRevenue = 0.0;

        List<String> bookings =
                FileManager.readLines("bookings.txt");

        List<String> rateRecords =
                FileManager.readLines("consultation_rates.txt");

        double[] rateRange =
                getRateRange(rateRecords);

        double minimumRate = rateRange[0];
        double maximumRate = rateRange[1];

        Set<String> patientIds =
                new HashSet<>();

        // Skip the first line because it is the header
        for (int i = 1; i < bookings.size(); i++) {

            String booking = bookings.get(i);

            if (booking == null || booking.trim().isEmpty()) {
                continue;
            }

            String[] parts = booking.split("\\|", -1);

            if (parts.length < 7) {
                continue;
            }

            String bookId = parts[0].trim();
            String patientId = parts[1].trim();
            String consultationDate = parts[3].trim();
            String status = parts[5].trim();

            if (!consultationDate.startsWith(month)) {
                continue;
            }

            patientIds.add(patientId);

            if (status.equalsIgnoreCase("SCHEDULED")) {

                appointments++;

            } else if (status.equalsIgnoreCase("COMPLETED")) {

                completed++;

                totalRevenue += generateAppointmentFee(
                        bookId,
                        minimumRate,
                        maximumRate
                );

            } else if (status.equalsIgnoreCase("CANCELLED")) {

                cancelled++;
            }
        }

        totalPatients = patientIds.size();
    }

    private double[] getRateRange(
            List<String> rateRecords) {

        double minimumRate = Double.MAX_VALUE;
        double maximumRate = -Double.MAX_VALUE;

        // Skip first line because it is the header
        for (int i = 1; i < rateRecords.size(); i++) {

            String record = rateRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            if (parts.length < 6) {
                continue;
            }

            try {

                double minRate =
                        Double.parseDouble(parts[2].trim());

                double maxRate =
                        Double.parseDouble(parts[3].trim());

                if (minRate < minimumRate) {
                    minimumRate = minRate;
                }

                if (maxRate > maximumRate) {
                    maximumRate = maxRate;
                }

            } catch (NumberFormatException e) {
                continue;
            }
        }

        // If no valid consultation rates exist
        if (minimumRate == Double.MAX_VALUE
                || maximumRate == -Double.MAX_VALUE) {

            minimumRate = 100.00;
            maximumRate = 250.00;
        }

        return new double[]{
            minimumRate,
            maximumRate
        };
    }

    private double generateAppointmentFee(
            String bookId,
            double minimumRate,
            double maximumRate) {

        Random random =
                new Random(bookId.hashCode());

        return minimumRate
                + (maximumRate - minimumRate)
                * random.nextDouble();
    }

    public int getTotalPatients() {
        return totalPatients;
    }

    public int getAppointments() {
        return appointments;
    }

    public int getCompleted() {
        return completed;
    }

    public int getCancelled() {
        return cancelled;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }
}