# Accounting Hierarchy Change – Frontend Integration Document

## Overview

The backend accounting hierarchy has been updated from **3 levels** to **4 levels**:

### Before
```
AccountType → AccountGroup → ChartOfAccount
```

### After
```
AccountType → AccountCategory → AccountGroup → ChartOfAccount
```

A new **AccountCategory** level has been inserted between **AccountType** and **AccountGroup**.

---

## Database Schema Change

### New Table: `account_category`

| Column            | Type         | Nullable | Description                      |
|-------------------|--------------|----------|----------------------------------|
| id                | BIGINT (PK)  | No       | Auto-generated ID                |
| name              | VARCHAR      | No       | Category name                    |
| account_type_id   | BIGINT (FK)  | No       | References `account_type.id`     |
| organization_id   | BIGINT (FK)  | No       | References `organization.id`     |
| created_date      | DATETIME     | No       | Auto-set on creation             |

### Modified Table: `account_group`

| Column               | Change     | Description                                   |
|----------------------|------------|-----------------------------------------------|
| ~~account_type_id~~  | **REMOVED**| No longer directly linked to AccountType      |
| account_category_id  | **ADDED**  | FK → `account_category.id` (NOT NULL)         |

---

## Updated Cascade Flow (UI Dropdowns)

```
Step 1:  Select Account Type        →  GET /{orgId}/getAccountCategories?accountType={id}
Step 2:  Select Account Category    →  GET /{orgId}/getAccountGroups?accountCategory={id}
Step 3:  Select Account Group       →  GET /{orgId}/allChartOfAccounts?accountGroup={id}
```

---

## API Changes

### 1. Account Category APIs (NEW)

#### GET `/{organizationId}/getAccountCategories`
Fetch categories by account type.

**Query Params:**
| Param        | Type | Required | Description        |
|--------------|------|----------|--------------------|
| accountType  | long | Yes      | Account Type ID    |

**Response:**
```json
{
  "responseCode": "00",
  "responseDescription": "Success",
  "data": {
    "count": 2,
    "data": [
      {
        "id": 1,
        "name": "Current Assets",
        "accountType": {
          "id": 1,
          "name": "ASSET"
        },
        "createdDate": "2026-04-12T10:00:00"
      }
    ]
  }
}
```

---

#### GET `/getAccountCategoryById/{accountCategoryId}`
Fetch single category by ID.

**Response:**
```json
{
  "responseCode": "00",
  "responseDescription": "Success",
  "data": {
    "id": 1,
    "name": "Current Assets",
    "accountType": {
      "id": 1,
      "name": "ASSET"
    },
    "createdDate": "2026-04-12T10:00:00"
  }
}
```

---

#### POST `/{organizationId}/accountCategory`
Create a new account category.

**Request Body:**
```json
{
  "name": "Current Assets",
  "accountTypeId": 1
}
```

**Response:**
```json
{
  "responseCode": "00",
  "responseDescription": "Success",
  "data": {
    "id": 1,
    "name": "Current Assets",
    "accountTypeId": 1,
    "organizationId": 1,
    "createdDate": "2026-04-12T10:00:00"
  }
}
```

---

#### PUT `/{organizationId}/accountCategory?categoryId={id}`
Update an existing account category.

**Query Params:**
| Param       | Type | Required | Description          |
|-------------|------|----------|----------------------|
| categoryId  | long | Yes      | Category ID to update|

**Request Body:**
```json
{
  "name": "Fixed Assets",
  "accountTypeId": 1
}
```

**Response:** Same structure as create.

---

### 2. Account Group APIs (CHANGED)

#### GET `/{organizationId}/getAccountGroups`

**⚠️ BREAKING CHANGE:** Query param renamed from `accountType` → `accountCategory`

**Query Params:**
| Param            | Type | Required | Description            |
|------------------|------|----------|------------------------|
| accountCategory  | long | Yes      | Account Category ID    |

**Response:**
```json
{
  "responseCode": "00",
  "responseDescription": "Success",
  "data": {
    "count": 1,
    "data": [
      {
        "id": 1,
        "name": "Cash & Bank",
        "accountCategory": {
          "id": 1,
          "name": "Current Assets",
          "accountType": {
            "id": 1,
            "name": "ASSET"
          },
          "createdDate": "2026-04-12T10:00:00"
        },
        "createdDate": "2026-04-12T10:00:00"
      }
    ]
  }
}
```

> **Note:** The `accountType` object inside AccountGroupDTO has been replaced with `accountCategory` object which itself contains `accountType`.

---

#### POST `/{organizationId}/accountGroup`

**⚠️ BREAKING CHANGE:** Request body field renamed from `accountTypeId` → `accountCategoryId`

**Request Body:**
```json
{
  "name": "Cash & Bank",
  "accountCategoryId": 1
}
```

**Response:**
```json
{
  "responseCode": "00",
  "responseDescription": "Success",
  "data": {
    "id": 1,
    "name": "Cash & Bank",
    "accountCategoryId": 1,
    "organizationId": 1,
    "createdDate": "2026-04-12T10:00:00"
  }
}
```

---

#### PUT `/{organizationId}/accountGroup?groupId={id}`

**⚠️ BREAKING CHANGE:** Request body field renamed from `accountTypeId` → `accountCategoryId`

**Request Body:**
```json
{
  "name": "Updated Group Name",
  "accountCategoryId": 1
}
```

---

### 3. Chart of Accounts APIs (CHANGED)

#### GET `/{organizationId}/allChartOfAccounts`

**New optional filter added:** `accountCategory`

**Query Params:**
| Param            | Type | Required | Description            |
|------------------|------|----------|------------------------|
| accountType      | long | No       | Filter by type (top)   |
| accountCategory  | long | No       | Filter by category (new)|
| accountGroup     | long | No       | Filter by group        |

**Response structure changed:**
The `accountGroup` object in each COA now contains `accountCategory` instead of `accountType`:

```json
{
  "id": 10,
  "code": "EXP001",
  "name": "Office Supplies",
  "accountGroup": {
    "id": 1,
    "name": "Operating Expenses",
    "accountCategory": {
      "id": 2,
      "name": "Direct Expenses",
      "accountType": {
        "id": 3,
        "name": "EXPENSE"
      },
      "createdDate": "2026-04-12T10:00:00"
    },
    "createdDate": "2026-04-12T10:00:00"
  },
  "isSystemGenerated": false,
  "status": "ACTIVE",
  "organizationAccountId": null,
  "createdDate": "2026-04-12T10:00:00",
  "updatedDate": "2026-04-12T10:00:00"
}
```

---

### 4. Unchanged APIs

| API | Change |
|-----|--------|
| `GET /getAllAccountTypes` | No change |
| `GET /chartOfAccount/getById/{id}` | No change |
| `GET /getAccountGroupById/{id}` | Response now contains `accountCategory` instead of `accountType` |
| `POST /{orgId}/expenseChartOfAccount` | No change (still takes `accountGroupId`) |
| `PUT /{orgId}/expenseChartOfAccount` | No change |

---

## Frontend Migration Checklist

- [ ] **Add Account Category management page** (CRUD for categories under each account type)
- [ ] **Update Account Group forms:** Change `accountTypeId` dropdown → `accountCategoryId` dropdown
- [ ] **Update Account Group listing:** The response now has `accountCategory` instead of `accountType`
- [ ] **Update `getAccountGroups` API call:** Change query param from `accountType=` → `accountCategory=`
- [ ] **Update Chart of Accounts listing:** Navigate `accountGroup.accountCategory.accountType` to get type name
- [ ] **Update cascade dropdowns** where applicable:
  - Account Type → Account Category → Account Group → Chart of Account
- [ ] **Update any breadcrumbs/navigation** that showed: Type > Group > COA → Now: Type > Category > Group > COA

---

## UI Flow Example

### Creating a Chart of Account (Expense)
1. User selects **Account Type** (e.g., "EXPENSE")
2. → API loads **Account Categories** for that type
3. User selects **Account Category** (e.g., "Direct Expenses")
4. → API loads **Account Groups** for that category
5. User selects **Account Group** (e.g., "Construction")
6. User enters **COA Name** and submits

### Creating an Account Group
1. User selects **Account Type**
2. → API loads **Account Categories**
3. User selects **Account Category**
4. User enters **Group Name** and submits with `accountCategoryId`

### Creating an Account Category
1. User selects **Account Type**
2. User enters **Category Name** and submits with `accountTypeId`

