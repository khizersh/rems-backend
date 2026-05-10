# Project Creation Frontend Integration Document

## Overview
This document outlines the changes required for the frontend to support the new project creation flow where users can select a property purchase and assign it to a NEW_PROJECT type.

## Key Changes

### 1. Project Creation Form Updates

#### API Endpoint
- **Endpoint**: `POST /api/projects`
- **Method**: POST

#### Request Payload Changes
Remove `organizationAccountId` from the request payload since payment is now attached to property, not project.

**Updated Request Structure:**
```json
{
  "name": "string",
  "address": "string",
  "floors": "number",
  "purchasingAmount": "number",
  "registrationAmount": "number",
  "constructionAmount": "number",
  "additionalAmount": "number",
  "information": "string",
  "projectType": "APARTMENT | SHOP",
  "organizationId": "number",
  "acquisitionType": "NEW_PROJECT | EXISTING_PROJECT",
  "propertyPurchaseId": "number", // Required for NEW_PROJECT
  "monthDuration": "number",
  "floorList": [
    {
      "floor": "number",
      "unitList": [
        {
          "serialNo": "string",
          "amount": "number",
          "squareFoot": "number",
          "unitType": "string",
          "paymentSchedule": {
            "actualAmount": "number",
            "miscellaneousAmount": "number",
            "developmentAmount": "number"
          }
        }
      ]
    }
  ]
}
```

#### Validation Rules
- For `acquisitionType: "NEW_PROJECT"`:
  - `propertyPurchaseId` is **required**
  - Must be a valid property purchase ID that exists in the system
- For `acquisitionType: "EXISTING_PROJECT"`:
  - `propertyPurchaseId` is **optional**

### 2. Property Purchase Selection

#### New UI Component Requirements
- Add a property purchase dropdown/selection component
- Only show when `acquisitionType` is "NEW_PROJECT"
- Fetch available property purchases for the organization

#### API for Property Purchase List
- **Endpoint**: `GET /api/property-purchases?organizationId={orgId}&status=ACTIVE`
- **Method**: GET
- **Response**:
```json
{
  "responseCode": "SUCCESS",
  "data": [
    {
      "id": "number",
      "propertyName": "string",
      "totalAmount": "number",
      "remainingAmount": "number",
      "location": "string",
      "sellerName": "string"
    }
  ]
}
```

### 3. Acquisition Type Selection

#### UI Updates
- Add acquisition type selection (NEW_PROJECT vs EXISTING_PROJECT)
- Show/hide property purchase selection based on acquisition type
- Update form validation based on selection

#### Acquisition Type Options
```json
[
  {
    "value": "NEW_PROJECT",
    "label": "New Project (Property Assignment)",
    "description": "Create a new project and assign an existing property purchase"
  },
  {
    "value": "EXISTING_PROJECT",
    "label": "Existing Project",
    "description": "Create a project without property assignment"
  }
]
```

### 4. Form Validation Updates

#### Frontend Validation Rules
```javascript
const validationRules = {
  name: { required: true, minLength: 2 },
  address: { required: true, minLength: 5 },
  floors: { required: true, min: 1 },
  projectType: { required: true },
  acquisitionType: { required: true },
  propertyPurchaseId: {
    required: (formData) => formData.acquisitionType === 'NEW_PROJECT',
    message: 'Property Purchase is required for new project acquisition'
  },
  monthDuration: { required: true, min: 1 }
};
```

### 5. Error Handling

#### Backend Error Responses
- **Property Purchase Required**: `"Property Purchase ID is required for new project acquisition. Please create a property purchase first."`
- **Invalid Property Purchase**: `"Invalid Property Purchase ID. Property purchase does not exist."`
- **Journal Entry Failure**: `"Failed to create journal entry for property assignment."`

#### Frontend Error Display
- Show specific error messages for property purchase validation
- Highlight property purchase selection field on validation errors

### 6. Success Flow

#### After Successful Project Creation
1. For NEW_PROJECT: Automatic journal entry created (DR Projects-Inventory, CR Property Land Inventory)
2. Project created with property assignment
3. Redirect to project details or project list

#### Success Response
```json
{
  "responseCode": "SUCCESS",
  "message": "Project added successfully!",
  "data": {
    "projectId": "number",
    "name": "string",
    "acquisitionType": "NEW_PROJECT",
    "propertyPurchaseId": "number"
  }
}
```

### 7. UI/UX Considerations

#### Form Layout Suggestions
```
Project Basic Information
├── Name *
├── Address *
├── Floors *
├── Project Type *
├── Acquisition Type * (NEW_PROJECT | EXISTING_PROJECT)

Property Assignment (shown only for NEW_PROJECT)
├── Property Purchase * (dropdown with search)

Financial Information
├── Purchasing Amount
├── Registration Amount
├── Construction Amount
├── Additional Amount
├── Month Duration *

Floor & Unit Details
└── [Dynamic floor/unit creation]
```

#### Property Purchase Dropdown
- Searchable dropdown with property details
- Show: Property Name, Location, Total Amount, Remaining Amount
- Format: "{Property Name} - {Location} (${Total Amount})"

### 8. Testing Scenarios

#### Happy Path - NEW_PROJECT
1. Select NEW_PROJECT acquisition type
2. Property purchase dropdown appears
3. Select valid property purchase
4. Fill other required fields
5. Submit form
6. Project created successfully
7. Journal entry created automatically

#### Happy Path - EXISTING_PROJECT
1. Select EXISTING_PROJECT acquisition type
2. Property purchase field hidden
3. Fill other required fields
4. Submit form
5. Project created successfully
6. No journal entry created

#### Error Scenarios
1. NEW_PROJECT without property purchase → Validation error
2. Invalid property purchase ID → Backend error
3. Journal entry creation failure → Transaction rollback

### 9. Migration Notes

#### Existing Projects
- Existing projects with `organizationAccountId` will continue to work
- No breaking changes for existing functionality

#### Data Consistency
- Ensure property purchases are properly linked to organizations
- Validate property purchase status before assignment

### 10. API Changes Summary

#### Removed Fields
- `organizationAccountId` from Project entity and API

#### Added/Modified Fields
- `propertyPurchaseId` (required for NEW_PROJECT)
- Enhanced validation for acquisition type logic

#### New Business Logic
- Property purchase validation for NEW_PROJECT
- Automatic journal entry creation for property assignment
- Internal asset reclassification (DR Projects-Inventory, CR Property Land Inventory)
