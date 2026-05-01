# Project Acquisition Type - Frontend Integration Guide

## Overview

This document outlines the frontend implementation requirements for the **Project Acquisition Type** feature. This feature allows users to create projects with two distinct acquisition options:
- **NEW_PROJECT**: New project purchase with immediate financial impact
- **EXISTING_PROJECT**: Existing project record for historical purposes only

---

## Table of Contents

1. [Feature Overview](#feature-overview)
2. [API Endpoints](#api-endpoints)
3. [Request/Response Examples](#requestresponse-examples)
4. [Frontend Workflow](#frontend-workflow)
5. [UI Requirements](#ui-requirements)
6. [Validation Rules](#validation-rules)
7. [Error Handling](#error-handling)
8. [State Management](#state-management)
9. [Code Examples](#code-examples)
10. [Testing Scenarios](#testing-scenarios)

---

## Feature Overview

### NEW_PROJECT Flow
When a user selects **NEW_PROJECT**:
- User must select an organization account
- Purchase costs are mandatory (> 0)
- System validates available funds in selected account
- Amount is deducted from organization account
- Journal entry is created automatically
- Financial impact is immediate

### EXISTING_PROJECT Flow
When a user selects **EXISTING_PROJECT**:
- Organization account is optional
- Costs are for record-keeping only
- No financial impact on organization accounts
- Useful for tracking historical projects

---

## API Endpoints

### 1. Create Project
```
POST /api/projects/create
Content-Type: application/json
```

**Endpoint**: Creates a new project with acquisition type

### 2. Update Project
```
PUT /api/projects/update
Content-Type: application/json
```

**Endpoint**: Updates an existing project

### 3. Get Project by ID
```
GET /api/projects/{projectId}
```

**Endpoint**: Retrieves project details including acquisition type

### 4. Get All Projects by Organization
```
GET /api/projects/organization/{organizationId}
```

**Endpoint**: Lists all projects for an organization

---

## Request/Response Examples

### Example 1: Create NEW_PROJECT

#### Request
```json
POST /api/projects/create
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Downtown Plaza Complex",
  "address": "123 Main Street, Downtown City",
  "acquisitionType": "NEW_PROJECT",
  "organizationAccountId": 5,
  "purchasingAmount": 50000.00,
  "additionalAmount": 5000.00,
  "registrationAmount": 1000.00,
  "projectType": "APARTMENT",
  "floors": 10,
  "monthDuration": 24,
  "information": "Modern commercial plaza with mixed-use units",
  "floorList": [
    {
      "floor": 1,
      "unitList": [
        {
          "serialNo": "101",
          "unitType": "APARTMENT",
          "amount": 300000,
          "squareFoot": 1200,
          "paymentPlanType": "MONTHLY",
          "paymentSchedule": {
            "actualAmount": 250000,
            "developmentAmount": 30000,
            "miscellaneousAmount": 20000
          }
        }
      ]
    }
  ]
}
```

#### Response - Success (200 OK)
```json
{
  "code": "SUCCESS",
  "message": "Project added successfully!",
  "data": {
    "projectId": 101,
    "name": "Downtown Plaza Complex",
    "address": "123 Main Street",
    "acquisitionType": "NEW_PROJECT",
    "organizationAccountId": 5,
    "totalAmount": 56000.00,
    "projectType": "APARTMENT",
    "floors": 10,
    "isActive": true,
    "createdBy": "user@example.com",
    "createdDate": "2026-04-27T10:30:00"
  }
}
```

#### Response - Error (400 Bad Request)
```json
{
  "code": "INVALID_PARAMETER",
  "message": "Insufficient funds in organization account for project purchase",
  "data": null
}
```

---

### Example 2: Create EXISTING_PROJECT

#### Request
```json
POST /api/projects/create
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Historic Building - Reference Only",
  "address": "456 Oak Avenue, Heritage District",
  "acquisitionType": "EXISTING_PROJECT",
  "organizationAccountId": null,
  "purchasingAmount": 100000.00,
  "additionalAmount": 0.00,
  "registrationAmount": 0.00,
  "projectType": "SHOP",
  "floors": 3,
  "monthDuration": 12,
  "information": "Existing property for portfolio management",
  "floorList": []
}
```

#### Response - Success (200 OK)
```json
{
  "code": "SUCCESS",
  "message": "Project added successfully!",
  "data": {
    "projectId": 102,
    "name": "Historic Building - Reference Only",
    "address": "456 Oak Avenue",
    "acquisitionType": "EXISTING_PROJECT",
    "organizationAccountId": null,
    "totalAmount": 100000.00,
    "projectType": "SHOP",
    "floors": 3,
    "isActive": true,
    "createdBy": "user@example.com",
    "createdDate": "2026-04-27T10:35:00"
  }
}
```

---

### Example 3: Get Project Details

#### Request
```
GET /api/projects/101
Authorization: Bearer {token}
```

#### Response
```json
{
  "code": "SUCCESS",
  "data": {
    "projectId": 101,
    "name": "Downtown Plaza Complex",
    "address": "123 Main Street, Downtown City",
    "acquisitionType": "NEW_PROJECT",
    "organizationAccountId": 5,
    "purchasingAmount": 50000.00,
    "additionalAmount": 5000.00,
    "registrationAmount": 1000.00,
    "totalAmount": 56000.00,
    "projectType": "APARTMENT",
    "floors": 10,
    "monthDuration": 24,
    "isActive": true,
    "floorList": [...]
  }
}
```

---

## Frontend Workflow

### Step 1: Project Type Selection
User selects the acquisition type at the start of project creation:

```
┌─────────────────────────────────────┐
│   SELECT PROJECT ACQUISITION TYPE   │
├─────────────────────────────────────┤
│ ○ NEW PROJECT (Financial Impact)    │
│ ○ EXISTING PROJECT (Historical)     │
└─────────────────────────────────────┘
```

### Step 2: Conditional Form Display

#### If NEW_PROJECT is selected:
```
Show:
├─ Project Details (name, address, etc.)
├─ Purchase Costs (purchasing_amount, additional_amount, registration_amount)
├─ Organization Account Selector (REQUIRED) ⭐
├─ Funds Validation Message (Show available balance)
├─ Floors & Units
└─ Submit Button
```

#### If EXISTING_PROJECT is selected:
```
Show:
├─ Project Details (name, address, etc.)
├─ Costs (optional - for reference only)
├─ Organization Account Selector (OPTIONAL) ⭐
├─ "No Financial Impact" Notice
├─ Floors & Units (optional)
└─ Submit Button
```

### Step 3: Form Validation

**For NEW_PROJECT:**
1. Validate acquisition type is selected ✓
2. Validate organization account is selected ✓
3. Validate purchase costs > 0 ✓
4. Fetch organization account balance
5. Show available balance to user
6. Validate user amount ≤ available balance
7. Show success/error message

**For EXISTING_PROJECT:**
1. Validate acquisition type is selected ✓
2. Show "No financial impact" notice
3. Allow optional organization account
4. Proceed with project creation

---

## UI Requirements

### 1. Acquisition Type Selector

**Component Type**: Radio Button Group or Toggle Switch

```html
<!-- Option 1: Radio Buttons -->
<div class="acquisition-type-selector">
  <div class="form-group">
    <label>Project Acquisition Type *</label>
    <div class="radio-group">
      <label>
        <input 
          type="radio" 
          name="acquisitionType" 
          value="NEW_PROJECT"
          (change)="onAcquisitionTypeChange('NEW_PROJECT')"
        />
        <span>New Project (Requires fund deduction)</span>
      </label>
      <label>
        <input 
          type="radio" 
          name="acquisitionType" 
          value="EXISTING_PROJECT"
          (change)="onAcquisitionTypeChange('EXISTING_PROJECT')"
        />
        <span>Existing Project (Historical record only)</span>
      </label>
    </div>
  </div>
</div>

<!-- Option 2: Toggle Switch -->
<div class="acquisition-type-toggle">
  <label>Project Type</label>
  <mat-slide-toggle 
    [(ngModel)]="isNewProject"
    (change)="onAcquisitionTypeToggle()">
    {{ isNewProject ? 'New Project' : 'Existing Project' }}
  </mat-slide-toggle>
</div>
```

### 2. Organization Account Selector

```html
<div class="form-group" *ngIf="acquisitionType === 'NEW_PROJECT'">
  <label>Organization Account * <span class="required">Required</span></label>
  <mat-select 
    [(ngModel)]="selectedOrganizationAccountId"
    (selectionChange)="onAccountSelected($event)"
    [disabled]="loadingAccounts">
    <mat-option *ngFor="let account of organizationAccounts" [value]="account.id">
      {{ account.name }} - Available Balance: ₹{{ account.totalAmount | currency }}
    </mat-option>
  </mat-select>
  
  <!-- Show selected account balance -->
  <div *ngIf="selectedAccount" class="account-info">
    <p class="balance-info">
      Available Balance: <strong>₹{{ selectedAccount.totalAmount | currency }}</strong>
    </p>
    <p class="required-amount">
      Required Amount: <strong>₹{{ totalProjectCost | currency }}</strong>
    </p>
    <p class="status" [ngClass]="{'insufficient': !hasSufficientFunds}">
      {{ hasSufficientFunds ? '✓ Sufficient funds' : '✗ Insufficient funds' }}
    </p>
  </div>
</div>
```

### 3. Cost Input Section

```html
<div class="costs-section">
  <h3>Project Costs</h3>
  
  <div class="form-group">
    <label>Purchasing Amount 
      <span *ngIf="acquisitionType === 'NEW_PROJECT'" class="required">*</span>
      <span *ngIf="acquisitionType === 'EXISTING_PROJECT'" class="optional">(Optional)</span>
    </label>
    <input 
      type="number" 
      [(ngModel)]="purchasingAmount"
      [required]="acquisitionType === 'NEW_PROJECT'"
      (change)="calculateTotalCost()"
      placeholder="Enter purchasing amount"
    />
  </div>

  <div class="form-group">
    <label>Additional Amount
      <span *ngIf="acquisitionType === 'NEW_PROJECT'" class="required">*</span>
    </label>
    <input 
      type="number" 
      [(ngModel)]="additionalAmount"
      (change)="calculateTotalCost()"
      placeholder="Enter additional amount"
    />
  </div>

  <div class="form-group">
    <label>Registration Amount
      <span *ngIf="acquisitionType === 'NEW_PROJECT'" class="required">*</span>
    </label>
    <input 
      type="number" 
      [(ngModel)]="registrationAmount"
      (change)="calculateTotalCost()"
      placeholder="Enter registration amount"
    />
  </div>

  <!-- Total Cost Display -->
  <div class="total-cost-display">
    <strong>Total Project Cost:</strong>
    <span class="amount">₹{{ totalProjectCost | currency }}</span>
  </div>

  <!-- Financial Impact Notice -->
  <div class="financial-impact-notice" *ngIf="acquisitionType === 'NEW_PROJECT'">
    <mat-icon>warning</mat-icon>
    <span>
      This amount will be deducted from your selected organization account.
      Journal entry will be created automatically.
    </span>
  </div>

  <div class="no-impact-notice" *ngIf="acquisitionType === 'EXISTING_PROJECT'">
    <mat-icon>info</mat-icon>
    <span>
      No financial impact. Costs are recorded for historical reference only.
    </span>
  </div>
</div>
```

### 4. Submit Button with Validation

```html
<div class="form-actions">
  <button 
    type="submit" 
    [disabled]="!isFormValid()"
    (click)="submitProject()">
    Create Project
  </button>
  <button type="button" (click)="cancel()">Cancel</button>
</div>

<!-- Loading & Status Messages -->
<div class="status-message" *ngIf="submitting">
  <mat-spinner diameter="20"></mat-spinner>
  <span>Creating project...</span>
</div>

<div class="success-message" *ngIf="submitSuccess">
  ✓ Project created successfully! Project ID: {{ newProjectId }}
</div>

<div class="error-message" *ngIf="submitError">
  ✗ Error: {{ submitError }}
</div>
```

---

## Validation Rules

### Frontend Validation (Immediate user feedback)

#### For NEW_PROJECT:
```javascript
validationRules = {
  acquisitionType: {
    required: true,
    value: 'NEW_PROJECT'
  },
  organizationAccountId: {
    required: true,
    message: 'Organization account must be selected for new project'
  },
  purchasingAmount: {
    required: true,
    min: 0.01,
    message: 'Purchase costs must be greater than zero'
  },
  totalCost: {
    maxValue: availableBalance,
    message: 'Insufficient funds in organization account'
  },
  name: {
    required: true,
    minLength: 3,
    maxLength: 100
  },
  address: {
    required: true,
    minLength: 5
  },
  projectType: {
    required: true
  },
  floors: {
    required: true,
    min: 1
  }
}
```

#### For EXISTING_PROJECT:
```javascript
validationRules = {
  acquisitionType: {
    required: true,
    value: 'EXISTING_PROJECT'
  },
  name: {
    required: true,
    minLength: 3,
    maxLength: 100
  },
  address: {
    required: true,
    minLength: 5
  },
  projectType: {
    required: true
  },
  floors: {
    required: true,
    min: 1
  }
  // Costs are optional
}
```

### Real-time Validation Messages

```html
<!-- Example: Funds validation -->
<div class="validation-message" [ngClass]="{'error': !hasSufficientFunds}">
  <span *ngIf="!hasSufficientFunds">
    ✗ Insufficient balance. 
    Required: ₹{{ totalProjectCost | currency }}, 
    Available: ₹{{ selectedAccount?.totalAmount | currency }}
  </span>
  <span *ngIf="hasSufficientFunds">
    ✓ Sufficient funds available
  </span>
</div>
```

---

## Error Handling

### HTTP Status Codes and Responses

| Status | Code | Message | Action |
|--------|------|---------|--------|
| 200 | SUCCESS | Project created successfully | Show success message & navigate |
| 400 | INVALID_PARAMETER | Insufficient funds | Show error & highlight account field |
| 400 | INVALID_PARAMETER | Org account must be selected | Show error & focus on account selector |
| 400 | INVALID_PARAMETER | Purchase costs must be > 0 | Show error & focus on costs |
| 400 | INVALID_PARAMETER | Invalid organization account | Show error & reload accounts |
| 400 | INVALID_PARAMETER | Acquisition type not valid | Show error & reload form |
| 500 | SYSTEM_FAILURE | Database error | Show generic error message |
| 500 | SYSTEM_FAILURE | Journal entry creation failed | Show error but project may be saved |

### Error Handling Code Example

```typescript
submitProject() {
  if (!this.isFormValid()) {
    this.showValidationErrors();
    return;
  }

  this.submitting = true;
  this.submitError = null;

  this.projectService.createProject(this.projectData).subscribe(
    (response) => {
      if (response.code === 'SUCCESS') {
        this.submitSuccess = true;
        this.newProjectId = response.data.projectId;
        setTimeout(() => this.navigateToProjectDetail(response.data.projectId), 2000);
      } else {
        this.submitError = response.message;
        this.highlightErrorFields(response);
      }
      this.submitting = false;
    },
    (error) => {
      this.submitting = false;
      
      if (error.status === 400) {
        this.submitError = error.error?.message || 'Invalid input. Please check your entries.';
      } else if (error.status === 500) {
        this.submitError = 'Server error. Please try again later.';
      } else {
        this.submitError = 'An unexpected error occurred. Please try again.';
      }

      // Log error for debugging
      console.error('Project creation error:', error);
      
      // Highlight the field that caused the error
      this.highlightErrorFields(error.error);
    }
  );
}

highlightErrorFields(error: any) {
  if (error.message.includes('Insufficient funds')) {
    this.accountFieldError = true;
  } else if (error.message.includes('Organization account must be selected')) {
    this.accountFieldError = true;
  } else if (error.message.includes('Purchase costs must be')) {
    this.costsFieldError = true;
  }
}
```

---

## State Management

### Local State Management (Component Level)

```typescript
export class CreateProjectComponent implements OnInit {
  
  // Form Data
  projectForm = {
    acquisitionType: 'NEW_PROJECT',
    name: '',
    address: '',
    purchasingAmount: 0,
    additionalAmount: 0,
    registrationAmount: 0,
    projectType: '',
    floors: 0,
    monthDuration: 0,
    organizationAccountId: null,
    floorList: []
  };

  // Organization Accounts
  organizationAccounts: OrganizationAccount[] = [];
  selectedOrganizationAccountId: number | null = null;
  selectedAccount: OrganizationAccount | null = null;
  loadingAccounts: boolean = false;

  // Cost Calculations
  purchasingAmount: number = 0;
  additionalAmount: number = 0;
  registrationAmount: number = 0;
  totalProjectCost: number = 0;
  hasSufficientFunds: boolean = false;

  // UI States
  submitting: boolean = false;
  submitSuccess: boolean = false;
  submitError: string | null = null;
  newProjectId: number | null = null;
  accountFieldError: boolean = false;
  costsFieldError: boolean = false;

  // Computed Properties
  get acquisitionType(): string {
    return this.projectForm.acquisitionType;
  }

  get isNewProject(): boolean {
    return this.projectForm.acquisitionType === 'NEW_PROJECT';
  }
}
```

### RxJS State Management (NgRx / Akita - Optional)

```typescript
// Project State
@Injectable({ providedIn: 'root' })
export class ProjectStore extends Store<ProjectState> {
  constructor(
    private projectService: ProjectService
  ) {
    super(initialState);
  }

  createProject(project: Project): Observable<Project> {
    return this.projectService.createProject(project).pipe(
      tap(response => {
        this.setState(state => ({
          ...state,
          projects: [...state.projects, response.data],
          loading: false,
          error: null
        }));
      }),
      catchError(error => {
        this.setState(state => ({
          ...state,
          error: error.message,
          loading: false
        }));
        throw error;
      })
    );
  }

  selectAccountDetails(accountId: number) {
    const account = this.state.organizationAccounts.find(a => a.id === accountId);
    return account;
  }
}
```

---

## Code Examples

### Angular Example

```typescript
import { Component, OnInit } from '@angular/core';
import { ProjectService } from '../services/project.service';
import { OrganizationAccountService } from '../services/organization-account.service';

@Component({
  selector: 'app-create-project',
  templateUrl: './create-project.component.html',
  styleUrls: ['./create-project.component.css']
})
export class CreateProjectComponent implements OnInit {

  acquisitionType: 'NEW_PROJECT' | 'EXISTING_PROJECT' = 'NEW_PROJECT';
  organizationAccounts: any[] = [];
  selectedAccountId: number | null = null;
  submitting = false;
  error: string | null = null;

  projectData = {
    name: '',
    address: '',
    purchasingAmount: 0,
    additionalAmount: 0,
    registrationAmount: 0,
    projectType: '',
    floors: 0,
    monthDuration: 0,
    organizationAccountId: null,
    acquisitionType: 'NEW_PROJECT'
  };

  constructor(
    private projectService: ProjectService,
    private accountService: OrganizationAccountService
  ) {}

  ngOnInit() {
    this.loadOrganizationAccounts();
  }

  loadOrganizationAccounts() {
    this.accountService.getAll().subscribe(
      (accounts) => {
        this.organizationAccounts = accounts;
      },
      (error) => {
        console.error('Error loading accounts:', error);
        this.error = 'Failed to load organization accounts';
      }
    );
  }

  onAcquisitionTypeChange(type: string) {
    this.acquisitionType = type as any;
    this.projectData.acquisitionType = type;
    
    // Reset account if switching to EXISTING_PROJECT
    if (type === 'EXISTING_PROJECT') {
      this.selectedAccountId = null;
      this.projectData.organizationAccountId = null;
    }
  }

  onAccountSelected(selectedId: number) {
    this.selectedAccountId = selectedId;
    this.projectData.organizationAccountId = selectedId;
  }

  calculateTotalCost() {
    this.projectData.purchasingAmount = parseFloat(this.projectData.purchasingAmount.toString()) || 0;
    this.projectData.additionalAmount = parseFloat(this.projectData.additionalAmount.toString()) || 0;
    this.projectData.registrationAmount = parseFloat(this.projectData.registrationAmount.toString()) || 0;
  }

  isFormValid(): boolean {
    if (!this.projectData.name || !this.projectData.address) {
      return false;
    }

    if (this.acquisitionType === 'NEW_PROJECT') {
      const totalCost = this.projectData.purchasingAmount + 
                       this.projectData.additionalAmount + 
                       this.projectData.registrationAmount;
      
      if (!this.selectedAccountId || totalCost <= 0) {
        return false;
      }

      const selectedAccount = this.organizationAccounts.find(a => a.id === this.selectedAccountId);
      if (selectedAccount && selectedAccount.totalAmount < totalCost) {
        return false;
      }
    }

    return true;
  }

  submitProject() {
    if (!this.isFormValid()) {
      this.error = 'Please fill all required fields correctly';
      return;
    }

    this.submitting = true;
    this.error = null;

    this.projectService.createProject(this.projectData).subscribe(
      (response) => {
        if (response.code === 'SUCCESS') {
          alert('Project created successfully!');
          // Navigate to project detail or project list
        } else {
          this.error = response.message;
        }
        this.submitting = false;
      },
      (error) => {
        this.submitting = false;
        this.error = error.error?.message || 'Failed to create project';
        console.error('Error:', error);
      }
    );
  }
}
```

### React Example

```typescript
import React, { useState, useEffect } from 'react';
import { ProjectService } from '../services/projectService';
import { OrganizationAccountService } from '../services/organizationAccountService';

export const CreateProjectForm: React.FC = () => {
  const [acquisitionType, setAcquisitionType] = useState<'NEW_PROJECT' | 'EXISTING_PROJECT'>('NEW_PROJECT');
  const [organizationAccounts, setOrganizationAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [projectData, setProjectData] = useState({
    name: '',
    address: '',
    purchasingAmount: 0,
    additionalAmount: 0,
    registrationAmount: 0,
    projectType: '',
    floors: 0,
    monthDuration: 0,
    organizationAccountId: null,
    acquisitionType: 'NEW_PROJECT'
  });

  useEffect(() => {
    loadOrganizationAccounts();
  }, []);

  const loadOrganizationAccounts = async () => {
    try {
      const accounts = await OrganizationAccountService.getAll();
      setOrganizationAccounts(accounts);
    } catch (err) {
      setError('Failed to load organization accounts');
    }
  };

  const handleAcquisitionTypeChange = (type: string) => {
    setAcquisitionType(type as any);
    if (type === 'EXISTING_PROJECT') {
      setSelectedAccountId(null);
    }
    setProjectData(prev => ({ ...prev, acquisitionType: type }));
  };

  const handleAccountSelect = (accountId: number) => {
    setSelectedAccountId(accountId);
    setProjectData(prev => ({ ...prev, organizationAccountId: accountId }));
  };

  const calculateTotalCost = () => {
    return (projectData.purchasingAmount || 0) + 
           (projectData.additionalAmount || 0) + 
           (projectData.registrationAmount || 0);
  };

  const getTotalCost = () => calculateTotalCost();
  const getSelectedAccount = () => organizationAccounts.find((a: any) => a.id === selectedAccountId);
  const hasSufficientFunds = () => {
    const account = getSelectedAccount();
    return account && account.totalAmount >= getTotalCost();
  };

  const isFormValid = (): boolean => {
    if (!projectData.name || !projectData.address) return false;

    if (acquisitionType === 'NEW_PROJECT') {
      if (!selectedAccountId || getTotalCost() <= 0) return false;
      if (!hasSufficientFunds()) return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!isFormValid()) {
      setError('Please fill all required fields correctly');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const response = await ProjectService.createProject(projectData);
      if (response.code === 'SUCCESS') {
        alert('Project created successfully!');
        // Navigate or reset form
      } else {
        setError(response.message);
      }
    } catch (err: any) {
      setError(err.error?.message || 'Failed to create project');
    } finally {
      setSubmitting(false);
    }
  };

  const selectedAccount = getSelectedAccount();
  const totalCost = getTotalCost();

  return (
    <form onSubmit={handleSubmit} className="create-project-form">
      <h2>Create Project</h2>

      {/* Acquisition Type Selection */}
      <div className="form-group">
        <label>Project Type *</label>
        <div className="radio-group">
          <label>
            <input
              type="radio"
              value="NEW_PROJECT"
              checked={acquisitionType === 'NEW_PROJECT'}
              onChange={(e) => handleAcquisitionTypeChange(e.target.value)}
            />
            New Project (Financial Impact)
          </label>
          <label>
            <input
              type="radio"
              value="EXISTING_PROJECT"
              checked={acquisitionType === 'EXISTING_PROJECT'}
              onChange={(e) => handleAcquisitionTypeChange(e.target.value)}
            />
            Existing Project (Historical)
          </label>
        </div>
      </div>

      {/* Organization Account Selection */}
      {acquisitionType === 'NEW_PROJECT' && (
        <div className="form-group">
          <label>Organization Account *</label>
          <select
            value={selectedAccountId || ''}
            onChange={(e) => handleAccountSelect(parseInt(e.target.value))}
            required
          >
            <option value="">Select an account</option>
            {organizationAccounts.map((account: any) => (
              <option key={account.id} value={account.id}>
                {account.name} - Balance: ₹{account.totalAmount}
              </option>
            ))}
          </select>
          
          {selectedAccount && (
            <div className="account-info">
              <p>Available Balance: ₹{selectedAccount.totalAmount}</p>
              <p>Required Amount: ₹{totalCost}</p>
              <p className={hasSufficientFunds() ? 'success' : 'error'}>
                {hasSufficientFunds() ? '✓ Sufficient funds' : '✗ Insufficient funds'}
              </p>
            </div>
          )}
        </div>
      )}

      {/* Project Details */}
      <div className="form-group">
        <label>Project Name *</label>
        <input
          type="text"
          value={projectData.name}
          onChange={(e) => setProjectData(prev => ({ ...prev, name: e.target.value }))}
          required
        />
      </div>

      <div className="form-group">
        <label>Address *</label>
        <input
          type="text"
          value={projectData.address}
          onChange={(e) => setProjectData(prev => ({ ...prev, address: e.target.value }))}
          required
        />
      </div>

      {/* Costs Section */}
      <div className="costs-section">
        <h3>Costs</h3>
        <div className="form-group">
          <label>Purchasing Amount {acquisitionType === 'NEW_PROJECT' ? '*' : ''}</label>
          <input
            type="number"
            value={projectData.purchasingAmount}
            onChange={(e) => setProjectData(prev => ({ ...prev, purchasingAmount: parseFloat(e.target.value) }))}
            required={acquisitionType === 'NEW_PROJECT'}
          />
        </div>

        <div className="form-group">
          <label>Additional Amount</label>
          <input
            type="number"
            value={projectData.additionalAmount}
            onChange={(e) => setProjectData(prev => ({ ...prev, additionalAmount: parseFloat(e.target.value) }))}
          />
        </div>

        <div className="form-group">
          <label>Registration Amount</label>
          <input
            type="number"
            value={projectData.registrationAmount}
            onChange={(e) => setProjectData(prev => ({ ...prev, registrationAmount: parseFloat(e.target.value) }))}
          />
        </div>

        <div className="total-cost">
          <strong>Total Project Cost:</strong>
          <span>₹{totalCost}</span>
        </div>

        {acquisitionType === 'NEW_PROJECT' && (
          <div className="notice">
            ⚠️ This amount will be deducted from your organization account
          </div>
        )}
      </div>

      {/* Error Display */}
      {error && <div className="error-message">{error}</div>}

      {/* Submit Button */}
      <button type="submit" disabled={submitting || !isFormValid()}>
        {submitting ? 'Creating...' : 'Create Project'}
      </button>
    </form>
  );
};
```

---

## Testing Scenarios

### Scenario 1: Create NEW_PROJECT with Sufficient Funds ✓

**Setup:**
- User selects "NEW_PROJECT"
- Organization Account has balance: ₹100,000
- Project costs: ₹50,000

**Expected Result:**
- Account selector is required and shows balance
- "Sufficient funds" message appears
- Submit button is enabled
- Project created successfully
- Balance updated to ₹50,000

---

### Scenario 2: Create NEW_PROJECT with Insufficient Funds ✗

**Setup:**
- User selects "NEW_PROJECT"
- Organization Account has balance: ₹30,000
- Project costs: ₹50,000

**Expected Result:**
- "Insufficient funds" error message appears
- Submit button is disabled
- Error: "Insufficient funds in organization account for project purchase"

---

### Scenario 3: Create EXISTING_PROJECT ✓

**Setup:**
- User selects "EXISTING_PROJECT"
- No organization account selected
- Project costs entered for reference

**Expected Result:**
- Account selector is optional
- "No financial impact" notice appears
- Project created successfully
- No account balance changes

---

### Scenario 4: NEW_PROJECT without Account Selection ✗

**Setup:**
- User selects "NEW_PROJECT"
- No account is selected

**Expected Result:**
- Submit button remains disabled
- Error: "Organization account must be selected for new project acquisition"

---

### Scenario 5: NEW_PROJECT with Zero/Negative Costs ✗

**Setup:**
- User selects "NEW_PROJECT"
- All cost fields are 0 or empty

**Expected Result:**
- Submit button remains disabled
- Error: "Purchase costs must be greater than zero for new project"

---

## HTTP Request Examples with CURL

### Create NEW_PROJECT
```bash
curl -X POST http://localhost:8080/api/projects/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Downtown Complex",
    "address": "123 Main St",
    "acquisitionType": "NEW_PROJECT",
    "organizationAccountId": 5,
    "purchasingAmount": 50000,
    "additionalAmount": 5000,
    "registrationAmount": 1000,
    "projectType": "APARTMENT",
    "floors": 10,
    "monthDuration": 24,
    "floorList": []
  }'
```

### Create EXISTING_PROJECT
```bash
curl -X POST http://localhost:8080/api/projects/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Historic Building",
    "address": "456 Oak Ave",
    "acquisitionType": "EXISTING_PROJECT",
    "organizationAccountId": null,
    "purchasingAmount": 100000,
    "additionalAmount": 0,
    "registrationAmount": 0,
    "projectType": "SHOP",
    "floors": 3,
    "monthDuration": 12,
    "floorList": []
  }'
```

---

## Summary Checklist for Frontend Implementation

- [ ] Create acquisition type selection component (radio/toggle)
- [ ] Create conditional form rendering based on acquisition type
- [ ] Implement organization account selector with balance display
- [ ] Add real-time cost calculations
- [ ] Implement fund sufficiency validation
- [ ] Add validation error messages
- [ ] Create error handling for all API responses
- [ ] Add loading states during API calls
- [ ] Implement success message and redirect
- [ ] Add unit tests for form validation
- [ ] Add integration tests for API calls
- [ ] Add E2E tests for user workflows
- [ ] Add accessibility features (ARIA labels, keyboard navigation)
- [ ] Add responsive design for mobile
- [ ] Document component properties and methods
- [ ] Add user permission checks

---

## Support & Questions

For questions or issues during implementation, please refer to:
- Backend API Documentation: `/api/projects`
- Backend Implementation: `ProjectService.java`
- Database Schema: Project table with `acquisition_type` column

**Contact Backend Team** for clarifications on business logic or financial calculations.

