package com.rem.backend.service;

import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.entity.expense.ExpenseType;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.organization.OrganizationAccount;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.repository.*;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ExpenseService {

    private final ExpenseRepo expenseRepo;
    private final ExpenseTypeRepo expenseTypeRepo;
    private final OrganizationAccountService organizationAccountService;
    private final VendorAccountRepo vendorAccountRepo;
    private final ProjectRepo projectRepo;
    private final ExpenseDetailRepo expenseDetailRepo;
    private final OrganizationAccoutRepo organizationAccoutRepo;

    public Map<String, Object> getExpenseList(long id, long id2, String filteredBy, Pageable pageable) {

        try {

            Page<Expense> expenses = null;
            ValidationService.validate(id, filteredBy);

            switch (filteredBy) {
                case "vendor":
                    expenses = expenseRepo.findAllByVendorAccountId(id, pageable);
                    break;
                case "project":
                    expenses = expenseRepo.findAllByProjectId(id, pageable);
                    break;
                case "project_vendor":
                    expenses = expenseRepo.findAllByProjectIdAndVendorAccountId(id, id2, pageable);
                    break;
                default:
                    expenses = expenseRepo.findAllByOrganizationId(id, pageable);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenses);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> getExpenseDetails(long expenseId) {

        try {

            List<ExpenseDetail> expenseDetails = new ArrayList<>();
            ValidationService.validate(expenseId, "expenseId");


            Optional<Expense> expenseOptional = expenseRepo.findById(expenseId);

            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense");

            Expense expense = expenseOptional.get();
            expenseDetails = expenseDetailRepo.findByExpenseId(expenseId);

            Map<String , Object> data = new HashMap<>();
            data.put("expense" , expense);
            data.put("expenseDetail" , expenseDetails);


            return ResponseMapper.buildResponse(Responses.SUCCESS, data);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> addExpense(Expense expense, String loggedInUser) {

        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getExpenseTypeId(), "expense type");
            ValidationService.validate(expense.getAmountPaid(), "amount paid");
            ValidationService.validate(expense.getTotalAmount(), "total amount");
            ValidationService.validate(expense.getOrganizationId(), "organization id");
            ValidationService.validate(expense.getOrganizationAccountId(), "organization account id");
            ValidationService.validate(expense.getCreditAmount(), "credit amount");


            Optional<VendorAccount> accountOptional = vendorAccountRepo.findById(expense.getVendorAccountId());
            if (!accountOptional.isPresent())
                throw new IllegalArgumentException("Invalid Vendor");


            VendorAccount vendorAccount = accountOptional.get();
            vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + expense.getTotalAmount());
            vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + expense.getAmountPaid());
            vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + expense.getCreditAmount());
            vendorAccountRepo.save(vendorAccount);


            Optional<ExpenseType> expenseTypeOptional = expenseTypeRepo.findById(expense.getExpenseTypeId());
            if (!expenseTypeOptional.isPresent())
                throw new IllegalArgumentException("Invalid Expense Type");

            OrganizationAccountDetail organizationAccountDetail = new OrganizationAccountDetail();
            organizationAccountDetail.setProjectId(expense.getProjectId());
            organizationAccountDetail.setComments("Purchasing from " + accountOptional.get().getName() + " for " + expenseTypeOptional.get().getName());
            organizationAccountDetail.setAmount(expense.getAmountPaid());
            organizationAccountDetail.setOrganizationAcctId(expense.getOrganizationAccountId());
            OrganizationAccount organizationAccount = organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);
            expense.setOrgAccountTitle(organizationAccount.getName());

            if (expense.getProjectId() > 0l) {
                Optional<Project> projectOptional = projectRepo.findById(expense.getProjectId());
                if (!projectOptional.isPresent())
                    throw new IllegalArgumentException("Invalid Project!");


                Project project = projectOptional.get();
                expense.setProjectName(project.getName());
                project.setConstructionAmount(project.getConstructionAmount() + expense.getAmountPaid());
                project.setTotalAmount(project.getTotalAmount() + expense.getAmountPaid());
                project.setUpdatedBy(loggedInUser);
                projectRepo.save(project);
            }


            expense.setVendorName(accountOptional.get().getName());
            expense.setExpenseTitle(expenseTypeOptional.get().getName());
            expense.setUpdatedBy(loggedInUser);
            expense.setCreatedBy(loggedInUser);
            expense.setPaymentStatus(getPaymentStatus(expense));



            expense = expenseRepo.save(expense);

            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(expense.getAmountPaid());
            expenseDetail.setOrganizationAccountId(expense.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setExpenseTitle(expenseTypeOptional.get().getName());
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);


            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public PaymentStatus getPaymentStatus(Expense expense){

        if (expense.getAmountPaid() == expense.getTotalAmount())
            return PaymentStatus.PAID;

        if (expense.getAmountPaid() == 0)
            return PaymentStatus.UNPAID;

        if (expense.getCreditAmount() > 0)
            return PaymentStatus.PENDING;

        return PaymentStatus.PENDING;
    }

    public Map<String , Object> addExpenseDetail(ExpenseDetail expenseDetail , String loggedInUser){

        try{

            ValidationService.validate(expenseDetail.getExpenseId() ,"expense id");
            ValidationService.validate(expenseDetail.getAmountPaid() ,"amount");
            ValidationService.validate(expenseDetail.getOrganizationAccountId() ,"org account");
            ValidationService.validate(loggedInUser ,"loggedInUser");

            Optional<Expense> expenseOptional = expenseRepo.findById(expenseDetail.getExpenseId());
            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense!");

            Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(expenseDetail.getOrganizationAccountId());
            if (organizationAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Account!");

            Expense expense = expenseOptional.get();
            OrganizationAccount organizationAccount = organizationAccountOptional.get();

            if (expenseDetail.getAmountPaid() + expense.getAmountPaid() > expense.getTotalAmount())
                throw new IllegalArgumentException("Exceed to total Amount!");

            expense.setAmountPaid(expense.getAmountPaid() + expenseDetail.getAmountPaid()) ;
            expense.setCreditAmount(expense.getCreditAmount() - expenseDetail.getAmountPaid());
            expense.setPaymentStatus(getPaymentStatus(expense));
            expenseRepo.save(expense);

//          SETTLING ORG ACCOUNT
            OrganizationAccountDetail organizationAccountDetail = new OrganizationAccountDetail();
            organizationAccountDetail.setProjectId(expense.getProjectId());
            organizationAccountDetail.setComments("Paying Debt of" + expense.getVendorName() + " for " + expense.getExpenseTitle());
            organizationAccountDetail.setAmount(expenseDetail.getAmountPaid());
            organizationAccountDetail.setOrganizationAcctId(expenseDetail.getOrganizationAccountId());
            organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);




//          SETTLING VENDOR ACCOUNT
            Optional<VendorAccount> accountOptional = vendorAccountRepo.findById(expense.getVendorAccountId());
            if (!accountOptional.isPresent())
                throw new IllegalArgumentException("Invalid Vendor");

            VendorAccount vendorAccount = accountOptional.get();
            vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + expense.getTotalAmount());
            vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + expense.getAmountPaid());
            vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + expense.getCreditAmount());
            vendorAccountRepo.save(vendorAccount);


            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetail.setUpdatedBy(loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseDetailRepo.save(expenseDetail));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> addExpenseType(ExpenseType expense, String loggedInUser) {

        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getOrganizationId(), "organization");
            ValidationService.validate(expense.getName(), "expense type");

            boolean exists = expenseTypeRepo.existsByNameContainingIgnoreCaseAndOrganizationId(expense.getName(), expense.getOrganizationId());
            if (exists)
                throw new IllegalArgumentException("Expense type already exists");


            expense.setCreatedBy(loggedInUser);
            expense.setUpdatedBy(loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseTypeRepo.save(expense));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllExpenseType(long orgId) {

        try {
            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseTypeRepo.findAllByOrganizationId(orgId));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
