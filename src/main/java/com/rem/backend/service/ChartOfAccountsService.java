package com.rem.backend.service;

import com.rem.backend.entity.account.AccountGroup;
import com.rem.backend.entity.account.ChartOfAccount;
import com.rem.backend.entity.organization.OrganizationAccount;
import com.rem.backend.enums.AccountStatus;
import com.rem.backend.repository.AccountGroupRepository;
import com.rem.backend.repository.AccountTypeRepository;
import com.rem.backend.repository.ChartOfAccountRepository;
import com.rem.backend.utility.Utility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChartOfAccountsService {

    private final AccountGroupRepository accountGroupRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final Utility utility;


    @Transactional
    public ChartOfAccount createChartOfAccount(OrganizationAccount organizationAccount) {

        // 1. Check if COA already exists for this organization account
        Optional<ChartOfAccount> existingCoa =
                chartOfAccountRepository.findByOrganizationAccountIdAndStatus(
                        organizationAccount.getId(),
                        AccountStatus.ACTIVE
                );

        if (existingCoa.isPresent()) {
            return existingCoa.get();
        }

        // 2. Fetch Bank / Cash account group
        AccountGroup bankCashGroup = accountGroupRepository
                .findByNameAndOrganization_OrganizationId(
                        "Bank/Cash",
                        organizationAccount.getOrganizationId()
                )
                .orElseThrow(() ->
                        new IllegalStateException("Bank/Cash account group not configured")
                );

        // 3. Create COA
        ChartOfAccount coa = new ChartOfAccount();
        coa.setOrganizationId(organizationAccount.getOrganizationId());
        coa.setAccountGroup(bankCashGroup);

        coa.setOrganizationAccountId(organizationAccount.getId());
        coa.setSystemGenerated(true);
        coa.setStatus(AccountStatus.ACTIVE);

        // 4. Naming & code
        coa.setName(
                organizationAccount.getBankName() +
                        " - " +
                        organizationAccount.getName()
        );

        coa.setCode(utility.generateAccountCode(
                organizationAccount.getOrganizationId(),
                "BANK"
        ));

        return chartOfAccountRepository.save(coa);
    }

}
