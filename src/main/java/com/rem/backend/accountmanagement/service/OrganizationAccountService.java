package com.rem.backend.accountmanagement.service;

import com.rem.backend.dto.analytic.DateRangeRequest;
import com.rem.backend.dto.analytic.OrganizationAccountDetailProjection;
import com.rem.backend.dto.orgAccount.TransferFundRequest;
import com.rem.backend.accountmanagement.entity.OrganizationAccount;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.repository.OrganizationAccountDetailRepo;
import com.rem.backend.repository.OrganizationAccoutRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.VendorAccountDetailRepo;
import com.rem.backend.service.AccountService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class OrganizationAccountService {

    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final ProjectRepo projectRepo;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;
    private final AccountService accountService;

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

    public Map<String, Object> getAccountDetailsByDateRangeAndByAccount(DateRangeRequest request, Pageable pageable) {
        Page<OrganizationAccountDetailProjection> response = null;
        try {
            ValidationService.validate(request.getOrganizationId(), "organization id");
            ValidationService.validate(request.getStartDate(), "start date");
            ValidationService.validate(request.getEndDate(), "end date");
            ValidationService.validate(request.getFilteredBy(), "filtered By");
            ValidationService.validate(request.getTransactionType(), "Transaction type");

            LocalDateTime startDate = Utility.getStartOfDay(request.getStartDate());
            LocalDateTime endDate = Utility.getEndOfDay(request.getEndDate());


            if (request.getTransactionType().equals(TransactionType.CREDIT)) {
                response = vendorAccountDetailRepo.findVendorPaymentsProjectionByOrganizationAndDateRangeWithoutPagination(
                        request.getOrganizationId(),
                        startDate,
                        endDate,
                        pageable);


            } else {
                if (request.getFilteredBy() == null || request.getFilteredBy().equals("all")) {
                    response = organizationAccountDetailRepo.findAllByOrganizationIdAndDateRange(
                            request.getOrganizationId(),
                            startDate,
                            endDate,
                            pageable);

                } else {

                    ValidationService.validate(request.getFilteredId(), "Account");
                    response = organizationAccountDetailRepo.
                            findAllByOrgAndAccountAndDateRange(
                                    request.getOrganizationId(),
                                    request.getFilteredId(),
                                    startDate,
                                    endDate,
                                    pageable
                            );
                }
            }



            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAccountDetailsByDateRangeAndByAccountWithoutPagination(DateRangeRequest request) {
        List<OrganizationAccountDetailProjection> response = null;
        try {
            ValidationService.validate(request.getOrganizationId(), "organization id");
            ValidationService.validate(request.getStartDate(), "start date");
            ValidationService.validate(request.getEndDate(), "end date");
            ValidationService.validate(request.getFilteredBy(), "filtered By");

            LocalDateTime startDate = Utility.getStartOfDay(request.getStartDate());
            LocalDateTime endDate = Utility.getEndOfDay(request.getEndDate());


            if (request.getTransactionType().equals(TransactionType.CREDIT)) {
                response = vendorAccountDetailRepo.findVendorPaymentsProjectionByOrganizationAndDateRangeWithoutPagination(
                        request.getOrganizationId(),
                        startDate,
                        endDate);


            } else {
                if (request.getFilteredBy() == null || request.getFilteredBy().equals("all")) {
                    response = organizationAccountDetailRepo.findAllByOrganizationIdAndDateRangeWithoutPagination(
                            request.getOrganizationId(),
                            startDate,
                            endDate
                    );

                } else {

                    ValidationService.validate(request.getFilteredId(), "Account");
                    response = organizationAccountDetailRepo.
                            findAllByOrgAndAccountAndDateRangeWithoutPagination(
                                    request.getOrganizationId(),
                                    request.getFilteredId(),
                                    startDate,
                                    endDate
                            );
                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
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


    public Map<String, Object> getOrgAccountDetailByOrgAcctId(long orgAcctId, Pageable pageable) {
        try {
            ValidationService.validate(orgAcctId, "orgId");
            Page<OrganizationAccountDetail> accountDetails = organizationAccountDetailRepo.findByOrganizationAcctId(orgAcctId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, accountDetails);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getOrgAccountDetailByOrgAcctIdWithoutPagination(long orgAcctId) {
        Map<String, Object> response = new HashMap<>();

        try {
            ValidationService.validate(orgAcctId, "orgId");
            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo.findById(orgAcctId);

            if (organizationAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Account");

            OrganizationAccount account = organizationAccountOptional.get();


            List<OrganizationAccountDetail> accountDetails = organizationAccountDetailRepo.findByOrganizationAcctIdOrderByIdDesc(orgAcctId);

            response.put("account", account);
            response.put("list", accountDetails);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addAccountByOrg(OrganizationAccount organizationAccount, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(organizationAccount.getAccountNo(), "orgId");
            ValidationService.validate(organizationAccount.getOrganizationId(), "orgId");
            ValidationService.validate(organizationAccount.getBankName(), "orgId");
            organizationAccount.setCreatedBy(loggedInUser);
            organizationAccount.setUpdatedBy(loggedInUser);
            OrganizationAccount saved = organizationAccountRepo.save(organizationAccount);

            accountService.createOrganizationAccount(saved,loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, saved);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> addOrgAcctDetail(OrganizationAccountDetail organizationAccountDetail, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(organizationAccountDetail.getOrganizationAcctId(), "organization account");
            ValidationService.validate(organizationAccountDetail.getCustomerAccountId(), "customer account");
            ValidationService.validate(organizationAccountDetail.getCustomerPaymentDetailId(), "customer payment");
            ValidationService.validate(organizationAccountDetail.getAmount(), "orgId");

            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo.findById(organizationAccountDetail.getOrganizationAcctId());
            if (!organizationAccountOptional.isPresent())
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid Account");


            OrganizationAccount organizationAccount = organizationAccountOptional.get();
            organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() + organizationAccountDetail.getAmount());

            organizationAccountDetail.setCreatedBy(loggedInUser);
            organizationAccountDetail.setUpdatedBy(loggedInUser);
            organizationAccountRepo.save(organizationAccount);
            return ResponseMapper.buildResponse(Responses.SUCCESS, organizationAccountDetailRepo.save(organizationAccountDetail));

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> transferFund(TransferFundRequest transferFundRequest, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(transferFundRequest.getFromAccountId(), "From Account");
            ValidationService.validate(transferFundRequest.getToAccountId(), "To Account");
            ValidationService.validate(transferFundRequest.getAmount(), "Amount");


            Optional<OrganizationAccount> fromAcccountOpt = organizationAccountRepo.findById(transferFundRequest.getFromAccountId());
            if (fromAcccountOpt.isEmpty())
                throw new IllegalArgumentException("Invalid From Account");


            Optional<OrganizationAccount> toAcccountOpt = organizationAccountRepo.findById(transferFundRequest.getToAccountId());
            if (toAcccountOpt.isEmpty())
                throw new IllegalArgumentException("To From Account");

            OrganizationAccount fromAccount = fromAcccountOpt.get();
            OrganizationAccount toAccount = toAcccountOpt.get();



            fromAccount.setTotalAmount(fromAccount.getTotalAmount() - transferFundRequest.getAmount());
            toAccount.setTotalAmount(toAccount.getTotalAmount() + transferFundRequest.getAmount());

            OrganizationAccountDetail fromAccountDetail = new OrganizationAccountDetail();
            fromAccountDetail.setOrganizationAcctId(fromAccount.getId());
            fromAccountDetail.setAmount(transferFundRequest.getAmount());
            fromAccountDetail.setComments("Internal fund transfer from account " + "\"" + fromAccount.getName() + "\"" + " to account " + "\"" + toAccount.getName() + "\"");
            fromAccountDetail.setTransactionType(TransactionType.CREDIT);
            fromAccountDetail.setCreatedBy(loggedInUser);
            fromAccountDetail.setUpdatedBy(loggedInUser);
            organizationAccountDetailRepo.save(fromAccountDetail);


            OrganizationAccountDetail toAccountDetail = new OrganizationAccountDetail();
            toAccountDetail.setOrganizationAcctId(toAccount.getId());
            toAccountDetail.setAmount(transferFundRequest.getAmount());
            toAccountDetail.setComments("Internal fund transfer from account " + "\"" + fromAccount.getName() + "\"" + " to account " + "\"" + toAccount.getName() + "\"");
            toAccountDetail.setTransactionType(TransactionType.DEBIT);
            toAccountDetail.setCreatedBy(loggedInUser);
            toAccountDetail.setUpdatedBy(loggedInUser);
            organizationAccountDetailRepo.save(toAccountDetail);

            organizationAccountRepo.save(fromAccount);
            organizationAccountRepo.save(toAccount);
            return ResponseMapper.buildResponse(Responses.SUCCESS, "Successfully updated!");

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public OrganizationAccount deductFromOrgAcct(OrganizationAccountDetail organizationAccountDetail, String loggedInUser) {
        try {
            ValidationService.validate(loggedInUser, "loggedInUser");
            ValidationService.validate(organizationAccountDetail.getOrganizationAcctId(), "organization account");
            ValidationService.validate(organizationAccountDetail.getAmount(), "orgId");

            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(organizationAccountDetail.getProjectId());

            if (projectOptional.isPresent())
                organizationAccountDetail.setProjectName(projectOptional.get().getName());


            organizationAccountDetail.setCreatedBy(loggedInUser);
            organizationAccountDetail.setUpdatedBy(loggedInUser);
            organizationAccountDetail.setTransactionType(TransactionType.CREDIT);
            Optional<OrganizationAccount> organizationAccountOptional = organizationAccountRepo.findById(organizationAccountDetail.getOrganizationAcctId());

            if (!organizationAccountOptional.isPresent())
                throw new IllegalArgumentException("Invalid Account");


            OrganizationAccount organizationAccount = organizationAccountOptional.get();
            double remainingAmount = organizationAccount.getTotalAmount() - organizationAccountDetail.getAmount();

            if (remainingAmount < 0)
                throw new IllegalArgumentException("Not Enough Funds for this account " + organizationAccount.getAccountNo());


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



    @Transactional
    public Map<String , Object> addOrDeductBalance(OrganizationAccountDetail request, long organizationId , String loggedInUser) {


        try{
            // ✅ 1. Validate mandatory fields
            if (request.getOrganizationAcctId() <= 0) {
                throw new IllegalArgumentException("Organization account id is required");
            }

            if (request.getTransactionType() == null) {
                throw new IllegalArgumentException("Transaction type (CREDIT/DEBIT) is required");
            }

            if (request.getAmount() <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }

            if (request.getComments() == null || request.getComments().trim().isEmpty()) {
                throw new IllegalArgumentException("Comments are required");
            }

            if (request.getTransactionCategory() == null ) {
                throw new IllegalArgumentException("Comments are required");
            }



            // ✅ 2. Fetch organization account
            OrganizationAccount account = organizationAccountRepo
                    .findByIdAndOrganizationId(request.getOrganizationAcctId(), organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization account not found"));


            if (request.getProjectId() != 0){
               Project project =  projectRepo.findById(request.getProjectId())
                       .orElseThrow(() -> new IllegalArgumentException("Invalid Project Selected"));

               request.setProjectName(project.getName());
            }

            double currentBalance = account.getTotalAmount();
            double amount = request.getAmount();
            double newBalance;

            // ✅ 3. Apply CREDIT / DEBIT logic
            if (request.getTransactionType() == TransactionType.DEBIT) {
                newBalance = currentBalance + amount;
            }
            else if (request.getTransactionType() == TransactionType.CREDIT) {
                if (currentBalance < amount) {
                    throw new IllegalArgumentException("Insufficient balance");
                }
                newBalance = currentBalance - amount;
            }
            else {
                throw new IllegalArgumentException("Invalid transaction type");
            }

            // ✅ 4. Update account balance
            account.setTotalAmount(newBalance);
            account.setUpdatedBy(loggedInUser);
            organizationAccountRepo.save(account);

            // ✅ 5. Save account detail entry
            request.setUpdatedBy(loggedInUser);
            request.setCreatedBy(loggedInUser);
            OrganizationAccountDetail savedDetail = organizationAccountDetailRepo.save(request);

            return ResponseMapper.buildResponse(Responses.SUCCESS ,savedDetail);
        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }catch (Exception e){
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE , e.getMessage());
        }

    }
}
