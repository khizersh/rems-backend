package com.rem.backend.service;

import com.rem.backend.orgaccountmanagement.entity.OrganizationAccount;
import com.rem.backend.dto.accounting.AccountCategoryDTO;
import com.rem.backend.dto.accounting.AccountGroupDTO;
import com.rem.backend.dto.accounting.AccountTypeDTO;
import com.rem.backend.dto.accounting.ChartOfAccountDTO;
import com.rem.backend.dto.accounting.CreateAccountCategoryRequest;
import com.rem.backend.dto.accounting.CreateAccountCategoryResponse;
import com.rem.backend.dto.accounting.CreateAccountGroupRequest;
import com.rem.backend.dto.accounting.CreateAccountGroupResponse;
import com.rem.backend.dto.accounting.CreateChartOfAccountRequest;
import com.rem.backend.accountingmanagement.entity.AccountCategory;
import com.rem.backend.accountingmanagement.entity.AccountGroup;
import com.rem.backend.accountingmanagement.entity.AccountType;
import com.rem.backend.accountingmanagement.entity.ChartOfAccount;
import com.rem.backend.entity.organization.Organization;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.repository.AccountCategoryRepository;
import com.rem.backend.repository.AccountGroupRepository;
import com.rem.backend.repository.AccountTypeRepository;
import com.rem.backend.repository.ChartOfAccountRepository;
import com.rem.backend.repository.OrganizationRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final AccountCategoryRepository categoryRepo;
    private final AccountTypeRepository typeRepo;
    private final OrganizationRepo organizationRepo;
    private final Utility utility;

    // ─── Helper: build AccountCategoryDTO from entity ───
    private AccountCategoryDTO toCategoryDTO(AccountCategory c) {
        return new AccountCategoryDTO(
                c.getId(),
                c.getName(),
                new AccountTypeDTO(
                        c.getAccountType().getId(),
                        c.getAccountType().getName()
                ),
                c.getCreatedDate().toString()
        );
    }

    private AccountGroupDTO toGroupDTO(AccountGroup g) {
        return new AccountGroupDTO(
                g.getId(),
                g.getName(),
                toCategoryDTO(g.getAccountCategory()),
                g.getCreatedDate().toString()
        );
    }

    // ═══════════════════════════════════════════════════
    //  CHART OF ACCOUNTS
    // ═══════════════════════════════════════════════════

    public Map<String, Object> getAllChartOfAccounts(
            Long organizationId,
            Long accountType,
            Long accountCategory,
            Long accountGroup
    ) {
        try {
            List<ChartOfAccount> accounts;

            if (accountGroup != null) {
                accounts = coaRepo
                        .findAllByOrganization_OrganizationIdAndAccountGroup_Id(
                                organizationId, accountGroup);
            } else if (accountCategory != null) {
                List<AccountGroup> groups =
                        groupRepo.findAllByOrganization_OrganizationIdAndAccountCategory_Id(
                                organizationId, accountCategory);
                accounts = coaRepo
                        .findAllByOrganization_OrganizationIdAndAccountGroupIn(
                                organizationId, groups);
            } else if (accountType != null) {
                List<AccountCategory> categories =
                        categoryRepo.findAllByAccountType_IdAndOrganization_OrganizationId(
                                accountType, organizationId);
                List<AccountGroup> groups = categories.stream()
                        .flatMap(cat -> groupRepo
                                .findAllByOrganization_OrganizationIdAndAccountCategory(
                                        organizationId, cat).stream())
                        .toList();
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
                            toGroupDTO(a.getAccountGroup()),
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

        } catch (Exception e) {
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
            AccountGroup group = groupRepo
                    .findById(request.getAccountGroupId())
                    .orElseThrow(() ->
                            new RuntimeException("Account group not found"));

            if (group.getOrganization().getOrganizationId() != organizationId) {
                throw new RuntimeException("Account group does not belong to organization");
            }

            if (!group.getAccountCategory().getAccountType().getName().equalsIgnoreCase("EXPENSE")) {
                throw new RuntimeException("COA can only be created under EXPENSE account type");
            }

            if (coaRepo.existsByNameAndOrganization_OrganizationId(
                    request.getName(), organizationId)) {
                throw new RuntimeException("Account code already exists");
            }

            ChartOfAccount coa = new ChartOfAccount();
            coa.setOrganization(group.getOrganization());
            coa.setAccountGroup(group);
            coa.setCode(utility.generateAccountCode(group.getOrganization().getOrganizationId(),
                    group.getAccountCategory().getAccountType().getName().substring(0, 3)));
            coa.setName(request.getName());
            coa.setStatus(AccountStatus.ACTIVE);
            coa.setSystemGenerated(false);
            coa.setOrganizationAccountId(null);

            ChartOfAccount saved = coaRepo.save(coa);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("code", saved.getCode());
            response.put("name", saved.getName());
            response.put("group", group.getName());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<String, Object> createOrganizationAccount(
            OrganizationAccount organizationAccount,
            String loggedInUser
    ) {
        try {
            AccountGroup group = groupRepo
                    .findByNameAndOrganization_OrganizationId("bank/cash", organizationAccount.getOrganizationId())
                    .orElseThrow(() ->
                            new RuntimeException("Account group not found"));

            ChartOfAccount coa = new ChartOfAccount();
            coa.setOrganization(group.getOrganization());
            coa.setAccountGroup(group);
            coa.setCode(utility.generateAccountCode(group.getOrganization().getOrganizationId(),
                    group.getAccountCategory().getAccountType().getName().substring(0, 3)));
            coa.setName(organizationAccount.getName());
            coa.setStatus(AccountStatus.ACTIVE);
            coa.setSystemGenerated(false);
            coa.setOrganizationAccountId(organizationAccount.getOrganizationId());

            ChartOfAccount saved = coaRepo.save(coa);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("code", saved.getCode());
            response.put("name", saved.getName());
            response.put("group", group.getName());

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

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
                    .getAccountCategory()
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
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getChartOfAccountById(long chartOfAccountId) {
        try {
            ChartOfAccount chartOfAccount = coaRepo
                    .findById(chartOfAccountId)
                    .orElseThrow(() ->
                            new RuntimeException("Chart Of Account Not Found!"));
            ChartOfAccountDTO chartOfAccountDTO = new ChartOfAccountDTO(
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
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    //  ACCOUNT TYPE
    // ═══════════════════════════════════════════════════

    public Map<String, Object> getAllAccountType() {
        try {
            List<AccountType> types = typeRepo.findAll();

            List<AccountTypeDTO> dtoList = types.stream()
                    .map(t -> new AccountTypeDTO(t.getId(), t.getName()))
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

    // ═══════════════════════════════════════════════════
    //  ACCOUNT CATEGORY  (new level)
    // ═══════════════════════════════════════════════════

    public Map<String, Object> getAccountCategories(
            Long accountTypeId, Long organizationId
    ) {
        try {
            List<AccountCategory> categories =
                    categoryRepo.findAllByAccountType_IdAndOrganization_OrganizationId(
                            accountTypeId, organizationId);

            List<AccountCategoryDTO> dtoList = categories.stream()
                    .map(this::toCategoryDTO)
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

    public Map<String, Object> getAccountCategoryById(Long accountCategoryId) {
        try {
            AccountCategory c = categoryRepo.findById(accountCategoryId)
                    .orElseThrow(() -> new RuntimeException("Account category not found"));

            return ResponseMapper.buildResponse(Responses.SUCCESS, toCategoryDTO(c));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> createAccountCategory(
            Long organizationId,
            CreateAccountCategoryRequest request,
            String loggedInUser
    ) {
        try {
            AccountType type = typeRepo.findById(request.getAccountTypeId())
                    .orElseThrow(() -> new RuntimeException("Account type not found"));

            if (categoryRepo.existsByNameAndOrganization_OrganizationId(request.getName(), organizationId)) {
                throw new RuntimeException("Account category already exists for this organization");
            }

            Organization organization = organizationRepo.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));

            AccountCategory category = new AccountCategory();
            category.setName(request.getName());
            category.setAccountType(type);
            category.setOrganization(organization);

            AccountCategory saved = categoryRepo.save(category);

            CreateAccountCategoryResponse dto = new CreateAccountCategoryResponse(
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

    @Transactional
    public Map<String, Object> updateAccountCategory(
            Long organizationId,
            Long categoryId,
            CreateAccountCategoryRequest request,
            String loggedInUser
    ) {
        try {
            AccountCategory category = categoryRepo.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Account category not found"));

            if (category.getOrganization().getOrganizationId() != organizationId) {
                throw new RuntimeException("Account category does not belong to this organization");
            }

            AccountType type = typeRepo.findById(request.getAccountTypeId())
                    .orElseThrow(() -> new RuntimeException("Account type not found"));

            if (categoryRepo.existsByNameAndOrganization_OrganizationIdAndIdNot(
                    request.getName(), organizationId, categoryId)) {
                throw new RuntimeException("Account category name already exists");
            }

            category.setName(request.getName());
            category.setAccountType(type);

            AccountCategory updated = categoryRepo.save(category);

            CreateAccountCategoryResponse dto = new CreateAccountCategoryResponse(
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

    // ═══════════════════════════════════════════════════
    //  ACCOUNT GROUP
    // ═══════════════════════════════════════════════════

    public Map<String, Object> getAccountGroups(
            Long accountCategoryId, Long organizationId, String loggedInUser
    ) {
        try {
            List<AccountGroup> groups =
                    groupRepo.findAllByAccountCategory_IdAndOrganization_OrganizationId(
                            accountCategoryId, organizationId);

            List<AccountGroupDTO> dtoList = groups.stream()
                    .filter(g -> !"Construction Expense".equalsIgnoreCase(g.getName()))
                    .map(this::toGroupDTO)
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

    public Map<String, Object> getAccountGroupById(Long accountGroupId) {
        try {
            AccountGroup g = groupRepo.findById(accountGroupId)
                    .orElseThrow(() -> new RuntimeException("Account group not found"));

            return ResponseMapper.buildResponse(Responses.SUCCESS, toGroupDTO(g));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> createAccountGroup(Long organizationId, CreateAccountGroupRequest request,
                                                  String loggedInUser) {
        try {
            AccountCategory category = categoryRepo.findById(request.getAccountCategoryId())
                    .orElseThrow(() -> new RuntimeException("Account category not found"));

            if (groupRepo.existsByNameAndOrganization_OrganizationId(request.getName(), organizationId)) {
                throw new RuntimeException("Account group already exists for this organization");
            }

            Optional<Organization> organization = organizationRepo.findById(organizationId);

            AccountGroup group = new AccountGroup();
            group.setName(request.getName());
            group.setOrganization(organization.get());
            group.setAccountCategory(category);

            AccountGroup saved = groupRepo.save(group);

            CreateAccountGroupResponse dto = new CreateAccountGroupResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getAccountCategory().getId(),
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
            AccountGroup group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Account group not found"));

            if (!(group.getOrganization().getOrganizationId() == (organizationId))) {
                throw new RuntimeException("Account group does not belong to this organization");
            }

            AccountCategory category = categoryRepo.findById(request.getAccountCategoryId())
                    .orElseThrow(() -> new RuntimeException("Account category not found"));

            if (groupRepo.existsByNameAndOrganization_OrganizationIdAndIdNot(
                    request.getName(), organizationId, groupId)) {
                throw new RuntimeException("Account group name already exists");
            }

            group.setName(request.getName());
            group.setAccountCategory(category);

            AccountGroup updated = groupRepo.save(group);

            CreateAccountGroupResponse dto = new CreateAccountGroupResponse(
                    updated.getId(),
                    updated.getName(),
                    updated.getAccountCategory().getId(),
                    updated.getOrganization().getOrganizationId(),
                    updated.getCreatedDate()
            );

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

}
