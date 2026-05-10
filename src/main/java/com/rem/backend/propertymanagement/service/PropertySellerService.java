package com.rem.backend.propertymanagement.service;

import com.rem.backend.propertymanagement.entity.PropertySeller;
import com.rem.backend.propertymanagement.repository.PropertySellerRepo;
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
public class PropertySellerService {

    private final PropertySellerRepo propertySellerRepo;

    @Transactional
    public Map<String, Object> addSeller(PropertySeller seller, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(seller.getOrganizationId(), "organizationId");
            ValidationService.validate(seller.getName(), "seller name");

            seller.setCreatedBy(loggedInUser);
            seller.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, propertySellerRepo.save(seller));
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateSeller(PropertySeller seller, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(seller.getId(), "seller id");
            ValidationService.validate(seller.getOrganizationId(), "organizationId");
            ValidationService.validate(seller.getName(), "seller name");

            Optional<PropertySeller> existingOpt = propertySellerRepo.findById(seller.getId());
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid Property Seller");
            }

            PropertySeller existing = existingOpt.get();

            // Keep createdBy/date from DB; align with current style: set updatedBy.
            seller.setCreatedBy(existing.getCreatedBy());
            seller.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, propertySellerRepo.save(seller));
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getSellerById(long id) {
        try {
            ValidationService.validate(id, "seller id");
            return propertySellerRepo.findByIdAndIsActiveTrue(id)
                    .<Map<String, Object>>map(s -> ResponseMapper.buildResponse(Responses.SUCCESS, s))
                    .orElseGet(() -> ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> listSellersByOrganization(long organizationId) {
        try {
            ValidationService.validate(organizationId, "organizationId");
            List<PropertySeller> list =
                    propertySellerRepo.findAllByOrganizationIdAndIsActiveTrueOrderByCreatedDateDesc(organizationId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}

