package hms.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a physical hospital asset (consultation room, ward, lab, etc.)
 */
public class Asset {

    private String assetId;
    private AssetType assetType;
    private String name;
    private String location;
    private int capacity; // legacy field 
    private String status; // AVAILABLE, UNAVAILABLE
    private String department; // legacy field 
    private String description;
    private LocalDateTime createdDate;
    
    // Constructor for new assets
    public Asset(String assetId, AssetType assetType, String name, String location,
                 int capacity, String department, String description) {
        this.assetId = assetId;
        this.assetType = assetType;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.status = "AVAILABLE";
        this.department = department;
        this.description = description;
        this.createdDate = LocalDateTime.now();
    }

    public Asset(String assetId, AssetType assetType, String name, String location, String description) {
        this(assetId, assetType, name, location, 0, "", description);
        this.status = "AVAILABLE";
    }
    
    // Constructor for loading from file
    public Asset(String assetId, String assetTypeStr, String name, String location,
                 int capacity, String status, String department, String description, String createdDateStr) {
        this.assetId = assetId;
        this.assetType = AssetType.fromString(assetTypeStr);
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.status = status;
        this.department = department;
        this.description = description;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            this.createdDate = LocalDateTime.parse(createdDateStr, formatter);
        } catch (Exception e) {
            this.createdDate = LocalDateTime.now();
        }
    }

    public Asset(String assetId, String assetTypeStr, String name, String location,
                 String status, String description) {
        this(assetId, assetTypeStr, name, location, 0, status, "", description, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
    
    // Convert to file format (pipe-separated)
    public String toFileLine() {
        return String.join("|",
                assetId,
                assetType.name(),
                name,
                location,
                status,
                description
        );
    }
    
    // ---- Getters and Setters ----
    
    public String getAssetId() { return assetId; }
    public AssetType getAssetType() { return assetType; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public String getDepartment() { return department; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setStatus(String status) { this.status = status; }
    public void setDepartment(String department) { this.department = department; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }
    
    public boolean isInMaintenance() {
        return "MAINTENANCE".equalsIgnoreCase(status);
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%s) | Capacity: %d | Status: %s",
                assetId, assetType.getDisplayName(), name, location, capacity, status);
    }
}
