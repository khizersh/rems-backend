# GRN + Warehouse Integration — Frontend Change Document

> **Module:** Purchase Management (GRN) + Warehouse Integration  
> **Backend Framework:** Spring Boot + JPA  
> **Frontend Framework:** React  
> **Base URL:** `http://localhost:8081`  
> **Authentication:** All requests require JWT Bearer token in `Authorization` header  
> **Last Updated:** March 18, 2026

---

## Table of Contents

1. [Overview of Changes](#1-overview-of-changes)
2. [New Enums](#2-new-enums)
3. [GRN Entity Changes](#3-grn-entity-changes)
4. [GRN Create API (Updated)](#4-grn-create-api-updated)
5. [GRN Update API (Updated)](#5-grn-update-api-updated)
6. [GRN Cancel API (New)](#6-grn-cancel-api-new)
7. [GRN Get By ID API (Updated Response)](#7-grn-get-by-id-api-updated-response)
8. [GRN Filter API (Updated)](#8-grn-filter-api-updated)
9. [GRN Get By PO ID (Updated Response)](#9-grn-get-by-po-id-updated-response)
10. [Warehouse Dropdown APIs](#10-warehouse-dropdown-apis)
11. [Frontend UI Flows](#11-frontend-ui-flows)
12. [cURL Commands Reference](#12-curl-commands-reference)
13. [React Integration Guide](#13-react-integration-guide)

---

## 1. Overview of Changes

### What Changed

The GRN module now integrates with the Warehouse module. When creating or updating a GRN, the user must select a **Receipt Type** that determines how goods are handled:

| Receipt Type | Behavior | Required Field |
|---|---|---|
| `WAREHOUSE_STOCK` | Goods go into a warehouse → stock is added automatically | `warehouseId` (required) |
| `DIRECT_CONSUME` | Goods consumed directly by a project → no stock entry | `directConsumeProjectId` (required) |
| `null` (not provided) | No warehouse or project assignment | Neither required |

### New Fields in GRN

| Field | Type | Description |
|---|---|---|
| `receiptType` | `string` (enum) | `WAREHOUSE_STOCK` or `DIRECT_CONSUME` or `null` |
| `warehouseId` | `number` (nullable) | ID of the target warehouse (required when `receiptType = WAREHOUSE_STOCK`) |
| `directConsumeProjectId` | `number` (nullable) | ID of project consuming goods directly (required when `receiptType = DIRECT_CONSUME`) |
| `warehouseName` | `string` (read-only) | Name of the warehouse (returned in GET responses) |
| `directConsumeProjectName` | `string` (read-only) | Name of the direct consume project (returned in GET responses) |

### New GRN Behaviors

1. **Create GRN with WAREHOUSE_STOCK** → Stock automatically added to selected warehouse with rates from PO items
2. **Create GRN with DIRECT_CONSUME** → No stock entry, goods marked as project consumption
3. **Update GRN** → Old warehouse stock is automatically reversed, then new stock is added
4. **Cancel GRN** → Warehouse stock is reversed, PO quantities reverted, status set to CANCELLED

---

## 2. New Enums

### ReceiptType

```javascript
const RECEIPT_TYPE = {
  WAREHOUSE_STOCK: "WAREHOUSE_STOCK",   // Goods go into warehouse
  DIRECT_CONSUME: "DIRECT_CONSUME"      // Goods consumed directly by project
};
```

### WarehouseType (for dropdown filtering)

```javascript
const WAREHOUSE_TYPE = {
  CENTRAL: "CENTRAL",
  PROJECT: "PROJECT",
  TEMP: "TEMP"
};
```

---

## 3. GRN Entity Changes

### Full GRN Object (GET Response)

```json
{
  "id": 1,
  "grnNumber": "GRN-20260318-001",
  "orgId": 1,
  "projectId": 5,
  "vendorId": 3,
  "poId": 7,
  "status": "RECEIVED",
  "receivedDate": "2026-03-18T10:30:00",
  "receiptType": "WAREHOUSE_STOCK",
  "warehouseId": 2,
  "directConsumeProjectId": null,
  "invoiceStatus": "NOT_INVOICED",
  "createdBy": "admin",
  "updatedBy": "admin",
  "createdDate": "2026-03-18T10:30:00",
  "updatedDate": "2026-03-18T10:30:00",
  "grnItemsList": [
    {
      "id": 10,
      "grnId": 1,
      "poItemId": 15,
      "itemId": 5,
      "quantityReceived": 100.0,
      "quantityInvoiced": 0.0,
      "itemName": "Cement Bags",
      "createdBy": "admin",
      "updatedBy": "admin",
      "createdDate": "2026-03-18T10:30:00",
      "updatedDate": "2026-03-18T10:30:00"
    }
  ],
  "projectName": "Ghousia Arcade",
  "vendorName": "ABC Suppliers",
  "poNumber": "PO-20260315-001",
  "poStatus": "PARTIAL",
  "warehouseName": "Central Warehouse Karachi",
  "directConsumeProjectName": null
}
```

---

## 4. GRN Create API (Updated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/grn/create` |
| **Auth** | Required (JWT Bearer) |
| **Content-Type** | `application/json` |

### Request Body

```json
{
  "poId": 7,
  "receivedDate": "2026-03-18T10:30:00",
  "receiptType": "WAREHOUSE_STOCK",
  "warehouseId": 2,
  "directConsumeProjectId": null,
  "grnItemsList": [
    {
      "poItemId": 15,
      "quantityReceived": 100.0
    },
    {
      "poItemId": 16,
      "quantityReceived": 50.0
    }
  ]
}
```

### Field Descriptions

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `poId` | `number` | **Yes** | Must be a valid PO that is not CLOSED or CANCELLED |
| `receivedDate` | `datetime` | No | Defaults to current time if null |
| `receiptType` | `string` | No | `WAREHOUSE_STOCK` or `DIRECT_CONSUME` or null |
| `warehouseId` | `number` | **Conditional** | **Required** when `receiptType = WAREHOUSE_STOCK`. Warehouse must be active. |
| `directConsumeProjectId` | `number` | **Conditional** | **Required** when `receiptType = DIRECT_CONSUME`. Project must exist. |
| `grnItemsList` | `array` | **Yes** | At least one item required |
| `grnItemsList[].poItemId` | `number` | **Yes** | Must be a valid PO item ID |
| `grnItemsList[].quantityReceived` | `number` | **Yes** | Must not exceed pending quantity |

### Conditional Validation Rules

| If `receiptType` is... | Then... |
|---|---|
| `WAREHOUSE_STOCK` | `warehouseId` is required; `directConsumeProjectId` is auto-cleared |
| `DIRECT_CONSUME` | `directConsumeProjectId` is required; `warehouseId` is auto-cleared |
| `null` | No warehouse or project required |

### What Happens Behind the Scenes

- **WAREHOUSE_STOCK**: Each GRN item's received quantity is added to the warehouse stock at the PO item's rate. Stock ledger entries are created.
- **DIRECT_CONSUME**: No stock changes. Goods are marked as consumed by the project.

### Success Response

```json
{
  "data": "GRN created successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

### Error Responses

```json
{
  "data": "Warehouse ID is required when receipt type is WAREHOUSE_STOCK",
  "responseMessage": "Invalid Parameter",
  "responseCode": "1001"
}
```

```json
{
  "data": "Cannot use inactive warehouse",
  "responseMessage": "Invalid Parameter",
  "responseCode": "1001"
}
```

---

## 5. GRN Update API (Updated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/grn/update/{grnId}` |
| **Auth** | Required (JWT Bearer) |
| **Content-Type** | `application/json` |

### Request Body

Same structure as Create. You can change the `receiptType`, `warehouseId`, or `directConsumeProjectId`.

```json
{
  "poId": 7,
  "receivedDate": "2026-03-18T10:30:00",
  "receiptType": "WAREHOUSE_STOCK",
  "warehouseId": 3,
  "directConsumeProjectId": null,
  "grnItemsList": [
    {
      "poItemId": 15,
      "quantityReceived": 120.0
    }
  ]
}
```

### What Happens Behind the Scenes on Update

1. **Old warehouse stock is reversed** (if previously `WAREHOUSE_STOCK`) — deducts old quantities
2. Old PO received quantities are reverted
3. New validations run
4. GRN items are replaced
5. New PO quantities are applied
6. **New warehouse stock is added** (if new `receiptType = WAREHOUSE_STOCK`)
7. PO status recalculated

### Scenario Matrix (Update)

| Old Receipt Type | New Receipt Type | Action |
|---|---|---|
| WAREHOUSE_STOCK (WH-1) | WAREHOUSE_STOCK (WH-1) | Reverse old → Add new (same WH) |
| WAREHOUSE_STOCK (WH-1) | WAREHOUSE_STOCK (WH-2) | Reverse old WH-1 → Add new to WH-2 |
| WAREHOUSE_STOCK (WH-1) | DIRECT_CONSUME | Reverse old WH-1 → No stock added |
| DIRECT_CONSUME | WAREHOUSE_STOCK (WH-1) | No reverse needed → Add new to WH-1 |
| DIRECT_CONSUME | DIRECT_CONSUME | No stock changes |
| null | WAREHOUSE_STOCK (WH-1) | No reverse needed → Add new to WH-1 |

---

## 6. GRN Cancel API (New)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/grn/cancel/{grnId}` |
| **Auth** | Required (JWT Bearer) |
| **Path Param** | `grnId` — ID of the GRN to cancel |
| **Request Body** | None |

### What Happens

1. Validates GRN is not already cancelled
2. Validates no invoices exist for this GRN (invoiceStatus must be `NOT_INVOICED`)
3. **Reverses warehouse stock** (if receipt type was `WAREHOUSE_STOCK`)
4. **Reverts PO received quantities**
5. Sets GRN status to `CANCELLED`
6. Recalculates PO status

### Success Response

```json
{
  "data": "GRN cancelled successfully",
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

### Error Responses

```json
{
  "data": "GRN is already cancelled",
  "responseMessage": "Invalid Parameter",
  "responseCode": "1001"
}
```

```json
{
  "data": "Cannot cancel GRN that has invoiced items. Please cancel invoices first.",
  "responseMessage": "Invalid Parameter",
  "responseCode": "1001"
}
```

---

## 7. GRN Get By ID API (Updated Response)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/grn/getById/{grnId}` |
| **Auth** | Required (JWT Bearer) |

### New Fields in Response

The response now includes these additional fields:

| Field | Type | Description |
|---|---|---|
| `receiptType` | `string` | `WAREHOUSE_STOCK`, `DIRECT_CONSUME`, or `null` |
| `warehouseId` | `number` | Warehouse ID (if WAREHOUSE_STOCK) |
| `warehouseName` | `string` | Warehouse name (resolved from warehouseId) |
| `directConsumeProjectId` | `number` | Project ID (if DIRECT_CONSUME) |
| `directConsumeProjectName` | `string` | Project name (resolved from directConsumeProjectId) |

### Full Response Example

```json
{
  "data": {
    "id": 1,
    "grnNumber": "GRN-20260318-001",
    "orgId": 1,
    "projectId": 5,
    "vendorId": 3,
    "poId": 7,
    "status": "RECEIVED",
    "receivedDate": "2026-03-18T10:30:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 2,
    "directConsumeProjectId": null,
    "invoiceStatus": "NOT_INVOICED",
    "projectName": "Ghousia Arcade",
    "vendorName": "ABC Suppliers",
    "poNumber": "PO-20260315-001",
    "poStatus": "PARTIAL",
    "warehouseName": "Central Warehouse Karachi",
    "directConsumeProjectName": null,
    "grnItemsList": [
      {
        "id": 10,
        "grnId": 1,
        "poItemId": 15,
        "itemId": 5,
        "quantityReceived": 100.0,
        "quantityInvoiced": 0.0,
        "itemName": "Cement Bags"
      }
    ]
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 8. GRN Filter API (Updated)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/grn/getByStatusAndDateRange` |
| **Auth** | Required (JWT Bearer) |

### Request Body (Updated with new filters)

```json
{
  "orgId": 1,
  "poId": null,
  "vendorId": null,
  "status": "RECEIVED",
  "startDate": "2026-01-01",
  "endDate": "2026-03-18",
  "invoiceStatus": null,
  "warehouseId": 2,
  "receiptType": "WAREHOUSE_STOCK",
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc"
}
```

### New Filter Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orgId` | `number` | **Yes** | Organization ID (mandatory) |
| `poId` | `number` | No | Filter by Purchase Order |
| `vendorId` | `number` | No | Filter by Vendor |
| `status` | `string` | No | `RECEIVED` or `CANCELLED` |
| `startDate` | `string` | No | Start date (ISO format `YYYY-MM-DD`) |
| `endDate` | `string` | No | End date (ISO format `YYYY-MM-DD`) |
| `invoiceStatus` | `string` | No | `NOT_INVOICED`, `PARTIALLY_INVOICED`, `FULLY_INVOICED` |
| `warehouseId` | `number` | No | **NEW** — Filter by warehouse |
| `receiptType` | `string` | No | **NEW** — `WAREHOUSE_STOCK` or `DIRECT_CONSUME` |
| `page` | `number` | No | Page number (default: 0) |
| `size` | `number` | No | Page size (default: 10) |
| `sortBy` | `string` | No | Sort field (default: `createdDate`) |
| `sortDir` | `string` | No | `asc` or `desc` (default: `desc`) |

### Filter Combinations

All filters are optional (except `orgId`). They can be combined freely:

```json
// Get all GRNs sent to a specific warehouse
{ "orgId": 1, "warehouseId": 2, "receiptType": "WAREHOUSE_STOCK" }

// Get all direct consume GRNs
{ "orgId": 1, "receiptType": "DIRECT_CONSUME" }

// Get all uninvoiced GRNs in a specific warehouse
{ "orgId": 1, "warehouseId": 2, "invoiceStatus": "NOT_INVOICED" }

// Get all GRNs (no filters)
{ "orgId": 1 }
```

### Response (each GRN now includes warehouse info)

```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "grnNumber": "GRN-20260318-001",
        "receiptType": "WAREHOUSE_STOCK",
        "warehouseId": 2,
        "warehouseName": "Central Warehouse Karachi",
        "directConsumeProjectId": null,
        "directConsumeProjectName": null,
        "projectName": "Ghousia Arcade",
        "vendorName": "ABC Suppliers",
        "poNumber": "PO-20260315-001",
        "grnItemsList": [...]
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  },
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

---

## 9. GRN Get By PO ID (Updated Response)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/grn/getByPoId/{poId}` |

Each GRN in the response now includes `warehouseName` and `directConsumeProjectName` alongside existing fields.

---

## 10. Warehouse Dropdown APIs

For the GRN form, you need a warehouse dropdown. Use these existing APIs:

### Get All Warehouses by Organization (for dropdown)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/warehouse/organization/{orgId}/type/{warehouseType}` |

Returns list of active warehouses filtered by type.

```
GET /api/warehouse/organization/1/type/CENTRAL
GET /api/warehouse/organization/1/type/PROJECT
```

### Response

```json
{
  "data": [
    {
      "id": 1,
      "name": "Central Warehouse Karachi",
      "code": "CWH-KHI-001",
      "warehouseType": "CENTRAL",
      "projectId": null,
      "organizationId": 1,
      "active": true
    },
    {
      "id": 2,
      "name": "Site Warehouse Block A",
      "code": "PWH-BLK-A",
      "warehouseType": "PROJECT",
      "projectId": 5,
      "organizationId": 1,
      "active": true
    }
  ],
  "responseMessage": "Request Success!",
  "responseCode": "0000"
}
```

### Get All Warehouses (Paginated)

```
POST /api/warehouse/getByOrganization
{
  "id": 1,
  "page": 0,
  "size": 100,
  "sortBy": "name",
  "sortDir": "asc"
}
```

---

## 11. Frontend UI Flows

### Flow 1: Create GRN Form (Updated)

The GRN creation form should now include a **Receipt Type** section:

```
┌─────────────────────────────────────────────────────────────┐
│ Create GRN                                                   │
├─────────────────────────────────────────────────────────────┤
│ Purchase Order: [PO Dropdown]                                │
│ Received Date:  [Date Picker]                                │
│                                                              │
│ ┌── Receipt Type ─────────────────────────────────────────┐  │
│ │  ○ Warehouse Stock    ○ Direct Consume    ○ None        │  │
│ │                                                         │  │
│ │  [If Warehouse Stock]:                                  │  │
│ │  Warehouse: [Dropdown - active warehouses]              │  │
│ │                                                         │  │
│ │  [If Direct Consume]:                                   │  │
│ │  Consume Project: [Dropdown - projects]                 │  │
│ └─────────────────────────────────────────────────────────┘  │
│                                                              │
│ ┌── GRN Items ────────────────────────────────────────────┐  │
│ │ Item        | PO Qty | Received | Pending | Receive Now │  │
│ │ Cement      | 500    | 200      | 300     | [___]       │  │
│ │ Steel Bars  | 100    | 0        | 100     | [___]       │  │
│ └─────────────────────────────────────────────────────────┘  │
│                                                              │
│                              [Cancel]  [Create GRN]          │
└─────────────────────────────────────────────────────────────┘
```

### Form Logic

```javascript
// State management
const [receiptType, setReceiptType] = useState(null); // null, "WAREHOUSE_STOCK", "DIRECT_CONSUME"
const [warehouseId, setWarehouseId] = useState(null);
const [directConsumeProjectId, setDirectConsumeProjectId] = useState(null);
const [warehouses, setWarehouses] = useState([]);

// When receipt type changes
const handleReceiptTypeChange = (type) => {
  setReceiptType(type);
  if (type === "WAREHOUSE_STOCK") {
    setDirectConsumeProjectId(null);
    // Fetch warehouses for dropdown
    fetchWarehouses(orgId);
  } else if (type === "DIRECT_CONSUME") {
    setWarehouseId(null);
    // Fetch projects for dropdown
  } else {
    setWarehouseId(null);
    setDirectConsumeProjectId(null);
  }
};

// Validation before submit
const validateForm = () => {
  if (receiptType === "WAREHOUSE_STOCK" && !warehouseId) {
    showError("Please select a warehouse");
    return false;
  }
  if (receiptType === "DIRECT_CONSUME" && !directConsumeProjectId) {
    showError("Please select a project for direct consumption");
    return false;
  }
  return true;
};

// Build request body
const buildGrnRequest = () => ({
  poId: selectedPoId,
  receivedDate: receivedDate,
  receiptType: receiptType,
  warehouseId: receiptType === "WAREHOUSE_STOCK" ? warehouseId : null,
  directConsumeProjectId: receiptType === "DIRECT_CONSUME" ? directConsumeProjectId : null,
  grnItemsList: grnItems.map(item => ({
    poItemId: item.poItemId,
    quantityReceived: item.quantityReceived
  }))
});
```

### Flow 2: GRN List Page (Updated)

Add new filter dropdowns to the existing GRN filter panel:

```
┌── Filters ──────────────────────────────────────────────┐
│ PO:        [Dropdown]   Vendor:     [Dropdown]           │
│ Status:    [Dropdown]   Invoice:    [Dropdown]           │
│ Receipt:   [WAREHOUSE_STOCK ▼]  Warehouse: [Central ▼]  │  ← NEW
│ Date From: [____]       Date To:    [____]               │
│                                     [Apply Filters]      │
└──────────────────────────────────────────────────────────┘

┌── GRN List ─────────────────────────────────────────────────────────┐
│ GRN #         | PO #     | Vendor  | Receipt Type    | Warehouse   │
│ GRN-001       | PO-001   | ABC     | WAREHOUSE_STOCK | Central WH  │
│ GRN-002       | PO-001   | ABC     | DIRECT_CONSUME  | —           │
│ GRN-003       | PO-002   | XYZ     | —               | —           │
└─────────────────────────────────────────────────────────────────────┘
```

### Flow 3: GRN Detail Page (Updated)

Show warehouse or direct consume info in the detail view:

```
┌── GRN Detail: GRN-20260318-001 ──────────────────────────┐
│ Status: RECEIVED          Invoice Status: NOT_INVOICED    │
│ PO: PO-20260315-001      Vendor: ABC Suppliers            │
│ Project: Ghousia Arcade   Received Date: 2026-03-18       │
│                                                           │
│ Receipt Type: WAREHOUSE_STOCK                             │  ← NEW
│ Warehouse: Central Warehouse Karachi                      │  ← NEW
│                                                           │
│ Items:                                                    │
│ ┌─────────┬──────────┬───────────┬──────────┐            │
│ │ Item    │ Received │ Invoiced  │ Status   │            │
│ │ Cement  │ 100      │ 0         │ Pending  │            │
│ │ Steel   │ 50       │ 0         │ Pending  │            │
│ └─────────┴──────────┴───────────┴──────────┘            │
│                                                           │
│ [Edit]  [Cancel GRN]  [Create Invoice]                    │
└───────────────────────────────────────────────────────────┘
```

### Flow 4: Cancel GRN

```javascript
const handleCancelGrn = async (grnId) => {
  const confirmed = await showConfirmDialog(
    "Cancel GRN",
    "Are you sure? This will reverse warehouse stock and revert PO quantities."
  );
  
  if (confirmed) {
    try {
      const response = await axios.post(
        `${BASE_URL}/api/grn/cancel/${grnId}`,
        null,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (response.data.responseCode === "0000") {
        showSuccess("GRN cancelled successfully");
        refreshGrnList();
      } else {
        showError(response.data.data);
      }
    } catch (error) {
      showError("Failed to cancel GRN");
    }
  }
};
```

---

## 12. cURL Commands Reference

### Create GRN with Warehouse Stock

```bash
curl -X POST http://localhost:8081/api/grn/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "poId": 7,
    "receivedDate": "2026-03-18T10:30:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 2,
    "directConsumeProjectId": null,
    "grnItemsList": [
      {
        "poItemId": 15,
        "quantityReceived": 100.0
      },
      {
        "poItemId": 16,
        "quantityReceived": 50.0
      }
    ]
  }'
```

### Create GRN with Direct Consume

```bash
curl -X POST http://localhost:8081/api/grn/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "poId": 7,
    "receivedDate": "2026-03-18T10:30:00",
    "receiptType": "DIRECT_CONSUME",
    "warehouseId": null,
    "directConsumeProjectId": 5,
    "grnItemsList": [
      {
        "poItemId": 15,
        "quantityReceived": 100.0
      }
    ]
  }'
```

### Create GRN without Warehouse (legacy/no receipt type)

```bash
curl -X POST http://localhost:8081/api/grn/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "poId": 7,
    "receivedDate": "2026-03-18T10:30:00",
    "grnItemsList": [
      {
        "poItemId": 15,
        "quantityReceived": 100.0
      }
    ]
  }'
```

### Update GRN (change warehouse)

```bash
curl -X POST http://localhost:8081/api/grn/update/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "poId": 7,
    "receivedDate": "2026-03-18T10:30:00",
    "receiptType": "WAREHOUSE_STOCK",
    "warehouseId": 3,
    "directConsumeProjectId": null,
    "grnItemsList": [
      {
        "poItemId": 15,
        "quantityReceived": 120.0
      }
    ]
  }'
```

### Cancel GRN

```bash
curl -X POST http://localhost:8081/api/grn/cancel/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### Get GRN By ID

```bash
curl -X GET http://localhost:8081/api/grn/getById/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### Filter GRNs by Warehouse

```bash
curl -X POST http://localhost:8081/api/grn/getByStatusAndDateRange \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "orgId": 1,
    "warehouseId": 2,
    "receiptType": "WAREHOUSE_STOCK",
    "page": 0,
    "size": 10,
    "sortBy": "createdDate",
    "sortDir": "desc"
  }'
```

### Filter GRNs — Direct Consume only

```bash
curl -X POST http://localhost:8081/api/grn/getByStatusAndDateRange \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "orgId": 1,
    "receiptType": "DIRECT_CONSUME",
    "page": 0,
    "size": 10,
    "sortBy": "createdDate",
    "sortDir": "desc"
  }'
```

### Filter GRNs — All filters combined

```bash
curl -X POST http://localhost:8081/api/grn/getByStatusAndDateRange \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "orgId": 1,
    "poId": 7,
    "vendorId": 3,
    "status": "RECEIVED",
    "startDate": "2026-01-01",
    "endDate": "2026-03-18",
    "invoiceStatus": "NOT_INVOICED",
    "warehouseId": 2,
    "receiptType": "WAREHOUSE_STOCK",
    "page": 0,
    "size": 10,
    "sortBy": "createdDate",
    "sortDir": "desc"
  }'
```

### Get Warehouses for Dropdown

```bash
# All CENTRAL warehouses for org
curl -X GET http://localhost:8081/api/warehouse/organization/1/type/CENTRAL

# All PROJECT warehouses for org
curl -X GET http://localhost:8081/api/warehouse/organization/1/type/PROJECT

# All warehouses paginated
curl -X POST http://localhost:8081/api/warehouse/getByOrganization \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "id": 1,
    "page": 0,
    "size": 100,
    "sortBy": "name",
    "sortDir": "asc"
  }'
```

---

## 13. React Integration Guide

### Axios Service Layer (grnService.js updates)

```javascript
import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/grn';

const authHeaders = (token) => ({
  headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
});

// Create GRN with warehouse support
export const createGrn = (payload, token) =>
  axios.post(`${BASE_URL}/create`, payload, authHeaders(token));

// Update GRN with warehouse support
export const updateGrn = (grnId, payload, token) =>
  axios.post(`${BASE_URL}/update/${grnId}`, payload, authHeaders(token));

// Cancel GRN (reverses stock)
export const cancelGrn = (grnId, token) =>
  axios.post(`${BASE_URL}/cancel/${grnId}`, null, authHeaders(token));

// Get GRN by ID (now includes warehouseName, directConsumeProjectName)
export const getGrnById = (grnId, token) =>
  axios.get(`${BASE_URL}/getById/${grnId}`, authHeaders(token));

// Filter GRNs (now supports warehouseId, receiptType filters)
export const getGrnsByFilters = (payload, token) =>
  axios.post(`${BASE_URL}/getByStatusAndDateRange`, payload, authHeaders(token));

// Get GRNs by PO ID (paginated, now includes warehouse info)
export const getGrnsByPoId = (poId, payload, token) =>
  axios.post(`${BASE_URL}/getByPoId/${poId}`, payload, authHeaders(token));
```

### Warehouse Service (warehouseService.js)

```javascript
import axios from 'axios';

const BASE_URL = 'http://localhost:8081/api/warehouse';

// Get warehouses by org and type (for dropdown)
export const getWarehousesByType = (orgId, type) =>
  axios.get(`${BASE_URL}/organization/${orgId}/type/${type}`);

// Get all warehouses paginated
export const getWarehousesByOrg = (payload, token) =>
  axios.post(`${BASE_URL}/getByOrganization`, payload, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
  });
```

### Receipt Type Radio Group Component

```jsx
const ReceiptTypeSelector = ({ value, onChange, warehouses, projects, warehouseId, projectId, onWarehouseChange, onProjectChange }) => (
  <div className="receipt-type-section">
    <label>Receipt Type</label>
    <div className="radio-group">
      <label>
        <input type="radio" value="WAREHOUSE_STOCK" checked={value === "WAREHOUSE_STOCK"} onChange={() => onChange("WAREHOUSE_STOCK")} />
        Warehouse Stock
      </label>
      <label>
        <input type="radio" value="DIRECT_CONSUME" checked={value === "DIRECT_CONSUME"} onChange={() => onChange("DIRECT_CONSUME")} />
        Direct Consume
      </label>
      <label>
        <input type="radio" value="" checked={!value} onChange={() => onChange(null)} />
        None
      </label>
    </div>

    {value === "WAREHOUSE_STOCK" && (
      <select value={warehouseId || ""} onChange={(e) => onWarehouseChange(Number(e.target.value))}>
        <option value="">Select Warehouse</option>
        {warehouses.map(wh => (
          <option key={wh.id} value={wh.id}>{wh.name} ({wh.code})</option>
        ))}
      </select>
    )}

    {value === "DIRECT_CONSUME" && (
      <select value={projectId || ""} onChange={(e) => onProjectChange(Number(e.target.value))}>
        <option value="">Select Project</option>
        {projects.map(p => (
          <option key={p.id} value={p.id}>{p.name}</option>
        ))}
      </select>
    )}
  </div>
);
```

### Display Helper for GRN List

```jsx
const getReceiptTypeDisplay = (grn) => {
  if (grn.receiptType === "WAREHOUSE_STOCK") {
    return { label: "Warehouse", value: grn.warehouseName || `WH-${grn.warehouseId}`, color: "blue" };
  }
  if (grn.receiptType === "DIRECT_CONSUME") {
    return { label: "Direct Consume", value: grn.directConsumeProjectName || `Project-${grn.directConsumeProjectId}`, color: "orange" };
  }
  return { label: "Not Assigned", value: "—", color: "gray" };
};
```

---

## API Summary Table

| # | Action | Method | Endpoint | New/Updated |
|---|--------|--------|----------|-------------|
| 1 | Create GRN | POST | `/api/grn/create` | **Updated** — now accepts `receiptType`, `warehouseId`, `directConsumeProjectId` |
| 2 | Update GRN | POST | `/api/grn/update/{grnId}` | **Updated** — reverses old stock, adds new stock |
| 3 | Cancel GRN | POST | `/api/grn/cancel/{grnId}` | **New** — reverses stock, reverts PO quantities |
| 4 | Get GRN by ID | GET | `/api/grn/getById/{grnId}` | **Updated** — response includes `warehouseName`, `directConsumeProjectName` |
| 5 | Get GRN by PO | POST | `/api/grn/getByPoId/{poId}` | **Updated** — response includes warehouse info |
| 6 | Filter GRNs | POST | `/api/grn/getByStatusAndDateRange` | **Updated** — new filters: `warehouseId`, `receiptType` |
| 7 | Get Warehouses by Type | GET | `/api/warehouse/organization/{orgId}/type/{type}` | Existing — use for dropdown |
| 8 | Get Warehouses (paginated) | POST | `/api/warehouse/getByOrganization` | Existing — use for full list |

---

## Important Notes for Frontend Developers

1. **Backward Compatible**: `receiptType`, `warehouseId`, and `directConsumeProjectId` are all optional. Existing GRN creation without these fields still works.

2. **Conditional UI**: Show warehouse dropdown only when `receiptType = WAREHOUSE_STOCK`. Show project dropdown only when `receiptType = DIRECT_CONSUME`.

3. **Stock Impact Warning**: When user selects `WAREHOUSE_STOCK`, show an informational message: *"Items will be added to warehouse stock upon GRN creation."*

4. **Cancel Confirmation**: Always show a confirmation dialog before cancelling a GRN. Include warning: *"This will reverse warehouse stock entries and revert PO received quantities."*

5. **Edit Mode Pre-fill**: When editing a GRN, pre-fill the receipt type radio, warehouse dropdown, and project dropdown from the existing GRN data (available in getById response).

6. **Filter Defaults**: For the filter page, `warehouseId` and `receiptType` default to `null` (show all).

7. **Disabled Cancel**: Disable the Cancel button if `invoiceStatus` is not `NOT_INVOICED`.
