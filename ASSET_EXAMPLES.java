// HOSPITAL ASSET MANAGEMENT SYSTEM - PRACTICAL USAGE EXAMPLES
// Use these code snippets as reference for common operations

// ============================================================================
// EXAMPLE 1: ADMIN SETUP - Adding New Assets to the Hospital
// ============================================================================

// Add multiple consultation rooms
for (int i = 1; i <= 5; i++) {
    String roomId = AssetManager.createAsset(
        AssetType.CONSULTATION_ROOM,
        "Consultation Room " + i,
        "Building A, Floor 2",
        1,
        "General Medicine",
        "Room equipped with standard diagnostic equipment"
    );
    System.out.println("Created room: " + roomId);
}

// Add imaging/X-ray rooms
AssetManager.createAsset(
    AssetType.IMAGING_ROOM,
    "X-Ray Lab 1",
    "Building B, Basement",
    1,
    "Radiology",
    "Digital X-ray system with PACS integration"
);

// Add laboratory spaces
AssetManager.createAsset(
    AssetType.LAB,
    "Hematology Lab",
    "Building C, Floor 1",
    3,
    "Pathology",
    "Blood testing and analysis facility"
);

// Add inpatient wards
AssetManager.createAsset(
    AssetType.INPATIENT_WARD,
    "Ward A - Cardiology",
    "Building A, Floor 3",
    20,
    "Cardiology",
    "20-bed ward dedicated to cardiac patients"
);

// ============================================================================
// EXAMPLE 2: DOCTOR WORKFLOW - Patient Consultation
// ============================================================================

// Doctor needs to book a consultation room for a patient
String doctorId = "DOC001";
String patientId = "PAT001";

// Check available consultation rooms
List<Asset> availableRooms = AssetManager.getAvailableAssets(AssetType.CONSULTATION_ROOM);
System.out.println("Available rooms: " + availableRooms.size());

if (!availableRooms.isEmpty()) {
    Asset room = availableRooms.get(0);
    
    // Allocate the room for 30 minutes
    String allocationId = AssetAllocationManager.allocateAsset(
        room.getAssetId(),
        doctorId,
        "GENERAL_MEDICINE",
        "Patient Consultation - " + patientId,
        LocalDateTime.now(),
        LocalDateTime.now().plusMinutes(30)
    );
    
    System.out.println("Room allocated: " + allocationId);
    System.out.println("Room status is now: " + 
        AssetManager.getAsset(room.getAssetId()).getStatus());
    
    // ... Conduct consultation ...
    
    // Release the room when done
    AssetAllocationManager.releaseAsset(allocationId);
    System.out.println("Room released and available for next patient");
}

// ============================================================================
// EXAMPLE 3: LABORATORY TEST BOOKING WITH REQUESTS
// ============================================================================

// Doctor requests a lab test
String testRequestId = AssetRequestManager.createRequest(
    AssetType.LAB.name(),
    "DOC002",
    "PATHOLOGY",
    "Complete blood count and liver function test for patient PAT002",
    "HIGH"  // Urgent test
);

System.out.println("Test request created: " + testRequestId);

// Lab manager reviews and approves the request
List<AssetRequest> pendingRequests = AssetRequestManager.getPendingRequests();
for (AssetRequest req : pendingRequests) {
    if (req.getPriority().equals("HIGH")) {
        // Find an available lab
        List<Asset> availableLabs = AssetManager.getAvailableAssets(AssetType.LAB);
        
        if (!availableLabs.isEmpty()) {
            Asset lab = availableLabs.get(0);
            
            // Approve and allocate the test slot
            boolean success = AssetRequestManager.approveAndAllocate(
                req.getRequestId(),
                lab.getAssetId(),
                LocalDateTime.now().plusHours(2).toString(),
                LocalDateTime.now().plusHours(3).toString()
            );
            
            if (success) {
                System.out.println("Lab test approved and scheduled!");
            }
        } else {
            // Reject if no labs available
            AssetRequestManager.rejectRequest(
                req.getRequestId(),
                "No available lab slots. Please schedule later."
            );
        }
    }
}

// ============================================================================
// EXAMPLE 4: CONFLICT DETECTION - Preventing Double Booking
// ============================================================================

String roomId = "ASSET001";
LocalDateTime requestedStart = LocalDateTime.now().plusHours(3);
LocalDateTime requestedEnd = requestedStart.plusMinutes(30);

// Check if there's a conflict
boolean hasConflict = AssetAllocationManager.hasConflict(roomId, requestedStart, requestedEnd);

if (hasConflict) {
    System.out.println("ERROR: Room is already booked during this time!");
    
    // Show doctor available slots for that day
    List<String> availableSlots = AssetAllocationManager.getAvailableSlots(
        roomId,
        requestedStart
    );
    System.out.println("Available time slots: " + availableSlots);
} else {
    // Safe to book
    String allocationId = AssetAllocationManager.allocateAsset(
        roomId,
        "DOC003",
        "GENERAL_MEDICINE",
        "Consultation",
        requestedStart,
        requestedEnd
    );
    System.out.println("Booking confirmed: " + allocationId);
}

// ============================================================================
// EXAMPLE 5: ASSET MAINTENANCE - Taking Assets Out of Service
// ============================================================================

// Asset needs maintenance
String assetToMaintain = "ASSET005";
Asset asset = AssetManager.getAsset(assetToMaintain);

// Set status to MAINTENANCE
AssetManager.updateAssetStatus(assetToMaintain, "MAINTENANCE");
System.out.println(asset.getName() + " is now under maintenance");

// Cancel any active allocations
List<AssetAllocation> activeAllocations = 
    AssetAllocationManager.getActiveAllocations(assetToMaintain);

for (AssetAllocation alloc : activeAllocations) {
    AssetAllocationManager.cancelAllocation(alloc.getAllocationId());
}

System.out.println("All active allocations cancelled due to maintenance");

// ... Perform maintenance ...

// Put asset back in service
AssetManager.updateAssetStatus(assetToMaintain, "AVAILABLE");
System.out.println(asset.getName() + " is back in service");

// ============================================================================
// EXAMPLE 6: REPORTING - Asset Utilization Statistics
// ============================================================================

// Get total assets by type
System.out.println("=== ASSET INVENTORY ===");
for (AssetType type : AssetType.values()) {
    List<Asset> assets = AssetManager.getAssetsByType(type);
    System.out.println(type.getDisplayName() + ": " + assets.size());
}

// Get asset availability
System.out.println("\n=== AVAILABILITY STATUS ===");
System.out.println("Available consultation rooms: " + 
    AssetManager.getAvailableCount(AssetType.CONSULTATION_ROOM));
System.out.println("Available labs: " + 
    AssetManager.getAvailableCount(AssetType.LAB));
System.out.println("Assets in maintenance: " + 
    AssetManager.getMaintenanceCount());

// Get allocation statistics
System.out.println("\n=== ALLOCATION STATISTICS ===");
System.out.println("Current active allocations: " + 
    AssetAllocationManager.getActiveAllocationCount());

// Get requests pending approval
System.out.println("\n=== PENDING REQUESTS ===");
List<AssetRequest> pending = AssetRequestManager.getPendingRequests();
System.out.println("Total pending requests: " + pending.size());

for (AssetRequest req : AssetRequestManager.getHighPriorityRequests()) {
    System.out.println(" - HIGH: " + req.getPurpose() + " (User: " + 
        req.getUserId() + ")");
}

// ============================================================================
// EXAMPLE 7: DEPARTMENT SPECIFIC OPERATIONS
// ============================================================================

String departmentName = "Cardiology";

// Get all assets managed by Cardiology department
List<Asset> cardiologyAssets = AssetManager.getAssetsByDepartment(departmentName);
System.out.println("Cardiology department has " + cardiologyAssets.size() + " assets");

for (Asset asset : cardiologyAssets) {
    System.out.println(" - " + asset.getName() + 
        " (" + asset.getStatus() + ")");
}

// Get all allocations for Cardiology department
List<AssetAllocation> deptAllocations = 
    AssetAllocationManager.getAllocationsByDepartment(departmentName);

System.out.println("\nCardiology allocations:");
for (AssetAllocation alloc : deptAllocations) {
    System.out.println(" - " + alloc.getPurpose() + 
        " (Status: " + alloc.getStatus() + ")");
}

// ============================================================================
// EXAMPLE 8: USER WORKFLOW - View Personal Allocations
// ============================================================================

String currentUserId = "DOC004";

// Get all allocations for current user (Doctor)
List<AssetAllocation> myAllocations = 
    AssetAllocationManager.getAllocationsForUser(currentUserId);

System.out.println("Your allocations:");
for (AssetAllocation alloc : myAllocations) {
    Asset asset = AssetManager.getAsset(alloc.getAssetId());
    System.out.println(" - Asset: " + asset.getName());
    System.out.println("   Purpose: " + alloc.getPurpose());
    System.out.println("   Time: " + alloc.getAllocationStart() + 
        " to " + alloc.getAllocationEnd());
    System.out.println("   Status: " + alloc.getStatus());
    System.out.println();
}

// Get all requests for current user
List<AssetRequest> myRequests = 
    AssetRequestManager.getRequestsForUser(currentUserId);

System.out.println("Your requests:");
for (AssetRequest req : myRequests) {
    System.out.println(" - " + req.getAssetTypeRequired() + 
        ": " + req.getPurpose());
    System.out.println("   Status: " + req.getStatus());
}

// ============================================================================
// EXAMPLE 9: UPDATING ASSET INFORMATION
// ============================================================================

// Get an asset
String assetId = "ASSET001";
Asset asset = AssetManager.getAsset(assetId);

if (asset != null) {
    // Make changes
    asset.setLocation("Building A, Floor 3");  // Relocated
    asset.setCapacity(2);  // Expanded to 2 seats
    asset.setDescription("Updated consultation room with new equipment");
    
    // Save changes
    AssetManager.updateAsset(asset);
    System.out.println("Asset updated successfully");
}

// ============================================================================
// EXAMPLE 10: ERROR HANDLING
// ============================================================================

try {
    // Try to allocate an asset that doesn't exist
    AssetAllocationManager.allocateAsset(
        "NONEXISTENT",
        "DOC005",
        "DEPT",
        "Test",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1)
    );
} catch (IllegalArgumentException e) {
    System.out.println("ERROR: " + e.getMessage());
    // Handle: Asset not found
}

try {
    // Try to allocate when asset is not available
    String assetId = "ASSET002";
    Asset asset = AssetManager.getAsset(assetId);
    
    if (!asset.isAvailable()) {
        throw new Exception("Asset is " + asset.getStatus());
    }
} catch (Exception e) {
    System.out.println("Cannot allocate: " + e.getMessage());
    // Show alternative options to user
}

// ============================================================================
// Tips for Integration:
// 1. Always check asset availability before attempting allocation
// 2. Handle exceptions for poor UX
// 3. Provide time slot suggestions when conflicts occur
// 4. Keep department assignments up-to-date
// 5. Regular maintenance schedule for assets
// 6. Review high-priority requests frequently
// 7. Archive completed allocations periodically for reporting
// ============================================================================
