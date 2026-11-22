package com.rem.backend.accountingmanagement.service;

import com.rem.backend.accountingmanagement.entity.JournalDetailEntry;
import com.rem.backend.accountingmanagement.entity.JournalEntry;
import com.rem.backend.accountingmanagement.repos.ChartOfAccountRepository;
import com.rem.backend.accountingmanagement.repos.JournalDetailEntryRepository;
import com.rem.backend.accountingmanagement.repos.JournalEntryRepository;
import com.rem.backend.accountingmanagement.utility.JournalUtilities;
import com.rem.backend.customermanagement.entity.Customer;
import com.rem.backend.customermanagement.entity.CustomerAccount;
import com.rem.backend.customermanagement.entity.CustomerPayment;
import com.rem.backend.enums.JournalEntryStatus;
import com.rem.backend.purchasemanagement.repository.GrnItemsRepo;
import com.rem.backend.purchasemanagement.repository.GrnRepo;
import com.rem.backend.purchasemanagement.repository.PurchaseOrderItemRepo;
import com.rem.backend.propertymanagement.repository.PropertyPurchaseRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerPaymentJournalReversalTest {
    @Mock JournalEntryRepository headers;
    @Mock JournalDetailEntryRepository details;
    @Mock ChartOfAccountRepository accounts;
    @Mock PropertyPurchaseRepo purchases;
    @Mock JournalUtilities utilities;
    @Mock GrnRepo grns;
    @Mock GrnItemsRepo grnItems;
    @Mock PurchaseOrderItemRepo purchaseOrderItems;

    private JournalEntryService service;

    @BeforeEach
    void setUp() {
        service = new JournalEntryService(headers, details, accounts, purchases, utilities, grns,
                grnItems, purchaseOrderItems);
    }

    @Test
    void reversesAndCancelsOriginalCustomerPaymentJournal() {
        Customer customer = new Customer();
        customer.setCustomerId(10L);
        customer.setOrganizationId(1L);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setCustomer(customer);

        CustomerPayment payment = new CustomerPayment();
        payment.setId(7L);

        JournalEntry original = new JournalEntry();
        original.setId(20L);
        original.setOrganizationId(1L);
        original.setReferenceType("CUSTOMER_PAYMENT");
        original.setStatus(JournalEntryStatus.POSTED);
        when(headers.findAllByOrganizationIdAndReferenceTypeAndAdditionalReferenceId(
                1L, "CUSTOMER_PAYMENT", 7L)).thenReturn(List.of(original));
        when(headers.findAllByOrganizationIdAndReferenceTypeAndAdditionalReferenceId(
                1L, "PAYMENT_POSTING", 7L)).thenReturn(List.of());
        when(headers.findAllByOrganizationIdAndReferenceTypeAndDescriptionEndingWith(
                anyLong(), anyString(), anyString())).thenReturn(List.of());
        when(details.findAllByJournalEntryId(20L)).thenReturn(List.of(
                journalLine(20, 3, 25, 0),
                journalLine(20, 4, 0, 25)));
        when(headers.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        service.reverseJournalEntriesForCustomerPayment(customerAccount, payment, "user");

        assertEquals(JournalEntryStatus.CANCELLED, original.getStatus());
        ArgumentCaptor<JournalEntry> headerCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(headers).save(headerCaptor.capture());
        assertEquals("CUSTOMER_PAYMENT_REVERSAL", headerCaptor.getValue().getReferenceType());
        assertEquals(7L, headerCaptor.getValue().getAdditionalReferenceId());

        ArgumentCaptor<JournalDetailEntry> detailCaptor = ArgumentCaptor.forClass(JournalDetailEntry.class);
        verify(details, times(2)).save(detailCaptor.capture());
        assertEquals(0, detailCaptor.getAllValues().get(0).getDebitAmount());
        assertEquals(25, detailCaptor.getAllValues().get(0).getCreditAmount());
        assertEquals(25, detailCaptor.getAllValues().get(1).getDebitAmount());
        assertEquals(0, detailCaptor.getAllValues().get(1).getCreditAmount());
        verify(headers).saveAll(List.of(original));
    }

    private static JournalDetailEntry journalLine(long journalId, long accountId, double debit, double credit) {
        JournalDetailEntry line = new JournalDetailEntry();
        line.setJournalEntryId(journalId);
        line.setChartOfAccountId(accountId);
        line.setDebitAmount(debit);
        line.setCreditAmount(credit);
        return line;
    }
}
