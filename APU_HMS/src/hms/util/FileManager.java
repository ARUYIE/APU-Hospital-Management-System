package hms.util;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

// reads and writes the user's info into txt file
//readLines(file name)
public final class FileManager {

    public static final String DATA_DIR = "data";

    private FileManager() { }

    /** Makes sure the data/ directory exists before any read/write happens. */
    private static void ensureDataDir() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Could not create data directory: " + e.getMessage());
        }
    }

    private static Path pathFor(String fileName) {
        ensureDataDir();
        return Paths.get(DATA_DIR, fileName);
    }

    /** Reads every non-blank line of a data file. Returns an empty list if the file doesn't exist. */
    public static List<String> readLines(String fileName) {
        Path path = pathFor(fileName);
        List<String> lines = new ArrayList<>();
        if (!Files.exists(path)) {
            return lines; // no data yet - not an error
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + fileName + ": " + e.getMessage());
        }
        return lines;
    }

    /** Appends a single line to the given data file (creating it if necessary). */
    public static void appendLine(String fileName, String line) {
        Path path = pathFor(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(
                path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing " + fileName + ": " + e.getMessage());
        }
    }

    /** Overwrites the whole data file with the given lines (used for updates/deletes). */
    public static void writeAllLines(String fileName, List<String> lines) {
        Path path = pathFor(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(
                path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing " + fileName + ": " + e.getMessage());
        }
    }
}
