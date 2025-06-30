package com.rem.backend.service;

import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.entity.vendor.VendorPayment;
import com.rem.backend.repository.VendorAccountDetailRepo;
import com.rem.backend.repository.VendorAccountRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class VendorAccountService {

    private final VendorAccountRepo vendorAccountRepository;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;


    public Map<String, Object> getAllVendorAccounts(long orgId, Pageable pageable) {
        try {
            Page<VendorAccount> vendorAccounts = vendorAccountRepository.findAllByOrganizationId(orgId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllVendorAccountsByOrg(long orgId) {
        try {
            List<Map<String , Object>> vendorAccounts = vendorAccountRepository.findAllByOrgId(orgId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getVendorDetailsByAccount(long acctId, Pageable pageable) {
        try {
            Page<VendorPayment> vendorAccounts = vendorAccountDetailRepo.findByVendorAccountId(acctId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Get all vendor accounts by name or get all
    public Map<String, Object> getAllVendorAccountsFilter(String nameFilter) {
        try {
            List<VendorAccount> vendorAccounts;
            if (nameFilter != null && !nameFilter.isBlank()) {
                vendorAccounts = vendorAccountRepository.findByNameContainingIgnoreCase(nameFilter);
            } else {
                vendorAccounts = vendorAccountRepository.findAll();
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccounts);
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Get a vendor account by ID
    public Map<String, Object> getVendorAccountById(long id) {
        try {
            ValidationService.validate(id, "vendorAccountId");
            Optional<VendorAccount> optional = vendorAccountRepository.findById(id);
            if (optional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, optional.get());
            } else {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Vendor account not found");
            }
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Create a vendor account
    public Map<String, Object> createVendorAccount(VendorAccount vendorAccount, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(vendorAccount.getName(), "name");
            ValidationService.validate(vendorAccount.getOrganizationId(), "organization");

            vendorAccount.setCreatedBy(loggedInUser);
            vendorAccount.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountRepository.save(vendorAccount));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> addOrUpdatePaymentHistory(VendorPayment vendorPayment, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(vendorPayment.getVendorAccountId(), "account");
            ValidationService.validate(vendorPayment.getTransactionType(), "transaction type");


//             TODO :: DO CALCUTALITION FOR DEBOT / CREDIT AND ALSO UPDATE VENDOR ACCOUNT
            vendorPayment.setCreatedBy(loggedInUser);
            vendorPayment.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountDetailRepo.save(vendorPayment));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Update a vendor account
    public Map<String, Object> updateVendorAccount(long id, VendorAccount updatedAccount, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(id, "vendorAccountId");

            Optional<VendorAccount> optional = vendorAccountRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Vendor account not found");
            }

            VendorAccount existing = optional.get();
            existing.setName(updatedAccount.getName());
            existing.setTotalAmountPaid(updatedAccount.getTotalAmountPaid());
            existing.setTotalCreditAmount(updatedAccount.getTotalCreditAmount());
            existing.setTotalBalanceAmount(updatedAccount.getTotalBalanceAmount());
            existing.setTotalAmount(updatedAccount.getTotalAmount());
            existing.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, vendorAccountRepository.save(existing));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ✅ Delete a vendor account
    public Map<String, Object> deleteVendorAccount(long id) {
        try {
            ValidationService.validate(id, "vendorAccountId");

            if (!vendorAccountRepository.existsById(id)) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Vendor account not found");
            }

            vendorAccountRepository.deleteById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Vendor account deleted");
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
