package hms.util;

import java.util.List;

// automatically generates the id for the next created user
public final class IDGenerator {

    private IDGenerator() { }

    /**
     * @param prefix   short prefix for the entity, e.g. "U", "W", "D", "A", "B"
     * @param fileName the data file whose existing lines determine the next number
     * @return a new ID like "U001", "U002", ...
     */
    public static String next(String prefix, String fileName) {
        List<String> lines = FileManager.readLines(fileName);
        int nextNumber = lines.size() + 1;
        return String.format("%s%03d", prefix, nextNumber);
    }
}
