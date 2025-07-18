package com.rem.backend.service;

import com.rem.backend.entity.organization.OrganizationAccount;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.repository.OrganizationAccountDetailRepo;
import com.rem.backend.repository.OrganizationAccoutRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class OrganizationAccountService {

    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final ProjectRepo projectRepo;

    public Map<String, Object> getOrgAccountsByOrgId(long orgId) {
        try {
            ValidationService.validate(orgId, "orgId");
            List list = organizationAccountRepo.findByOrganizationId(orgId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getOrgAccountsById(long acctId) {
        try {
            ValidationService.validate(acctId, "account");
            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo.findById(acctId);

            if (organizationAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Account");


            return ResponseMapper.buildResponse(Responses.SUCCESS, organizationAccountOptional.get());

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getOrgAccountDetailByOrgAcctId(long orgAcctId , Pageable pageable) {
        try {
            ValidationService.validate(orgAcctId, "orgId");
            Page<OrganizationAccountDetail> accountDetails = organizationAccountDetailRepo.findByOrganizationAcctId(orgAcctId , pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, accountDetails);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> addAccountByOrg(OrganizationAccount organizationAccount , String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(organizationAccount.getAccountNo(), "orgId");
            ValidationService.validate(organizationAccount.getOrganizationId(), "orgId");
            ValidationService.validate(organizationAccount.getBankName(), "orgId");
            organizationAccount.setCreatedBy(loggedInUser);
            organizationAccount.setUpdatedBy(loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, organizationAccountRepo.save(organizationAccount));

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> addOrgAcctDetail(OrganizationAccountDetail organizationAccountDetail , String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(organizationAccountDetail.getOrganizationAcctId(), "organization account");
            ValidationService.validate(organizationAccountDetail.getCustomerAccountId(), "customer account");
            ValidationService.validate(organizationAccountDetail.getCustomerPaymentDetailId(), "customer payment");
            ValidationService.validate(organizationAccountDetail.getAmount(), "orgId");
            organizationAccountDetail.setCreatedBy(loggedInUser);
            organizationAccountDetail.setUpdatedBy(loggedInUser);
            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo.findById(organizationAccountDetail.getOrganizationAcctId());
            if (!organizationAccountOptional.isPresent())
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid Account");


            OrganizationAccount organizationAccount = organizationAccountOptional.get();
            organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() + organizationAccountDetail.getAmount());
            organizationAccountRepo.save(organizationAccount);
            return ResponseMapper.buildResponse(Responses.SUCCESS, organizationAccountDetailRepo.save(organizationAccountDetail));

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public OrganizationAccount deductFromOrgAcct(OrganizationAccountDetail organizationAccountDetail , String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(organizationAccountDetail.getOrganizationAcctId(), "organization account");
            ValidationService.validate(organizationAccountDetail.getAmount(), "orgId");

            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(organizationAccountDetail.getProjectId());

            if (!projectOptional.isPresent())
                throw new IllegalArgumentException("Invalid Project");



            organizationAccountDetail.setProjectName(projectOptional.get().getName());
            organizationAccountDetail.setCreatedBy(loggedInUser);
            organizationAccountDetail.setUpdatedBy(loggedInUser);
            organizationAccountDetail.setTransactionType(TransactionType.DEBIT);
            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo.findById(organizationAccountDetail.getOrganizationAcctId());

            if (!organizationAccountOptional.isPresent())
                throw new IllegalArgumentException("Invalid Account");


            OrganizationAccount organizationAccount = organizationAccountOptional.get();
            double remainingAmount = organizationAccount.getTotalAmount() - organizationAccountDetail.getAmount();

            if (remainingAmount < 0)
                throw new IllegalArgumentException("Not Enough Funds for this account");


            organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() - organizationAccountDetail.getAmount());
            organizationAccount.setUpdatedBy(loggedInUser);
            organizationAccountRepo.save(organizationAccount);
            organizationAccountDetailRepo.save(organizationAccountDetail);

            return organizationAccountOptional.get();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

}
