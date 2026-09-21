package hms.util;

import java.util.List;

// automatically generates the id for the next created user
public final class IDGenerator {

    public static String next(String prefix, String fileName) {
        List<String> lines = FileManager.readLines(fileName);
        int highestNumber = 0;
        String normalizedPrefix = prefix.toUpperCase();
        for (String line : lines) {
            String[] fields = line.split("\\|", -1);
            if (fields.length == 0 || !fields[0].toUpperCase().startsWith(normalizedPrefix)) {
                continue;
            }
            String suffix = fields[0].substring(normalizedPrefix.length());
            try {
                highestNumber = Math.max(highestNumber, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // Ignore header rows and IDs with a different format.
            }
        }
        int nextNumber = highestNumber + 1;
        return String.format("%s%03d", prefix, nextNumber);
    }
}
