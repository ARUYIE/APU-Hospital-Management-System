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

    private static final String PROJECT_FOLDER_NAME = "APU_HMS";

    // makes it so netbeans and vsc can both find the file path for data folder
    private static Path projectRoot() {
        String userDir = System.getProperty("user.dir");
        Path start = Paths.get(userDir).toAbsolutePath();

        Path found = findProjectRoot(start);
        if (found != null) {
            return found;
        }

        try {
            Path classLocation = Paths.get(
                    FileManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
            found = findProjectRoot(classLocation);
            if (found != null) {
                return found;
            }
        } catch (Exception e) {
            System.err.println("Could not resolve project root from class location: " + e.getMessage());
        }

        Path byName = findAncestorNamed(start, PROJECT_FOLDER_NAME);
        if (byName != null) {
            return byName;
        }

        Path childProject = findChildProjectDir(start);
        if (childProject != null) {
            return childProject;
        }

        System.err.println("Could not locate project root (folder containing 'src'); "
                + "using current directory instead: " + start);
        return start;
    }

    private static Path findProjectRoot(Path from) {
        Path candidate = from;
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve("src"))) {
                return candidate;
            }
            Path projectDir = candidate.resolve(PROJECT_FOLDER_NAME);
            if (Files.isDirectory(projectDir) && Files.isDirectory(projectDir.resolve("src"))) {
                return projectDir;
            }
            candidate = candidate.getParent();
        }
        return null;
    }

    private static Path findChildProjectDir(Path from) {
        Path projectDir = from.resolve(PROJECT_FOLDER_NAME);
        if (Files.isDirectory(projectDir) && Files.isDirectory(projectDir.resolve("src"))) {
            return projectDir;
        }
        return null;
    }

    private static Path findAncestorNamed(Path from, String folderName) {
        Path candidate = from;
        while (candidate != null) {
            Path nameElement = candidate.getFileName();
            if (nameElement != null && nameElement.toString().equalsIgnoreCase(folderName)) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return null;
    }

    // Makes sure the data/ directory exists before any read/write happens. 
    private static void ensureDataDir() {
        try {
            Files.createDirectories(projectRoot().resolve(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Could not create data directory: " + e.getMessage());
        }
    }

    private static Path pathFor(String fileName) {
        ensureDataDir();
        return projectRoot().resolve(DATA_DIR).resolve(fileName);
    }

    //Reads every non-blank line of a data file. Returns an empty list if the file doesn't exist. 
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

    // Appends a single line to the given data file (creating it if necessary). 
    public static void appendLine(String fileName, String line) {
        Path path = pathFor(fileName);

        try {
            boolean fileExists = Files.exists(path);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                // Create headers when the file is created for the first time
                 if (!fileExists) {

                    switch (fileName.toLowerCase()) {

                        case "report.txt":
                            writer.write(
                                    "REPORT_PERIOD|"
                                    + "TOTAL_PATIENTS|"
                                    + "APPOINTMENTS|"
                                    + "COMPLETED|"
                                    + "CANCELLED|"
                                    + "TOTAL_REVENUE"
                            );
                            writer.newLine();
                            break;

                        case "roster.txt":
                            writer.write(
                                    "ROSTER_ID|"
                                    + "DOCTOR_NAME|"
                                    + "MANAGED_BY|"
                                    + "DEPARTMENT|"
                                    + "DATE|"
                                    + "SHIFT|"
                                    + "STATUS"
                            );
                            writer.newLine();
                            break;

                        case "bookings.txt":
                            writer.write(
                                    "BOOK_ID|"
                                    + "PATIENT_ID|"
                                    + "DOCTOR_ID|"
                                    + "CONSULTATION_DATE|"
                                    + "CONSULTATION_TIME|"
                                    + "STATUS|"
                                    + "NOTES"
                            );
                            writer.newLine();
                            break;

                        case "consultation_rates.txt":
                            writer.write(
                                    "SPECIALTY|"
                                    + "BASE_RATE|"
                                    + "MIN_RATE|"
                                    + "MAX_RATE|"
                                    + "CURRENCY|"
                                    + "EFFECTIVE_DATE"
                            );
                            writer.newLine();
                            break;
                    }
                }

                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println(
                    "Error writing " + fileName + ": " + e.getMessage()
            );
        }
    }

    // Overwrites the whole data file with the given lines (used for updates/deletes). 
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
