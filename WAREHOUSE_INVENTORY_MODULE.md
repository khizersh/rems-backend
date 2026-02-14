# Warehouse & Inventory Module Documentation

## Overview

This module provides comprehensive warehouse and inventory management for the Real Estate ERP system. It integrates with existing Purchase (PO/GRN/Invoice) and Expense modules while maintaining clean separation of concerns.

## Architecture

### Core Components

1. **Entities**
   - `Warehouse` - Warehouse master data
   - `Stock` - Current stock balances per warehouse/item
   - `StockLedger` - Audit trail of all stock movements
   - `ExpenseItem` - Itemized expense records with stock impact

2. **Enums**
   - `WarehouseType` - CENTRAL, PROJECT, TEMP
   - `StockRefType` - GRN, DIRECT_EXPENSE_PURCHASE, MATERIAL_ISSUE, TRANSFER, ADJUSTMENT
   - `ReceiptType` - WAREHOUSE_STOCK, DIRECT_CONSUME

3. **Services**
   - `InventoryService` - Core stock management engine
   - `WarehouseService` - Warehouse CRUD operations
   - `StockService` - Stock queries and operations
   - `WarehouseIntegrationService` - Business logic integration
   - `StockLedgerService` - Stock movement history

4. **Controllers**
   - `WarehouseController` - Warehouse management APIs
   - `StockController` - Stock operations and queries
   - `StockLedgerController` - Stock movement reports
   - `WarehouseIntegrationController` - Integration endpoints

## Key Features

### 1. Warehouse Management
- Create warehouses with types (CENTRAL, PROJECT, TEMP)
- Project warehouses are linked to specific projects
- Warehouse activation/deactivation
- Organization-based warehouse filtering

### 2. Stock Management
- Real-time stock tracking per warehouse and item
- Average cost calculation using weighted average method
- Stock reservations for future allocations
- Negative stock prevention
- Low stock alerts

### 3. Stock Movements
- All stock changes are recorded in StockLedger
- Support for various reference types (GRN, Expense, Transfer, etc.)
- Complete audit trail with timestamps and user tracking
- Stock adjustments (increase/decrease)
- Inter-warehouse transfers

### 4. Integration Points

#### GRN Integration
```java
// In GRN approval process:
if (grn.receiptType == WAREHOUSE_STOCK) {
    // Add stock to specified warehouse
    inventoryService.addStock(warehouseId, itemId, quantity, rate, ...);
} else if (grn.receiptType == DIRECT_CONSUME) {
    // Direct consumption - no stock entry
    // Mark for project consumption tracking
}
```

#### Expense Integration
```java
// For each expense item:
if (expenseItem.stockEffect == true) {
    // Add to warehouse stock
    inventoryService.addStock(warehouseId, itemId, quantity, rate, ...);
} else {
    // Direct project consumption only
}
```

## API Endpoints

### Warehouse Management

```
POST   /api/warehouse/create                    - Create warehouse
PUT    /api/warehouse/update/{id}              - Update warehouse  
GET    /api/warehouse/get/{id}                 - Get warehouse details
POST   /api/warehouse/getByOrganization        - List warehouses by organization (pageable)
POST   /api/warehouse/getByProject             - List warehouses by project (pageable)
GET    /api/warehouse/organization/{orgId}/type/{type} - List warehouses by type
POST   /api/warehouse/deactivate/{id}         - Deactivate warehouse
POST   /api/warehouse/activate/{id}           - Activate warehouse
```

### Stock Operations

```
POST   /api/stock/getByWarehouse               - Get stock by warehouse (pageable)
POST   /api/stock/getByItem                   - Get stock by item (pageable)
GET    /api/stock/warehouse/{wId}/item/{iId}  - Get specific stock
GET    /api/stock/inventory/summary/{wId}     - Inventory summary
GET    /api/stock/available/warehouse/{wId}   - Available stock by warehouse
GET    /api/stock/available/item/{iId}        - Available stock by item
GET    /api/stock/total/item/{iId}            - Total quantity by item
GET    /api/stock/low-stock                   - Low stock alerts
POST   /api/stock/adjust                      - Stock adjustment
POST   /api/stock/transfer                    - Stock transfer
POST   /api/stock/reserve                     - Reserve stock
POST   /api/stock/release-reservation         - Release reservation
```

### Stock Ledger

```
POST   /api/stock-ledger/getByWarehouse        - Ledger by warehouse (pageable)
POST   /api/stock-ledger/getByItem            - Ledger by item (pageable)
POST   /api/stock-ledger/getByWarehouseAndItem - Ledger by warehouse and item (pageable)
GET    /api/stock-ledger/reference/{type}/{refId} - Ledger by reference
GET    /api/stock-ledger/reference-type/{type} - Ledger by reference type
GET    /api/stock-ledger/warehouse/{wId}/date-range - Ledger by date range
GET    /api/stock-ledger/item/{iId}/date-range - Ledger by date range
GET    /api/stock-ledger/reference-list/{type}/{refId} - Get list by reference
```

### Integration

```
POST   /api/warehouse-integration/expense-items     - Process expense items
POST   /api/warehouse-integration/issue-material    - Issue material
GET    /api/warehouse-integration/expense-items/{expenseId} - Get expense items
```

## Business Rules

### Stock Rules
1. Stock quantity cannot go negative
2. All stock movements must be recorded in StockLedger
3. Average rate calculated using weighted average method
4. Stock reservations reduce available quantity
5. All stock operations are transactional

### Warehouse Rules
1. PROJECT warehouses must have projectId
2. Warehouse codes must be unique per organization
3. Only active warehouses can receive stock
4. Warehouse deactivation preserves stock data

### Integration Rules
1. GRN with receiptType=WAREHOUSE_STOCK adds to warehouse stock
2. GRN with receiptType=DIRECT_CONSUME bypasses warehouse
3. Expense items with stockEffect=true create stock entries
4. Material issues deduct from warehouse stock

## Data Model

### Stock Table
```sql
CREATE TABLE stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    avg_rate DECIMAL(15,4) DEFAULT 0,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warehouse_item (warehouse_id, item_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_item_id (item_id)
);
```

### Stock Ledger Table
```sql
CREATE TABLE stock_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    ref_type VARCHAR(50) NOT NULL,
    ref_id BIGINT NOT NULL,
    txn_date TIMESTAMP NOT NULL,
    qty_in DECIMAL(15,4) NOT NULL DEFAULT 0,
    qty_out DECIMAL(15,4) NOT NULL DEFAULT 0,
    balance_after DECIMAL(15,4) NOT NULL,
    rate DECIMAL(15,4) DEFAULT 0,
    amount DECIMAL(15,4) DEFAULT 0,
    remarks TEXT,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_item_id (item_id),
    INDEX idx_ref_type_ref_id (ref_type, ref_id),
    INDEX idx_txn_date (txn_date)
);
```

## API Usage Examples

### Getting Warehouses by Organization (with pagination)
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

### Getting Stock by Warehouse (with pagination)
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

### Getting Stock Ledger by Warehouse and Item (with pagination)
```json
POST /api/stock-ledger/getByWarehouseAndItem
{
    "id": 1,
    "id2": 100,
    "page": 0,
    "size": 25,
    "sortBy": "txnDate",
    "sortDir": "desc"
}
```

## Usage Examples

### Creating a Warehouse
```java
WarehouseCreateRequestDTO request = new WarehouseCreateRequestDTO();
request.setName("Main Warehouse");
request.setCode("WH001");
request.setWarehouseType(WarehouseType.CENTRAL);
request.setOrganizationId(1L);

warehouseService.createWarehouse(request, "admin");
```

### Adding Stock
```java
inventoryService.addStock(
    warehouseId: 1L,
    itemId: 100L,
    quantity: new BigDecimal("50.00"),
    rate: new BigDecimal("25.50"),
    refType: StockRefType.GRN,
    refId: 12345L,
    remarks: "Stock from GRN-001",
    loggedInUser: "admin"
);
```

### Transferring Stock
```java
inventoryService.transferStock(
    fromWarehouseId: 1L,
    toWarehouseId: 2L,
    itemId: 100L,
    quantity: new BigDecimal("10.00"),
    refId: System.currentTimeMillis(),
    remarks: "Transfer to project warehouse",
    loggedInUser: "admin"
);
```

## Validation Rules

1. **Quantity Validation**: All quantities must be positive
2. **Warehouse Validation**: Warehouse must exist and be active
3. **Item Validation**: Item must exist in items table
4. **Stock Validation**: Sufficient stock for deductions
5. **User Validation**: Logged in user required for all operations

## Error Handling

The module uses consistent error handling with:
- `IllegalArgumentException` for validation errors
- Transactional rollback on failures
- Descriptive error messages
- Standard response wrapper format

## Performance Considerations

1. **Indexes**: Proper indexing on warehouse_id, item_id, ref_type+ref_id
2. **Transactions**: All stock operations are transactional
3. **Batch Operations**: Support for bulk operations where applicable
4. **Pagination**: All listing APIs support pagination
5. **Query Optimization**: Efficient queries with proper joins

## Testing

The module should be tested with:
1. Unit tests for service layer methods
2. Integration tests for controller endpoints
3. Transaction tests for rollback scenarios
4. Performance tests for large datasets
5. Concurrency tests for stock operations

## Security

1. **Authentication**: JWT token validation on all endpoints
2. **Authorization**: Organization-based access control
3. **Audit Trail**: Complete tracking of who did what when
4. **Data Validation**: Input validation on all APIs
5. **SQL Injection Prevention**: Parameterized queries only

This completes the Warehouse & Inventory module implementation for the Real Estate ERP system.
