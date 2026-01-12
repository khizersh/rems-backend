package com.rem.backend.service;

import com.rem.backend.dto.accounting.CreateChartOfAccountRequest;
import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.AccountType;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.repository.AccountGroupRepository;
import com.rem.backend.repository.AccountTypeRepository;
import com.rem.backend.repository.ChartOfAccountRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final ChartOfAccountRepository coaRepo;
    private final AccountGroupRepository groupRepo;
    private final AccountTypeRepository typeRepo;

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


    public Map<String, Object> createChartOfAccount(
            long organizationId,
            CreateChartOfAccountRequest request
    ) {

        try {
            AccountGroup group = groupRepo
                    .findById(request.getAccountGroupId())
                    .orElseThrow(() ->
                            new RuntimeException("Account group not found"));

            if (group.getOrganization().getOrganizationId() != organizationId) {
                throw new RuntimeException("Account group does not belong to organization");
            }

            if (coaRepo.existsByCodeAndOrganizationId(
                    request.getCode(), organizationId)) {
                throw new RuntimeException("Account code already exists");
            }

            ChartOfAccount coa = new ChartOfAccount();
            coa.setCode(request.getCode());
            coa.setName(request.getName());
            coa.setOrganizationId(organizationId);
            coa.setAccountGroup(group);
            coa.setReferenceId(request.getReferenceId());
            coa.setStatus(AccountStatus.ACTIVE);

            ChartOfAccount saved = coaRepo.save(coa);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("code", saved.getCode());
            response.put("name", saved.getName());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
