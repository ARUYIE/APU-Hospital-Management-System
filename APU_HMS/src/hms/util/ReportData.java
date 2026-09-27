package hms.util;

import hms.role.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;

public class ReportData {

    private final String month;

    private int totalPatients;
    private int appointments;
    private int completed;
    private int cancelled;
    private double totalRevenue;

    private final Map<String, Integer> departmentAppointments;
    private final Map<String, Double> monthlyRevenue;
    private final Map<String, Integer> appointmentStatus;
    private final Map<String, Integer> doctorWorkload;

    /*
     * LocalDate is used as the key so the weeks remain
     * chronologically ordered.
     *
     * The chart can format the date as:
     * Sep 6
     * Sep 13
     * Sep 20
     */
    private final Map<LocalDate, Integer> weeklyAppointmentVolume;

    public ReportData(String month) {

        this.month = month;

        departmentAppointments = new LinkedHashMap<>();
        monthlyRevenue = new TreeMap<>();
        appointmentStatus = new LinkedHashMap<>();
        doctorWorkload = new LinkedHashMap<>();
        weeklyAppointmentVolume = new TreeMap<>();

        calculateReport();
        calculateDepartmentAppointments();
        calculateMonthlyRevenue();
        calculateAppointmentStatus();
        calculateDoctorWorkload();
        calculateWeeklyAppointmentVolume();
    }

    /*
     * ---------------------------------------------------------
     * MAIN REPORT
     * ---------------------------------------------------------
     */
    private void calculateReport() {

        totalPatients = 0;
        appointments = 0;
        completed = 0;
        cancelled = 0;
        totalRevenue = 0.0;

        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        Set<String> uniquePatients =
                new HashSet<>();

        double[] rateRange = getRateRange();

        double minimumRate = rateRange[0];
        double maximumRate = rateRange[1];

        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 7) {
                continue;
            }

            String bookId = parts[0].trim();
            String patientId = parts[1].trim();
            String dateText = parts[3].trim();
            String status = parts[5].trim();

            LocalDate date;

            try {
                date = LocalDate.parse(dateText);
            } catch (Exception e) {
                continue;
            }

            String recordMonth =
                    date.format(
                            DateTimeFormatter.ofPattern("yyyy-MM")
                    );

            if (!recordMonth.equals(month)) {
                continue;
            }

            if (!patientId.isEmpty()) {
                uniquePatients.add(patientId);
            }

            if (status.equalsIgnoreCase("SCHEDULED")) {

                appointments++;

            } else if (status.equalsIgnoreCase("COMPLETED")) {

                appointments++;
                completed++;

                totalRevenue +=
                        generateAppointmentFee(
                                bookId,
                                minimumRate,
                                maximumRate
                        );

            } else if (status.equalsIgnoreCase("CANCELLED")) {

                cancelled++;
            }
        }

        totalPatients = uniquePatients.size();
    }

    /*
     * ---------------------------------------------------------
     * DEPARTMENT APPOINTMENTS
     * ---------------------------------------------------------
     *
     * SERVICE_TYPE from bookings.txt is used as the
     * department/service category.
     *
     * Cancelled appointments are excluded.
     */
    private void calculateDepartmentAppointments() {

        departmentAppointments.clear();

        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 7) {
                continue;
            }

            String dateText = parts[3].trim();
            String status = parts[5].trim();
            String serviceType = parts[6].trim();

            LocalDate date;

            try {
                date = LocalDate.parse(dateText);
            } catch (Exception e) {
                continue;
            }

            String recordMonth =
                    date.format(
                            DateTimeFormatter.ofPattern("yyyy-MM")
                    );

            if (!recordMonth.equals(month)) {
                continue;
            }

            if (status.equalsIgnoreCase("CANCELLED")) {
                continue;
            }

            if (serviceType.isEmpty()) {
                serviceType = "Unknown";
            }

            departmentAppointments.merge(
                    serviceType,
                    1,
                    Integer::sum
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * MONTHLY REVENUE
     * ---------------------------------------------------------
     *
     * Revenue is calculated directly from bookings.txt.
     *
     * Only COMPLETED appointments generate revenue.
     *
     * The result contains all months found in bookings.txt,
     * allowing the revenue chart to display a monthly trend.
     */
    private void calculateMonthlyRevenue() {

        monthlyRevenue.clear();

        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        double[] rateRange = getRateRange();

        double minimumRate = rateRange[0];
        double maximumRate = rateRange[1];

        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 7) {
                continue;
            }

            String bookId = parts[0].trim();
            String dateText = parts[3].trim();
            String status = parts[5].trim();

            LocalDate date;

            try {
                date = LocalDate.parse(dateText);
            } catch (Exception e) {
                continue;
            }

            /*
             * Revenue only comes from completed appointments.
             */
            if (!status.equalsIgnoreCase("COMPLETED")) {
                continue;
            }

            String revenueMonth =
                    date.format(
                            DateTimeFormatter.ofPattern("yyyy-MM")
                    );

            double fee =
                    generateAppointmentFee(
                            bookId,
                            minimumRate,
                            maximumRate
                    );

            monthlyRevenue.merge(
                    revenueMonth,
                    fee,
                    Double::sum
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * APPOINTMENT STATUS
     * ---------------------------------------------------------
     */
    private void calculateAppointmentStatus() {

        appointmentStatus.clear();

        appointmentStatus.put("COMPLETED", 0);
        appointmentStatus.put("CANCELLED", 0);

        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 7) {
                continue;
            }

            String dateText = parts[3].trim();
            String status = parts[5].trim();

            LocalDate date;

            try {
                date = LocalDate.parse(dateText);
            } catch (Exception e) {
                continue;
            }

            String recordMonth =
                    date.format(
                            DateTimeFormatter.ofPattern("yyyy-MM")
                    );

            if (!recordMonth.equals(month)) {
                continue;
            }

            if (status.equalsIgnoreCase("COMPLETED")) {

                appointmentStatus.merge(
                        "COMPLETED",
                        1,
                        Integer::sum
                );

            } else if (status.equalsIgnoreCase("CANCELLED")) {

                appointmentStatus.merge(
                        "CANCELLED",
                        1,
                        Integer::sum
                );
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * DOCTOR WORKLOAD
     * ---------------------------------------------------------
     *
     * Counts non-cancelled appointments assigned to each doctor.
     */
    private void calculateDoctorWorkload() {

        doctorWorkload.clear();

        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        List<User> users =
                UserRepository.loadAll();

        Map<String, String> doctorNames =
                new HashMap<>();

        for (User user : users) {

            if (user == null) {
                continue;
            }

            if (user.getRole() == null) {
                continue;
            }

            if (!user.getRole()
                    .name()
                    .equalsIgnoreCase("DOCTOR")) {
                continue;
            }

            doctorNames.put(
                    user.getUserId(),
                    user.getFullName()
            );
        }

        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 7) {
                continue;
            }

            String doctorId = parts[2].trim();
            String dateText = parts[3].trim();
            String status = parts[5].trim();

            LocalDate date;

            try {
                date = LocalDate.parse(dateText);
            } catch (Exception e) {
                continue;
            }

            String recordMonth =
                    date.format(
                            DateTimeFormatter.ofPattern("yyyy-MM")
                    );

            if (!recordMonth.equals(month)) {
                continue;
            }

            if (status.equalsIgnoreCase("CANCELLED")) {
                continue;
            }

            String doctorName =
                    doctorNames.get(doctorId);

            if (doctorName == null || doctorName.isEmpty()) {
                doctorName = doctorId;
            }

            doctorWorkload.merge(
                    doctorName,
                    1,
                    Integer::sum
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * WEEKLY APPOINTMENT VOLUME
     * ---------------------------------------------------------
     *
     * Weeks start on SUNDAY.
     *
     * Example:
     *
     * 2026-09-06 -> Sep 6
     * 2026-09-07 -> Sep 6
     * 2026-09-12 -> Sep 6
     *
     * 2026-09-13 -> Sep 13
     * 2026-09-14 -> Sep 13
     *
     * CANCELLED appointments are excluded.
     */
    private void calculateWeeklyAppointmentVolume() {

        weeklyAppointmentVolume.clear();

        List<String> bookingRecords =
                FileManager.readLines("bookings.txt");

        for (int i = 1; i < bookingRecords.size(); i++) {

            String record = bookingRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 7) {
                continue;
            }

            String dateText = parts[3].trim();
            String status = parts[5].trim();

            LocalDate date;

            try {
                date = LocalDate.parse(dateText);
            } catch (Exception e) {
                continue;
            }

            String recordMonth =
                    date.format(
                            DateTimeFormatter.ofPattern("yyyy-MM")
                    );

            if (!recordMonth.equals(month)) {
                continue;
            }

            if (status.equalsIgnoreCase("CANCELLED")) {
                continue;
            }

            /*
             * Find the Sunday at the beginning of
             * the appointment's week.
             */
            LocalDate sunday =
                    date.with(
                            TemporalAdjusters.previousOrSame(
                                    DayOfWeek.SUNDAY
                            )
                    );

            weeklyAppointmentVolume.merge(
                    sunday,
                    1,
                    Integer::sum
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * CONSULTATION RATE RANGE
     * ---------------------------------------------------------
     *
     * Finds:
     * - smallest MIN_RATE
     * - largest MAX_RATE
     *
     * Fallback:
     * RM100 - RM250
     */
    private double[] getRateRange() {

        double minimumRate = Double.MAX_VALUE;
        double maximumRate = Double.MIN_VALUE;

        List<String> rateRecords =
                FileManager.readLines(
                        "consultation_rates.txt"
                );

        for (int i = 1; i < rateRecords.size(); i++) {

            String record = rateRecords.get(i);

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    ManageRecordsHelper.splitRecord(record);

            if (parts.length < 6) {
                continue;
            }

            try {

                double minRate =
                        Double.parseDouble(
                                parts[2].trim()
                        );

                double maxRate =
                        Double.parseDouble(
                                parts[3].trim()
                        );

                if (minRate < minimumRate) {
                    minimumRate = minRate;
                }

                if (maxRate > maximumRate) {
                    maximumRate = maxRate;
                }

            } catch (NumberFormatException e) {
                // Ignore invalid rate records.
            }
        }

        if (minimumRate == Double.MAX_VALUE
                || maximumRate == Double.MIN_VALUE) {

            minimumRate = 100.0;
            maximumRate = 250.0;
        }

        return new double[]{
                minimumRate,
                maximumRate
        };
    }

    /*
     * ---------------------------------------------------------
     * GENERATE APPOINTMENT FEE
     * ---------------------------------------------------------
     *
     * The booking ID is used as the seed so the same
     * appointment always produces the same fee.
     */
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

    /*
     * ---------------------------------------------------------
     * GETTERS
     * ---------------------------------------------------------
     */

    public String getMonth() {
        return month;
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

    public Map<String, Integer> getDepartmentAppointments() {
        return departmentAppointments;
    }

    public Map<String, Double> getMonthlyRevenue() {
        return monthlyRevenue;
    }

    public Map<String, Integer> getAppointmentStatus() {
        return appointmentStatus;
    }

    public Map<String, Integer> getDoctorWorkload() {
        return doctorWorkload;
    }

    public Map<LocalDate, Integer> getWeeklyAppointmentVolume() {
        return weeklyAppointmentVolume;
    }
}