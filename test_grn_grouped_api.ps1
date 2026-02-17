# GRN Grouped by PO ID API - PowerShell Test Script
# Replace YOUR_JWT_TOKEN with actual JWT token
# Replace localhost:8080 with your server URL if different

Write-Host "Testing GRN Grouped by PO ID API..." -ForegroundColor Green

# Test 1: Basic request
Write-Host "`n1. Basic Request (First page, sorted by last GRN date):" -ForegroundColor Yellow
$body1 = @{
    id = 1
    page = 0
    size = 10
    sortBy = "lastGrnDate"
    sortDir = "desc"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/grn/getGroupedByPoId" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer YOUR_JWT_TOKEN"} `
  -Body $body1

# Test 2: Sort by GRN count
Write-Host "`n2. Sort by GRN Count (Ascending):" -ForegroundColor Yellow
$body2 = @{
    id = 1
    page = 0
    size = 10
    sortBy = "totalGrnCount"
    sortDir = "asc"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/grn/getGroupedByPoId" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer YOUR_JWT_TOKEN"} `
  -Body $body2

# Test 3: Sort by PO number
Write-Host "`n3. Sort by PO Number (Alphabetical):" -ForegroundColor Yellow
$body3 = @{
    id = 1
    page = 0
    size = 10
    sortBy = "poNumber"
    sortDir = "asc"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/grn/getGroupedByPoId" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer YOUR_JWT_TOKEN"} `
  -Body $body3

# Test 4: Large page size
Write-Host "`n4. Large Page Size (20 records):" -ForegroundColor Yellow
$body4 = @{
    id = 1
    page = 0
    size = 20
    sortBy = "lastGrnDate"
    sortDir = "desc"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/grn/getGroupedByPoId" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer YOUR_JWT_TOKEN"} `
  -Body $body4

# Test 5: Second page
Write-Host "`n5. Second Page:" -ForegroundColor Yellow
$body5 = @{
    id = 1
    page = 1
    size = 10
    sortBy = "lastGrnDate"
    sortDir = "desc"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/grn/getGroupedByPoId" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer YOUR_JWT_TOKEN"} `
  -Body $body5

Write-Host "`nTest completed!" -ForegroundColor Green
Write-Host "Note: Replace YOUR_JWT_TOKEN with actual token and ensure the server is running." -ForegroundColor Cyan
