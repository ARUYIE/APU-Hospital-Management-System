# Quick Integration Guide - Hospital Asset Management System

## Files Created

### Core Utility Classes (in `src/hms/util/`)
1. **AssetType.java** - Enum defining asset types (Consultation Rooms, Wards, Labs, Imaging, etc.)
2. **Asset.java** - Model class representing a physical hospital asset
3. **AssetAllocation.java** - Model class for asset booking/allocation
4. **AssetRequest.java** - Model class for formal asset requests
5. **AssetManager.java** - CRUD operations and queries for assets
6. **AssetAllocationManager.java** - Manages asset allocations and conflict detection
7. **AssetRequestManager.java** - Manages formal asset requests and approvals

### UI Panels (in `src/hms/gui/panels/`)
1. **ManageAssetsPanel.java** - Admin panel for managing asset inventory
2. **RequestAssetPanel.java** - User panel for requesting and allocating assets

### Documentation
1. **ASSET_MANAGEMENT_GUIDE.md** - Complete system documentation
2. **ASSET_EXAMPLES.java** - Practical code examples and usage patterns

## Quick Start

### 1. Compile the Code
```bash
cd APU_HMS
mvn clean compile
```

### 2. Add Menu Items to Your Dashboard

In `src/hms/gui/DashboardFrame.java`, add:

```java
// For Admin Staff - Add to menu
JMenuItem manageAssetsItem = new JMenuItem("Manage Hospital Assets");
manageAssetsItem.addActionListener(e -> {
    ManageAssetsPanel panel = new ManageAssetsPanel();
    // Create and show in a dialog or new frame
    JFrame frame = new JFrame("Hospital Asset Management");
    frame.add(panel);
    frame.setSize(1000, 700);
    frame.setLocationRelativeTo(this);
    frame.setVisible(true);
});
adminMenu.add(manageAssetsItem);

// For Doctors/Staff - Add to menu
JMenuItem requestAssetItem = new JMenuItem("Request/Allocate Asset");
requestAssetItem.addActionListener(e -> {
    String userId = Session.getInstance().getCurrentUser().getUserId();
    RequestAssetPanel panel = new RequestAssetPanel(userId);
    // Create and show in a dialog
    JDialog dialog = new JDialog();
    dialog.add(panel);
    dialog.setSize(1000, 600);
    dialog.setLocationRelativeTo(this);
    dialog.setModal(true);
    dialog.setVisible(true);
});
doctorMenu.add(requestAssetItem);
```

### 3. Initialize Sample Data (Optional)

Create a method to set up initial assets:

```java
// Call this once during setup
private static void initializeSampleAssets() {
    // Add consultation rooms
    for (int i = 1; i <= 5; i++) {
        AssetManager.createAsset(
            AssetType.CONSULTATION_ROOM,
            "Consultation Room " + i,
            "Building A, Floor " + (i % 3 + 1),
            1,
            "General Medicine",
            "Standard consultation room"
        );
    }
    
    // Add labs
    AssetManager.createAsset(
        AssetType.LAB,
        "Pathology Lab",
        "Building B, Basement",
        3,
        "Pathology",
        "Blood and tissue analysis"
    );
    
    // Add imaging
    AssetManager.createAsset(
        AssetType.IMAGING_ROOM,
        "X-Ray Room 1",
        "Building C, Floor 1",
        1,
        "Radiology",
        "Digital X-ray system"
    );
    
    // Add wards
    AssetManager.createAsset(
        AssetType.INPATIENT_WARD,
        "General Ward A",
        "Building A, Floor 3",
        20,
        "General Medicine",
        "20-bed general ward"
    );
}
```

## Data Files

The system automatically creates and manages these files in the `data/` folder:

- `hospital_assets.txt` - Complete asset inventory
- `asset_allocations.txt` - Booking records
- `asset_requests.txt` - Request queue

## Key Features

✅ **Asset Management**
- Create, read, update, delete assets
- Track asset status (available, occupied, maintenance, out of service)
- Filter by type, status, department, location

✅ **Allocation System**
- Book assets with time slots
- Automatic conflict detection
- Time-based availability queries
- Support for multiple simultaneous allocations

✅ **Request Workflow**
- Doctors can request needed assets
- Priority-based request handling (LOW, MEDIUM, HIGH)
- Admin approval/rejection with notes
- Automatic allocation upon approval

✅ **User Interfaces**
- Admin panel for inventory management
- User panel for asset requests and quick booking
- Real-time availability checking
- Statistics and status displays

## Common Operations

### Add an Asset (Admin)
1. Click "Manage Hospital Assets"
2. Click "Add Asset" button
3. Fill in asset details (type, name, location, capacity, department)
4. Click "Save"

### Request an Asset (Doctor/Staff)
1. Click "Request/Allocate Asset"
2. Fill in request form (asset type, purpose, priority)
3. Click "Submit Request"
4. Check "Your Requests" for status
5. Select available asset and set time slot to allocate

### Approve a Request (Admin)
1. Open asset manager
2. Review pending requests
3. Select available asset
4. Click "Approve" and assign time slot

### Release Asset (Doctor/Staff)
1. View your allocations
2. Select completed allocation
3. Click "Release" to mark as done
4. Asset becomes available for others

## Error Handling

The system prevents:
- ❌ Double-booking (same asset, overlapping times)
- ❌ Allocating unavailable assets
- ❌ Invalid time ranges
- ❌ Missing asset types

Always catch exceptions when integrating:

```java
try {
    AssetAllocationManager.allocateAsset(...);
} catch (IllegalArgumentException e) {
    // Asset not found
    showError("Asset not found: " + e.getMessage());
} catch (IllegalStateException e) {
    // Asset not available or time conflict
    showError("Cannot allocate: " + e.getMessage());
}
```

## Database Schema

### Assets (hospital_assets.txt)
```
ASSET001|CONSULTATION_ROOM|Room A1|Building 1, Floor 2|1|AVAILABLE|Cardiology|Description|2024-09-01T08:00:00
```

### Allocations (asset_allocations.txt)
```
ALLOC001|ASSET001|DOC001|CARDIOLOGY|Consultation|2024-09-01T09:00:00|2024-09-01T10:00:00|COMPLETED|Notes
```

### Requests (asset_requests.txt)
```
REQ001|LAB|DOC002|PATHOLOGY|Blood test|HIGH|APPROVED|Approved on 2024-09-01
```

## Testing

Run the application and test these scenarios:

1. **Add Asset** - Create consultation room
2. **List Assets** - View all assets
3. **Request Asset** - Submit asset request
4. **Allocate Asset** - Book available asset
5. **Check Conflict** - Try double-booking
6. **Release Asset** - Complete allocation
7. **Update Status** - Change to maintenance
8. **Cancel Request** - Reject pending request

## Performance Considerations

- Assets up to 1000: No issues
- Allocations up to 10,000: Smooth performance
- Daily archived allocations recommended for large deployments

## Future Enhancements

- Recurring allocations
- Email notifications
- Calendar integration
- Asset maintenance scheduling
- Usage analytics and reports
- Mobile app support
- Inventory tracking for consumables

## Support & Documentation

- **Full Guide**: See `ASSET_MANAGEMENT_GUIDE.md`
- **Code Examples**: See `ASSET_EXAMPLES.java`
- **API Docs**: JavaDoc comments in each class

---

**System Version**: 1.0  
**Created**: September 2024  
**Compatible With**: Java 8+, Swing GUI Framework
