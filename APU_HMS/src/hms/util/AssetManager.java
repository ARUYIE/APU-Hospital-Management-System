package hms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages hospital assets (CRUD operations and queries)
 */
public class AssetManager {
    
    private static final String ASSETS_FILE = "hospital_assets.txt";

    private AssetManager() { 

    }
    
    public static String createAsset(AssetType type, String name, String location,
                                    int capacity, String department, String description) {
        String assetId = IDGenerator.next("ASSET", ASSETS_FILE);
        Asset asset = new Asset(assetId, type, name, location, description);
        asset.setStatus("AVAILABLE");
        asset.setCapacity(capacity);
        asset.setDepartment(department);

        List<String> lines = FileManager.readLines(ASSETS_FILE);
        lines.add(asset.toFileLine());
        FileManager.writeAllLines(ASSETS_FILE, lines);

        return assetId;
    }
    
    public static Asset getAsset(String assetId) {
        List<Asset> assets = getAllAssets();
        return assets.stream()
                .filter(a -> a.getAssetId().equals(assetId))
                .findFirst()
                .orElse(null);
    }

    public static List<Asset> getAllAssets() {
        List<String> lines = FileManager.readLines(ASSETS_FILE);
        return lines.stream()
                .map(AssetManager::parseAsset)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    public static List<Asset> getAssetsByType(AssetType type) {
        return getAllAssets().stream()
                .filter(a -> a.getAssetType() == type)
                .collect(Collectors.toList());
    }

    public static List<Asset> getAvailableAssets(AssetType type) {
        return getAllAssets().stream()
                .filter(a -> a.getAssetType() == type && a.isAvailable())
                .collect(Collectors.toList());
    }

    public static List<Asset> getAssetsByDepartment(String department) {
        return getAllAssets().stream()
                .filter(a -> a.getDepartment().equalsIgnoreCase(department))
                .collect(Collectors.toList());
    }

    public static List<Asset> getAssetsByLocation(String location) {
        return getAllAssets().stream()
                .filter(a -> a.getLocation().equalsIgnoreCase(location))
                .collect(Collectors.toList());
    }

    private static String getHeaderLine() {
        List<String> lines = FileManager.readLines(ASSETS_FILE);
        if (!lines.isEmpty()) {
            String firstLine = lines.get(0);
            String[] parts = firstLine.split("\\|", -1);
            if (parts.length > 0 && ("ASSET_ID".equalsIgnoreCase(parts[0]) || "ASSETID".equalsIgnoreCase(parts[0]))) {
                return firstLine;
            }
        }
        return "ASSET_ID|ROOM_TYPE|ROOM_NAME|LOCATION|STATUS|RESERVED_BY";
    }

    public static boolean updateAsset(Asset asset) {
        List<Asset> assets = getAllAssets();
        boolean found = false;
        
        for (int i = 0; i < assets.size(); i++) {
            if (assets.get(i).getAssetId().equals(asset.getAssetId())) {
                assets.set(i, asset);
                found = true;
                break;
            }
        }
        
        if (found) {
            List<String> lines = new ArrayList<>();
            lines.add(getHeaderLine());
            lines.addAll(assets.stream()
                    .map(Asset::toFileLine)
                    .collect(Collectors.toList()));
            FileManager.writeAllLines(ASSETS_FILE, lines);
        }
        
        return found;
    }

    public static boolean updateAssetStatus(String assetId, String newStatus) {
        Asset asset = getAsset(assetId);
        if (asset != null) {
            asset.setStatus(newStatus);
            return updateAsset(asset);
        }
        return false;
    }
    
    /**
     * Delete an asset
     */
    public static boolean deleteAsset(String assetId) {
        List<Asset> assets = getAllAssets();
        boolean removed = assets.removeIf(a -> a.getAssetId().equals(assetId));
        
        if (removed) {
            List<String> lines = new ArrayList<>();
            lines.add(getHeaderLine());
            lines.addAll(assets.stream()
                    .map(Asset::toFileLine)
                    .collect(Collectors.toList()));
            FileManager.writeAllLines(ASSETS_FILE, lines);
        }
        
        return removed;
    }
    
    /**
     * Get count of available assets by type
     */
    public static int getAvailableCount(AssetType type) {
        if (type == null) {
            // Return total available if no type specified
            return (int) getAllAssets().stream()
                    .filter(Asset::isAvailable)
                    .count();
        }
        return (int) getAllAssets().stream()
                .filter(a -> a.getAssetType() == type && a.isAvailable())
                .count();
    }
    
    /**
     * Get count of assets in maintenance
     */
    public static int getMaintenanceCount() {
        return (int) getAllAssets().stream()
                .filter(Asset::isInMaintenance)
                .count();
    }
    
    /**
     * Parse a line from the assets file into an Asset object
     */
    private static Asset parseAsset(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split("\\|", -1);
        if (parts.length == 0 || "ASSET_ID".equalsIgnoreCase(parts[0]) || "ASSETID".equalsIgnoreCase(parts[0])) {
            return null;
        }

        if (parts.length >= 9) {
            return new Asset(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    Integer.parseInt(parts[4]),
                    parts[5],
                    parts[6],
                    parts[7],
                    parts[8]
            );
        }

        if (parts.length >= 7) {
            return new Asset(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4],
                    parts[6].isEmpty() ? parts[5] : parts[5] + " | Department: " + parts[6]
            );
        }

        if (parts.length >= 6) {
            return new Asset(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4],
                    parts[5]
            );
        }

        return null;
    }
}
