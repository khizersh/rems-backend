# HR Reporting API — Frontend Integration Guide

Base path: **`/api/reporting/hr`**

This document is for frontend integration of the new **HR Reporting module**. It follows the
exact same conventions as the existing Admin (`/api/reporting/admin`) and Accountant
(`/api/reporting/accountant`) reporting APIs, so if those are already integrated, this module
will feel identical — same envelope, same DTOs, same filter model.

---

## 1. Authentication & Authorization

- All endpoints require a valid JWT (same auth flow as the rest of the app — `Authorization: Bearer <token>`).
- The organization is **always derived from the logged-in user** on the backend. You never send an `organizationId`.
- The caller must hold one of these roles: `HR_ROLE`, `HR_MANAGER_ROLE`, `ADMIN_ROLE`, `FULL_ADMIN_ROLE`.
  If the user lacks an allowed role, the API returns `INVALID_PARAMETER` (see error shape below) — treat this as "access denied" in the UI.

---

## 2. Standard Response Envelope

Every endpoint returns:

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": { "...ReportingView..." }
}
```

Response codes you may see:

| Code | Meaning | Frontend handling |
|------|---------|--------------------|
| `0000` | Success | Render `data` |
| `0002` | Invalid parameter / unauthorized / bad filter | Show validation or "access denied" message using `responseMessage` |
| `9999` | System failure | Show generic error / retry |

---

## 3. The `ReportingView` Payload (same shape for every endpoint)

```ts
interface ReportingView {
  key: string;                 // stable identifier, e.g. "hr-dashboard"
  title: string;
  subtitle: string;
  generatedAt: string;         // ISO datetime
  filtersApplied: AppliedFilters;
  summaryCards: KpiCard[];     // KPI tiles row
  breakdowns: GroupedSummary[]; // grouped label/value blocks (e.g. status splits)
  charts: ChartData[];         // trend / distribution charts
  tables: TableData[];         // drill-down tables
  alerts: ExceptionItem[];     // warnings / info banners
}
```

Every section is **always present** as a (possibly empty) array/object — never `null` or missing.
Render each section only if it has content; this keeps every screen driven by the same generic component set.

### 3.1 KpiCard (summaryCards)

```ts
interface KpiCard {
  key: string;
  label: string;
  value: number;               // raw numeric value (rounded to 2dp for currency)
  formattedValue: string;      // compact display string, e.g. "1.23M", "42"
  changePercent: number | null; // period-over-period % change, null if no baseline
  trend: "UP" | "DOWN" | "FLAT";
  colorHint: "PRIMARY" | "SUCCESS" | "WARNING" | "DANGER" | "INFO" | "NEUTRAL";
  icon: string;                 // FontAwesome icon name, e.g. "FaUsers"
  type: "TEXT" | "NUMBER" | "CURRENCY" | "PERCENT" | "DATE" | "DATETIME" | "BADGE" | "BOOLEAN";
}
```

Use `type` to decide formatting (e.g. `CURRENCY` → prefix currency symbol) and `colorHint` to
theme the card background/accent. `icon` values are plain FontAwesome6 solid icon names — map
them to your icon set as needed (fallback to a generic icon if unmapped).

### 3.2 GroupedSummary (breakdowns)

```ts
interface GroupedSummary {
  key: string;
  label: string;
  total: number;
  formattedTotal: string | null;
  items: LabeledValue[];
}

interface LabeledValue {
  key: string;
  label: string;
  value: number;
  formattedValue: string;
  type: "TEXT" | "NUMBER" | "CURRENCY" | "PERCENT" | "DATE" | "DATETIME" | "BADGE" | "BOOLEAN";
  colorHint: "PRIMARY" | "SUCCESS" | "WARNING" | "DANGER" | "INFO" | "NEUTRAL";
}
```

Good fit for donut-legend style widgets, stacked bars, or a simple label→value list panel.

### 3.3 ChartData (charts)

```ts
interface ChartData {
  key: string;
  title: string;
  type: "LINE" | "AREA" | "BAR" | "STACKED_BAR" | "PIE" | "DONUT";
  labels: string[];            // x-axis labels or pie/donut legend labels
  series: ChartSeries[];
}

interface ChartSeries {
  name: string;
  data: number[];              // index-aligned with `labels`
  colorHint: "PRIMARY" | "SUCCESS" | "WARNING" | "DANGER" | "INFO" | "NEUTRAL" | null;
}
```

### 3.4 TableData (tables)

```ts
interface TableData {
  key: string;
  title: string;
  columns: TableColumn[];
  rows: Record<string, any>[]; // each row keyed by column.key
}

interface TableColumn {
  key: string;
  label: string;
  type: "TEXT" | "NUMBER" | "CURRENCY" | "PERCENT" | "DATE" | "DATETIME" | "BADGE" | "BOOLEAN";
}
```

Render as a generic dynamic table component: iterate `columns` for headers, then for each row
read `row[column.key]` and format by `column.type`.

### 3.5 ExceptionItem (alerts)

```ts
interface ExceptionItem {
  key: string;
  severity: "INFO" | "WARNING" | "CRITICAL";
  title: string;
  description: string;
  value: number | null;
  formattedValue: string | null;
  referenceId: number | null;
  referenceType: string | null;
  colorHint: "PRIMARY" | "SUCCESS" | "WARNING" | "DANGER" | "INFO" | "NEUTRAL";
}
```

Render as a banner/toast list above or alongside the dashboard. Map `severity` to banner style
(`INFO` = blue, `WARNING` = amber, `CRITICAL` = red).

### 3.6 AppliedFilters

Echoes back the filters that were actually recognized/applied, so the UI can display "Showing:
Jun 2026" etc.

```ts
interface AppliedFilters {
  fromDate: string | null;
  toDate: string | null;
  year: number | null;
  month: number | null;
  projectId: number | null;
  propertyId: number | null;
  vendorId: number | null;
  customerId: number | null;
  warehouseId: number | null;
  bookingStatus: string | null;
  paymentStatus: string | null;
  accountType: string | null;
  accountCategory: string | null;
  accountCode: string | null;
  topN: number | null;
}
```

> Note: HR reports only make use of `fromDate`/`toDate`/`year`/`month`. The other fields exist
> because the filter object is shared across all reporting modules — just don't send them for HR calls.

---

## 4. Query Filters (request parameters)

All HR endpoints accept the same optional query parameters:

| Param | Type | Notes |
|-------|------|-------|
| `fromDate` | string | Format `d-M-yyyy`, e.g. `1-6-2026` |
| `toDate` | string | Format `d-M-yyyy` |
| `year` | number | e.g. `2026` |
| `month` | number | `1`–`12`, only meaningful combined with `year` |

**Resolution precedence** (only one applies per request):
1. If `fromDate`/`toDate` given → that explicit range is used.
2. Else if `year` given → that year (or that specific year+month) is used.
3. Else → defaults to the **trailing 12 months ending today**.

Recommended default UX: a date-range picker (populates `fromDate`/`toDate`) plus a quick
"This Month" / "This Year" toggle (populates `year`/`month`).

For `/payroll-overview` and `/department-overview`, the backend also uses `year`/`month` (or the
resolved range's end month) to pick **which single payroll month** to summarize — so these two
screens are best paired with a **month picker**, not a date range.

---

## 5. Endpoints

### `GET /api/reporting/hr/dashboard`
Executive HR snapshot: headcount, today's attendance, leave backlog, this month's payroll cost,
hiring/attrition trend, and payroll cost trend.

**Cards:** `totalEmployees`, `activeEmployees`, `totalDepartments`, `presentToday`, `absentToday`,
`onLeaveToday`, `pendingLeaveRequests`, `payrollCostThisMonth`.

**Breakdowns:** `employeeStatus`, `attendanceToday`.

**Charts:** Hiring vs Attrition (bar), Monthly Payroll Cost (line).

**Alerts:** Leave approval backlog (≥5 pending), low attendance rate today (<80%).

---

### `GET /api/reporting/hr/employee-overview`
Workforce composition and hiring/attrition trend for the selected period.

**Cards:** `totalEmployees`, `activeEmployees`, `newHires` (period), `separations` (period),
`avgBasicSalary`.

**Breakdowns:** `employeeStatus`, `employmentType`, `genderDistribution`.

**Charts:** Hiring vs Attrition (bar, monthly).

---

### `GET /api/reporting/hr/department-overview`
Department structure: headcount, allocated budget vs. actual payroll cost for the selected month.

**Cards:** `totalDepartments`, `activeDepartments`, `budgetAllocated`, `payrollCostSelectedMonth`.

**Breakdowns:** `headcountByDepartment`.

**Tables:** `departmentTable` — columns: Department, Headcount, Budget Allocated, Payroll Cost (Selected Month).

> Uses `year`/`month` filters to pick the payroll month (defaults to the current month).

---

### `GET /api/reporting/hr/attendance-overview`
Today's attendance snapshot plus a period attendance-rate trend.

**Cards:** `presentToday`, `absentToday`, `lateToday`, `onLeaveToday`, `attendanceRatePeriod` (%).

**Breakdowns:** `attendanceStatusPeriod` (status counts across the selected period).

**Charts:** Attendance Rate Trend (line, monthly %).

**Alerts:** Attendance rate below 80% for the period.

---

### `GET /api/reporting/hr/leave-overview`
Leave request status, approved leave days by type, monthly leave-days trend and a pending
requests drill-down table.

**Cards:** `pendingLeaveRequests`, `approvedLeaveRequests`, `rejectedLeaveRequests`,
`cancelledLeaveRequests`, `approvedLeaveDays` (period).

**Breakdowns:** `leaveRequestStatus`, `leaveDaysByType` (ANNUAL/SICK/CASUAL/...).

**Charts:** Approved Leave Days (Monthly) (bar).

**Tables:** `pendingLeaveRequests` — columns: Employee, Department, Leave Type, Start Date,
End Date, Days, Reason (top 20 by soonest start date).

**Alerts:** Leave approval backlog (≥5 pending, org-wide — not period-limited).

---

### `GET /api/reporting/hr/payroll-overview`
Payroll cost and composition for a **single selected month** (`year`/`month`, defaults to
current month), plus a payroll-cost trend across the selected period and a top-earners table.

**Cards:** `netPayrollCost`, `grossPayrollCost`, `avgNetSalary`, `slipsGenerated`,
`amendmentAdditions`, `amendmentDeductions` (all "selected month").

**Breakdowns:** `payrollSummary` (gross/allowances/deductions/net), `salarySlipStatus`
(GENERATED/PAID/CANCELLED), `payrollByDepartment`.

**Charts:** Monthly Payroll Cost (line) — built from **processed payroll runs**, so months with
no completed payroll run show as 0.

**Tables:** `topEarners` — top 10 by net salary for the selected month; columns: Employee,
Department, Designation, Net Salary.

---

### `GET /api/reporting/hr/compensation-breakdown`
Current recurring allowance/deduction structure across the organization (not period-scoped —
this reflects the live, active allowance/deduction setup on employee records).

**Cards:** `totalRecurringAllowances`, `totalRecurringDeductions`.

**Breakdowns:** `allowanceBreakdown`, `deductionBreakdown` (by allowance/deduction name).

**Charts:** Allowance Distribution (donut), Deduction Distribution (donut).

---

## 6. Example Requests

```
GET /api/reporting/hr/dashboard
GET /api/reporting/hr/employee-overview?fromDate=1-1-2026&toDate=30-6-2026
GET /api/reporting/hr/department-overview?year=2026&month=6
GET /api/reporting/hr/attendance-overview?year=2026
GET /api/reporting/hr/leave-overview?fromDate=1-6-2026&toDate=30-6-2026
GET /api/reporting/hr/payroll-overview?year=2026&month=6
GET /api/reporting/hr/compensation-breakdown
```

## 7. Example Response (abridged) — `/dashboard`

```json
{
  "responseCode": "0000",
  "responseMessage": "Request Success!",
  "data": {
    "key": "hr-dashboard",
    "title": "HR Dashboard",
    "subtitle": "Workforce, attendance, leave and payroll overview",
    "generatedAt": "2026-06-15T10:30:00",
    "filtersApplied": { "fromDate": null, "toDate": null, "year": null, "month": null },
    "summaryCards": [
      { "key": "totalEmployees", "label": "Total Employees", "value": 128, "formattedValue": "128",
        "changePercent": null, "trend": "FLAT", "colorHint": "PRIMARY", "icon": "FaUsers", "type": "NUMBER" },
      { "key": "presentToday", "label": "Present Today", "value": 110, "formattedValue": "110",
        "changePercent": null, "trend": "FLAT", "colorHint": "SUCCESS", "icon": "FaUserClock", "type": "NUMBER" },
      { "key": "payrollCostThisMonth", "label": "Payroll Cost (This Month)", "value": 4520000.0,
        "formattedValue": "4.52M", "changePercent": null, "trend": "FLAT", "colorHint": "WARNING",
        "icon": "FaMoneyCheckDollar", "type": "CURRENCY" }
    ],
    "breakdowns": [
      { "key": "employeeStatus", "label": "Employee Status", "total": 128, "formattedTotal": null,
        "items": [
          { "key": "active", "label": "ACTIVE", "value": 118, "formattedValue": "118", "type": "NUMBER", "colorHint": "NEUTRAL" },
          { "key": "on_leave", "label": "ON_LEAVE", "value": 6, "formattedValue": "6", "type": "NUMBER", "colorHint": "NEUTRAL" }
        ]
      }
    ],
    "charts": [
      { "key": "hiringVsAttrition", "title": "Hiring vs Attrition", "type": "BAR",
        "labels": ["Jul 2025", "Aug 2025", "...", "Jun 2026"],
        "series": [
          { "name": "New Hires", "data": [3, 2, 5], "colorHint": "SUCCESS" },
          { "name": "Separations", "data": [1, 0, 2], "colorHint": "DANGER" }
        ]
      }
    ],
    "tables": [],
    "alerts": [
      { "key": "pendingLeaveBacklog", "severity": "WARNING", "title": "Leave approval backlog",
        "description": "7 leave requests are awaiting approval.", "value": 7, "formattedValue": "7",
        "referenceId": null, "referenceType": null, "colorHint": "WARNING" }
    ]
  }
}
```

---

## 8. UI Composition Suggestions

- **HR Dashboard** page → `dashboard` endpoint: KPI row + 2 breakdown widgets + 2 trend charts + alert banners.
- **Employees** section → `employee-overview` (top summary/trend) with a link to your existing employee list/search screens for row-level detail.
- **Departments** section → `department-overview`: KPI row + department table (sortable by headcount/payroll cost) + a bar/donut of `headcountByDepartment`.
- **Attendance** section → `attendance-overview`: "today" KPI strip + attendance-rate line chart; pair with a date-range filter.
- **Leave** section → `leave-overview`: KPI strip + leave-days-by-type donut + pending-requests table (this table is your action queue — wire row actions to your existing approve/reject leave endpoints).
- **Payroll** section → `payroll-overview` (month picker) for cost/composition + `compensation-breakdown` (no period filter) for the recurring allowance/deduction structure.

## 9. Known Data Limitations (surfaced as alerts/notes, not silent gaps)

- Payroll monthly trend chart only reflects **processed/completed payroll runs**; months without a completed run show 0 even if salary slips exist for that month individually (check `payroll-overview`'s `salarySlipStatus` breakdown for slip-level detail instead).
- `leave-overview`'s pending-requests table is capped at the 20 soonest-starting pending requests; use existing leave-management screens for full pagination.
- Attendance rate is computed only from attendance rows that were actually logged; days with no attendance record for an employee are not counted as absent.
