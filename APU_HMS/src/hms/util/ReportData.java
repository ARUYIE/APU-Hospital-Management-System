/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hms.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;

public class ReportData {
    Random random = new Random();
    
    public String getReportPeriod() {
        System.out.println(YearMonth.now().toString());
        return YearMonth.now().toString();
    }

    public int getTotalPatients() {
        List<String> users = FileManager.readLines("users.txt");

        int count = 0;

        for (String record : users) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            // Skip header
            if (parts.length > 0 && "ID".equalsIgnoreCase(parts[0].trim())) {
                continue;
            }

            if (parts.length > 0 && "PATIENT".equalsIgnoreCase(parts[1].trim())) {
                count++;
            }
        }
        
        System.out.println(count);
        return count;
    }

    public int getAppointments() {
        List<String> bookings = FileManager.readLines("bookings.txt");

        int count = 0;

        for (String record : bookings) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            if (parts.length > 0
                    && "BOOK_ID".equalsIgnoreCase(parts[0].trim())) {
                continue;
            }

            count++;
        }

        System.out.println(count);
        return count;
    }

    public int getCompleted() {
        List<String> bookings = FileManager.readLines("bookings.txt");

        int count = 0;

        for (String record : bookings) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            if (parts.length >= 6
                    && "COMPLETED".equalsIgnoreCase(parts[5].trim())) {
                count++;
            }
        }

        System.out.println(count);
        return count;
    }

    public int getCancelled() {
        List<String> bookings = FileManager.readLines("bookings.txt");

        int count = 0;

        for (String record : bookings) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            if (parts.length >= 6
                    && "CANCELLED".equalsIgnoreCase(parts[5].trim())) {
                count++;
            }
        }

        System.out.println(count);
        return count;
    }

    public int getActiveDepartments() {
        List<String> departments = FileManager.readLines("department.txt");

        int count = 0;

        for (String record : departments) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            if (parts.length > 0
                    && "DEPTARTMENT_ID".equalsIgnoreCase(parts[0].trim())) {
                continue;
            }

            count++;
        }

        System.out.println(count);
        return count;
    }

    public double getTotalRevenue() {
        List<String> bookings = FileManager.readLines("bookings.txt");
        List<String> consultationRates = FileManager.readLines("consultation_rates.txt");

        double totalRevenue = 0.0;

        for (String booking : bookings) {
            if (booking == null || booking.trim().isEmpty()) {
                continue;
            }

            String[] bookingParts = booking.split("\\|", -1);

            if (bookingParts.length < 6) {
                continue;
            }

            // Only completed appointments generate revenue
            if (!"COMPLETED".equalsIgnoreCase(bookingParts[5].trim())) {
                continue;
            }

            String doctorId = bookingParts[2].trim();

            for (int i = 0; i < random.nextInt(10, 100); i++) {
                totalRevenue += random.nextInt(100, 250);
            }
        }

        System.out.println(totalRevenue);
        return totalRevenue;
    }
}
