package com.rem.backend.propertymanagement.service;

import com.rem.backend.organizationmanagement.entity.OrganizationAccount;
import com.rem.backend.organizationmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.organizationmanagement.enums.TransactionCategory;
import com.rem.backend.organizationmanagement.service.OrganizationAccountService;
import com.rem.backend.propertymanagement.entity.PropertyPayment;
import com.rem.backend.propertymanagement.entity.PropertyPurchase;
import com.rem.backend.propertymanagement.repository.PropertyPaymentRepo;
import com.rem.backend.propertymanagement.repository.PropertyPurchaseRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationAccoutRepo;
import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PropertyPaymentService {

    private final PropertyPurchaseRepo propertyPurchaseRepo;
    private final PropertyPaymentRepo propertyPaymentRepo;
    private final OrganizationAccoutRepo organizationAccoutRepo;
    private final OrganizationAccountService organizationAccountService;
    private final JournalEntryService journalEntryService;

    @Transactional
    public Map<String, Object> addPayment(PropertyPayment payment, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(payment.getOrganizationId(), "organizationId");
            ValidationService.validate(payment.getPropertyPurchaseId(), "propertyPurchaseId");
            ValidationService.validate(payment.getOrganizationAccountId(), "organizationAccountId");
            ValidationService.validate(payment.getPaymentType(), "paymentType");
            ValidationService.validate(payment.getAmount(), "amount");

            if (payment.getAmount() <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than 0");
            }

            Optional<PropertyPurchase> purchaseOpt = propertyPurchaseRepo.findByIdAndOrganizationId(
                    payment.getPropertyPurchaseId(),
                    payment.getOrganizationId()
            );
            if (purchaseOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid Property Purchase");
            }

            PropertyPurchase purchase = purchaseOpt.get();

            if (payment.getAmount() > purchase.getRemainingAmount() + 0.01) {
                throw new IllegalArgumentException("Payment exceeds remaining payable amount");
            }

            Optional<OrganizationAccount> orgAccountOpt = organizationAccoutRepo.findById(payment.getOrganizationAccountId());
            if (orgAccountOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid Organization Account");
            }

            // Deduct from org account (same pattern used in ExpenseService)
            OrganizationAccountDetail detail = new OrganizationAccountDetail();
            detail.setAmount(payment.getAmount());
            detail.setOrganizationAcctId(payment.getOrganizationAccountId());
            detail.setComments(
                    payment.getComments() != null ? payment.getComments() : "Property seller payment"
            );
            // Existing system uses CUSTOMER_PAYMENT as a generic deduction category.
            detail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);

            OrganizationAccount updatedOrgAccount =
                    organizationAccountService.deductFromOrgAcct(detail, loggedInUser);

            // Persist payment
            payment.setCreatedBy(loggedInUser);
            payment.setUpdatedBy(loggedInUser);
            PropertyPayment savedPayment = propertyPaymentRepo.save(payment);

            // Update payable balances on purchase
            double newPaid = purchase.getPaidAmount() + payment.getAmount();
            double newRemaining = purchase.getTotalAmount() - newPaid;
            if (newRemaining < 0) newRemaining = 0;

            purchase.setPaidAmount(newPaid);
            purchase.setRemainingAmount(newRemaining);
            purchase.setUpdatedBy(loggedInUser);
            propertyPurchaseRepo.save(purchase);

            // Accounting: DR Property Seller Payable, CR Bank/Cash
            journalEntryService.createJournalEntryForPropertyPayment(
                    purchase,
                    savedPayment,
                    updatedOrgAccount,
                    loggedInUser
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, savedPayment);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> listPaymentsByPurchase(long purchaseId) {
        try {
            ValidationService.validate(purchaseId, "propertyPurchaseId");
            List<PropertyPayment> list =
                    propertyPaymentRepo.findByPropertyPurchaseIdOrderByCreatedDateDesc(purchaseId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}

