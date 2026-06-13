# REMS Reporting Module - Frontend Integration Guide

**Last Updated:** June 7, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [API Endpoints](#api-endpoints)
3. [Data Models](#data-models)
4. [Enums & Constants](#enums--constants)
5. [Dashboard System](#dashboard-system)
6. [Report System](#report-system)
7. [Filtering & Pagination](#filtering--pagination)
8. [Export Functionality](#export-functionality)
9. [Authentication & Authorization](#authentication--authorization)
10. [Example Usage](#example-usage)

---

## Overview

The Reporting Module provides a unified, role-aware interface for:
- **Multiple role-specific dashboards** with KPI cards and aggregated metrics
- **Detailed reporting** across business domains (Finance, HR, Warehouse, Procurement, Sales)
- **Dynamic filter UIs** based on report metadata
- **Report navigation** as a three-level sidebar tree (Reporting → Domain → Report)
- **CSV export** capability for report data

All APIs are prefixed with `/api/reporting` and are secured via JWT authentication.

---

## API Endpoints

### 1. Navigation (Sidebar Menu)

**GET** `/api/reporting/navigation`

Returns a three-level sidebar tree structure for report navigation.

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "finance": {
      "domain": "Finance & Accounting",
      "icon": "FaMoneyBillWave",
      "reports": [
        {
          "key": "finance-expense-report",
          "title": "Expense Report",
          "icon": "FaReceipt",
          "route": "/dashboard/reporting/finance/finance-expense-report"
        }
      ]
    },
    "hr": {
      "domain": "Human Resources",
      "icon": "FaUsers",
      "reports": [ /* HR reports */ ]
    },
    "warehouse": {
      "domain": "Warehouse & Inventory",
      "icon": "FaWarehouse",
      "reports": [ /* Warehouse reports */ ]
    },
    "procurement": {
      "domain": "Procurement",
      "icon": "FaTruck",
      "reports": [ /* Procurement reports */ ]
    },
    "sales": {
      "domain": "Sales & Bookings",
      "icon": "FaChartLine",
      "reports": [ /* Sales reports */ ]
    }
  }
}
```

---

### 2. Report Catalog (Flat List)

**GET** `/api/reporting/catalog`

Returns a flat list of all reports accessible to the current user based on their role.

**Response:**
```json
{
  "status": "SUCCESS",
  "data": [
    {
      "key": "finance-expense-report",
      "title": "Expense Report",
      "description": "All expenses within a date range with paid/credit breakdown.",
      "domain": "finance",
      "domainLabel": "Finance & Accounting",
      "route": "/dashboard/reporting/finance/finance-expense-report",
      "icon": "FaReceipt",
      "allowedRoles": ["ACCOUNTANT", "ADMIN"],
      "supportedFilters": ["dateFrom", "dateTo"],
      "columns": [
        {
          "key": "expenseTitle",
          "label": "Title",
          "type": "TEXT",
          "sortable": true,
          "sortKey": "expenseTitle"
        },
        {
          "key": "totalAmount",
          "label": "Total",
          "type": "CURRENCY",
          "sortable": true,
          "sortKey": "totalAmount"
        }
      ]
    }
  ]
}
```

---

### 3. Report Definition (Metadata)

**GET** `/api/reporting/reports/{reportKey}/definition`

Returns the metadata (filters, columns, allowed roles) for a specific report. Use this to build the filter UI dynamically.

**Parameters:**
- `reportKey` (path): Unique identifier for the report (e.g., `finance-expense-report`)

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "key": "finance-expense-report",
    "title": "Expense Report",
    "description": "All expenses within a date range with paid/credit breakdown.",
    "domain": "finance",
    "domainLabel": "Finance & Accounting",
    "route": "/dashboard/reporting/finance/finance-expense-report",
    "icon": "FaReceipt",
    "allowedRoles": ["ACCOUNTANT"],
    "supportedFilters": ["dateFrom", "dateTo"],
    "columns": [
      {
        "key": "expenseTitle",
        "label": "Title",
        "type": "TEXT",
        "sortable": true,
        "sortKey": "expenseTitle"
      },
      {
        "key": "vendorName",
        "label": "Vendor",
        "type": "TEXT",
        "sortable": false,
        "sortKey": "vendorName"
      },
      {
        "key": "totalAmount",
        "label": "Total",
        "type": "CURRENCY",
        "sortable": true,
        "sortKey": "totalAmount"
      },
      {
        "key": "paymentStatus",
        "label": "Status",
        "type": "BADGE",
        "sortable": false,
        "sortKey": "paymentStatus"
      },
      {
        "key": "createdDate",
        "label": "Date",
        "type": "DATETIME",
        "sortable": true,
        "sortKey": "createdDate"
      }
    ]
  }
}
```

---

### 4. Run Report

**POST** `/api/reporting/reports/{reportKey}`

Executes a report with optional filters, pagination, and sorting.

**Parameters:**
- `reportKey` (path): Unique identifier for the report

**Request Body:**
```json
{
  "page": 0,
  "size": 10,
  "sortBy": "createdDate",
  "sortDir": "desc",
  "filter": {
    "dateFrom": "2026-05-01",
    "dateTo": "2026-06-07",
    "organizationId": 1,
    "projectId": null,
    "vendorId": null,
    "customerId": null,
    "employeeId": null,
    "warehouseId": null,
    "departmentId": null,
    "month": null,
    "year": null,
    "status": null,
    "search": null,
    "extra": {}
  },
  "exportFormat": "NONE"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "reportKey": "finance-expense-report",
    "title": "Expense Report",
    "domain": "finance",
    "columns": [
      {
        "key": "expenseTitle",
        "label": "Title",
        "type": "TEXT",
        "sortable": true,
        "sortKey": "expenseTitle"
      },
      {
        "key": "totalAmount",
        "label": "Total",
        "type": "CURRENCY",
        "sortable": true,
        "sortKey": "totalAmount"
      }
    ],
    "rows": [
      {
        "id": 1,
        "expenseTitle": "Office Supplies",
        "vendorName": "Vendor A",
        "projectName": "Project X",
        "expenseType": "SUPPLIES",
        "totalAmount": 1500.00,
        "amountPaid": 1500.00,
        "creditAmount": 0.00,
        "paymentStatus": "PAID",
        "createdDate": "2026-06-01T10:30:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "summary": {
      "totalAmount": 45000.00,
      "amountPaid": 35000.00,
      "creditAmount": 10000.00,
      "recordCount": 42
    },
    "generatedAt": "2026-06-07T12:30:00"
  }
}
```

---

### 5. Export Report

**POST** `/api/reporting/reports/{reportKey}/export`

Exports a filtered report in the requested format (currently CSV only).

**Parameters:**
- `reportKey` (path): Unique identifier for the report
- `format` (query, optional): Export format. Default is `CSV`. Supported: `CSV`, `PDF`, `EXCEL`

**Request Body:** Same as Run Report

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "downloadUrl": "/downloads/reports/finance-expense-report-20260607-123000.csv",
    "fileName": "finance-expense-report-20260607-123000.csv",
    "format": "CSV"
  }
}
```

---

### 6. Dashboard (User's Role)

**GET** `/api/reporting/dashboard`

Returns the dashboard for the logged-in user's primary role (ADMIN > ACCOUNTANT > HR > OPERATIONS).

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "role": "ACCOUNTANT",
    "title": "Finance Dashboard",
    "subtitle": "Accounting & cash overview",
    "organizationId": 1,
    "generatedAt": "2026-06-07T12:30:00",
    "sections": [
      {
        "key": "liquidity",
        "title": "Liquidity",
        "icon": "FaCoins",
        "cards": [
          {
            "key": "cashBalance",
            "label": "Cash & Bank Balance",
            "value": 150000.00,
            "type": "CURRENCY",
            "icon": "FaWallet",
            "color": "blue",
            "hint": null
          },
          {
            "key": "receivable",
            "label": "Total Receivable",
            "value": 75000.00,
            "type": "CURRENCY",
            "icon": "FaHandHoldingUsd",
            "color": "green",
            "hint": null
          },
          {
            "key": "payable",
            "label": "Total Payable",
            "value": 45000.00,
            "type": "CURRENCY",
            "icon": "FaFileInvoiceDollar",
            "color": "red",
            "hint": null
          }
        ],
        "charts": []
      },
      {
        "key": "movement",
        "title": "Cash Movement (Last 30 Days)",
        "icon": "FaExchangeAlt",
        "cards": [
          {
            "key": "credit30",
            "label": "Credits (30d)",
            "value": 125000.00,
            "type": "CURRENCY",
            "icon": "FaArrowDown",
            "color": "green",
            "hint": null
          },
          {
            "key": "debit30",
            "label": "Debits (30d)",
            "value": 95000.00,
            "type": "CURRENCY",
            "icon": "FaArrowUp",
            "color": "red",
            "hint": null
          }
        ],
        "charts": []
      },
      {
        "key": "spend",
        "title": "Spend (Last 30 Days)",
        "icon": "FaReceipt",
        "cards": [
          {
            "key": "expenseTotal30",
            "label": "Expenses (30d)",
            "value": 45000.00,
            "type": "CURRENCY",
            "icon": "FaReceipt",
            "color": "orange",
            "hint": null
          },
          {
            "key": "expensePaid30",
            "label": "Paid (30d)",
            "value": 35000.00,
            "type": "CURRENCY",
            "icon": "FaCheckCircle",
            "color": "green",
            "hint": null
          },
          {
            "key": "expenseCredit30",
            "label": "On Credit (30d)",
            "value": 10000.00,
            "type": "CURRENCY",
            "icon": "FaClock",
            "color": "red",
            "hint": null
          }
        ],
        "charts": []
      }
    ]
  }
}
```

---

### 7. Dashboard for Specific Role

**GET** `/api/reporting/dashboard/{role}`

Returns the dashboard for a specific role. Users can only view their own role's dashboard unless they are ADMIN.

**Parameters:**
- `role` (path): Role name (e.g., `ADMIN`, `ACCOUNTANT`, `HR`, `OPERATIONS`)

**Response:** Same structure as Dashboard endpoint

---

## Data Models

### ReportRequest

Standard request envelope for running a report.

```json
{
  "page": 0,                  // Page number (0-indexed)
  "size": 10,                 // Records per page (max 500)
  "sortBy": "createdDate",    // Column to sort by
  "sortDir": "desc",          // Sort direction: "asc" or "desc"
  "filter": { /* ... */ },    // Filter object (see ReportFilter)
  "exportFormat": "NONE"      // Export format: NONE, CSV, PDF, EXCEL
}
```

### ReportFilter

Generic, extensible filter payload shared by all reports. Individual providers only read the fields they support.

```json
{
  "dateFrom": "2026-05-01",        // ISO date string (yyyy-MM-dd)
  "dateTo": "2026-06-07",          // ISO date string
  "organizationId": 1,              // Organization ID
  "projectId": null,                // Project ID
  "vendorId": null,                 // Vendor ID
  "customerId": null,               // Customer ID
  "employeeId": null,               // Employee ID
  "warehouseId": null,              // Warehouse ID
  "departmentId": null,             // Department ID
  "month": null,                    // Month (1-12)
  "year": null,                     // Year
  "status": null,                   // Generic status filter
  "search": null,                   // Free-text search
  "extra": {}                       // Custom report-specific filters
}
```

### ReportResult

Standard response for a report execution.

```json
{
  "reportKey": "finance-expense-report",
  "title": "Expense Report",
  "domain": "finance",
  "columns": [ /* ReportColumn[] */ ],
  "rows": [ /* Any[] - report data rows */ ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "summary": {
    "totalAmount": 45000.00,
    "amountPaid": 35000.00,
    "creditAmount": 10000.00,
    "recordCount": 42
  },
  "generatedAt": "2026-06-07T12:30:00"
}
```

### ReportColumn

Column metadata for dynamic table rendering.

```json
{
  "key": "expenseTitle",           // Property name in row objects
  "label": "Title",                // Display label
  "type": "TEXT",                  // ColumnType enum
  "sortable": true,                // Can be sorted
  "sortKey": "expenseTitle"        // Backend sort property
}
```

### DashboardResponse

Role-specific dashboard payload.

```json
{
  "role": "ACCOUNTANT",
  "title": "Finance Dashboard",
  "subtitle": "Accounting & cash overview",
  "organizationId": 1,
  "generatedAt": "2026-06-07T12:30:00",
  "sections": [ /* DashboardSection[] */ ]
}
```

### DashboardSection

Logical grouping of dashboard content.

```json
{
  "key": "liquidity",
  "title": "Liquidity",
  "icon": "FaCoins",
  "cards": [ /* DashboardCard[] */ ],
  "charts": [ /* DashboardSeries[] */ ]
}
```

### DashboardCard

A single KPI tile on a dashboard.

```json
{
  "key": "cashBalance",
  "label": "Cash & Bank Balance",
  "value": 150000.00,
  "type": "CURRENCY",
  "icon": "FaWallet",
  "color": "blue",
  "hint": null
}
```

---

## Enums & Constants

### ColumnType

Hints for frontend rendering of report columns and dashboard metrics.

```
TEXT       - Plain text
NUMBER     - Numeric value
CURRENCY   - Monetary amount
DATE       - Date only (yyyy-MM-dd)
DATETIME   - Date and time
BADGE      - Badge/tag (e.g., status)
BOOLEAN    - True/false
PERCENT    - Percentage (0-100)
```

### ReportDomain

Business domains in the reporting system.

```
FINANCE       - Finance & Accounting (icon: FaMoneyBillWave)
HR            - Human Resources (icon: FaUsers)
WAREHOUSE     - Warehouse & Inventory (icon: FaWarehouse)
PROCUREMENT   - Procurement (icon: FaTruck)
SALES         - Sales & Bookings (icon: FaChartLine)
GENERAL       - General (icon: FaThLarge)
```

### ReportingRole

Logical reporting roles for dashboard personalization and report visibility.

```
ADMIN       - Full access to all reports and dashboards
ACCOUNTANT  - Finance-focused (expense, cash, payables/receivables)
HR          - Human resources (employees, attendance, payroll)
OPERATIONS  - Warehouse and procurement operations
```

**Note:** Role resolution is flexible. The system accepts common naming variations:
- "Account", "Accounting", "Finance" → ACCOUNTANT
- "HR", "Human", "Payroll" → HR
- "Operations", "Ops", "Warehouse" → OPERATIONS
- "Admin", "Super" → ADMIN

### ExportFormat

Supported export formats.

```
NONE       - No export
CSV        - Comma-separated values (implemented)
PDF        - PDF format (reserved)
EXCEL      - Excel spreadsheet (reserved)
```

---

## Dashboard System

### Available Dashboards by Role

#### Admin Dashboard
- **Sections:**
  - Financial Overview (Cash, Receivable, Payable, Expenses 30d)
  - Workforce (Total & Active Employees)
  - Operations (Projects, Warehouses, Open POs, Active Bookings)

#### Accountant Dashboard
- **Sections:**
  - Liquidity (Cash, Receivable, Payable)
  - Cash Movement - Last 30 Days (Credits, Debits)
  - Spend - Last 30 Days (Total Expenses, Paid, On Credit)

#### HR Dashboard
- **Sections:**
  - Workforce (Total Employees, Active, Departments)
  - Attendance & Leave - Today (Present, On Leave, Pending Requests)
  - Payroll - Current Month (Month Total, Slips Generated)

#### Operations Dashboard
- **Sections:**
  - Inventory (Warehouses)
  - Procurement (Open POs, Partial POs)
  - Operations (Projects, Active Bookings)

---

## Report System

### Available Reports

#### Finance Domain

**1. Expense Report** (`finance-expense-report`)
- **Allowed Roles:** ACCOUNTANT
- **Supported Filters:** dateFrom, dateTo
- **Columns:** Title, Vendor, Project, Type, Total, Paid, Credit, Status, Date
- **Summary:** totalAmount, amountPaid, creditAmount, recordCount

**2. Cash Transaction Report** (`finance-cash-transaction-report`)
- **Allowed Roles:** ACCOUNTANT
- **Supported Filters:** dateFrom, dateTo, transactionType
- **Columns:** Reference, Type, Amount, Balance, Date

**3. Vendor Payable Report** (`finance-vendor-payable-report`)
- **Allowed Roles:** ACCOUNTANT
- **Supported Filters:** vendorId, status
- **Columns:** Vendor, Balance, ReferenceNo, DueDate

#### Sales Domain

**1. Booking Report** (`sales-booking-report`)
- **Allowed Roles:** OPERATIONS
- **Supported Filters:** projectId
- **Columns:** Booking #, Project, Unit, Unit Serial, Customer, Completed, Booked On
- **Summary:** recordCount

#### Warehouse Domain

**1. Stock Report** (`warehouse-stock-report`)
- **Allowed Roles:** OPERATIONS
- **Supported Filters:** warehouseId
- **Columns:** Item, Quantity, Reorder Level, Last Updated

**2. Low Stock Report** (`warehouse-low-stock-report`)
- **Allowed Roles:** OPERATIONS
- **Supported Filters:** warehouseId, threshold
- **Columns:** Item, Current Stock, Reorder Level, Warehouse

---

## Filtering & Pagination

### Pagination Parameters

All report endpoints support standard pagination:

```json
{
  "page": 0,              // 0-indexed page number
  "size": 10              // Records per page (capped at 500)
}
```

### Sorting

```json
{
  "sortBy": "createdDate",   // Column key to sort by (must be in definition)
  "sortDir": "desc"          // "asc" or "desc"
}
```

**Note:** Only columns with `"sortable": true` support sorting.

### Common Filter Operators

Filters support both simple direct values and date ranges:

- **Date Range:** Both `dateFrom` and `dateTo` (ISO format: yyyy-MM-dd)
- **Entity IDs:** organizationId, projectId, vendorId, customerId, etc.
- **Status:** Generic status field (varies by report)
- **Search:** Free-text search (report-specific)
- **Custom Filters:** Use the `extra` object for report-specific filters

---

## Export Functionality

### Export Endpoints

**POST** `/api/reporting/reports/{reportKey}/export`

**Query Parameters:**
- `format`: Export format (CSV, PDF, EXCEL). Default: CSV

**Request Body:** Same as Run Report endpoint

### Export Response

```json
{
  "status": "SUCCESS",
  "data": {
    "downloadUrl": "/downloads/reports/finance-expense-report-20260607-123000.csv",
    "fileName": "finance-expense-report-20260607-123000.csv",
    "format": "CSV"
  }
}
```

### CSV Export Details

- All columns from the report definition are included
- Summary data is appended at the end
- Headers use the column labels
- Data is formatted according to ColumnType (dates, currency, etc.)
- Maximum export size: 5000 records per download

---

## Authentication & Authorization

### JWT Token

All requests require a valid JWT token in the `Authorization` header:

```
Authorization: Bearer {jwt_token}
```

### User Context

The JWT token contains:
- **username:** Identifier for the user
- **organizationId:** Organization the user belongs to
- **roles:** Array of raw role names (mapped to ReportingRole internally)

### Role-Based Access Control

- **Navigation:** Only shows reports accessible to the user's roles
- **Report Execution:** Validates user role against report's `allowedRoles`
- **Dashboard Access:**
  - Users can view their own role's dashboard
  - ADMIN users can view dashboards for any role
  - Non-admin users cannot view other roles' dashboards

### Tenant Scoping

- All reports are automatically scoped to the user's organization
- The `organizationId` from the filter is validated against the user's organization
- Cross-organization data access is not permitted

---

## Example Usage

### Example 1: Load Navigation

```javascript
async function loadNavigation() {
  const response = await fetch('/api/reporting/navigation', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${jwtToken}`,
      'Content-Type': 'application/json'
    }
  });
  
  const result = await response.json();
  if (result.status === 'SUCCESS') {
    // Build sidebar tree from result.data
    displayNavigation(result.data);
  }
}
```

### Example 2: Get Report Definition & Build Filter UI

```javascript
async function loadReportDefinition(reportKey) {
  const response = await fetch(`/api/reporting/reports/${reportKey}/definition`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${jwtToken}`
    }
  });
  
  const result = await response.json();
  if (result.status === 'SUCCESS') {
    const definition = result.data;
    
    // Build filter UI based on supportedFilters
    const filterUI = buildFilterForm(definition.supportedFilters);
    
    // Build table columns based on columns array
    const columns = definition.columns.map(col => ({
      field: col.key,
      headerName: col.label,
      sortable: col.sortable,
      type: mapColumnType(col.type)
    }));
    
    return { definition, filterUI, columns };
  }
}
```

### Example 3: Execute a Report

```javascript
async function executeReport(reportKey, filters, page = 0, size = 10) {
  const request = {
    page: page,
    size: size,
    sortBy: 'createdDate',
    sortDir: 'desc',
    filter: {
      organizationId: currentUserOrg,
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo,
      projectId: filters.projectId || null,
      vendorId: filters.vendorId || null,
      extra: filters.extra || {}
    },
    exportFormat: 'NONE'
  };
  
  const response = await fetch(`/api/reporting/reports/${reportKey}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${jwtToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  const result = await response.json();
  if (result.status === 'SUCCESS') {
    const { columns, rows, page, totalPages, summary } = result.data;
    
    // Render table with columns and rows
    renderTable(columns, rows);
    
    // Display pagination controls
    displayPagination(page, totalPages);
    
    // Show summary metrics
    displaySummary(summary);
  }
}
```

### Example 4: Export Report

```javascript
async function exportReport(reportKey, filters, format = 'CSV') {
  const request = {
    page: 0,
    size: 5000,
    filter: {
      organizationId: currentUserOrg,
      ...filters
    },
    exportFormat: format
  };
  
  const response = await fetch(`/api/reporting/reports/${reportKey}/export?format=${format}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${jwtToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  const result = await response.json();
  if (result.status === 'SUCCESS') {
    // Trigger file download
    window.open(result.data.downloadUrl, '_blank');
  }
}
```

### Example 5: Load Dashboard

```javascript
async function loadDashboard(role = null) {
  const url = role 
    ? `/api/reporting/dashboard/${role}`
    : '/api/reporting/dashboard';
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${jwtToken}`
    }
  });
  
  const result = await response.json();
  if (result.status === 'SUCCESS') {
    const dashboard = result.data;
    
    // Render dashboard title and subtitle
    displayDashboardHeader(dashboard.title, dashboard.subtitle);
    
    // Render each section with its cards
    dashboard.sections.forEach(section => {
      const sectionElement = createSection(section.key, section.title, section.icon);
      
      section.cards.forEach(card => {
        const cardElement = createCard(
          card.label,
          card.value,
          card.type,
          card.icon,
          card.color
        );
        sectionElement.appendChild(cardElement);
      });
      
      dashboardContainer.appendChild(sectionElement);
    });
  }
}
```

### Example 6: Handle Column Type in Frontend

```javascript
function formatCellValue(value, columnType) {
  switch (columnType) {
    case 'CURRENCY':
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
      }).format(value);
    
    case 'NUMBER':
      return new Intl.NumberFormat('en-US').format(value);
    
    case 'DATE':
      return new Date(value).toLocaleDateString();
    
    case 'DATETIME':
      return new Date(value).toLocaleString();
    
    case 'PERCENT':
      return `${value}%`;
    
    case 'BOOLEAN':
      return value ? 'Yes' : 'No';
    
    case 'BADGE':
      return `<span class="badge">${value}</span>`;
    
    case 'TEXT':
    default:
      return value;
  }
}
```

---

## Error Handling

All error responses follow these codes:

```json
{
  "status": "INVALID_PARAMETER",  // Field validation error
  "message": "description"
}
```

```json
{
  "status": "INVALID_USER",       // Authentication/authorization failure
  "message": "description"
}
```

```json
{
  "status": "SYSTEM_FAILURE",     // Unexpected server error
  "message": "description"
}
```

---

## Performance Considerations

1. **Pagination:** Always paginate large datasets (size ≤ 100 recommended for UI)
2. **Filtering:** Use filters to reduce result sets before export
3. **Export Size Cap:** Exports are capped at 5000 records
4. **Timezone:** All dates are in UTC; convert in frontend if needed
5. **Caching:** Dashboard data changes frequently; avoid aggressive caching

---

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2026-06-07 | 1.0 | Initial release: Navigation, Catalog, Reports, Dashboards, Export |

---

## Support & Contact

For integration questions or bug reports, contact the backend team.

