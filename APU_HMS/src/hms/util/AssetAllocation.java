package hms.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents an allocation of an asset to a user/department
 */
public class AssetAllocation {
    
    private String allocationId;
    private String assetId;
    private String userId; // Doctor, patient, department staff
    private String departmentId;
    private String purpose; // Consultation, treatment, surgery, lab test, etc.
    private LocalDateTime allocationStart;
    private LocalDateTime allocationEnd;
    private String status; // ALLOCATED, COMPLETED, CANCELLED
    private String notes;
    
    // Constructor for new allocation
    public AssetAllocation(String allocationId, String assetId, String userId,
                          String departmentId, String purpose, LocalDateTime start, LocalDateTime end) {
        this.allocationId = allocationId;
        this.assetId = assetId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.purpose = purpose;
        this.allocationStart = start;
        this.allocationEnd = end;
        this.status = "ALLOCATED";
        this.notes = "";
    }
    
    // Constructor for loading from file
    public AssetAllocation(String allocationId, String assetId, String userId,
                          String departmentId, String purpose, String startStr, String endStr,
                          String status, String notes) {
        this.allocationId = allocationId;
        this.assetId = assetId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.purpose = purpose;
        this.status = status;
        this.notes = notes;
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            this.allocationStart = LocalDateTime.parse(startStr, formatter);
            this.allocationEnd = LocalDateTime.parse(endStr, formatter);
        } catch (Exception e) {
            this.allocationStart = LocalDateTime.now();
            this.allocationEnd = LocalDateTime.now().plusHours(1);
        }
    }
    
    // Convert to file format (pipe-separated)
    public String toFileLine() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return String.join("|",
                allocationId,
                assetId,
                userId,
                departmentId,
                purpose,
                allocationStart.format(formatter),
                allocationEnd.format(formatter),
                status,
                notes
        );
    }
    
    // ---- Getters and Setters ----
    
    public String getAllocationId() { return allocationId; }
    public String getAssetId() { return assetId; }
    public String getUserId() { return userId; }
    public String getDepartmentId() { return departmentId; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getAllocationStart() { return allocationStart; }
    public LocalDateTime getAllocationEnd() { return allocationEnd; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setAllocationEnd(LocalDateTime end) { this.allocationEnd = end; }
    
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return "ALLOCATED".equalsIgnoreCase(status) && 
               now.isAfter(allocationStart) && now.isBefore(allocationEnd);
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(allocationEnd);
    }
    
    @Override
    public String toString() {
        return String.format("[%s] Asset: %s | User: %s | Purpose: %s | Status: %s | From: %s To: %s",
                allocationId, assetId, userId, purpose, status, allocationStart, allocationEnd);
    }
}
