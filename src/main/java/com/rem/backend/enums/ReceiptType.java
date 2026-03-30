package com.rem.backend.enums;

/**
 * GRN Receipt Type:
 * - STOCK: Material received into warehouse, adds to inventory stock
 * - DIRECT: Material directly consumed by project (no stock entry),
 *           construction amount increases when vendor payment is made
 */
public enum ReceiptType {
    STOCK,      // Material goes to warehouse (previously WAREHOUSE_STOCK)
    DIRECT      // Material directly used in project (previously DIRECT_CONSUME)
}
