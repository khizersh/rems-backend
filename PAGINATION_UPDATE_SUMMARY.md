# Warehouse & Stock Module API Pagination Update

## Summary of Changes

Updated all pagination APIs in the Warehouse & Stock module controllers to follow the consistent pagination pattern used in BookingController.

## Changes Made

### 1. WarehouseController.java
**Before:**
- Used `@GetMapping` with `@RequestParam` for pagination parameters
- Individual parameters: `page`, `size`, `sortBy`, `sortDir`
- Direct `Map<String, Object>` return type

**After:**
- Changed to `@PostMapping` with `@RequestBody FilterPaginationRequest`
- Uses `ResponseEntity<?>` return type
- Consistent pagination parameter handling with `request.getId()`

**Updated endpoints:**
- `/organization/{organizationId}` → `/getByOrganization`
- `/project/{projectId}` → `/getByProject`

### 2. StockController.java
**Before:**
- `@GetMapping` endpoints with `@PathVariable` and `@RequestParam`
- Individual pagination parameters

**After:**
- `@PostMapping` endpoints with `@RequestBody FilterPaginationRequest`
- Uses `ResponseEntity<?>` for pageable endpoints

**Updated endpoints:**
- `/warehouse/{warehouseId}` → `/getByWarehouse`
- `/item/{itemId}` → `/getByItem`

### 3. StockLedgerController.java
**Before:**
- `@GetMapping` endpoints with individual parameters
- Manual `Sort` and `Pageable` construction

**After:**
- `@PostMapping` endpoints for main queries with `FilterPaginationRequest`
- Consistent pagination pattern
- Uses `ResponseEntity<?>`

**Updated endpoints:**
- `/warehouse/{warehouseId}` → `/getByWarehouse`
- `/item/{itemId}` → `/getByItem`
- Added `/getByWarehouseAndItem` for combined queries

## API Format Consistency

All pagination APIs now follow this pattern:

```java
@PostMapping("/getBy...")
public ResponseEntity<?> getDataBy...(@RequestBody FilterPaginationRequest request) {
    Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            request.getSortDir().equalsIgnoreCase("asc")
                    ? Sort.by(request.getSortBy()).ascending()
                    : Sort.by(request.getSortBy()).descending());

    Map<String, Object> dataPage = service.getData(request.getId(), pageable);
    return ResponseEntity.ok(dataPage);
}
```

## FilterPaginationRequest Structure

```java
public class FilterPaginationRequest {
    private long id;        // Primary ID (warehouseId, itemId, etc.)
    private long id2;       // Secondary ID for combined queries
    private String filteredBy;  // Filter type
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "asc";
}
```

## Benefits

1. **Consistency**: All pagination APIs follow the same pattern
2. **Flexibility**: `FilterPaginationRequest` supports complex filtering with `id`, `id2`, and `filteredBy`
3. **Standardization**: Matches existing project conventions
4. **Maintainability**: Easier to maintain and extend

## Backward Compatibility

Some endpoints that don't require pagination (like specific item lookups) remain as `@GetMapping` to maintain RESTful principles:
- `/warehouse/{id}` (single warehouse lookup)
- `/warehouse/{warehouseId}/item/{itemId}` (specific stock lookup)
- Reference-based lookups that typically return small datasets

## Testing

After these changes, test the following:

1. **Warehouse pagination**: `POST /api/warehouse/getByOrganization`
2. **Stock pagination**: `POST /api/stock/getByWarehouse`
3. **Ledger pagination**: `POST /api/stock-ledger/getByWarehouse`
4. **Combined queries**: `POST /api/stock-ledger/getByWarehouseAndItem`

## Sample API Calls

### Get Warehouses by Organization
```json
POST /api/warehouse/getByOrganization
{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "name",
    "sortDir": "asc"
}
```

### Get Stock by Warehouse
```json
POST /api/stock/getByWarehouse
{
    "id": 1,
    "page": 0,
    "size": 20,
    "sortBy": "itemId",
    "sortDir": "desc"
}
```

### Get Ledger by Warehouse and Item
```json
POST /api/stock-ledger/getByWarehouseAndItem
{
    "id": 1,      // warehouseId
    "id2": 100,   // itemId
    "page": 0,
    "size": 25,
    "sortBy": "txnDate",
    "sortDir": "desc"
}
```

All pagination APIs in the Warehouse & Stock module now follow the consistent pattern used throughout the application.
