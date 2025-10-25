package com.rem.backend.service;

import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.entity.expense.ExpenseType;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.organization.OrganizationAccount;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.vendor.VendorAccount;
import com.rem.backend.entity.vendor.VendorPayment;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.TransactionType;
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
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final VendorAccountService vendorAccountService;
    private final VendorAccountDetailRepo vendorAccountDetailRepo;


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

            Map<String, Object> data = new HashMap<>();
            data.put("expense", expense);
            data.put("expenseDetail", expenseDetails);


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
            ValidationService.validate(expense.getExpenseType(), "expense type");
            ValidationService.validate(expense.getAmountPaid(), "amount paid");
            ValidationService.validate(expense.getTotalAmount(), "total amount");
            ValidationService.validate(expense.getOrganizationId(), "organization id");
            ValidationService.validate(expense.getOrganizationAccountId(), "organization account id");


            OrganizationAccountDetail organizationAccountDetail = new OrganizationAccountDetail();

            if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                ValidationService.validate(expense.getProjectId(), "Project");
                ValidationService.validate(expense.getVendorAccountId(), "Vendor");

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


                if (expense.getProjectId() > 0l) {
                    Optional<Project> projectOptional = projectRepo.findById(expense.getProjectId());
                    if (!projectOptional.isPresent())
                        throw new IllegalArgumentException("Invalid Project!");


                    Project project = projectOptional.get();
                    expense.setProjectName(project.getName());
                    project.setConstructionAmount(project.getConstructionAmount() + expense.getTotalAmount());
                    project.setTotalAmount(project.getTotalAmount() + expense.getTotalAmount());
                    project.setUpdatedBy(loggedInUser);
                    projectRepo.save(project);
                }
                expense.setVendorName(accountOptional.get().getName());
                expense.setExpenseTitle(expenseTypeOptional.get().getName());
                organizationAccountDetail.setProjectId(expense.getProjectId());

            } else {
                expense.setExpenseTitle("Miscellaneous Expense");

            }

            expense.setUpdatedBy(loggedInUser);
            expense.setCreatedBy(loggedInUser);
            expense.setPaymentStatus(getPaymentStatus(expense));
            expense = expenseRepo.save(expense);


            organizationAccountDetail.setExpenseId(expense.getId());
            organizationAccountDetail.setComments(expense.getComments());
            organizationAccountDetail.setAmount(expense.getAmountPaid());
            organizationAccountDetail.setOrganizationAcctId(expense.getOrganizationAccountId());
            OrganizationAccount organizationAccount = organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);
            expense.setOrgAccountTitle(organizationAccount.getName());


            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(expense.getId());
            expenseDetail.setAmountPaid(expense.getAmountPaid());
            expenseDetail.setOrganizationAccountId(expense.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(organizationAccount.getName());
            expenseDetail.setExpenseTitle(expense.getExpenseTitle());
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);


            if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {
                VendorPayment vendorPayment = new VendorPayment();
                vendorPayment.setAmountPaid(expense.getAmountPaid());
                vendorPayment.setOrganizationAccountId(expense.getOrganizationAccountId());
                vendorPayment.setCreditAmount(expense.getCreditAmount());
                vendorPayment.setProjectId(expense.getProjectId());
                vendorPayment.setProjectId(expense.getProjectId());
                if (expense.getCreditAmount() == 0) {
                    vendorPayment.setTransactionType(TransactionType.DEBIT);
                } else if (expense.getAmountPaid() == 0) {
                    vendorPayment.setTransactionType(TransactionType.CREDIT);
                } else {
                    vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
                }
                vendorPayment.setVendorAccountId(expense.getVendorAccountId());
                vendorPayment.setVendorAccountId(expense.getVendorAccountId());
                vendorPayment.setExpenseId(expense.getId());
                vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, expense);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> updateExpense(Expense newExpense, String loggedInUser) {

        try {
            ValidationService.validate(newExpense.getId(), "Expense");
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(newExpense.getExpenseType(), "newExpense type");
            ValidationService.validate(newExpense.getAmountPaid(), "amount paid");
            ValidationService.validate(newExpense.getTotalAmount(), "total amount");
            ValidationService.validate(newExpense.getOrganizationId(), "organization id");
            ValidationService.validate(newExpense.getOrganizationAccountId(), "organization account id");

            boolean isExpenseTypeChanges = false;

            Optional<Expense> expenseOptional = expenseRepo.findById(newExpense.getId());
            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense!");

            Expense oldExpense = expenseOptional.get();

            if (!newExpense.getExpenseType().equals(oldExpense.getExpenseType())) {
                isExpenseTypeChanges = true;
                if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.MISCELLANEOUS)) {

                    Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(oldExpense.getVendorAccountId());
                    if (vendorAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account!");

                    Optional<VendorPayment> vendorPaymentOptional = vendorAccountDetailRepo.findByExpenseId(oldExpense.getId());
                    if (vendorPaymentOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account Detail!");

                    VendorAccount vendorAccount = vendorAccountOptional.get();
                    VendorPayment vendorPayment = vendorPaymentOptional.get();

                    vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() - (vendorPayment.getAmountPaid() + vendorPayment.getCreditAmount()));
                    vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() - vendorPayment.getAmountPaid());
                    vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() - vendorPayment.getCreditAmount());
                    vendorAccount.setUpdatedBy(loggedInUser);
                    vendorAccountRepo.save(vendorAccount);

                    vendorAccountDetailRepo.delete(vendorPayment);

                    newExpense.setProjectId(0l);
                    newExpense.setProjectName("");
                    newExpense.setVendorAccountId(0l);
                    newExpense.setVendorName("");
                    newExpense.setExpenseTitle("Miscellaneous Expense");


                    Optional<Project> projectOptional = projectRepo.findById(oldExpense.getProjectId());
                    if (projectOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Project!");

                    Project oldProject = projectOptional.get();

                    oldProject.setTotalAmount(oldProject.getTotalAmount() - oldExpense.getTotalAmount());
                    oldProject.setConstructionAmount(oldProject.getConstructionAmount() - oldExpense.getTotalAmount());


                    expenseRepo.save(newExpense);
                    projectRepo.save(oldProject);


                }
                else if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                    Optional<Project> projectOptional = projectRepo.findById(newExpense.getProjectId());
                    if (projectOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Project!");


                    Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(newExpense.getVendorAccountId());
                    if (vendorAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account!");

                    Optional<ExpenseType> expenseTypeOptional = expenseTypeRepo.findById(newExpense.getExpenseTypeId());
                    if (!expenseTypeOptional.isPresent())
                        throw new IllegalArgumentException("Invalid Expense Type");

                    VendorAccount vendorAccount = vendorAccountOptional.get();


//                    TODO :: ADD NEW ROWS FOR VENDOR ACCOUNT AND DETAIL

                    vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + newExpense.getTotalAmount());
                    vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + newExpense.getAmountPaid());
                    vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + newExpense.getCreditAmount());
                    vendorAccountRepo.save(vendorAccount);


                    VendorPayment vendorPayment = new VendorPayment();
                    vendorPayment.setAmountPaid(newExpense.getAmountPaid());
                    vendorPayment.setOrganizationAccountId(newExpense.getOrganizationAccountId());
                    vendorPayment.setCreditAmount(newExpense.getCreditAmount());
                    vendorPayment.setProjectId(newExpense.getProjectId());
                    vendorPayment.setProjectId(newExpense.getProjectId());
                    if (newExpense.getCreditAmount() == 0) {
                        vendorPayment.setTransactionType(TransactionType.DEBIT);
                    } else if (newExpense.getAmountPaid() == 0) {
                        vendorPayment.setTransactionType(TransactionType.CREDIT);
                    } else {
                        vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
                    }
                    vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                    vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                    vendorPayment.setExpenseId(newExpense.getId());
                    vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);


                    newExpense.setVendorName(vendorAccount.getName());


                    Optional<Project> newProjectOptional = projectRepo.findById(newExpense.getProjectId());
                    if (newProjectOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Updated Project!");

                    Project newProject = newProjectOptional.get();



                    newExpense.setProjectName(newProject.getName());
                    newExpense.setExpenseTitle(expenseTypeOptional.get().getName());

                    double updatedConstructionAmount = newProject.getConstructionAmount() + (newExpense.getTotalAmount());
                    double updatedTotalAmount = newProject.getTotalAmount() + (newExpense.getTotalAmount());

                    newProject.setTotalAmount(updatedTotalAmount);
                    newProject.setConstructionAmount(updatedConstructionAmount);
                    newProject.setUpdatedBy(loggedInUser);

                    projectRepo.save(newProject);


                }
            }

            expenseDetailRepo.deleteByExpenseId(oldExpense.getId());

            if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)) {

                ValidationService.validate(newExpense.getProjectId(), "Project");
                ValidationService.validate(newExpense.getVendorAccountId(), "Vendor");

                if (newExpense.getOrganizationAccountId() != oldExpense.getOrganizationAccountId()) {

                    Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(oldExpense.getOrganizationAccountId());
                    if (organizationAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account!");


                    Optional<OrganizationAccountDetail> organizationAccountDetailOptional = organizationAccountDetailRepo.findByExpenseId(oldExpense.getId());
                    if (organizationAccountDetailOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account Detail!");

                    OrganizationAccount organizationAccount = organizationAccountOptional.get();
                    OrganizationAccountDetail organizationAccountDetail = organizationAccountDetailOptional.get();

                    organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() + organizationAccountDetail.getAmount());
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    organizationAccountDetailRepo.delete(organizationAccountDetail);

                    organizationAccountDetail = new OrganizationAccountDetail();

                    organizationAccountDetail.setExpenseId(newExpense.getId());
                    organizationAccountDetail.setComments(newExpense.getComments());
                    organizationAccountDetail.setAmount(newExpense.getAmountPaid());
                    organizationAccountDetail.setOrganizationAcctId(newExpense.getOrganizationAccountId());

//                    TODO :: ADD NEW ROWS IN ORG ACCOUNT DETAIL

                    organizationAccount = organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);

                    newExpense.setOrgAccountTitle(organizationAccount.getName());

                }
                else {


                    Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(newExpense.getOrganizationAccountId());
                    if (organizationAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account!");


                    Optional<OrganizationAccountDetail> organizationAccountDetailOptional = organizationAccountDetailRepo.findByExpenseId(newExpense.getId());
                    if (organizationAccountDetailOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Organization Account Detail!");

                    OrganizationAccount organizationAccount = organizationAccountOptional.get();
                    OrganizationAccountDetail organizationAccountDetail = organizationAccountDetailOptional.get();

                    double latestAmountOrg = organizationAccount.getTotalAmount() + (oldExpense.getAmountPaid() - newExpense.getAmountPaid());
                    organizationAccount.setTotalAmount(latestAmountOrg);
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    organizationAccountDetail.setAmount(newExpense.getAmountPaid());
                    organizationAccountDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(organizationAccountDetail);
                }

                if (isExpenseTypeChanges == false) {

                    Optional<Project> projectOptional = projectRepo.findById(oldExpense.getProjectId());
                    if (projectOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Project!");

                    Project oldProject = projectOptional.get();

                    Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(oldExpense.getVendorAccountId());
                    if (vendorAccountOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account!");

                    Optional<VendorPayment> vendorPaymentOptional = vendorAccountDetailRepo.findByExpenseId(oldExpense.getId());
                    if (vendorPaymentOptional.isEmpty())
                        throw new IllegalArgumentException("Invalid Vendor Account Detail!");


                    VendorAccount vendorAccount = vendorAccountOptional.get();
                    VendorPayment vendorPayment = vendorPaymentOptional.get();

                    if (newExpense.getVendorAccountId() != oldExpense.getVendorAccountId()) {

                        vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() - vendorPayment.getAmountPaid());
                        vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() - vendorPayment.getCreditAmount());
                        vendorAccount.setTotalAmount(vendorAccount.getTotalAmountPaid() + vendorAccount.getTotalCreditAmount());

                        vendorAccountRepo.save(vendorAccount);
                        vendorAccountDetailRepo.delete(vendorPayment);

//                    TODO :: ADD NEW ROWS FOR VENDOR ACCOUNT AND DETAIL


                        Optional<VendorAccount> accountOptional = vendorAccountRepo.findById(newExpense.getVendorAccountId());
                        if (!accountOptional.isPresent())
                            throw new IllegalArgumentException("Invalid Vendor");

                        vendorAccount = accountOptional.get();
                        vendorAccount.setTotalAmount(vendorAccount.getTotalAmount() + newExpense.getTotalAmount());
                        vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + newExpense.getAmountPaid());
                        vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() + newExpense.getCreditAmount());
                        vendorAccountRepo.save(vendorAccount);


                        vendorPayment = new VendorPayment();
                        vendorPayment.setAmountPaid(newExpense.getAmountPaid());
                        vendorPayment.setOrganizationAccountId(newExpense.getOrganizationAccountId());
                        vendorPayment.setCreditAmount(newExpense.getCreditAmount());
                        vendorPayment.setProjectId(newExpense.getProjectId());
                        vendorPayment.setProjectId(newExpense.getProjectId());
                        if (newExpense.getCreditAmount() == 0) {
                            vendorPayment.setTransactionType(TransactionType.DEBIT);
                        } else if (newExpense.getAmountPaid() == 0) {
                            vendorPayment.setTransactionType(TransactionType.CREDIT);
                        } else {
                            vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
                        }
                        vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                        vendorPayment.setVendorAccountId(newExpense.getVendorAccountId());
                        vendorPayment.setExpenseId(newExpense.getId());
                        vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);


                        newExpense.setVendorName(vendorAccount.getName());

                    }
                    else {

                        double totalPaidAmount = vendorAccount.getTotalAmountPaid() + (newExpense.getAmountPaid() - oldExpense.getAmountPaid());
                        double totalCreditAmount = vendorAccount.getTotalCreditAmount() + (newExpense.getCreditAmount() - oldExpense.getCreditAmount());
                        double totalAmount = vendorAccount.getTotalAmount() + (newExpense.getAmountPaid() - oldExpense.getAmountPaid()) + (newExpense.getCreditAmount() - oldExpense.getCreditAmount());

                        vendorAccount.setTotalAmount(totalAmount);
                        vendorAccount.setTotalAmountPaid(totalPaidAmount);
                        vendorAccount.setTotalCreditAmount(totalCreditAmount);

                        vendorAccountRepo.save(vendorAccount);

                        vendorPayment.setCreditAmount(newExpense.getCreditAmount());
                        vendorPayment.setAmountPaid(newExpense.getAmountPaid());
                        vendorPayment.setUpdatedBy(loggedInUser);

                        vendorAccountDetailRepo.save(vendorPayment);
                    }

                    if (newExpense.getProjectId() != oldExpense.getProjectId()) {

                        Optional<Project> newProjectOptional = projectRepo.findById(newExpense.getProjectId());
                        if (newProjectOptional.isEmpty())
                            throw new IllegalArgumentException("Invalid Updated Project!");

                        oldProject.setTotalAmount(oldProject.getTotalAmount() - oldExpense.getTotalAmount());
                        oldProject.setConstructionAmount(oldProject.getConstructionAmount() - oldExpense.getTotalAmount());

                        projectRepo.save(oldProject);


                        Project newProject = newProjectOptional.get();

                        double updatedConstructionAmount = newProject.getConstructionAmount() + (newExpense.getTotalAmount());
                        double updatedTotalAmount = newProject.getTotalAmount() + (newExpense.getTotalAmount());

                        newProject.setTotalAmount(updatedTotalAmount);
                        newProject.setConstructionAmount(updatedConstructionAmount);
                        newProject.setUpdatedBy(loggedInUser);

                        projectRepo.save(newProject);

                        newExpense.setProjectName(newProject.getName());


                    } else {

                        double updatedConstructionAmount = oldProject.getConstructionAmount() + (newExpense.getTotalAmount() - oldExpense.getTotalAmount());
                        double updatedTotalAmount = oldProject.getTotalAmount() + (newExpense.getTotalAmount() - oldExpense.getTotalAmount());

                        oldProject.setUpdatedBy(loggedInUser);
                        oldProject.setConstructionAmount(updatedConstructionAmount);
                        oldProject.setTotalAmount(updatedTotalAmount);

                        projectRepo.save(oldProject);
                    }
                }


            } // handling type change
            else if (newExpense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.MISCELLANEOUS)) {

                Optional<OrganizationAccount> organizationAccountOptional = organizationAccoutRepo.findById(newExpense.getOrganizationAccountId());
                if (organizationAccountOptional.isEmpty())
                    throw new IllegalArgumentException("Invalid Organization Account!");


                Optional<OrganizationAccountDetail> organizationAccountDetailOptional = organizationAccountDetailRepo.findByExpenseId(newExpense.getId());
                if (organizationAccountDetailOptional.isEmpty())
                    throw new IllegalArgumentException("Invalid Organization Account Detail!");

                OrganizationAccount organizationAccount = organizationAccountOptional.get();
                OrganizationAccountDetail organizationAccountDetail = organizationAccountDetailOptional.get();

                if (newExpense.getOrganizationAccountId() != oldExpense.getOrganizationAccountId()) {

                    organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() + organizationAccountDetail.getAmount());
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    organizationAccountDetailRepo.delete(organizationAccountDetail);

                    organizationAccountDetail = new OrganizationAccountDetail();

                    organizationAccountDetail.setExpenseId(newExpense.getId());
                    organizationAccountDetail.setComments(newExpense.getComments());
                    organizationAccountDetail.setAmount(newExpense.getAmountPaid());
                    organizationAccountDetail.setOrganizationAcctId(newExpense.getOrganizationAccountId());

//                    TODO :: ADD NEW ROWS IN ORG ACCOUNT DETAIL

                    organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);


                } else {

                    double latestAmountOrg = organizationAccount.getTotalAmount() + (oldExpense.getAmountPaid() - newExpense.getAmountPaid());
                    organizationAccount.setTotalAmount(latestAmountOrg);
                    organizationAccount.setUpdatedBy(loggedInUser);
                    organizationAccoutRepo.save(organizationAccount);

                    organizationAccountDetail.setAmount(newExpense.getAmountPaid());
                    organizationAccountDetail.setUpdatedBy(loggedInUser);
                    organizationAccountDetailRepo.save(organizationAccountDetail);
                }
            } // handling type change

            if (newExpense.getCreditAmount() == 0)
                newExpense.setPaymentStatus(PaymentStatus.PAID);

            newExpense = expenseRepo.save(newExpense);

            ExpenseDetail expenseDetail = new ExpenseDetail();
            expenseDetail.setExpenseId(newExpense.getId());
            expenseDetail.setAmountPaid(newExpense.getAmountPaid());
            expenseDetail.setOrganizationAccountId(newExpense.getOrganizationAccountId());
            expenseDetail.setOrganizationAccountTitle(newExpense.getOrgAccountTitle());
            expenseDetail.setExpenseTitle(newExpense.getExpenseTitle());
            expenseDetail.setUpdatedBy(loggedInUser);
            expenseDetail.setCreatedBy(loggedInUser);
            expenseDetailRepo.save(expenseDetail);


            return ResponseMapper.buildResponse(Responses.SUCCESS, newExpense);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> deleteExpense(long expenseId , String loggedInUser) {

      try{

          Optional<Expense> expenseOptional = expenseRepo.findById(expenseId);

          if (expenseOptional.isEmpty())
              throw new IllegalArgumentException("Invalid Expense");

          Expense expense = expenseOptional.get();

          ValidationService.validate(expense.getId(), "Expense");
          ValidationService.validate(loggedInUser, "logged in user");
          ValidationService.validate(expense.getExpenseType(), "newExpense type");
          ValidationService.validate(expense.getAmountPaid(), "amount paid");
          ValidationService.validate(expense.getTotalAmount(), "total amount");
          ValidationService.validate(expense.getOrganizationId(), "organization id");
          ValidationService.validate(expense.getOrganizationAccountId(), "organization account id");



          if (expense.getExpenseType().equals(com.rem.backend.enums.ExpenseType.CONSTRUCTION)){


              Optional<Project> projectOptional = projectRepo.findById(expense.getProjectId());
              if (projectOptional.isPresent()){
                  Project project = projectOptional.get();
                  project.setConstructionAmount(project.getConstructionAmount() - expense.getTotalAmount());
                  project.setTotalAmount(project.getTotalAmount() - expense.getTotalAmount());
                  project.setUpdatedBy(loggedInUser);
                  projectRepo.save(project);
              }

              Optional<OrganizationAccount> orgAccountOptional = organizationAccoutRepo.findById(expense.getOrganizationAccountId());
              if (orgAccountOptional.isPresent()){
                  OrganizationAccount account = orgAccountOptional.get();
                  account.setTotalAmount(account.getTotalAmount() + expense.getAmountPaid());
                  account.setUpdatedBy(loggedInUser);
                  organizationAccoutRepo.save(account);
                  organizationAccountDetailRepo.deleteByExpenseId(expense.getId());
              }

              Optional<VendorAccount> vendorAccountOptional = vendorAccountRepo.findById(expense.getVendorAccountId());
              if (vendorAccountOptional.isPresent()){
                  VendorAccount account = vendorAccountOptional.get();
                  account.setTotalAmount(account.getTotalAmount() - expense.getTotalAmount());
                  account.setTotalCreditAmount(account.getTotalCreditAmount() - expense.getCreditAmount());
                  account.setTotalAmountPaid(account.getTotalCreditAmount() - expense.getAmountPaid());
                  account.setUpdatedBy(loggedInUser);
                  vendorAccountRepo.save(account);
                  vendorAccountDetailRepo.deleteByExpenseId(expense.getId());
              }

          }else{

              Optional<OrganizationAccount> orgAccountOptional = organizationAccoutRepo.findById(expense.getOrganizationAccountId());
              if (orgAccountOptional.isPresent()){
                  OrganizationAccount account = orgAccountOptional.get();
                  account.setTotalAmount(account.getTotalAmount() + expense.getAmountPaid());
                  account.setUpdatedBy(loggedInUser);
                  organizationAccoutRepo.save(account);
                  organizationAccountDetailRepo.deleteByExpenseId(expense.getId());
              }
          }

          expenseDetailRepo.deleteByExpenseId(expense.getId());
          expenseRepo.delete(expense);


          return ResponseMapper.buildResponse(Responses.SUCCESS, "Successfully Deleted!");

      } catch (IllegalArgumentException e) {
          return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
      } catch (Exception e) {
          e.printStackTrace();
          return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
      }
    }

    public PaymentStatus getPaymentStatus(Expense expense) {

        if (expense.getAmountPaid() == expense.getTotalAmount())
            return PaymentStatus.PAID;

        if (expense.getAmountPaid() == 0)
            return PaymentStatus.UNPAID;

        if (expense.getCreditAmount() > 0)
            return PaymentStatus.PENDING;

        return PaymentStatus.PENDING;
    }


    // PAYING BACK TO VENDOR
    public Map<String, Object> addExpenseDetail(ExpenseDetail expenseDetail, String loggedInUser) {

        try {

            ValidationService.validate(expenseDetail.getExpenseId(), "expense id");
            ValidationService.validate(expenseDetail.getAmountPaid(), "amount");
            ValidationService.validate(expenseDetail.getOrganizationAccountId(), "org account");
            ValidationService.validate(loggedInUser, "loggedInUser");

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

            double lastCreditAmount = expense.getCreditAmount();
            expense.setAmountPaid(expense.getAmountPaid() + expenseDetail.getAmountPaid());
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
            vendorAccount.setTotalAmountPaid(vendorAccount.getTotalAmountPaid() + expenseDetail.getAmountPaid());
            vendorAccount.setTotalCreditAmount(vendorAccount.getTotalCreditAmount() - expenseDetail.getAmountPaid());
            vendorAccountRepo.save(vendorAccount);


            VendorPayment vendorPayment = new VendorPayment();
            vendorPayment.setAmountPaid(expenseDetail.getAmountPaid());
            vendorPayment.setOrganizationAccountId(expenseDetail.getOrganizationAccountId());
            vendorPayment.setCreditAmount(lastCreditAmount - expenseDetail.getAmountPaid());
            vendorPayment.setProjectId(expense.getProjectId());
            if (expense.getCreditAmount() - expenseDetail.getAmountPaid() > 0) {
                vendorPayment.setTransactionType(TransactionType.DEBIT_CREDIT);
            } else {
                vendorPayment.setTransactionType(TransactionType.DEBIT);
            }
            vendorPayment.setVendorAccountId(expense.getVendorAccountId());
            vendorAccountService.addPaymentHistory(vendorPayment, loggedInUser);


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


    public Map<String, Object> updateExpenseType(ExpenseType expense, String loggedInUser) {

        try {
            ValidationService.validate(loggedInUser, "logged in user");
            ValidationService.validate(expense.getOrganizationId(), "organization");
            ValidationService.validate(expense.getName(), "expense type");


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


    public Map<String, Object> getExpenseTypeById(long id) {

        try {

            ValidationService.validate(id, "expense");

            Optional<ExpenseType> expenseType = expenseTypeRepo.findById(id);

            if (expenseType.isEmpty())
                throw new IllegalArgumentException("Invalid Expense Type");

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseType.get());
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getExpenseById(long id) {

        try {

            ValidationService.validate(id, "expense");

            Optional<Expense> expenseOptional = expenseRepo.findById(id);

            if (expenseOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Expense");

            return ResponseMapper.buildResponse(Responses.SUCCESS, expenseOptional.get());
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }
}
