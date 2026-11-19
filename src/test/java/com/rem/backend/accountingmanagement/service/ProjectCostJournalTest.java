package com.rem.backend.accountingmanagement.service;

import com.rem.backend.accountingmanagement.entity.ChartOfAccount;
import com.rem.backend.accountingmanagement.entity.JournalDetailEntry;
import com.rem.backend.accountingmanagement.entity.JournalEntry;
import com.rem.backend.accountingmanagement.repos.ChartOfAccountRepository;
import com.rem.backend.accountingmanagement.repos.JournalDetailEntryRepository;
import com.rem.backend.accountingmanagement.repos.JournalEntryRepository;
import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.projectmanagement.entity.ProjectCost;
import com.rem.backend.projectmanagement.entity.ProjectCostPayment;
import com.rem.backend.projectmanagement.enums.ProjectCostPaymentType;
import com.rem.backend.propertymanagement.repository.PropertyPurchaseRepo;
import com.rem.backend.purchasemanagement.repository.GrnItemsRepo;
import com.rem.backend.purchasemanagement.repository.GrnRepo;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderItemRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectCostJournalTest {
    @Mock JournalEntryRepository headers;
    @Mock JournalDetailEntryRepository details;
    @Mock ChartOfAccountRepository accounts;
    @Mock PropertyPurchaseRepo purchases;
    @Mock JournalUtilities utilities;
    @Mock GrnRepo grns;
    @Mock GrnItemsRepo grnItems;
    @Mock PurchaseOrderItemRepo purchaseOrderItems;

    private JournalEntryService service;
    private ProjectCost cost;

    @BeforeEach
    void setUp() {
        service = new JournalEntryService(headers, details, accounts, purchases, utilities, grns,
                grnItems, purchaseOrderItems);
        cost = new ProjectCost();
        cost.setId(42L);
        cost.setOrganizationId(1L);
        cost.setProjectId(4L);
        cost.setTitle("Registration");
        cost.setPaymentType(ProjectCostPaymentType.CREDIT);
        cost.setTotalAmount(new BigDecimal("10000.00"));
        cost.setPaidAmount(new BigDecimal("0.00"));
        cost.setPendingAmount(new BigDecimal("10000.00"));
        lenient().when(headers.save(any())).thenAnswer(invocation -> {
            JournalEntry header = invocation.getArgument(0);
            header.setId(100L);
            return header;
        });
    }

    @Test
    void creationPostsInventoryAndPayableWithoutBank() {
        ChartOfAccount inventory = account(3);
        ChartOfAccount payable = account(5);
        when(utilities.constructionInventory(1L)).thenReturn(inventory);
        when(utilities.vendorPayable(1L)).thenReturn(payable);

        service.createJournalEntryForProjectCost(cost, "user");

        ArgumentCaptor<JournalDetailEntry> entries = ArgumentCaptor.forClass(JournalDetailEntry.class);
        verify(details, times(2)).save(entries.capture());
        List<JournalDetailEntry> lines = entries.getAllValues();
        assertLine(lines, 3, 10000, 0);
        assertLine(lines, 5, 0, 10000);
        verifyNoInteractions(accounts);
    }

    @Test
    void installmentDebitsPayableAndCreditsSelectedOrganizationBank() {
        when(utilities.vendorPayable(1L)).thenReturn(account(5));
        when(accounts.findFirstByOrganization_OrganizationIdAndOrganizationAccountIdAndStatusAndAccountGroup_NameIgnoreCase(
                1L, 7L, AccountStatus.ACTIVE, "Bank/Cash")).thenReturn(Optional.of(account(81)));
        ProjectCostPayment payment = new ProjectCostPayment();
        payment.setId(20L);
        payment.setOrganizationAccountId(7L);
        payment.setAmount(new BigDecimal("5000.00"));

        service.createJournalEntryForProjectCostPayment(cost, payment, "user");

        ArgumentCaptor<JournalDetailEntry> entries = ArgumentCaptor.forClass(JournalDetailEntry.class);
        verify(details, times(2)).save(entries.capture());
        assertLine(entries.getAllValues(), 5, 5000, 0);
        assertLine(entries.getAllValues(), 81, 0, 5000);
        verify(utilities, never()).constructionInventory(anyLong());
    }

    private static ChartOfAccount account(long id) {
        ChartOfAccount account = new ChartOfAccount();
        account.setId(id);
        return account;
    }

    private static void assertLine(List<JournalDetailEntry> lines, long account, double debit, double credit) {
        JournalDetailEntry line = lines.stream().filter(entry -> entry.getChartOfAccountId() == account)
                .findFirst().orElseThrow();
        assertEquals(debit, line.getDebitAmount());
        assertEquals(credit, line.getCreditAmount());
        assertTrue(line.getDebitAmount() > 0 || line.getCreditAmount() > 0);
    }
}
