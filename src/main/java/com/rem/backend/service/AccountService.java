package com.rem.backend.service;

import com.rem.backend.dto.accounting.AccountGroupDTO;
import com.rem.backend.dto.accounting.AccountTypeDTO;
import com.rem.backend.dto.accounting.ChartOfAccountDTO;
import com.rem.backend.dto.accounting.CreateAccountGroupRequest;
import com.rem.backend.dto.accounting.CreateAccountGroupResponse;
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
import com.rem.backend.utility.Utility;
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
    private final Utility utility;

    public Map<String, Object> getAllChartOfAccounts(
            Long organizationId,
            Long accountType,
            Long accountGroup
    ) {

        try {
            List<ChartOfAccount> accounts;

            if (accountGroup != null) {
                accounts = coaRepo
                        .findAllByOrganization_OrganizationIdAndAccountGroup_Id(
                                organizationId, accountGroup);
            } else if (accountType != null) {
                AccountType type = typeRepo.findById(accountType)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid account type"));

                List<AccountGroup> groups =
                        groupRepo.findAllByOrganization_OrganizationIdAndAccountType(
                                organizationId, type);

                accounts = coaRepo
                        .findAllByOrganization_OrganizationIdAndAccountGroupIn(
                                organizationId, groups);
            } else {
                accounts = coaRepo
                        .findAllByOrganization_OrganizationIdAndStatus(
                                organizationId, AccountStatus.ACTIVE);
            }

            List<ChartOfAccountDTO> dtoList = accounts.stream()
                    .map(a -> new ChartOfAccountDTO(
                            a.getId(),
                            a.getCode(),
                            a.getName(),
                            new AccountGroupDTO(
                                    a.getAccountGroup().getId(),
                                    a.getAccountGroup().getName(),
                                    new AccountTypeDTO(
                                            a.getAccountGroup().getAccountType().getId(),
                                            a.getAccountGroup().getAccountType().getName()
                                    ),
                                    a.getAccountGroup().getCreatedDate().toString()
                            ),
                            a.isSystemGenerated(),
                            a.getStatus().name(),
                            a.getOrganizationAccountId(),
                            a.getCreatedDate().toString(),
                            a.getUpdatedDate().toString()
                    ))
                    .toList();




            Map<String, Object> response = new HashMap<>();
            response.put("count", dtoList.size());
            response.put("data", dtoList);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAllAccountType() {
        try {
            List<AccountType> types = typeRepo.findAll();

            // Map entities to DTOs
            List<AccountTypeDTO> dtoList = types.stream()
                    .map(t -> new AccountTypeDTO(
                            t.getId(),
                            t.getName()
                    ))
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("count", dtoList.size());
            response.put("data", dtoList);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAccountGroups(
            Long accountType, Long organizationId, String loggedInUser
    ) {

        try {
                List<AccountGroup> groups =
                        groupRepo.findAllByAccountType_IdAndOrganization_OrganizationId(
                                accountType,organizationId);


            List<AccountGroupDTO> dtoList = groups.stream()
                    .filter(g -> !"Construction".equalsIgnoreCase(g.getName()))
                    .map(g -> new AccountGroupDTO(
                            g.getId(),
                            g.getName(),
                            new AccountTypeDTO(
                                    g.getAccountType().getId(),
                                    g.getAccountType().getName()
                            ),
                            g.getCreatedDate().toString()
                    ))
                    .toList();


            Map<String, Object> response = new HashMap<>();
            response.put("count", dtoList.size());
            response.put("data", dtoList);
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
            if (coaRepo.existsByNameAndOrganization_OrganizationId(
                    request.getName(), organizationId)) {
                throw new RuntimeException("Account code already exists");
            }

            // 5. Create COA
            ChartOfAccount coa = new ChartOfAccount();
            coa.setOrganization(group.getOrganization());
            coa.setAccountGroup(group);
            coa.setCode(utility.generateAccountCode(group.getOrganization().getOrganizationId(),
                    group.getAccountType().getName().substring(0,3)));
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
            if (groupRepo.existsByNameAndOrganization_OrganizationId(request.getName(), organizationId)) {
                throw new RuntimeException("Account group already exists for this organization");
            }

            Optional<Organization> organization = organizationRepo.findById(organizationId);


            // 3. Create group
            AccountGroup group = new AccountGroup();
            group.setName(request.getName());
            group.setOrganization(organization.get());
            group.setAccountType(type);

            AccountGroup saved = groupRepo.save(group);

            CreateAccountGroupResponse dto = new CreateAccountGroupResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getAccountType().getId(),
                    saved.getOrganization().getOrganizationId(),
                    saved.getCreatedDate()
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> updateAccountGroup(
            Long organizationId,
            Long groupId,
            CreateAccountGroupRequest request,
            String loggedInUser
    ) {
        try {
            // 1. Fetch existing group
            AccountGroup group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Account group not found"));

            // 2. Validate organization ownership
            if (!(group.getOrganization().getOrganizationId() == (organizationId))) {
                throw new RuntimeException("Account group does not belong to this organization");
            }

            // 3. Validate account type
            AccountType type = typeRepo.findById(request.getAccountTypeId())
                    .orElseThrow(() -> new RuntimeException("Account type not found"));

            // 4. Check duplicate name (excluding current group)
            if (groupRepo.existsByNameAndOrganization_OrganizationIdAndIdNot(
                    request.getName(), organizationId, groupId)) {
                throw new RuntimeException("Account group name already exists");
            }

            // 5. Update fields
            group.setName(request.getName());
            group.setAccountType(type);

            AccountGroup updated = groupRepo.save(group);

            CreateAccountGroupResponse dto = new CreateAccountGroupResponse(
                    updated.getId(),
                    updated.getName(),
                    updated.getAccountType().getId(),
                    updated.getOrganization().getOrganizationId(),
                    updated.getCreatedDate()
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }



    @Transactional
    public Map<String, Object> updateExpenseChartOfAccountName(
            long organizationId,
            long coaId,
            String newName,
            String loggedInUser
    ) {
        try {
            if (newName == null || newName.trim().isEmpty()) {
                throw new RuntimeException("Account name cannot be empty");
            }

            ChartOfAccount coa = coaRepo.findById(coaId)
                    .orElseThrow(() -> new RuntimeException("Chart of account not found"));

            if (coa.getOrganization().getOrganizationId() != organizationId) {
                throw new RuntimeException("COA does not belong to organization");
            }

            if (!coa.getAccountGroup()
                    .getAccountType()
                    .getName()
                    .equalsIgnoreCase("EXPENSE")) {
                throw new RuntimeException("Only EXPENSE COA can be updated");
            }

            if (coaRepo.existsByNameAndOrganization_OrganizationIdAndIdNot(
                    newName, organizationId, coaId)) {
                throw new RuntimeException("Account name already exists");
            }

            coa.setName(newName);
            ChartOfAccount updated = coaRepo.save(coa);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("code", updated.getCode());
            response.put("name", updated.getName());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    e.getMessage()
            );
        }
    }

    public Map<String, Object> getChartOfAccountById(long chartOfAccountId) {

        try {
            ChartOfAccount chartOfAccount = coaRepo
                    .findById(chartOfAccountId)
                    .orElseThrow(() ->
                            new RuntimeException("Chart Of Account Not Found!"));
            ChartOfAccountDTO chartOfAccountDTO =   new ChartOfAccountDTO(
                    chartOfAccount.getId(),
                    chartOfAccount.getCode(),
                    chartOfAccount.getName(),
                    null,
                    chartOfAccount.isSystemGenerated(),
                    chartOfAccount.getStatus().name(),
                    chartOfAccount.getOrganizationAccountId(),
                    chartOfAccount.getCreatedDate().toString(),
                    chartOfAccount.getUpdatedDate().toString()
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, chartOfAccountDTO);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    e.getMessage()
            );
        }
    }

    public Map<String, Object> getAccountGroupById(
            Long accountGroupId
    ) {

        try {
            Optional<AccountGroup>  groups = groupRepo.findById(accountGroupId);

            AccountGroup g = groups.get();


            AccountGroupDTO group = new AccountGroupDTO(
                    g.getId(),
                    g.getName(),
                    new AccountTypeDTO(
                            g.getAccountType().getId(),
                            g.getAccountType().getName()
                    ),
                    g.getCreatedDate().toString()
            );


            return ResponseMapper.buildResponse(Responses.SUCCESS, group);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

}
