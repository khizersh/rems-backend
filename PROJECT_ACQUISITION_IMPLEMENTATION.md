# Project Acquisition Type Implementation

## Overview
Implemented an enhanced Project Creation workflow with acquisition type selection (NEW_PROJECT vs EXISTING_PROJECT) and associated financial impact handling.

## Components Implemented

### 1. Project Acquisition Type Enum
**File:** `src/main/java/com/rem/backend/enums/ProjectAcquisitionType.java`

```java
public enum ProjectAcquisitionType {
    NEW_PROJECT,      // New project purchase with financial impact
    EXISTING_PROJECT  // Existing project, for historical records only
}
```

### 2. Project Entity Updates
**File:** `src/main/java/com/rem/backend/entity/project/Project.java`

Added two new fields:
- `acquisitionType` (ProjectAcquisitionType) - Enum field, mandatory
- `organizationAccountId` (Long) - Nullable, required for NEW_PROJECT

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private ProjectAcquisitionType acquisitionType;

@Column(nullable = true)
private Long organizationAccountId;
```

### 3. Project Service Changes
**File:** `src/main/java/com/rem/backend/service/ProjectService.java`

Enhanced `createProject()` method with acquisition type logic:

#### For NEW_PROJECT:
- Validates organization account selection
- Validates purchase costs > 0
- Verifies sufficient funds in organization account
- Deducts total project cost from organization account
- Creates organization account detail entry
- Creates journal entry for project acquisition (double-entry bookkeeping)

#### For EXISTING_PROJECT:
- No financial impact
- Costs are recorded for historical purposes
- Organization account ID is optional

**Key Implementation (lines 100-163):**
```java
if (project.getAcquisitionType() == ProjectAcquisitionType.NEW_PROJECT) {
    // Validate and process new project purchase
    // 1. Check organization account selection
    // 2. Validate purchase costs
    // 3. Verify sufficient funds
    // 4. Deduct amount from organization account
    // 5. Create account detail entry
    // 6. Create journal entry
} else if (project.getAcquisitionType() == ProjectAcquisitionType.EXISTING_PROJECT) {
    // No financial impact, record for history only
}
```

### 4. Journal Entry Service
**File:** `src/main/java/com/rem/backend/service/JournalEntryService.java`

Implemented `createJournalEntryForProjectAcquisition()` method (lines 1508-1575)

#### Accounting Entries (Double-Entry Bookkeeping):
- **Debit:** Construction Inventory (asset increase)
- **Credit:** Bank Account (asset decrease)

The journal entry:
- Records project acquisition cost
- Maintains debit = credit balance
- Links to project and organization account
- Proper logging and error handling

**Key Features:**
- Validates bank account exists
- Validates debit equals credit
- Comprehensive error logging
- Transaction-level consistency

## Workflow Example

```
User creates a project with:
├── Acquisition Type: NEW_PROJECT
├── Organization Account: Checking Account (Balance: 100,000)
└── Total Cost: 50,000

System performs:
1. ✓ Validates acquisition type
2. ✓ Checks organization account (must exist)
3. ✓ Validates purchase costs > 0 (50,000 > 0)
4. ✓ Verifies funds (100,000 >= 50,000)
5. ✓ Updates organization account (100,000 - 50,000 = 50,000)
6. ✓ Creates account detail entry
7. ✓ Creates journal entry:
   - DR Construction Inventory    50,000
   - CR Bank Account              50,000
8. ✓ Saves project with acquisition type

Result: Project created, funds deducted, journal entry recorded
```

## Validations

1. **Acquisition Type:** Mandatory field, must be valid enum
2. **Organization Account:**
   - Required for NEW_PROJECT
   - Must exist in database
   - Must have sufficient funds
3. **Purchase Costs:**
   - Required for NEW_PROJECT (> 0)
   - Optional for EXISTING_PROJECT
4. **Double-Entry Bookkeeping:**
   - Total Debit must equal Total Credit
   - Threshold: 0.01 tolerance

## Error Handling

- `IllegalArgumentException` - For validation failures (caught and returned as response)
- `RuntimeException` - For database or journal entry failures (rolled back via @Transactional)
- Proper HTTP response mapping with error messages

## Testing Scenarios

### Scenario 1: NEW_PROJECT - Success
```
Input: NEW_PROJECT with valid org account and sufficient funds
Expected: Project created, funds deducted, journal entry recorded
```

### Scenario 2: NEW_PROJECT - Insufficient Funds
```
Input: NEW_PROJECT with insufficient balance
Expected: Exception thrown, transaction rolled back, error response
```

### Scenario 3: NEW_PROJECT - Missing Org Account
```
Input: NEW_PROJECT with null organization account
Expected: Validation error, project not created
```

### Scenario 4: EXISTING_PROJECT
```
Input: EXISTING_PROJECT with or without costs
Expected: Project created, no financial impact
```

## Dependencies

- `ProjectAcquisitionType` enum
- `OrganizationAccount` entity
- `OrganizationAccountDetail` entity
- `JournalEntryService` for journal entry creation
- Double-entry bookkeeping standards

## Key Concepts

### Double-Entry Bookkeeping
Every transaction is recorded in two accounts:
- One account is DEBITED (increased for assets)
- One account is CREDITED (decreased for assets)
- Total debits must always equal total credits

### Asset Purchase Accounting
```
DR Construction Inventory (Asset - Increase)
CR Bank/Cash (Asset - Decrease)
```

This reflects that organization's physical assets (project) increased while cash/bank account decreased.

## Status
✅ **IMPLEMENTATION COMPLETE**

All components have been implemented and integrated:
- ✅ ProjectAcquisitionType enum created
- ✅ Project entity updated with new fields
- ✅ ProjectService.createProject() enhanced with acquisition logic
- ✅ JournalEntryService.createJournalEntryForProjectAcquisition() implemented
- ✅ Full validation and error handling
- ✅ Double-entry bookkeeping compliance
- ✅ Transactional consistency

## API Usage

### Create NEW_PROJECT:
```json
POST /api/projects/create
{
  "name": "Downtown Complex",
  "address": "123 Main St",
  "acquisition_type": "NEW_PROJECT",
  "organization_account_id": 5,
  "purchasing_amount": 50000,
  "additional_amount": 5000,
  "registration_amount": 1000,
  "project_type": "APARTMENT",
  "floors": 10,
  "month_duration": 24
}
```

### Create EXISTING_PROJECT:
```json
POST /api/projects/create
{
  "name": "Historic Building",
  "address": "456 Oak Ave",
  "acquisition_type": "EXISTING_PROJECT",
  "organization_account_id": null,
  "purchasing_amount": 100000,
  "additional_amount": 0,
  "registration_amount": 0,
  "project_type": "SHOP",
  "floors": 3,
  "month_duration": 12
}
```

## Next Steps (Optional Enhancements)

1. **API Documentation** - Add Swagger/OpenAPI annotations
2. **Audit Trail** - Track acquisition type changes
3. **Cost Allocation** - Allocate project costs across units
4. **Financial Reports** - Generate project acquisition reports
5. **Depreciation** - Calculate depreciation for project assets
6. **Budget Management** - Track budget vs. actual project costs

