package hms.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages asset allocations (bookings/assignments)
 */
public class AssetAllocationManager {
    
    private static final String ALLOCATIONS_FILE = "asset_allocations.txt";
    
    private AssetAllocationManager() { }
    
    /**
     * Allocate an asset to a user
     */
    public static String allocateAsset(String assetId, String userId, String departmentId,
                                       String purpose, LocalDateTime start, LocalDateTime end) {
        // Check if asset exists
        Asset asset = AssetManager.getAsset(assetId);
        if (asset == null) {
            throw new IllegalArgumentException("Asset not found: " + assetId);
        }
        
        // Check if asset is available
        if (!asset.isAvailable()) {
            throw new IllegalStateException("Asset is not available: " + asset.getStatus());
        }
        
        // Check for conflicts
        if (hasConflict(assetId, start, end)) {
            throw new IllegalStateException("Asset already allocated during this time period");
        }
        
        String allocationId = IDGenerator.next("ALLOC", ALLOCATIONS_FILE);
        AssetAllocation allocation = new AssetAllocation(allocationId, assetId, userId,
                departmentId, purpose, start, end);
        
        List<String> lines = FileManager.readLines(ALLOCATIONS_FILE);
        lines.add(allocation.toFileLine());
        FileManager.writeAllLines(ALLOCATIONS_FILE, lines);
        
        // Update asset status to OCCUPIED
        AssetManager.updateAssetStatus(assetId, "OCCUPIED");
        
        return allocationId;
    }
    
    /**
     * Get allocation by ID
     */
    public static AssetAllocation getAllocation(String allocationId) {
        List<AssetAllocation> allocations = getAllAllocations();
        return allocations.stream()
                .filter(a -> a.getAllocationId().equals(allocationId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get all allocations
     */
    public static List<AssetAllocation> getAllAllocations() {
        List<String> lines = FileManager.readLines(ALLOCATIONS_FILE);
        return lines.stream()
                .map(line -> parseAllocation(line))
                .collect(Collectors.toList());
    }
    
    /**
     * Get allocations for a specific asset
     */
    public static List<AssetAllocation> getAllocationsForAsset(String assetId) {
        return getAllAllocations().stream()
                .filter(a -> a.getAssetId().equals(assetId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get active allocations for an asset
     */
    public static List<AssetAllocation> getActiveAllocations(String assetId) {
        return getAllocationsForAsset(assetId).stream()
                .filter(AssetAllocation::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get allocations for a specific user
     */
    public static List<AssetAllocation> getAllocationsForUser(String userId) {
        return getAllAllocations().stream()
                .filter(a -> a.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get allocations by department
     */
    public static List<AssetAllocation> getAllocationsByDepartment(String departmentId) {
        return getAllAllocations().stream()
                .filter(a -> a.getDepartmentId().equals(departmentId))
                .collect(Collectors.toList());
    }
    
    /**
     * Release/complete an allocation
     */
    public static boolean releaseAsset(String allocationId) {
        AssetAllocation allocation = getAllocation(allocationId);
        if (allocation == null) {
            return false;
        }
        
        allocation.setStatus("COMPLETED");
        updateAllocation(allocation);
        
        // Check if there are other active allocations for this asset
        List<AssetAllocation> activeAllocations = getActiveAllocations(allocation.getAssetId());
        if (activeAllocations.isEmpty()) {
            AssetManager.updateAssetStatus(allocation.getAssetId(), "AVAILABLE");
        }
        
        return true;
    }
    
    /**
     * Cancel an allocation
     */
    public static boolean cancelAllocation(String allocationId) {
        AssetAllocation allocation = getAllocation(allocationId);
        if (allocation == null) {
            return false;
        }
        
        allocation.setStatus("CANCELLED");
        updateAllocation(allocation);
        
        // Check if there are other active allocations for this asset
        List<AssetAllocation> activeAllocations = getActiveAllocations(allocation.getAssetId());
        if (activeAllocations.isEmpty()) {
            AssetManager.updateAssetStatus(allocation.getAssetId(), "AVAILABLE");
        }
        
        return true;
    }
    
    /**
     * Update allocation details
     */
    public static boolean updateAllocation(AssetAllocation allocation) {
        List<AssetAllocation> allocations = getAllAllocations();
        boolean found = false;
        
        for (int i = 0; i < allocations.size(); i++) {
            if (allocations.get(i).getAllocationId().equals(allocation.getAllocationId())) {
                allocations.set(i, allocation);
                found = true;
                break;
            }
        }
        
        if (found) {
            List<String> lines = allocations.stream()
                    .map(AssetAllocation::toFileLine)
                    .collect(Collectors.toList());
            FileManager.writeAllLines(ALLOCATIONS_FILE, lines);
        }
        
        return found;
    }
    
    /**
     * Check if an asset has a time conflict
     */
    public static boolean hasConflict(String assetId, LocalDateTime start, LocalDateTime end) {
        return getAllocationsForAsset(assetId).stream()
                .filter(a -> "ALLOCATED".equalsIgnoreCase(a.getStatus()))
                .anyMatch(a -> {
                    LocalDateTime allocStart = a.getAllocationStart();
                    LocalDateTime allocEnd = a.getAllocationEnd();
                    return !(end.isBefore(allocStart) || start.isAfter(allocEnd));
                });
    }
    
    /**
     * Get number of active allocations
     */
    public static int getActiveAllocationCount() {
        return (int) getAllAllocations().stream()
                .filter(AssetAllocation::isActive)
                .count();
    }
    
    /**
     * Get available time slots for an asset
     */
    public static List<String> getAvailableSlots(String assetId, LocalDateTime day) {
        List<String> slots = new ArrayList<>();
        List<AssetAllocation> allocations = getActiveAllocations(assetId);
        
        // Generate hourly slots for the day
        LocalDateTime dayStart = day.withHour(9).withMinute(0);
        LocalDateTime dayEnd = day.withHour(17).withMinute(0);
        
        for (LocalDateTime slot = dayStart; slot.isBefore(dayEnd); slot = slot.plusHours(1)) {
            LocalDateTime nextSlot = slot.plusHours(1);
            if (!hasConflict(assetId, slot, nextSlot)) {
                slots.add(slot.toString());
            }
        }
        
        return slots;
    }
    
    /**
     * Parse a line from the allocations file into an AssetAllocation object
     */
    private static AssetAllocation parseAllocation(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 9) {
            throw new IllegalArgumentException("Invalid allocation line format: " + line);
        }
        
        return new AssetAllocation(
                parts[0],  // allocationId
                parts[1],  // assetId
                parts[2],  // userId
                parts[3],  // departmentId
                parts[4],  // purpose
                parts[5],  // allocationStart
                parts[6],  // allocationEnd
                parts[7],  // status
                parts[8]   // notes
        );
    }
}
