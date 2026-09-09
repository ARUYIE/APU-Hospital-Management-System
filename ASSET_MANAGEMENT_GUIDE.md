# Hospital Asset Management System - Developer Guide

## Overview

This asset management system allows hospitals to efficiently manage and allocate physical assets such as consultation rooms, inpatient wards, labs, X-ray/imaging rooms, operation theatres, pharmacies, and medical equipment.

## System Architecture

### Core Components

#### 1. **AssetType.java** (Enum)
Defines all available asset types in the hospital system:
- `CONSULTATION_ROOM` - Doctor consultation spaces
- `INPATIENT_WARD` - Patient wards with beds
- `LAB` - Laboratory facilities
- `IMAGING_ROOM` - X-Ray and imaging centers
- `OPERATION_THEATRE` - Surgical operating rooms
- `PHARMACY` - Medication dispensaries
- `EQUIPMENT` - Mobile/reusable medical equipment

#### 2. **Asset.java** (Model)
Represents a physical hospital asset with properties:
- `assetId` - Unique identifier (auto-generated)
- `assetType` - Type of asset
- `name` - Asset display name
- `location` - Building/floor location
- `capacity` - Number of beds, slots, or units (for sizing)
- `status` - Current status (AVAILABLE, OCCUPIED, MAINTENANCE, OUT_OF_SERVICE)
- `department` - Department that owns/manages the asset
- `description` - Additional details
- `createdDate` - Date asset was added to system

#### 3. **AssetAllocation.java** (Model)
Tracks assignment of assets to users/departments:
- `allocationId` - Unique allocation identifier
- `assetId` - Which asset is allocated
- `userId` - Doctor/staff member using it
- `departmentId` - Department responsible
- `purpose` - Reason for allocation (consultation, surgery, lab test, etc.)
- `allocationStart` - Start time of use
- `allocationEnd` - End time of use
- `status` - Status (ALLOCATED, COMPLETED, CANCELLED)
- `notes` - Additional notes

#### 4. **AssetRequest.java** (Model)
Represents a formal request for asset allocation:
- `requestId` - Unique request identifier
- `assetTypeRequired` - Type of asset needed
- `userId` - Person requesting
- `departmentId` - Department
- `purpose` - Intended use
- `priority` - Request urgency (LOW, MEDIUM, HIGH)
- `status` - Status (PENDING, APPROVED, REJECTED, FULFILLED)
- `notes` - Admin notes/rejection reason

#### 5. **AssetManager.java** (Utility)
CRUD operations for assets. Key methods:

```java
// Create a new asset
String assetId = AssetManager.createAsset(
    AssetType.CONSULTATION_ROOM,
    "Room A1",
    "Building 1, Floor 2",
    1,
    "Cardiology",
    "Modern consultation room with equipment"
);

// Retrieve assets
Asset asset = AssetManager.getAsset(assetId);
List<Asset> all = AssetManager.getAllAssets();
List<Asset> byType = AssetManager.getAssetsByType(AssetType.LAB);
List<Asset> available = AssetManager.getAvailableAssets(AssetType.CONSULTATION_ROOM);
List<Asset> byDept = AssetManager.getAssetsByDepartment("Cardiology");

// Update asset
asset.setStatus("MAINTENANCE");
AssetManager.updateAsset(asset);
AssetManager.updateAssetStatus(assetId, "AVAILABLE");

// Delete
AssetManager.deleteAsset(assetId);

// Statistics
int availableCount = AssetManager.getAvailableCount(AssetType.CONSULTATION_ROOM);
int maintenanceCount = AssetManager.getMaintenanceCount();
```

#### 6. **AssetAllocationManager.java** (Utility)
Manages booking and allocation of assets. Key methods:

```java
// Allocate an asset
String allocationId = AssetAllocationManager.allocateAsset(
    "ASSET001",           // Asset ID
    "DOC001",             // Doctor/User ID
    "CARDIOLOGY",         // Department
    "Patient Consultation",
    LocalDateTime.parse("2024-09-01T09:00:00"),
    LocalDateTime.parse("2024-09-01T10:00:00")
);

// Retrieve allocations
AssetAllocation allocation = AssetAllocationManager.getAllocation(allocationId);
List<AssetAllocation> assetAllocations = AssetAllocationManager.getAllocationsForAsset("ASSET001");
List<AssetAllocation> active = AssetAllocationManager.getActiveAllocations("ASSET001");
List<AssetAllocation> userAllocations = AssetAllocationManager.getAllocationsForUser("DOC001");

// Release or cancel
AssetAllocationManager.releaseAsset(allocationId);        // Complete allocation
AssetAllocationManager.cancelAllocation(allocationId);     // Cancel booking

// Conflict checking
boolean hasConflict = AssetAllocationManager.hasConflict(
    "ASSET001",
    startTime,
    endTime
);

// Available time slots for scheduling
List<String> slots = AssetAllocationManager.getAvailableSlots("ASSET001", date);

// Statistics
int activeCount = AssetAllocationManager.getActiveAllocationCount();
```

#### 7. **AssetRequestManager.java** (Utility)
Manages formal asset requests. Key methods:

```java
// Create a request
String requestId = AssetRequestManager.createRequest(
    AssetType.LAB.name(),
    "DOC002",           // Doctor requesting
    "LABORATORY",       // Department
    "Blood test needed",
    "HIGH"              // Priority
);

// Retrieve requests
AssetRequest req = AssetRequestManager.getRequest(requestId);
List<AssetRequest> pending = AssetRequestManager.getPendingRequests();
List<AssetRequest> userReqs = AssetRequestManager.getRequestsForUser("DOC002");
List<AssetRequest> highPriority = AssetRequestManager.getHighPriorityRequests();

// Approve and allocate
boolean approved = AssetRequestManager.approveAndAllocate(
    requestId,
    "ASSET002",  // Asset to allocate
    "2024-09-01T14:00:00",
    "2024-09-01T15:00:00"
);

// Reject request
AssetRequestManager.rejectRequest(requestId, "No available assets currently");
```

## Data Storage

All data is persisted in text files in the `data/` folder:

| File | Purpose |
|------|---------|
| `hospital_assets.txt` | Asset inventory |
| `asset_allocations.txt` | Asset bookings/usage |
| `asset_requests.txt` | Formal requests |

**File Format**: Pipe-separated values (pipe character `|` separates fields)

Example asset line:
```
ASSET001|CONSULTATION_ROOM|Room A1|Building 1, Floor 2|1|AVAILABLE|Cardiology|Modern consultation room|2024-09-01T08:00:00
```

## UI Components

### ManageAssetsPanel.java
Administrative panel for managing the entire asset inventory:
- **Features**:
  - List all assets in table format
  - Filter by asset type
  - Filter by status
  - Add new assets
  - Edit existing assets
  - Delete assets
  - View allocations for each asset
  - Statistics display

**Usage**:
```java
ManageAssetsPanel panel = new ManageAssetsPanel();
// Add to frame or dialog
```

### RequestAssetPanel.java
User-facing panel for doctors/staff to request and allocate assets:
- **Features**:
  - Submit asset requests with priority
  - View personal requests and their status
  - Browse available assets by type
  - Quick allocation with time slots
  - Auto-refresh available assets

**Usage**:
```java
RequestAssetPanel panel = new RequestAssetPanel(userId);
// Add to frame or dialog
```

## Workflow Examples

### Example 1: Managing Consultation Rooms

```java
// Admin adds a consultation room
String roomId = AssetManager.createAsset(
    AssetType.CONSULTATION_ROOM,
    "Consultation Room A",
    "Building 1, Level 2",
    1,
    "General Practice",
    "Equipped with latest diagnostic tools"
);

// Doctor allocates the room for a patient
AssetAllocationManager.allocateAsset(
    roomId,
    "DOC001",
    "GENERAL_PRACTICE",
    "Patient Consultation - Mr. John Doe",
    LocalDateTime.now(),
    LocalDateTime.now().plusMinutes(30)
);

// After consultation is complete
AssetAllocationManager.releaseAsset(allocationId);  // Room becomes AVAILABLE again
```

### Example 2: Laboratory Booking Process

```java
// Doctor submits a lab request
String requestId = AssetRequestManager.createRequest(
    AssetType.LAB.name(),
    "DOC002",
    "PATHOLOGY",
    "Complete blood count for patient XYZ",
    "HIGH"
);

// Lab manager approves and allocates a lab slot
List<Asset> labs = AssetManager.getAvailableAssets(AssetType.LAB);
Asset lab = labs.get(0);

AssetRequestManager.approveAndAllocate(
    requestId,
    lab.getAssetId(),
    "2024-09-01T15:00:00",
    "2024-09-01T16:00:00"
);

// Request status is now APPROVED and asset is OCCUPIED
```

### Example 3: Checking Asset Availability

```java
// Get all available consultation rooms
List<Asset> available = AssetManager.getAvailableAssets(AssetType.CONSULTATION_ROOM);

// Check if a specific room has conflicts
boolean conflicted = AssetAllocationManager.hasConflict(
    "ASSET001",
    requestedStart,
    requestedEnd
);

// Get available time slots for the day
List<String> availableSlots = AssetAllocationManager.getAvailableSlots(
    "ASSET001",
    LocalDateTime.now()
);
```

## Integration with Existing System

### Adding to DashboardFrame

```java
// In DashboardFrame.java, add menu option:
JMenuItem manageAssetsItem = new JMenuItem("Manage Hospital Assets");
manageAssetsItem.addActionListener(e -> {
    ManageAssetsPanel panel = new ManageAssetsPanel();
    // Show in dialog or frame
});
```

### For Doctors/Staff

```java
// In appropriate user role class:
JMenuItem requestAssetItem = new JMenuItem("Request/Allocate Asset");
requestAssetItem.addActionListener(e -> {
    RequestAssetPanel panel = new RequestAssetPanel(currentUserId);
    // Show in dialog or frame
});
```

## Error Handling

Key exceptions to handle:

```java
try {
    AssetAllocationManager.allocateAsset(...);
} catch (IllegalArgumentException e) {
    // Asset doesn't exist
    System.out.println("Asset not found: " + e.getMessage());
} catch (IllegalStateException e) {
    // Asset not available or time conflict
    System.out.println("Cannot allocate: " + e.getMessage());
}
```

## Important Notes

1. **Status Transitions**:
   - AVAILABLE → OCCUPIED (when allocated)
   - OCCUPIED → AVAILABLE (when released, if no other active allocations)
   - Any status → MAINTENANCE (manual update)
   - Any status → OUT_OF_SERVICE (manual update)

2. **Conflict Detection**: The system automatically prevents double-booking by checking time overlaps

3. **Cascade Updates**: Releasing an allocation automatically updates the asset status if no other active allocations exist

4. **Request Workflow**: Requests must be approved before becoming allocations

5. **Time Format**: All date-times use ISO 8601 format: `YYYY-MM-DDTHH:MM:SS`

## Future Enhancements

- Asset maintenance scheduling
- Recurring allocations (recurring appointments)
- Asset condition tracking and history
- Analytics and utilization reports
- Integration with calendar systems
- Notification system for asset availability
- Multi-asset booking (allocate multiple assets together)
- Cost tracking by asset usage

---

**File Locations**:
- Utilities: `src/hms/util/`
- UI Panels: `src/hms/gui/panels/`
- Data Files: `data/`
