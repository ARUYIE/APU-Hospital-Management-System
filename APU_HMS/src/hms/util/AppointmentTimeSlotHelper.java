package hms.util;

import java.awt.Color;
import java.awt.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

public class AppointmentTimeSlotHelper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    public static void populateAvailableSlots(JComboBox<String> timeComboBox, String doctorNameOrId, String selectedDate) {
        timeComboBox.removeAllItems();

        if (doctorNameOrId == null || doctorNameOrId.trim().isEmpty() || selectedDate == null || selectedDate.trim().isEmpty()) {
            return;
        }

        // Search time from roster.xtt
        String shiftRange = null;
        List<String> rosterLines = FileManager.readLines("roster.txt");
        for (int i = 1; i < rosterLines.size(); i++) {
            String line = rosterLines.get(i);
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = ManageRecordsHelper.splitRecord(line);
            if (parts.length >= 6) {
                String docName = parts[1].trim();
                String rosterDate = parts[4].trim();
                String shift = parts[5].trim(); // e.g., "09:30 - 17:00"

                if ((docName.equalsIgnoreCase(doctorNameOrId) || ManageRecordsHelper.findName(docName).equalsIgnoreCase(doctorNameOrId))
                        && rosterDate.equals(selectedDate)) {
                    shiftRange = shift;
                    break;
                }
            }
        }

        if (shiftRange == null || !shiftRange.contains("-")) {
            timeComboBox.addItem("No shift scheduled for this date");
            return;
        }

        String[] times = shiftRange.split("-");
        try {
            LocalTime startTime = LocalTime.parse(times[0].trim(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(times[1].trim(), TIME_FORMATTER);

            // Collect time slot from booking.txt
            List<String> bookedTimes = new ArrayList<>();
            List<String> bookingLines = FileManager.readLines("bookings.txt");
            for (int i = 1; i < bookingLines.size(); i++) {
                String line = bookingLines.get(i);
                if (line == null || line.trim().isEmpty()) continue;
                String[] parts = ManageRecordsHelper.splitRecord(line);
                if (parts.length >= 6) {
                    String bDoc = parts[2].trim();
                    String bDate = parts[3].trim();
                    String bTime = parts[4].trim();
                    String bStatus = parts[5].trim();

                    if ((bDoc.equalsIgnoreCase(doctorNameOrId) || ManageRecordsHelper.findName(bDoc).equalsIgnoreCase(doctorNameOrId))
                            && bDate.equals(selectedDate)
                            && !bStatus.equalsIgnoreCase("CANCELLED")) {
                        bookedTimes.add(bTime);
                    }
                }
            }

            // Generate 30-minute slots
            List<SlotItem> slotItems = new ArrayList<>();
            LocalTime current = startTime;
            while (current.plusMinutes(30).compareTo(endTime) <= 0) {
                String slotStr = current.format(TIME_FORMATTER);
                boolean isBooked = bookedTimes.contains(slotStr);
                slotItems.add(new SlotItem(slotStr, isBooked));
                current = current.plusMinutes(30);
            }

            for (SlotItem item : slotItems) {
                timeComboBox.addItem(item.toString());
            }

            //custom cell renderer to grey out booked slots
            timeComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value != null && value.toString().contains("(Booked)")) {
                        if (!isSelected) {
                            c.setBackground(Color.LIGHT_GRAY);
                            c.setForeground(Color.DARK_GRAY);
                        }
                        setEnabled(false);
                    } else {
                        if (!isSelected) {
                            c.setBackground(Color.WHITE);
                            c.setForeground(Color.BLACK);
                        }
                        setEnabled(true);
                    }
                    return c;
                }
            });

        } catch (Exception e) {
            timeComboBox.addItem("Invalid shift format");
        }
    }

    private static class SlotItem {
        private final String time;
        private final boolean booked;

        public SlotItem(String time, boolean booked) {
            this.time = time;
            this.booked = booked;
        }

        @Override
        public String toString() {
            return booked ? time + " (Booked)" : time;
        }
    }
}