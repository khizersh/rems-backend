package com.rem.backend.dto.analytic;

import java.time.LocalDateTime;

public interface OrganizationAccountDetailProjection {
    String getAccountName();
    String getTransactionType();
    double getAmount();
    String getComments();
    String getProjectName();
    String getCustomerName();
    String getUnitSerialNo();
    String getCreatedBy();
    String getUpdatedBy();
    LocalDateTime getCreatedDate();
    LocalDateTime getUpdatedDate();
}
