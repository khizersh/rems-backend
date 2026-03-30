# Warehouse & Inventory Management — Frontend Integration Guide

> **Module:** Warehouse Management  
> **Backend Framework:** Spring Boot + JPA  
> **Frontend Framework:** React  
> **Base URL:** `http://localhost:8081`  
> **Authentication:** All requests require JWT Bearer token in `Authorization` header  
> **Last Updated:** March 14, 2026

---

## Table of Contents

1. [Module Overview](#1-module-overview)
2. [Authentication](#2-authentication)
3. [Standard Response Format](#3-standard-response-format)
4. [Enums Reference](#4-enums-reference)
5. [Common Request DTOs](#5-common-request-dtos)
6. [Warehouse APIs](#6-warehouse-apis)
7. [Stock APIs](#7-stock-apis)
8. [Stock Ledger APIs](#8-stock-ledger-apis)
9. [Warehouse Integration APIs](#9-warehouse-integration-apis)
10. [cURL Commands Reference](#10-curl-commands-reference)
11. [React UI Pages & Components](#11-react-ui-pages--components)
12. [Axios Service Layer](#12-axios-service-layer)
13. [UI/UX Guidelines](#13-uiux-guidelines)

---

## 1. Module Overview

The Warehouse & Inventory module provides:

- **Warehouse Management** — Create, update, activate/deactivate warehouses (CENTRAL, PROJECT, TEMP types)
- **Stock Tracking** — Real-time stock per warehouse per item with quantity, reserved quantity, and weighted average rate
- **Stock Ledger** — Append-only audit trail of every stock movement (GRN, expense, transfer, adjustment, material issue)
- **Stock Operations** — Adjust stock, transfer between warehouses, reserve/release stock
- **Integration** — GRN approval auto-adds stock; expense items optionally add stock; material issue deducts stock

### Architecture

```
WarehouseController ──→ WarehouseService ──→ WarehouseRepository
StockController ──→ StockService ──→ InventoryService ──→ StockRepository + StockLedgerRepository
StockLedgerController ──→ StockLedgerService ──→ StockLedgerRepository
WarehouseIntegrationController ──→ WarehouseIntegrationService ──→ InventoryService
```

### Key Business Rules

- Stock is tracked **per warehouse + per item** (unique constraint)
- All stock mutations go through `InventoryService` — never direct DB updates
- **Negative stock is prevented** — deductions are validated against available quantity
- **Weighted average rate** is recalculated on every stock addition
- Every stock change creates a **StockLedger** entry (audit trail)
- `reservedQuantity` is separate — available = quantity - reservedQuantity

---

## 2. Authentication

All API calls require a valid JWT token:

```
Authorization: Bearer <your_jwt_token>
```

The backend extracts `loggedInUser` from the JWT token via security context. You do NOT need to send username in the request body.

---

## 3. Standard Response Format

All APIs return a consistent response wrapper:

### Success Response

```json
{
  "data": { ... },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

### Error Response — Invalid Parameter

```json
{
  "data": "Error description message",
  "responseMessage": "Invalid Parameter!",
  "responseCode": "0002"
}
```

### Error Response — System Failure

```json
{
  "data": "Error description message",
  "responseMessage": "System Failure!",
  "responseCode": "9999"
}
```

### Response Codes

| Code | Meaning |
|------|---------|
| `0000` | Success |
| `0001` | No Data Found |
| `0002` | Invalid Parameter / Bad Request |
| `0003` | Invalid Credentials |
| `9999` | System Failure |

### Paginated Response Shape (inside `data`)

When the API returns paginated data, the `data` field contains a Spring `Page` object:

```json
{
  "data": {
    "content": [ /* array of objects */ ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { "sorted": true, "unsorted": false, "empty": false },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 25,
    "totalPages": 3,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false,
    "numberOfElements": 10,
    "empty": false
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 4. Enums Reference

### WarehouseType

| Value | Description |
|-------|-------------|
| `CENTRAL` | Main central warehouse |
| `PROJECT` | Project-specific warehouse (requires `projectId`) |
| `TEMP` | Temporary storage warehouse |

### StockRefType

| Value | Description |
|-------|-------------|
| `GRN` | Goods Receipt Note — stock added from GRN approval |
| `DIRECT_EXPENSE_PURCHASE` | Stock added from expense item with stockEffect=true |
| `MATERIAL_ISSUE` | Stock deducted for project material consumption |
| `TRANSFER` | Stock moved between warehouses |
| `ADJUSTMENT` | Manual stock adjustment (increase or decrease) |

### ReceiptType (used in GRN)

| Value | Description |
|-------|-------------|
| `WAREHOUSE_STOCK` | GRN goods go into a warehouse (adds stock) |
| `DIRECT_CONSUME` | GRN goods consumed directly by project (no stock entry) |

---

## 5. Common Request DTOs

### FilterPaginationRequest

Used by all paginated POST endpoints:

```json
{
  "id": 1,
  "id2": 0,
  "filteredBy": "",
  "page": 0,
  "size": 10,
  "sortBy": "id",
  "sortDir": "desc"
}
```

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `id` | `number` | Yes | — | Primary filter ID (warehouseId, itemId, organizationId, etc.) |
| `id2` | `number` | No | `0` | Secondary filter ID (used in warehouse+item combined queries) |
| `filteredBy` | `string` | No | `""` | Additional filter string |
| `page` | `number` | Yes | `0` | Page number (0-indexed) |
| `size` | `number` | Yes | `10` | Page size |
| `sortBy` | `string` | Yes | `"createdDate"` | Sort field name |
| `sortDir` | `string` | Yes | `"asc"` | Sort direction: `"asc"` or `"desc"` |

---

## 6. Warehouse APIs

**Base Path:** `/api/warehouse`

### 6.1 Create Warehouse

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse/create` |
| **Auth** | Required |
| **Content-Type** | `application/json` |

**Request Body:**

```json
{
  "name": "Central Warehouse Karachi",
  "code": "CWH-KHI-001",
  "warehouseType": "CENTRAL",
  "organizationId": 1,
  "projectId": null,
  "active": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | `string` | Yes | Cannot be blank |
| `code` | `string` | Yes | Cannot be blank, max 20 chars, unique per org, auto-uppercased |
| `warehouseType` | `string` | Yes | One of: `CENTRAL`, `PROJECT`, `TEMP` |
| `organizationId` | `number` | Yes | Must be valid organization |
| `projectId` | `number` | Conditional | **Required** when `warehouseType = PROJECT` |
| `active` | `boolean` | No | Defaults to `true` |

**Success Response:**

```json
{
  "data": {
    "id": 1,
    "name": "Central Warehouse Karachi",
    "code": "CWH-KHI-001",
    "warehouseType": "CENTRAL",
    "projectId": null,
    "organizationId": 1,
    "active": true,
    "createdBy": "hammadvb",
    "updatedBy": "hammadvb",
    "createdAt": "2026-03-14T10:30:00",
    "updatedAt": "2026-03-14T10:30:00"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 6.2 Update Warehouse

| Property | Value |
|----------|-------|
| **Method** | `PUT` |
| **URL** | `/api/warehouse/update/{id}` |
| **Auth** | Required |
| **Path Param** | `id` — warehouse ID |

**Request Body:** Same as Create Warehouse.

**Success Response:** Same shape as Create, with updated fields.

---

### 6.3 Get Warehouse by ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/warehouse/get/{id}` |
| **Auth** | Not required |
| **Path Param** | `id` — warehouse ID |

**Success Response:**

```json
{
  "data": {
    "id": 1,
    "name": "Central Warehouse Karachi",
    "code": "CWH-KHI-001",
    "warehouseType": "CENTRAL",
    "projectId": null,
    "organizationId": 1,
    "active": true,
    "createdBy": "hammadvb",
    "updatedBy": "hammadvb",
    "createdAt": "2026-03-14T10:30:00",
    "updatedAt": "2026-03-14T10:30:00"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 6.4 Get Warehouses by Organization (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse/getByOrganization` |
| **Auth** | Required |
| **Note** | `id` in request body = `organizationId` |

**Request Body:**

```json
{
  "id": 1,
  "page": 0,
  "size": 10,
  "sortBy": "id",
  "sortDir": "desc"
}
```

**Success Response:** Paginated response with warehouse list inside `data.content`.

---

### 6.5 Get Warehouses by Project (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse/getByProject` |
| **Auth** | Required |
| **Note** | `id` in request body = `projectId` |

**Request Body:**

```json
{
  "id": 5,
  "page": 0,
  "size": 10,
  "sortBy": "id",
  "sortDir": "desc"
}
```

**Success Response:** Paginated response. Only returns **active** warehouses.

---

### 6.6 Get Warehouses by Type

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/warehouse/organization/{organizationId}/type/{warehouseType}` |
| **Auth** | Not required |
| **Path Params** | `organizationId` (number), `warehouseType` (`CENTRAL` / `PROJECT` / `TEMP`) |

**Success Response:**

```json
{
  "data": [
    {
      "id": 1,
      "name": "Central Warehouse Karachi",
      "code": "CWH-KHI-001",
      "warehouseType": "CENTRAL",
      "organizationId": 1,
      "active": true,
      ...
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 6.7 Deactivate Warehouse

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse/deactivate/{id}` |
| **Auth** | Required |
| **Path Param** | `id` — warehouse ID |
| **Request Body** | None |

**Success Response:**

```json
{
  "data": "Warehouse deactivated successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 6.8 Activate Warehouse

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse/activate/{id}` |
| **Auth** | Required |
| **Path Param** | `id` — warehouse ID |
| **Request Body** | None |

**Success Response:**

```json
{
  "data": "Warehouse activated successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 7. Stock APIs

**Base Path:** `/api/stock`

### 7.1 Get Stock by Warehouse (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock/getByWarehouse` |
| **Auth** | Required |
| **Note** | `id` in request body = `warehouseId` |

**Request Body:**

```json
{
  "id": 1,
  "page": 0,
  "size": 10,
  "sortBy": "id",
  "sortDir": "desc"
}
```

**Success Response (each item in `data.content`):**

```json
{
  "id": 1,
  "warehouseId": 1,
  "itemId": 5,
  "quantity": 500.0000,
  "reservedQuantity": 50.0000,
  "avgRate": 125.5000,
  "createdBy": "hammadvb",
  "updatedBy": "hammadvb",
  "createdAt": "2026-03-14T10:30:00",
  "updatedAt": "2026-03-14T11:00:00"
}
```

---

### 7.2 Get Stock by Item (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock/getByItem` |
| **Auth** | Required |
| **Note** | `id` in request body = `itemId` |

**Request Body:**

```json
{
  "id": 5,
  "page": 0,
  "size": 10,
  "sortBy": "id",
  "sortDir": "desc"
}
```

---

### 7.3 Get Stock by Warehouse and Item

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock/warehouse/{warehouseId}/item/{itemId}` |
| **Auth** | Not required |
| **Path Params** | `warehouseId` (number), `itemId` (number) |

**Success Response:**

```json
{
  "data": {
    "id": 1,
    "warehouseId": 1,
    "itemId": 5,
    "quantity": 500.0000,
    "reservedQuantity": 50.0000,
    "avgRate": 125.5000,
    "createdBy": "hammadvb",
    "updatedBy": "hammadvb",
    "createdAt": "2026-03-14T10:30:00",
    "updatedAt": "2026-03-14T11:00:00"
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

> Returns `null` in `data` if no stock record exists for the combination.

---

### 7.4 Get Available Stock by Warehouse

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock/available/warehouse/{warehouseId}` |
| **Auth** | Not required |

Returns all stock rows where `quantity > 0` for the given warehouse.

**Success Response:**

```json
{
  "data": [
    {
      "id": 1,
      "warehouseId": 1,
      "itemId": 5,
      "quantity": 500.0000,
      "reservedQuantity": 50.0000,
      "avgRate": 125.5000,
      ...
    },
    {
      "id": 2,
      "warehouseId": 1,
      "itemId": 12,
      "quantity": 200.0000,
      ...
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 7.5 Get Available Stock by Item

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock/available/item/{itemId}` |
| **Auth** | Not required |

Returns all warehouses that have stock > 0 for the given item.

---

### 7.6 Get Inventory Summary

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock/inventory/summary/{warehouseId}` |
| **Auth** | Not required |

Returns a calculated summary per item with available quantity and total value.

**Success Response:**

```json
{
  "data": [
    {
      "warehouseId": 1,
      "warehouseName": null,
      "itemId": 5,
      "itemName": null,
      "quantity": 500.0000,
      "reservedQuantity": 50.0000,
      "availableQuantity": 450.0000,
      "avgRate": 125.5000,
      "totalValue": 62750.0000
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

> **Note:** `availableQuantity = quantity - reservedQuantity`, `totalValue = quantity * avgRate`

---

### 7.7 Get Total Quantity by Item (Across All Warehouses)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock/total/item/{itemId}` |
| **Auth** | Not required |

**Success Response:**

```json
{
  "data": {
    "itemId": 5,
    "totalQuantity": 1250.0000
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 7.8 Get Low Stock Items

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock/low-stock` |
| **Auth** | Not required |
| **Query Param** | `threshold` (BigDecimal, optional, default = `10`) |

**Example:** `/api/stock/low-stock?threshold=25`

Returns all stock records where `quantity <= threshold`.

---

### 7.9 Adjust Stock

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock/adjust` |
| **Auth** | Required |

**Request Body:**

```json
{
  "warehouseId": 1,
  "itemId": 5,
  "quantity": 50.00,
  "increase": true,
  "remarks": "Physical count adjustment"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `warehouseId` | `number` | Yes | Must exist and be active |
| `itemId` | `number` | Yes | Must exist |
| `quantity` | `number` | Yes | Must be > 0 (min: 0.0001) |
| `increase` | `boolean` | Yes | `true` = add stock, `false` = deduct stock |
| `remarks` | `string` | No | Reason for adjustment |

**Success Response:**

```json
{
  "data": "Stock adjusted successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

**Error (insufficient stock on decrease):**

```json
{
  "data": "Insufficient stock. Available: 30.0000, Requested: 50.00",
  "responseMessage": "Invalid Parameter!",
  "responseCode": "0002"
}
```

---

### 7.10 Transfer Stock

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock/transfer` |
| **Auth** | Required |

**Request Body:**

```json
{
  "fromWarehouseId": 1,
  "toWarehouseId": 2,
  "itemId": 5,
  "quantity": 100.00,
  "remarks": "Transfer to project site"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `fromWarehouseId` | `number` | Yes | Must exist, have sufficient stock |
| `toWarehouseId` | `number` | Yes | Must exist and be active |
| `itemId` | `number` | Yes | Must exist |
| `quantity` | `number` | Yes | Must be > 0 (min: 0.0001) |
| `remarks` | `string` | No | Transfer reason |

> **Rule:** `fromWarehouseId` and `toWarehouseId` cannot be the same.  
> **Behavior:** Creates TWO ledger entries — one OUT from source, one IN to destination. Transfers use the source warehouse's average rate.

**Success Response:**

```json
{
  "data": "Stock transferred successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 7.11 Reserve Stock

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock/reserve` |
| **Auth** | Required |
| **Query Params** | `warehouseId`, `itemId`, `quantity` |

**Example:** `POST /api/stock/reserve?warehouseId=1&itemId=5&quantity=50`

Marks a portion of stock as reserved. Reserved stock cannot be deducted by other operations.

**Success Response:**

```json
{
  "data": "Stock reserved successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 7.12 Release Reserved Stock

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock/release-reservation` |
| **Auth** | Required |
| **Query Params** | `warehouseId`, `itemId`, `quantity` |

**Example:** `POST /api/stock/release-reservation?warehouseId=1&itemId=5&quantity=25`

Releases previously reserved stock back to available pool.

---

## 8. Stock Ledger APIs

**Base Path:** `/api/stock-ledger`

The stock ledger is an **append-only audit trail** of all stock movements. Every add, deduct, transfer, adjustment, and material issue creates ledger entries.

### Stock Ledger Object

```json
{
  "id": 1,
  "warehouseId": 1,
  "itemId": 5,
  "refType": "GRN",
  "refId": 42,
  "txnDate": "2026-03-14T10:30:00",
  "qtyIn": 100.0000,
  "qtyOut": 0.0000,
  "balanceAfter": 500.0000,
  "rate": 125.5000,
  "amount": 12550.0000,
  "remarks": "Stock received from GRN: GRN-2026-001",
  "createdBy": "hammadvb",
  "updatedBy": "hammadvb",
  "createdAt": "2026-03-14T10:30:00",
  "updatedAt": "2026-03-14T10:30:00"
}
```

| Field | Description |
|-------|-------------|
| `refType` | Source of the movement — see StockRefType enum |
| `refId` | ID of the source record (GRN item ID, expense item ID, etc.) |
| `txnDate` | Transaction timestamp |
| `qtyIn` | Quantity added (> 0 for additions, 0 for deductions) |
| `qtyOut` | Quantity removed (> 0 for deductions, 0 for additions) |
| `balanceAfter` | Stock balance after this transaction |
| `rate` | Unit rate used for this transaction |
| `amount` | Total amount (qty × rate) |

---

### 8.1 Get Ledger by Warehouse (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock-ledger/getByWarehouse` |
| **Auth** | Required |
| **Note** | `id` = `warehouseId` |

**Request Body:**

```json
{
  "id": 1,
  "page": 0,
  "size": 20,
  "sortBy": "txnDate",
  "sortDir": "desc"
}
```

---

### 8.2 Get Ledger by Item (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock-ledger/getByItem` |
| **Auth** | Required |
| **Note** | `id` = `itemId` |

**Request Body:**

```json
{
  "id": 5,
  "page": 0,
  "size": 20,
  "sortBy": "txnDate",
  "sortDir": "desc"
}
```

---

### 8.3 Get Ledger by Warehouse and Item (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/stock-ledger/getByWarehouseAndItem` |
| **Auth** | Required |
| **Note** | `id` = `warehouseId`, `id2` = `itemId` |

**Request Body:**

```json
{
  "id": 1,
  "id2": 5,
  "page": 0,
  "size": 20,
  "sortBy": "txnDate",
  "sortDir": "desc"
}
```

---

### 8.4 Get Ledger by Reference (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock-ledger/reference/{refType}/{refId}` |
| **Auth** | Not required |
| **Path Params** | `refType` (StockRefType enum), `refId` (number) |
| **Query Params** | `page` (default 0), `size` (default 20), `sortBy` (default `txnDate`), `sortDir` (default `desc`) |

**Example:** `GET /api/stock-ledger/reference/GRN/42?page=0&size=10`

---

### 8.5 Get Ledger by Reference Type (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock-ledger/reference-type/{refType}` |
| **Auth** | Not required |
| **Path Param** | `refType`: `GRN`, `DIRECT_EXPENSE_PURCHASE`, `MATERIAL_ISSUE`, `TRANSFER`, `ADJUSTMENT` |
| **Query Params** | `page`, `size`, `sortBy`, `sortDir` |

**Example:** `GET /api/stock-ledger/reference-type/TRANSFER?page=0&size=20`

---

### 8.6 Get Ledger by Warehouse and Date Range (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock-ledger/warehouse/{warehouseId}/date-range` |
| **Auth** | Not required |
| **Path Param** | `warehouseId` |
| **Query Params** | `startDate` (ISO DateTime), `endDate` (ISO DateTime), `page`, `size`, `sortBy`, `sortDir` |

**Example:**  
`GET /api/stock-ledger/warehouse/1/date-range?startDate=2026-03-01T00:00:00&endDate=2026-03-14T23:59:59&page=0&size=20`

> **Important:** Date format must be ISO 8601: `yyyy-MM-ddTHH:mm:ss`

---

### 8.7 Get Ledger by Item and Date Range (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock-ledger/item/{itemId}/date-range` |
| **Auth** | Not required |
| **Path Param** | `itemId` |
| **Query Params** | `startDate`, `endDate`, `page`, `size`, `sortBy`, `sortDir` |

---

### 8.8 Get Ledger by Reference (Non-Paginated List)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/stock-ledger/reference-list/{refType}/{refId}` |
| **Auth** | Not required |

Returns all ledger entries (no pagination) for a specific reference.

**Example:** `GET /api/stock-ledger/reference-list/GRN/42`

**Success Response:**

```json
{
  "data": [
    {
      "id": 1,
      "warehouseId": 1,
      "itemId": 5,
      "refType": "GRN",
      "refId": 42,
      "txnDate": "2026-03-14T10:30:00",
      "qtyIn": 100.0000,
      "qtyOut": 0.0000,
      "balanceAfter": 500.0000,
      ...
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 9. Warehouse Integration APIs

**Base Path:** `/api/warehouse-integration`

These APIs integrate the warehouse module with Purchase (GRN) and Expense modules.

### 9.1 Process Expense Items

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse-integration/expense-items` |
| **Auth** | Required |

Creates expense item records and optionally adds stock to warehouse.

**Request Body:**

```json
{
  "expenseId": 10,
  "expenseItems": [
    {
      "itemId": 5,
      "unitId": 1,
      "quantity": 100.00,
      "rate": 125.50,
      "amount": 12550.00,
      "warehouseId": 1,
      "stockEffect": true
    },
    {
      "itemId": 8,
      "unitId": 2,
      "quantity": 50.00,
      "rate": 200.00,
      "amount": 10000.00,
      "warehouseId": null,
      "stockEffect": false
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `expenseId` | `number` | Yes | Existing expense ID |
| `expenseItems` | `array` | Yes | List of items (must not be empty) |
| `expenseItems[].itemId` | `number` | Yes | Item ID |
| `expenseItems[].unitId` | `number` | Yes | Unit of measurement ID |
| `expenseItems[].quantity` | `number` | Yes | Must be > 0 |
| `expenseItems[].rate` | `number` | Yes | Cannot be negative |
| `expenseItems[].amount` | `number` | Yes | Cannot be negative |
| `expenseItems[].warehouseId` | `number` | Conditional | **Required** when `stockEffect = true` |
| `expenseItems[].stockEffect` | `boolean` | Yes | `true` = add to warehouse stock, `false` = direct project consumption |

> **Behavior:** Deletes existing expense items for the given `expenseId` before creating new ones. If `stockEffect = true`, calls `InventoryService.addStock()`.

**Success Response:**

```json
{
  "data": "Expense items processed successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 9.2 Issue Material

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/warehouse-integration/issue-material` |
| **Auth** | Required |

Deducts stock from a warehouse for project consumption.

**Request Body:**

```json
{
  "warehouseId": 1,
  "itemId": 5,
  "quantity": 25.00,
  "projectId": 10,
  "remarks": "Material for Block A construction"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `warehouseId` | `number` | Yes | Must have sufficient stock |
| `itemId` | `number` | Yes | Must exist in warehouse |
| `quantity` | `number` | Yes | Must be > 0, cannot exceed available stock |
| `projectId` | `number` | No | Target project |
| `remarks` | `string` | No | Notes |

**Success Response:**

```json
{
  "data": "Material issued successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 9.3 Get Expense Items by Expense ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/warehouse-integration/expense-items/{expenseId}` |
| **Auth** | Not required |

**Success Response:**

```json
{
  "data": [
    {
      "id": 1,
      "expenseId": 10,
      "itemId": 5,
      "unitId": 1,
      "quantity": 100.0000,
      "rate": 125.5000,
      "amount": 12550.0000,
      "warehouseId": 1,
      "stockEffect": true,
      "createdBy": "hammadvb",
      "updatedBy": "hammadvb",
      "createdAt": "2026-03-14T10:30:00",
      "updatedAt": "2026-03-14T10:30:00"
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

### 9.4 Get Stock Effect Items by Warehouse

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/warehouse-integration/stock-effect-items/warehouse/{warehouseId}` |
| **Auth** | Not required |

Returns only expense items where `stockEffect = true` for the given warehouse.

---

## 10. cURL Commands Reference

### Warehouse APIs

```bash
# 6.1 Create Warehouse
curl -X POST http://localhost:8081/api/warehouse/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "Central Warehouse Karachi",
    "code": "CWH-KHI-001",
    "warehouseType": "CENTRAL",
    "organizationId": 1,
    "projectId": null,
    "active": true
  }'

# 6.1 Create PROJECT Warehouse (projectId required)
curl -X POST http://localhost:8081/api/warehouse/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "Project Site Warehouse",
    "code": "PWH-SITE-001",
    "warehouseType": "PROJECT",
    "organizationId": 1,
    "projectId": 5,
    "active": true
  }'

# 6.2 Update Warehouse
curl -X PUT http://localhost:8081/api/warehouse/update/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "Central Warehouse Karachi - Updated",
    "code": "CWH-KHI-001",
    "warehouseType": "CENTRAL",
    "organizationId": 1,
    "projectId": null,
    "active": true
  }'

# 6.3 Get Warehouse by ID
curl -X GET http://localhost:8081/api/warehouse/get/1

# 6.4 Get Warehouses by Organization (Paginated)
curl -X POST http://localhost:8081/api/warehouse/getByOrganization \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "id",
    "sortDir": "desc"
  }'

# 6.5 Get Warehouses by Project (Paginated)
curl -X POST http://localhost:8081/api/warehouse/getByProject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 5,
    "page": 0,
    "size": 10,
    "sortBy": "id",
    "sortDir": "desc"
  }'

# 6.6 Get Warehouses by Type
curl -X GET http://localhost:8081/api/warehouse/organization/1/type/CENTRAL

# 6.7 Deactivate Warehouse
curl -X POST http://localhost:8081/api/warehouse/deactivate/1 \
  -H "Authorization: Bearer <TOKEN>"

# 6.8 Activate Warehouse
curl -X POST http://localhost:8081/api/warehouse/activate/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### Stock APIs

```bash
# 7.1 Get Stock by Warehouse (Paginated)
curl -X POST http://localhost:8081/api/stock/getByWarehouse \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 10,
    "sortBy": "id",
    "sortDir": "desc"
  }'

# 7.2 Get Stock by Item (Paginated)
curl -X POST http://localhost:8081/api/stock/getByItem \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 5,
    "page": 0,
    "size": 10,
    "sortBy": "id",
    "sortDir": "desc"
  }'

# 7.3 Get Stock by Warehouse and Item
curl -X GET http://localhost:8081/api/stock/warehouse/1/item/5

# 7.4 Get Available Stock by Warehouse
curl -X GET http://localhost:8081/api/stock/available/warehouse/1

# 7.5 Get Available Stock by Item
curl -X GET http://localhost:8081/api/stock/available/item/5

# 7.6 Get Inventory Summary
curl -X GET http://localhost:8081/api/stock/inventory/summary/1

# 7.7 Get Total Quantity by Item
curl -X GET http://localhost:8081/api/stock/total/item/5

# 7.8 Get Low Stock Items
curl -X GET "http://localhost:8081/api/stock/low-stock?threshold=25"

# 7.9 Adjust Stock (Increase)
curl -X POST http://localhost:8081/api/stock/adjust \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "warehouseId": 1,
    "itemId": 5,
    "quantity": 50.00,
    "increase": true,
    "remarks": "Physical count adjustment - surplus found"
  }'

# 7.9 Adjust Stock (Decrease)
curl -X POST http://localhost:8081/api/stock/adjust \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "warehouseId": 1,
    "itemId": 5,
    "quantity": 10.00,
    "increase": false,
    "remarks": "Physical count adjustment - shortage found"
  }'

# 7.10 Transfer Stock
curl -X POST http://localhost:8081/api/stock/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "fromWarehouseId": 1,
    "toWarehouseId": 2,
    "itemId": 5,
    "quantity": 100.00,
    "remarks": "Transfer to project site warehouse"
  }'

# 7.11 Reserve Stock
curl -X POST "http://localhost:8081/api/stock/reserve?warehouseId=1&itemId=5&quantity=50" \
  -H "Authorization: Bearer <TOKEN>"

# 7.12 Release Reserved Stock
curl -X POST "http://localhost:8081/api/stock/release-reservation?warehouseId=1&itemId=5&quantity=25" \
  -H "Authorization: Bearer <TOKEN>"
```

### Stock Ledger APIs

```bash
# 8.1 Get Ledger by Warehouse (Paginated)
curl -X POST http://localhost:8081/api/stock-ledger/getByWarehouse \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 20,
    "sortBy": "txnDate",
    "sortDir": "desc"
  }'

# 8.2 Get Ledger by Item (Paginated)
curl -X POST http://localhost:8081/api/stock-ledger/getByItem \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 5,
    "page": 0,
    "size": 20,
    "sortBy": "txnDate",
    "sortDir": "desc"
  }'

# 8.3 Get Ledger by Warehouse and Item (Paginated)
curl -X POST http://localhost:8081/api/stock-ledger/getByWarehouseAndItem \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 1,
    "id2": 5,
    "page": 0,
    "size": 20,
    "sortBy": "txnDate",
    "sortDir": "desc"
  }'

# 8.4 Get Ledger by Reference (Paginated)
curl -X GET "http://localhost:8081/api/stock-ledger/reference/GRN/42?page=0&size=10&sortBy=txnDate&sortDir=desc"

# 8.5 Get Ledger by Reference Type (Paginated)
curl -X GET "http://localhost:8081/api/stock-ledger/reference-type/TRANSFER?page=0&size=20&sortBy=txnDate&sortDir=desc"

# 8.6 Get Ledger by Warehouse and Date Range
curl -X GET "http://localhost:8081/api/stock-ledger/warehouse/1/date-range?startDate=2026-03-01T00:00:00&endDate=2026-03-14T23:59:59&page=0&size=20&sortBy=txnDate&sortDir=desc"

# 8.7 Get Ledger by Item and Date Range
curl -X GET "http://localhost:8081/api/stock-ledger/item/5/date-range?startDate=2026-03-01T00:00:00&endDate=2026-03-14T23:59:59&page=0&size=20&sortBy=txnDate&sortDir=desc"

# 8.8 Get Ledger by Reference (Non-Paginated)
curl -X GET http://localhost:8081/api/stock-ledger/reference-list/GRN/42
```

### Warehouse Integration APIs

```bash
# 9.1 Process Expense Items
curl -X POST http://localhost:8081/api/warehouse-integration/expense-items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "expenseId": 10,
    "expenseItems": [
      {
        "itemId": 5,
        "unitId": 1,
        "quantity": 100.00,
        "rate": 125.50,
        "amount": 12550.00,
        "warehouseId": 1,
        "stockEffect": true
      },
      {
        "itemId": 8,
        "unitId": 2,
        "quantity": 50.00,
        "rate": 200.00,
        "amount": 10000.00,
        "warehouseId": null,
        "stockEffect": false
      }
    ]
  }'

# 9.2 Issue Material
curl -X POST http://localhost:8081/api/warehouse-integration/issue-material \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "warehouseId": 1,
    "itemId": 5,
    "quantity": 25.00,
    "projectId": 10,
    "remarks": "Material for Block A construction"
  }'

# 9.3 Get Expense Items by Expense ID
curl -X GET http://localhost:8081/api/warehouse-integration/expense-items/10

# 9.4 Get Stock Effect Items by Warehouse
curl -X GET http://localhost:8081/api/warehouse-integration/stock-effect-items/warehouse/1
```

---

## 11. React UI Pages & Components

### 11.1 Recommended Page Structure

```
src/
  pages/
    warehouse/
      WarehouseListPage.jsx          # List all warehouses (paginated)
      WarehouseCreatePage.jsx         # Create warehouse form
      WarehouseEditPage.jsx           # Edit warehouse form
      WarehouseDetailPage.jsx         # Single warehouse detail view
    stock/
      StockOverviewPage.jsx           # Stock dashboard per warehouse
      StockByItemPage.jsx             # Stock across warehouses for an item
      StockAdjustmentPage.jsx         # Manual stock adjustment form
      StockTransferPage.jsx           # Transfer stock between warehouses
      LowStockAlertsPage.jsx          # Low stock items list
    stockLedger/
      StockLedgerPage.jsx             # Ledger view with filters
    integration/
      ExpenseItemsPage.jsx            # Expense-to-stock integration
      MaterialIssuePage.jsx           # Material issue form
  components/
    warehouse/
      WarehouseTable.jsx              # Reusable warehouse table
      WarehouseForm.jsx               # Reusable create/edit form
      WarehouseTypeBadge.jsx          # Color-coded type badge
      WarehouseStatusBadge.jsx        # Active/Inactive badge
    stock/
      StockTable.jsx                  # Reusable stock table
      InventorySummaryCard.jsx        # Summary cards
      StockAdjustmentForm.jsx         # Adjust stock form
      StockTransferForm.jsx           # Transfer stock form
    ledger/
      LedgerTable.jsx                 # Ledger entries table
      LedgerFilterBar.jsx             # Filter by refType, date range
      RefTypeBadge.jsx                # Color-coded ref type badge
  services/
    warehouseService.js               # Warehouse API calls
    stockService.js                   # Stock API calls
    stockLedgerService.js             # Ledger API calls
    warehouseIntegrationService.js    # Integration API calls
```

### 11.2 Warehouse List Page

**Data Source:** `POST /api/warehouse/getByOrganization`

| Column | Source Field | Notes |
|--------|-------------|-------|
| Name | `name` | Text, clickable to detail page |
| Code | `code` | Uppercase text |
| Type | `warehouseType` | Color-coded badge |
| Project | `projectId` | Resolve to project name if available |
| Status | `active` | Green (Active) / Red (Inactive) badge |
| Created | `createdAt` | Formatted date |
| Actions | — | View, Edit, Activate/Deactivate buttons |

**Features:**
- Paginated table with configurable page size (10, 20, 50)
- Sortable columns (click header to toggle asc/desc)
- Filter dropdown for warehouse type
- "Create Warehouse" button at top
- Confirmation dialog before activate/deactivate

### 11.3 Warehouse Create / Edit Form

| Field | Component | Required | Notes |
|-------|-----------|----------|-------|
| Name | `<input type="text">` | Yes | |
| Code | `<input type="text">` | Yes | Max 20 chars, auto-uppercase |
| Type | `<select>` | Yes | Options: CENTRAL, PROJECT, TEMP |
| Organization | Pre-filled from user context | Yes | Hidden or read-only |
| Project | `<select>` (dynamic) | If type=PROJECT | Show only when PROJECT type is selected |
| Active | `<switch>` | No | Default: true |

**Validation Rules:**
- Name cannot be blank
- Code cannot be blank, max 20 characters
- Code must be unique per organization (API will return error if duplicate)
- Project ID is required when type = PROJECT

### 11.4 Stock Overview Page (Warehouse Inventory)

**Data Source:** `POST /api/stock/getByWarehouse` + `GET /api/stock/inventory/summary/{warehouseId}`

**Layout:**
1. **Warehouse selector dropdown** at top
2. **KPI Cards row:**
   - Total Items (count of stock rows)
   - Total Quantity (sum)
   - Total Value (sum of quantity × avgRate)
   - Low Stock Items (count where qty ≤ threshold)
3. **Stock table** (paginated):

| Column | Source | Notes |
|--------|--------|-------|
| Item | `itemId` | Resolve to item name |
| Quantity | `quantity` | BigDecimal, 4 decimal places |
| Reserved | `reservedQuantity` | |
| Available | `quantity - reservedQuantity` | Calculated |
| Avg Rate | `avgRate` | Currency format |
| Total Value | `quantity × avgRate` | Calculated |
| Actions | — | Adjust, Transfer, View Ledger |

4. **Action buttons:** Adjust Stock, Transfer Stock, Issue Material

### 11.5 Stock Ledger Page

**Data Sources:** Multiple endpoints based on filter selection

**Filter Bar:**
- Warehouse dropdown
- Item dropdown
- Reference Type dropdown (GRN, TRANSFER, ADJUSTMENT, etc.)
- Date Range picker (start date, end date)
- Search/Apply button

**Ledger Table:**

| Column | Source | Notes |
|--------|--------|-------|
| Date | `txnDate` | Formatted datetime |
| Type | `refType` | Color-coded badge |
| Ref ID | `refId` | Clickable link to source record |
| In | `qtyIn` | Green text, show only if > 0 |
| Out | `qtyOut` | Red text, show only if > 0 |
| Balance | `balanceAfter` | Running balance |
| Rate | `rate` | Currency |
| Amount | `amount` | Currency |
| Remarks | `remarks` | Truncated with tooltip |

### 11.6 Stock Adjustment Modal/Page

**Form fields:**
- Warehouse (dropdown, required)
- Item (dropdown, required — filter by selected warehouse's stock)
- Quantity (number input, required, > 0)
- Type (radio: Increase / Decrease)
- Remarks (textarea, optional)

**API:** `POST /api/stock/adjust`

### 11.7 Stock Transfer Modal/Page

**Form fields:**
- From Warehouse (dropdown, required)
- To Warehouse (dropdown, required — exclude selected "from")
- Item (dropdown, required — filter by source warehouse's stock)
- Quantity (number input, required, > 0)
- Remarks (textarea, optional)

Show current available stock for the selected item in source warehouse.

**API:** `POST /api/stock/transfer`

### 11.8 Material Issue Page

**Form fields:**
- Warehouse (dropdown, required)
- Item (dropdown, required — filter by selected warehouse's available stock)
- Quantity (number input, required, > 0, max = available)
- Project (dropdown, optional)
- Remarks (textarea, optional)

**API:** `POST /api/warehouse-integration/issue-material`

---

## 12. Axios Service Layer

### warehouseService.js

```javascript
import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/warehouse';

const authHeaders = (token) => ({
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  },
});

// 6.1 Create Warehouse
export const createWarehouse = (data, token) =>
  axios.post(`${BASE_URL}/create`, data, authHeaders(token));

// 6.2 Update Warehouse
export const updateWarehouse = (id, data, token) =>
  axios.put(`${BASE_URL}/update/${id}`, data, authHeaders(token));

// 6.3 Get Warehouse by ID
export const getWarehouseById = (id) =>
  axios.get(`${BASE_URL}/get/${id}`);

// 6.4 Get Warehouses by Organization (Paginated)
export const getWarehousesByOrganization = (payload, token) =>
  axios.post(`${BASE_URL}/getByOrganization`, payload, authHeaders(token));

// 6.5 Get Warehouses by Project (Paginated)
export const getWarehousesByProject = (payload, token) =>
  axios.post(`${BASE_URL}/getByProject`, payload, authHeaders(token));

// 6.6 Get Warehouses by Type
export const getWarehousesByType = (organizationId, warehouseType) =>
  axios.get(`${BASE_URL}/organization/${organizationId}/type/${warehouseType}`);

// 6.7 Deactivate Warehouse
export const deactivateWarehouse = (id, token) =>
  axios.post(`${BASE_URL}/deactivate/${id}`, null, authHeaders(token));

// 6.8 Activate Warehouse
export const activateWarehouse = (id, token) =>
  axios.post(`${BASE_URL}/activate/${id}`, null, authHeaders(token));
```

### stockService.js

```javascript
import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/stock';

const authHeaders = (token) => ({
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  },
});

// 7.1 Get Stock by Warehouse (Paginated)
export const getStockByWarehouse = (payload, token) =>
  axios.post(`${BASE_URL}/getByWarehouse`, payload, authHeaders(token));

// 7.2 Get Stock by Item (Paginated)
export const getStockByItem = (payload, token) =>
  axios.post(`${BASE_URL}/getByItem`, payload, authHeaders(token));

// 7.3 Get Stock by Warehouse and Item
export const getStockByWarehouseAndItem = (warehouseId, itemId) =>
  axios.get(`${BASE_URL}/warehouse/${warehouseId}/item/${itemId}`);

// 7.4 Get Available Stock by Warehouse
export const getAvailableStockByWarehouse = (warehouseId) =>
  axios.get(`${BASE_URL}/available/warehouse/${warehouseId}`);

// 7.5 Get Available Stock by Item
export const getAvailableStockByItem = (itemId) =>
  axios.get(`${BASE_URL}/available/item/${itemId}`);

// 7.6 Get Inventory Summary
export const getInventorySummary = (warehouseId) =>
  axios.get(`${BASE_URL}/inventory/summary/${warehouseId}`);

// 7.7 Get Total Quantity by Item
export const getTotalQuantityByItem = (itemId) =>
  axios.get(`${BASE_URL}/total/item/${itemId}`);

// 7.8 Get Low Stock Items
export const getLowStockItems = (threshold = 10) =>
  axios.get(`${BASE_URL}/low-stock`, { params: { threshold } });

// 7.9 Adjust Stock
export const adjustStock = (data, token) =>
  axios.post(`${BASE_URL}/adjust`, data, authHeaders(token));

// 7.10 Transfer Stock
export const transferStock = (data, token) =>
  axios.post(`${BASE_URL}/transfer`, data, authHeaders(token));

// 7.11 Reserve Stock
export const reserveStock = (warehouseId, itemId, quantity, token) =>
  axios.post(`${BASE_URL}/reserve`, null, {
    ...authHeaders(token),
    params: { warehouseId, itemId, quantity },
  });

// 7.12 Release Reserved Stock
export const releaseReservedStock = (warehouseId, itemId, quantity, token) =>
  axios.post(`${BASE_URL}/release-reservation`, null, {
    ...authHeaders(token),
    params: { warehouseId, itemId, quantity },
  });
```

### stockLedgerService.js

```javascript
import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/stock-ledger';

const authHeaders = (token) => ({
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  },
});

// 8.1 Get Ledger by Warehouse (Paginated)
export const getLedgerByWarehouse = (payload, token) =>
  axios.post(`${BASE_URL}/getByWarehouse`, payload, authHeaders(token));

// 8.2 Get Ledger by Item (Paginated)
export const getLedgerByItem = (payload, token) =>
  axios.post(`${BASE_URL}/getByItem`, payload, authHeaders(token));

// 8.3 Get Ledger by Warehouse and Item (Paginated)
export const getLedgerByWarehouseAndItem = (payload, token) =>
  axios.post(`${BASE_URL}/getByWarehouseAndItem`, payload, authHeaders(token));

// 8.4 Get Ledger by Reference (Paginated)
export const getLedgerByReference = (refType, refId, page = 0, size = 20) =>
  axios.get(`${BASE_URL}/reference/${refType}/${refId}`, {
    params: { page, size, sortBy: 'txnDate', sortDir: 'desc' },
  });

// 8.5 Get Ledger by Reference Type (Paginated)
export const getLedgerByRefType = (refType, page = 0, size = 20) =>
  axios.get(`${BASE_URL}/reference-type/${refType}`, {
    params: { page, size, sortBy: 'txnDate', sortDir: 'desc' },
  });

// 8.6 Get Ledger by Warehouse and Date Range
export const getLedgerByWarehouseDateRange = (warehouseId, startDate, endDate, page = 0, size = 20) =>
  axios.get(`${BASE_URL}/warehouse/${warehouseId}/date-range`, {
    params: { startDate, endDate, page, size, sortBy: 'txnDate', sortDir: 'desc' },
  });

// 8.7 Get Ledger by Item and Date Range
export const getLedgerByItemDateRange = (itemId, startDate, endDate, page = 0, size = 20) =>
  axios.get(`${BASE_URL}/item/${itemId}/date-range`, {
    params: { startDate, endDate, page, size, sortBy: 'txnDate', sortDir: 'desc' },
  });

// 8.8 Get Ledger by Reference (Non-Paginated)
export const getLedgerByReferenceList = (refType, refId) =>
  axios.get(`${BASE_URL}/reference-list/${refType}/${refId}`);
```

### warehouseIntegrationService.js

```javascript
import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/warehouse-integration';

const authHeaders = (token) => ({
  headers: {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  },
});

// 9.1 Process Expense Items
export const processExpenseItems = (data, token) =>
  axios.post(`${BASE_URL}/expense-items`, data, authHeaders(token));

// 9.2 Issue Material
export const issueMaterial = (data, token) =>
  axios.post(`${BASE_URL}/issue-material`, data, authHeaders(token));

// 9.3 Get Expense Items by Expense ID
export const getExpenseItems = (expenseId) =>
  axios.get(`${BASE_URL}/expense-items/${expenseId}`);

// 9.4 Get Stock Effect Items by Warehouse
export const getStockEffectItemsByWarehouse = (warehouseId) =>
  axios.get(`${BASE_URL}/stock-effect-items/warehouse/${warehouseId}`);
```

---

## 13. UI/UX Guidelines

### Color Coding

**Warehouse Type Badges:**

| Type | Color | CSS Example |
|------|-------|-------------|
| `CENTRAL` | Blue | `background: #2563eb; color: white;` |
| `PROJECT` | Orange | `background: #ea580c; color: white;` |
| `TEMP` | Gray | `background: #6b7280; color: white;` |

**Warehouse Status Badges:**

| Status | Color | CSS Example |
|--------|-------|-------------|
| Active | Green | `background: #16a34a; color: white;` |
| Inactive | Red | `background: #dc2626; color: white;` |

**Stock Reference Type Badges:**

| RefType | Color | CSS Example |
|---------|-------|-------------|
| `GRN` | Blue | `background: #3b82f6;` |
| `DIRECT_EXPENSE_PURCHASE` | Purple | `background: #8b5cf6;` |
| `MATERIAL_ISSUE` | Orange | `background: #f97316;` |
| `TRANSFER` | Teal | `background: #14b8a6;` |
| `ADJUSTMENT` | Yellow | `background: #eab308;` |

**Ledger Entry Colors:**

| Type | Color |
|------|-------|
| `qtyIn > 0` (Stock In) | Green text `#16a34a` |
| `qtyOut > 0` (Stock Out) | Red text `#dc2626` |

### Toast Notifications

Show toast messages for all operations:

| Action | Type | Message |
|--------|------|---------|
| Create warehouse | Success | "Warehouse created successfully" |
| Update warehouse | Success | "Warehouse updated successfully" |
| Activate/Deactivate | Success | "Warehouse activated/deactivated successfully" |
| Stock adjusted | Success | "Stock adjusted successfully" |
| Stock transferred | Success | "Stock transferred successfully" |
| Material issued | Success | "Material issued successfully" |
| Insufficient stock | Error | Show error message from API response |
| Duplicate code | Error | "Warehouse code already exists for this organization" |

### Loading States

- Show loading spinner/skeleton during API calls
- Disable form submit button while request is in progress
- Show table skeleton loader for paginated lists

### Error Handling

```javascript
// Example error handler
const handleApiResponse = (response) => {
  const { responseCode, responseMessage, data } = response.data;
  
  if (responseCode === '0000') {
    // Success
    toast.success(responseMessage);
    return data;
  } else if (responseCode === '0002') {
    // Validation error
    toast.error(typeof data === 'string' ? data : responseMessage);
    return null;
  } else if (responseCode === '9999') {
    // System failure
    toast.error('An unexpected error occurred. Please try again.');
    return null;
  }
};
```

### Responsive Design

- Tables should be horizontally scrollable on mobile
- Forms should stack vertically on small screens
- Use modal dialogs for quick actions (adjust, transfer) on desktop
- Use full pages for complex forms on mobile

### Number Formatting

- Quantities: 4 decimal places (e.g., `500.0000`)
- Rates/Amounts: 2 decimal places with currency symbol (e.g., `₨ 125.50`)
- Use `toLocaleString()` for number formatting with commas

```javascript
const formatQuantity = (val) => Number(val).toFixed(4);
const formatCurrency = (val) => `₨ ${Number(val).toLocaleString('en-PK', { minimumFractionDigits: 2 })}`;
```

---

## API Summary Table

| # | Action | Method | Endpoint | Auth | Body Type |
|---|--------|--------|----------|------|-----------|
| 6.1 | Create Warehouse | POST | `/api/warehouse/create` | Yes | WarehouseCreateRequestDTO |
| 6.2 | Update Warehouse | PUT | `/api/warehouse/update/{id}` | Yes | WarehouseCreateRequestDTO |
| 6.3 | Get Warehouse | GET | `/api/warehouse/get/{id}` | No | — |
| 6.4 | List by Org | POST | `/api/warehouse/getByOrganization` | Yes | FilterPaginationRequest |
| 6.5 | List by Project | POST | `/api/warehouse/getByProject` | Yes | FilterPaginationRequest |
| 6.6 | List by Type | GET | `/api/warehouse/organization/{orgId}/type/{type}` | No | — |
| 6.7 | Deactivate | POST | `/api/warehouse/deactivate/{id}` | Yes | — |
| 6.8 | Activate | POST | `/api/warehouse/activate/{id}` | Yes | — |
| 7.1 | Stock by Warehouse | POST | `/api/stock/getByWarehouse` | Yes | FilterPaginationRequest |
| 7.2 | Stock by Item | POST | `/api/stock/getByItem` | Yes | FilterPaginationRequest |
| 7.3 | Stock by WH+Item | GET | `/api/stock/warehouse/{whId}/item/{itemId}` | No | — |
| 7.4 | Available by WH | GET | `/api/stock/available/warehouse/{whId}` | No | — |
| 7.5 | Available by Item | GET | `/api/stock/available/item/{itemId}` | No | — |
| 7.6 | Inventory Summary | GET | `/api/stock/inventory/summary/{whId}` | No | — |
| 7.7 | Total Qty by Item | GET | `/api/stock/total/item/{itemId}` | No | — |
| 7.8 | Low Stock Items | GET | `/api/stock/low-stock?threshold=N` | No | — |
| 7.9 | Adjust Stock | POST | `/api/stock/adjust` | Yes | StockAdjustmentRequestDTO |
| 7.10 | Transfer Stock | POST | `/api/stock/transfer` | Yes | StockTransferRequestDTO |
| 7.11 | Reserve Stock | POST | `/api/stock/reserve?params` | Yes | Query Params |
| 7.12 | Release Reserve | POST | `/api/stock/release-reservation?params` | Yes | Query Params |
| 8.1 | Ledger by WH | POST | `/api/stock-ledger/getByWarehouse` | Yes | FilterPaginationRequest |
| 8.2 | Ledger by Item | POST | `/api/stock-ledger/getByItem` | Yes | FilterPaginationRequest |
| 8.3 | Ledger by WH+Item | POST | `/api/stock-ledger/getByWarehouseAndItem` | Yes | FilterPaginationRequest |
| 8.4 | Ledger by Ref | GET | `/api/stock-ledger/reference/{refType}/{refId}` | No | Query Params |
| 8.5 | Ledger by RefType | GET | `/api/stock-ledger/reference-type/{refType}` | No | Query Params |
| 8.6 | Ledger WH+DateRange | GET | `/api/stock-ledger/warehouse/{whId}/date-range` | No | Query Params |
| 8.7 | Ledger Item+DateRange | GET | `/api/stock-ledger/item/{itemId}/date-range` | No | Query Params |
| 8.8 | Ledger Ref List | GET | `/api/stock-ledger/reference-list/{refType}/{refId}` | No | — |
| 9.1 | Process Expense Items | POST | `/api/warehouse-integration/expense-items` | Yes | ExpenseItemRequestDTO |
| 9.2 | Issue Material | POST | `/api/warehouse-integration/issue-material` | Yes | MaterialIssueRequestDTO |
| 9.3 | Get Expense Items | GET | `/api/warehouse-integration/expense-items/{expenseId}` | No | — |
| 9.4 | Stock Effect Items | GET | `/api/warehouse-integration/stock-effect-items/warehouse/{whId}` | No | — |

---

**Total APIs: 30**

> This document covers the complete Warehouse & Inventory Management module. All APIs follow the standard response wrapper pattern (`responseCode`, `responseMessage`, `data`). Paginated endpoints use `FilterPaginationRequest` for POST-based pagination and query parameters for GET-based pagination.
