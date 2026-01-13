package com.rem.backend.service;

import com.rem.backend.dto.accounting.CreateAccountGroupRequest;
import com.rem.backend.dto.accounting.CreateChartOfAccountRequest;
import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.AccountType;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.organization.Organization;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.repository.AccountGroupRepository;
import com.rem.backend.repository.AccountTypeRepository;
import com.rem.backend.repository.ChartOfAccountRepository;
import com.rem.backend.repository.OrganizationRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final ChartOfAccountRepository coaRepo;
    private final AccountGroupRepository groupRepo;
    private final AccountTypeRepository typeRepo;
    private final OrganizationRepo organizationRepo;

    public Map<String, Object> getAllChartOfAccounts(
            Long organizationId,
            Long accountType,
            Long accountGroup
    ) {

        try {
            List<ChartOfAccount> accounts;

            if (accountGroup != null) {
                accounts = coaRepo
                        .findAllByOrganizationIdAndAccountGroup_Id(
                                organizationId, accountGroup);
            } else if (accountType != null) {
                AccountType type = typeRepo.findById(accountType)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid account type"));

                List<AccountGroup> groups =
                        groupRepo.findAllByOrganization_OrganizationIdAndAccountType(
                                organizationId, type);

                accounts = coaRepo
                        .findAllByOrganizationIdAndAccountGroupIn(
                                organizationId, groups);
            } else {
                accounts = coaRepo
                        .findAllByOrganizationIdAndStatus(
                                organizationId, AccountStatus.ACTIVE);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("count", accounts.size());
            response.put("data", accounts);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAllAccountType(
    ) {

        try {
            List<AccountType> type = typeRepo.findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("count", type.size());
            response.put("data", type);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAccountGroups(
            Long accountType
    ) {

        try {
                List<AccountGroup> groups =
                        groupRepo.findAllByAccountType_Id(
                                accountType);


            Map<String, Object> response = new HashMap<>();
            response.put("count", groups.size());
            response.put("data", groups);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> createExpenseChartOfAccount(
            long organizationId,
            CreateChartOfAccountRequest request,
            String loggedInUser
    ) {

        try {
            // 1. Fetch Account Group
            AccountGroup group = groupRepo
                    .findById(request.getAccountGroupId())
                    .orElseThrow(() ->
                            new RuntimeException("Account group not found"));

            // 2. Validate ownership
            if (group.getOrganization().getOrganizationId() != organizationId) {
                throw new RuntimeException("Account group does not belong to organization");
            }

            // 3. Validate group type = EXPENSE
            if (!group.getAccountType().getName().equalsIgnoreCase("EXPENSE")) {
                throw new RuntimeException("COA can only be created under EXPENSE account type");
            }

            // 4. Ensure unique code per organization
            if (coaRepo.existsByCodeAndOrganizationId(
                    request.getCode(), organizationId)) {
                throw new RuntimeException("Account code already exists");
            }

            // 5. Create COA
            ChartOfAccount coa = new ChartOfAccount();
            coa.setOrganizationId(organizationId);
            coa.setAccountGroup(group);
            coa.setCode(request.getCode());
            coa.setName(request.getName());
            coa.setStatus(AccountStatus.ACTIVE);
            coa.setSystemGenerated(false);
            coa.setOrganizationAccountId(null);

            ChartOfAccount saved = coaRepo.save(coa);

            // 6. Response
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("code", saved.getCode());
            response.put("name", saved.getName());
            response.put("group", group.getName());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    e.getMessage()
            );
        }
    }

    public Map<String, Object> createAccountGroup(Long organizationId, CreateAccountGroupRequest request,
                                                  String loggedInUser) {
        try {
            // 1. Validate AccountType exists
            AccountType type = typeRepo.findById(request.getAccountTypeId())
                    .orElseThrow(() -> new RuntimeException("Account type not found"));

            // 2. Check duplicate group
            if (groupRepo.existsByNameAndOrganizationId(request.getName(), organizationId)) {
                throw new RuntimeException("Account group already exists for this organization");
            }

            Optional<Organization> organization = organizationRepo.findById(organizationId);


            // 3. Create group
            AccountGroup group = new AccountGroup();
            group.setName(request.getName());
            group.setOrganization(organization.get());
            group.setAccountType(type);

            AccountGroup saved = groupRepo.save(group);

            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
