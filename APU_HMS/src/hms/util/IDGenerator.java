package hms.util;

import java.util.List;

// automatically generates the id for the next created user
public final class IDGenerator {

    public static String next(String prefix, String fileName) {
        List<String> lines = FileManager.readLines(fileName);
        int nextNumber = lines.size() + 1;
        return String.format("%s%03d", prefix, nextNumber);
    }
}
