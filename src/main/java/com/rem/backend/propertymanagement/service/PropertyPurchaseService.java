package com.rem.backend.propertymanagement.service;

import com.rem.backend.propertymanagement.entity.PropertyAsset;
import com.rem.backend.propertymanagement.entity.PropertyPurchase;
import com.rem.backend.propertymanagement.repository.PropertyAssetRepo;
import com.rem.backend.propertymanagement.repository.PropertyPurchaseRepo;
import com.rem.backend.propertymanagement.repository.PropertySellerRepo;
import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PropertyPurchaseService {

    private final PropertySellerRepo propertySellerRepo;
    private final PropertyPurchaseRepo propertyPurchaseRepo;
    private final PropertyAssetRepo propertyAssetRepo;
    private final JournalEntryService journalEntryService;

    @Transactional
    public Map<String, Object> createPurchase(PropertyPurchase purchase, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(purchase.getOrganizationId(), "organizationId");
            ValidationService.validate(purchase.getPropertySellerId(), "propertySellerId");
            ValidationService.validate(purchase.getTotalAmount(), "totalAmount");

            if (purchase.getTotalAmount() <= 0) {
                throw new IllegalArgumentException("Total amount must be greater than 0");
            }

            if (purchase.getPaidAmount() < 0) {
                throw new IllegalArgumentException("Paid amount cannot be negative");
            }

            // Seller must exist (and is NOT vendor module)
            if (propertySellerRepo.findByIdAndIsActiveTrue(purchase.getPropertySellerId()).isEmpty()) {
                throw new IllegalArgumentException("Invalid Property Seller");
            }

            // For purchase creation: enforce clean initial balances (no fixed plan; payments handled separately)
            purchase.setPaidAmount(0.0);
            purchase.setRemainingAmount(purchase.getTotalAmount());

            purchase.setCreatedBy(loggedInUser);
            purchase.setUpdatedBy(loggedInUser);

            PropertyPurchase savedPurchase = propertyPurchaseRepo.save(purchase);

            // Accounting: DR Property/Land Inventory, CR Property Seller Payable
            journalEntryService.createJournalEntryForPropertyPurchase(savedPurchase, loggedInUser);

            // Operational asset: created after purchase
            PropertyAsset asset = new PropertyAsset();
            asset.setOrganizationId(savedPurchase.getOrganizationId());
            asset.setPropertyPurchaseId(savedPurchase.getId());
            asset.setAcquiredAmount(savedPurchase.getTotalAmount());
            asset.setCreatedBy(loggedInUser);
            asset.setUpdatedBy(loggedInUser);

            PropertyAsset savedAsset = propertyAssetRepo.save(asset);

            Map<String, Object> data = new HashMap<>();
            data.put("propertyPurchase", savedPurchase);
            data.put("propertyAsset", savedAsset);

            return ResponseMapper.buildResponse(Responses.SUCCESS, data);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getPurchaseById(long purchaseId) {
        try {
            ValidationService.validate(purchaseId, "purchaseId");
            Optional<PropertyPurchase> purchaseOpt = propertyPurchaseRepo.findById(purchaseId);
            if (purchaseOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, purchaseOpt.get());
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> listPurchasesByOrganization(long organizationId) {
        try {
            ValidationService.validate(organizationId, "organizationId");
            List<PropertyPurchase> list =
                    propertyPurchaseRepo.findAllByOrganizationIdOrderByCreatedDateDesc(organizationId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}

