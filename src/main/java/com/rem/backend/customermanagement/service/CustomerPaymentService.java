package com.rem.backend.customermanagement.service;

import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.organizationmanagement.enums.TransactionCategory;
import com.rem.backend.organizationmanagement.service.OrganizationAccountService;
import com.rem.backend.organizationmanagement.entity.OrganizationAccount;
import com.rem.backend.customermanagement.entity.CustomerAccount;
import com.rem.backend.customermanagement.entity.CustomerPayment;
import com.rem.backend.customermanagement.entity.CustomerPaymentDetail;
import com.rem.backend.organizationmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.organizationmanagement.repository.OrganizationAccoutRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationAccountDetailRepo;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.bookingmanagement.repository.BookingRepository;
import com.rem.backend.customermanagement.repository.CustomerAccountRepo;
import com.rem.backend.customermanagement.repository.CustomerPaymentDetailRepo;
import com.rem.backend.customermanagement.repository.CustomerPaymentRepo;
import com.rem.backend.customermanagement.repository.CustomerRepo;
import com.rem.backend.projectmanagement.repository.UnitRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;

import static com.rem.backend.enums.PaymentStatus.PAID;

@Service
@AllArgsConstructor
public class CustomerPaymentService {
    private final CustomerPaymentRepo customerPaymentRepo;
    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerRepo customerRepo;
    private final UnitRepo unitRepo;
    private final OrganizationAccountService organizationAccountService;
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final BookingRepository bookingRepository;
    private final JournalEntryService journalEntryService;

    public Map<String, Object> getPaymentsByCustomerAccountId(long customerAccountId, Pageable pageable) {
        try {
            ValidationService.validate(customerAccountId, "customerAccountId");
            Page<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountId(customerAccountId, pageable);
            populateOrganizationAccountDetails(payments.getContent());
            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getPaymentDetailsByPaymentId(long customerPaymentId) {
        try {
            ValidationService.validate(customerPaymentId, "customerPaymentId");

            Optional<CustomerPayment> customerPaymentOptional = customerPaymentRepo.findById(customerPaymentId);
            if (customerPaymentOptional.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Customer payment not found");
            }

            CustomerPayment customerPayment = customerPaymentOptional.get();
            List<CustomerPaymentDetail> paymentDetails = customerPaymentDetailRepo.findByCustomerPaymentId(customerPaymentId);

            Map<String, Object> response = new HashMap<>();
            response.put("payment", customerPayment);
            response.put("paymentDetails", paymentDetails);
            response.put("organizationAccountDetails",
                    getOrganizationAccountDetailsForPayment(customerPaymentId, paymentDetails));


            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getAllPaymentDetailsByAccountId(long customerAccountId) {
        try {
            ValidationService.validate(customerAccountId, "customerAccountId");
            List<CustomerPaymentDetail> customerPaymentDetails = new ArrayList<>();

            List<CustomerPayment> customerPayments = customerPaymentRepo
                    .findByCustomerAccountId(customerAccountId);

            Map<String, Object> response = new HashMap<>();


            for (CustomerPayment customerPayment : customerPayments) {
                if (!customerPayment.getPaymentStatus().equals(PaymentStatus.UNPAID)) {
                    List<CustomerPaymentDetail> paymentDetails = customerPaymentDetailRepo.findByCustomerPaymentId(customerPayment.getId());
                    customerPaymentDetails.addAll(paymentDetails);
                }

            }

            Optional<CustomerAccount> customerAccount = customerAccountRepo.findById(customerAccountId);
            if (customerAccount.isPresent()) {

                Map<String, Object> customerDetail = customerRepo.getAllDetailsByCustomerId(customerAccount.get().getCustomer().getCustomerId()
                        , customerAccount.get().getUnit().getId());

                double grandTotal = customerPaymentDetails.stream()
                        .mapToDouble(CustomerPaymentDetail::getAmount)
                        .sum();
                double totalAmount = customerAccount.get().getTotalAmount();
                double balanceAmount = totalAmount - grandTotal;

                response.put("totalAmount", totalAmount);
                response.put("grandTotal", grandTotal);
                response.put("balanceAmount", balanceAmount);
                response.put("customer", customerDetail);
            }

            response.put("paymentDetails", customerPaymentDetails);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getPaymentDetailsByPaymentIdOnlyData(long customerPaymentId, CustomerPayment customerPayment) {
        try {
            ValidationService.validate(customerPaymentId, "customerPaymentId");

            if (customerPayment == null) {
                return null;
            }

            List<CustomerPaymentDetail> paymentDetails = customerPaymentDetailRepo.findByCustomerPaymentId(customerPaymentId);

            Map<String, Object> response = new HashMap<>();
            response.put("payment", customerPayment);
            response.put("paymentDetails", paymentDetails);

            return response;

        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Transactional
    public Map<String, Object> updateCustomerPayment(CustomerPayment customerPayment, String loggedInUser) {
        try {
            ValidationService.validate(customerPayment.getId(), "customerPayment");
            ValidationService.validate(customerPayment.getCustomerAccountId(), "customer account");
            ValidationService.validate(customerPayment.getOrganizationAccountDetails(), "receiving account");
            Optional<CustomerAccount> customerAccountOp = customerAccountRepo.findById(customerPayment.getCustomerAccountId());

            if (customerAccountOp.isEmpty())
                throw new IllegalArgumentException("Invalid Account!");

            CustomerAccount customerAccount = customerAccountOp.get();
            Optional<com.rem.backend.bookingmanagement.entity.Booking> bookingOptional = bookingRepository
                    .findByCustomerIdAndUnitIdAndIsActiveTrue(customerAccount.getCustomer().getCustomerId(), customerAccount.getUnit().getId());
            if (bookingOptional.isPresent() && bookingOptional.get().isBookingComplete()) {
                throw new IllegalArgumentException("Booking is marked complete; no further payments are allowed.");
            }

            if (customerPayment.getCustomerPaymentDetails() == null ||
                    customerPayment.getCustomerPaymentDetails().size() == 0)
                throw new IllegalArgumentException("Invalid Payment");


            Double totalReceivedAmount = customerAccount.getTotalBalanceAmount();

            double currentPaidAmount = customerPayment.getCustomerPaymentDetails().stream().mapToDouble(p -> p.getAmount()).sum();


            if (totalReceivedAmount < currentPaidAmount)
                throw new IllegalArgumentException("Invalid Amount!");

            customerPayment.setSerialNo(0);

            double receivingAccountAmount = customerPayment.getOrganizationAccountDetails().stream().
                    mapToDouble(OrganizationAccountDetail::getAmount).sum();

            if (customerPayment.getCustomerPaymentDetails().size() > 0)
                customerPayment.setPaymentType(PaymentType.CUSTOM);

            if (customerPayment.getCustomerPaymentDetails().size() == 1)
                customerPayment.setPaymentType(customerPayment.getCustomerPaymentDetails().get(0).getPaymentType());

            customerPayment.setPaymentStatus(PAID);
            customerPayment.setCustomerAccountId(customerPayment.getCustomerAccountId());


            if (receivingAccountAmount > 0)
                customerPayment.setPaymentAddedToAccount(true);

            customerPayment.setAmount(currentPaidAmount);
            customerPayment.setAmount(currentPaidAmount);
            customerPayment.setCreatedBy(loggedInUser);
            customerPayment.setUpdatedBy(loggedInUser);
            customerPayment = customerPaymentRepo.save(customerPayment);


            if (customerPayment.getCustomerPaymentDetails().size() > 0) {


                for (CustomerPaymentDetail customerPaymentDetail : customerPayment.getCustomerPaymentDetails()) {

                    ValidationService.validate(customerPaymentDetail.getPaymentType(), "paymentType");
                    ValidationService.validate(customerPaymentDetail.getAmount(), "detail amount");
                    ValidationService.validate(customerPaymentDetail.getCustomerPaymentReason(), "payment reason");

                    if (customerPaymentDetail.getPaymentType().equals(PaymentType.CHEQUE)) {
                        ValidationService.validate(customerPaymentDetail.getChequeNo(), "cheque no");
                        ValidationService.validate(customerPaymentDetail.getChequeDate(), "cheque date");
                    }
                    customerPaymentDetail.setCustomerPaymentId(customerPayment.getId());
                    customerPaymentDetail.setCreatedBy(loggedInUser);
                    customerPaymentDetail.setUpdatedBy(loggedInUser);
                    customerPaymentDetailRepo.save(customerPaymentDetail);

                }

            }


            Optional<CustomerAccount> customerAccountOptional = customerAccountRepo.findById(customerPayment.getCustomerAccountId());

            if (customerAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Customer Account");

            Map<String, Object> customerData = customerRepo.getAllDetailsByCustomerId(
                    customerAccountOptional.get().getCustomer().getCustomerId(),
                    customerAccountOptional.get().getUnit().getId()

            );


            String customerName = "", unitSerial = "", projectName = "";
            long projectId = 0l;
            long bookingId = 0l;
            if (customerData != null) {
                customerName = customerData.get("customerName").toString();
                projectName = customerData.get("projectName").toString();
                unitSerial = customerData.get("unitSerial").toString();
                projectId = Long.valueOf(customerData.get("projectId").toString());
                bookingId = Long.valueOf(customerData.get("bookingId").toString());

            }


            if (receivingAccountAmount > 0) {
                if (receivingAccountAmount != currentPaidAmount)
                    throw new IllegalArgumentException("Receiving Amount is not matched!");

                for (OrganizationAccountDetail organizationAccountDetail : customerPayment.getOrganizationAccountDetails()) {
                    organizationAccountDetail.setCustomerName(customerName);
                    organizationAccountDetail.setProjectName(projectName);
                    organizationAccountDetail.setUnitSerialNo(unitSerial);
                    organizationAccountDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
                    organizationAccountDetail.setProjectId(projectId);
                    organizationAccountDetail.setCustomerId(customerAccountOptional.get().getCustomer().getCustomerId());
                    organizationAccountDetail.setCustomerAccountId(customerAccountOptional.get().getId());
                    organizationAccountDetail.setCustomerPaymentId(customerPayment.getId());
                    organizationAccountDetail.setComments("Paid By " + customerName + " for Unit # " + unitSerial + " of " + projectName);
                    organizationAccountService.addOrgAcctDetail(organizationAccountDetail, loggedInUser);

                }
            }


            customerAccount.setTotalPaidAmount(customerAccount.getTotalPaidAmount() + currentPaidAmount);
            customerAccount.setTotalBalanceAmount(customerAccount.getTotalBalanceAmount() - currentPaidAmount);

            journalEntryService.createJournalEntryForCustomerPayment(
                    customerAccount,
                    customerPayment,
                    customerPayment.getOrganizationAccountDetails(),
                    customerAccountOptional.get().getUnit().getId(),
                    bookingId,
                    loggedInUser

            );
            customerAccountRepo.save(customerAccount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, customerAccountOp);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> addCustomerPaymentToOrgAccount(CustomerPayment customerPaymentRequest, String loggedInUser) {
        try {
            ValidationService.validate(customerPaymentRequest.getId(), "customer payment");
            ValidationService.validate(customerPaymentRequest.getOrganizationAccountDetails(), "receiving account");
            Optional<CustomerPayment> customerPaymentOptional = customerPaymentRepo.findById(customerPaymentRequest.getId());

            if (customerPaymentOptional.isEmpty())
                throw new IllegalArgumentException("Invalid customer payment");

            CustomerPayment customerPayment = customerPaymentOptional.get();

            if (customerPayment.isPaymentAddedToAccount())
                throw new IllegalArgumentException("Already posted to accounts");


            double receivingAccountAmount = customerPaymentRequest.getOrganizationAccountDetails().stream().
                    mapToDouble(OrganizationAccountDetail::getAmount).sum();

            if (receivingAccountAmount > 0)
                customerPayment.setPaymentAddedToAccount(true);

            customerPayment.setUpdatedBy(loggedInUser);


            Optional<CustomerAccount> customerAccountOptional =
                    customerAccountRepo.findById(customerPayment.getCustomerAccountId());

            if (customerAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Customer Account");

            Map<String, Object> customerData = customerRepo.getAllDetailsByCustomerId(
                    customerAccountOptional.get().getCustomer().getCustomerId(),
                    customerAccountOptional.get().getUnit().getId()

            );


            String customerName = "", unitSerial = "", projectName = "";
            long projectId = 0l;
            long bookingId = 0l;
            if (customerData != null) {
                customerName = customerData.get("customerName").toString();
                projectName = customerData.get("projectName").toString();
                unitSerial = customerData.get("unitSerial").toString();
                projectId = Long.valueOf(customerData.get("projectId").toString());
                bookingId = Long.valueOf(customerData.get("bookingId").toString());

            }


            if (receivingAccountAmount > 0) {
                if (receivingAccountAmount != customerPayment.getAmount())
                    throw new IllegalArgumentException("Amount is not matched!");

                for (OrganizationAccountDetail organizationAccountDetail : customerPaymentRequest.getOrganizationAccountDetails()) {
                    organizationAccountDetail.setCustomerName(customerName);
                    organizationAccountDetail.setProjectName(projectName);
                    organizationAccountDetail.setUnitSerialNo(unitSerial);
                    organizationAccountDetail.setProjectId(projectId);
                    organizationAccountDetail.setCustomerPaymentId(customerPaymentRequest.getId());
                    organizationAccountDetail.setCustomerId(customerAccountOptional.get().getCustomer().getCustomerId());
                    organizationAccountDetail.setCustomerAccountId(customerAccountOptional.get().getId());
                    organizationAccountDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
                    organizationAccountDetail.setComments("Paid By " + customerName + " for Unit # " + unitSerial + " of " + projectName);
                    organizationAccountService.addOrgAcctDetail(organizationAccountDetail, loggedInUser);

                }
            }

            journalEntryService.createJournalEntryForPaymentPosting(
                    customerAccountOptional.get(),
                    customerPayment,
                    customerPaymentRequest.getOrganizationAccountDetails(),
                    customerAccountOptional.get().getUnit().getId(),
                    bookingId,
                    loggedInUser

            );
            customerPaymentRepo.save(customerPayment);
            return ResponseMapper.buildResponse(Responses.SUCCESS, customerAccountOptional);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


//    @Transactional
//    public Map<String, Object> updateCustomerPayment(CustomerPayment customerPayment, String loggedInUser) {
//        try {
//            ValidationService.validate(customerPayment.getId(), "customerPayment");
//            ValidationService.validate(customerPayment.getCustomerAccountId(), "customer account");
//            ValidationService.validate(customerPayment.getOrganizationAccountDetails(), "receiving account");
//            Optional<CustomerAccount> payments = customerAccountRepo.findById(customerPayment.getId());
//
//            if (payments.isEmpty())
//                throw new IllegalArgumentException("Invalid Payment!");
//
//            if (payments.get().getPaymentStatus().equals(PAID))
//                throw new IllegalArgumentException("Payment already paid!");
//
//            CustomerPayment payment = payments.get();
//            double totalAmount = payment.getRemainingAmount();
//
/// /
//            double customerPaidAmount = 0;
//
//            PaymentType paymentType = customerPayment.getPaymentType();
//
//
//            List<CustomerPaymentDetail> customerPaymentDetails = customerPaymentDetailRepo.findByCustomerPaymentId(customerPayment.getId());
//
//            double sumOfPaidAmount = customerPaymentDetails.stream()
//                    .mapToDouble(CustomerPaymentDetail::getAmount)
//                    .sum();
//
//            if (customerPayment.getCustomerPaymentDetails().size() > 0) {
//
//
//                for (CustomerPaymentDetail customerPaymentDetail : customerPayment.getCustomerPaymentDetails()) {
//
//                    ValidationService.validate(customerPaymentDetail.getPaymentType(), "paymentType");
//                    ValidationService.validate(customerPaymentDetail.getAmount(), "detail amount");
//                    ValidationService.validate(customerPaymentDetail.getCustomerPaymentReason(), "payment reason");
//
//                    if (customerPaymentDetail.getPaymentType().equals(PaymentType.CHEQUE)) {
//                        ValidationService.validate(customerPaymentDetail.getChequeNo(), "cheque no");
//                        ValidationService.validate(customerPaymentDetail.getChequeDate(), "cheque date");
//                    }
//                    customerPaymentDetail.setCustomerPaymentId(customerPayment.getId());
//                    customerPaymentDetail.setCreatedBy(loggedInUser);
//                    customerPaymentDetail.setUpdatedBy(loggedInUser);
//                    customerPaymentDetailRepo.save(customerPaymentDetail);
//
//                    customerPaidAmount += customerPaymentDetail.getAmount();
//                }//
//
//                paymentType = PaymentType.CUSTOM;
//            }
//
//
//            Optional<CustomerAccount> customerAccountOptional = customerAccountRepo.findById(customerPayment.getCustomerAccountId());
//
//            if (customerAccountOptional.isEmpty())
//                throw new IllegalArgumentException("Invalid Customer Account");
//
//            Map<String, Object> customerData = customerRepo.getAllDetailsByCustomerId(
//                    customerAccountOptional.get().getCustomer().getCustomerId(),
//                    customerAccountOptional.get().getUnit().getId()
//
//            );
//
//
//            String customerName = "", unitSerial = "", projectName = "";
//            long projectId = 0l;
//            if (customerData != null) {
//                customerName = customerData.get("customerName").toString();
//                projectName = customerData.get("projectName").toString();
//                unitSerial = customerData.get("unitSerial").toString();
//                projectId = Long.valueOf(customerData.get("projectId").toString());
//
//            }
//
//            double receivingAccountAmount = customerPayment.getOrganizationAccountDetails().stream().
//                    mapToDouble(OrganizationAccountDetail::getAmount).sum();
//
//            if (receivingAccountAmount > 0) {
//                if (receivingAccountAmount != customerPaidAmount)
//                    throw new IllegalArgumentException("Receiving Amount is not matched!");
//
//                for (OrganizationAccountDetail organizationAccountDetail : customerPayment.getOrganizationAccountDetails()) {
//                    organizationAccountDetail.setCustomerName(customerName);
//                    organizationAccountDetail.setProjectName(projectName);
//                    organizationAccountDetail.setUnitSerialNo(unitSerial);
//                    organizationAccountDetail.setProjectId(projectId);
//                    organizationAccountDetail.setComments("Paid By " + customerName + " for Unit # " + unitSerial + " of " + projectName);
//                    organizationAccountService.addOrgAcctDetail(organizationAccountDetail, loggedInUser);
//                }
//            }
//
//
//            double remainingAmount = totalAmount - customerPaidAmount;
//
//            if (remainingAmount == 0) {
////                WHEN PAID AMOUNT IS EQUAL TO INSTALLMENT AMOUNT
//                payment.setReceivedAmount(sumOfPaidAmount + customerPaidAmount);
//                payment.setRemainingAmount(remainingAmount);
//                payment.setPaymentStatus(PAID);
//                payment.setPaymentType(paymentType);
//                payment.setUpdatedBy(loggedInUser);
//                customerPaymentRepo.save(payment);
//            } else if (remainingAmount > 0) {
////                WHEN PAID AMOUNT IS LESS THAN INSTALLMENT AMOUNT
//                payment.setReceivedAmount(sumOfPaidAmount + customerPaidAmount);
//                payment.setRemainingAmount(remainingAmount);
//                payment.setPaymentStatus(PENDING);
//                payment.setPaymentType(paymentType);
//                payment.setUpdatedBy(loggedInUser);
//                customerPaymentRepo.save(payment);
//            } else {
////                WHEN PAID AMOUNT IS GREATER THAN INSTALLMENT AMOUNT
//
//                List<CustomerPayment> customerPayments = customerPaymentRepo
//                        .findByCustomerAccountId(customerPayment.getCustomerAccountId()).stream()
//                        .filter(customerPaymentSaved -> customerPaymentSaved.getSerialNo() >= customerPayment.getSerialNo())
//                        .collect(Collectors.toList());
//
//                for (CustomerPayment defaultPayment : customerPayments) {
//                    if (defaultPayment.getRemainingAmount() - customerPaidAmount <= 0) {
//                        // amount fully deductable
//                        defaultPayment.setReceivedAmount(defaultPayment.getAmount());
//                        defaultPayment.setRemainingAmount(0);
//                        defaultPayment.setPaymentStatus(PAID);
//                        payment.setPaymentType(paymentType);
//                        payment.setUpdatedBy(loggedInUser);
//                        customerPaymentRepo.save(defaultPayment);
//                        customerPaidAmount -= defaultPayment.getRemainingAmount();
//
//                    } else {

    /// /                        partially amount deductable
//                        defaultPayment.setReceivedAmount(customerPaidAmount);
//                        defaultPayment.setRemainingAmount(defaultPayment.getRemainingAmount() - customerPaidAmount);
//                        defaultPayment.setPaymentStatus(PENDING);
//                        payment.setPaymentType(paymentType);
//                        payment.setUpdatedBy(loggedInUser);
//                        customerPaymentRepo.save(defaultPayment);
//                        customerPaidAmount -= defaultPayment.getAmount();
//                    }
//                    if (customerPaidAmount <= 0) break;
//                }
//            }
//
//
//            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
//        } catch (IllegalArgumentException e) {
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
//        } catch (Exception e) {
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            e.printStackTrace();
//            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
//        }
//    }
    @Transactional
    public Map<String, Object> updateCustomerPaymentDetail(CustomerPaymentDetail customerPayment, String loggedInUser) {
        try {
            ValidationService.validate(customerPayment.getId(), "customer Payment");
            ValidationService.validate(customerPayment.getCustomerPaymentReason(), "reason");
            ValidationService.validate(customerPayment.getPaymentType(), "payment type");

            CustomerPaymentDetail existingDetail = customerPaymentDetailRepo.findById(customerPayment.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Payment!"));
            CustomerPayment parentPayment = customerPaymentRepo.findById(existingDetail.getCustomerPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer payment not found"));
            if (Double.compare(existingDetail.getAmount(), customerPayment.getAmount()) != 0) {
                throw new IllegalArgumentException("Use the full payment edit endpoint to change the amount");
            }

            if (customerPayment.getPaymentType().equals(PaymentType.CHEQUE)) {
                ValidationService.validate(customerPayment.getChequeNo(), "cheque no");
                ValidationService.validate(customerPayment.getChequeDate(), "cheque date");
            }

            existingDetail.setPaymentType(customerPayment.getPaymentType());
            existingDetail.setCustomerPaymentReason(customerPayment.getCustomerPaymentReason());
            existingDetail.setChequeNo(customerPayment.getChequeNo());
            existingDetail.setChequeDate(customerPayment.getChequeDate());
            existingDetail.setUpdatedBy(loggedInUser);
            existingDetail.setUpdatedDate(LocalDateTime.now());
            CustomerPaymentDetail savedDetail = customerPaymentDetailRepo.save(existingDetail);
            List<CustomerPaymentDetail> parentDetails =
                    customerPaymentDetailRepo.findByCustomerPaymentId(parentPayment.getId());
            parentPayment.setPaymentType(parentDetails.size() == 1
                    ? parentDetails.get(0).getPaymentType()
                    : PaymentType.CUSTOM);
            parentPayment.setUpdatedBy(loggedInUser);
            parentPayment.setUpdatedDate(LocalDateTime.now());
            customerPaymentRepo.save(parentPayment);

            return ResponseMapper.buildResponse(Responses.SUCCESS, savedDetail);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> editCustomerPayment(CustomerPayment request, String loggedInUser) {
        try {
            ValidationService.validate(request.getId(), "customer payment");
            if (request.getCustomerPaymentDetails() == null || request.getCustomerPaymentDetails().isEmpty()) {
                throw new IllegalArgumentException("At least one payment detail is required");
            }

            CustomerPayment payment = customerPaymentRepo.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Payment"));
            CustomerAccount customerAccount = customerAccountRepo.findById(payment.getCustomerAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Customer Account"));
            List<CustomerPaymentDetail> previousDetails =
                    customerPaymentDetailRepo.findByCustomerPaymentId(payment.getId());
            List<OrganizationAccountDetail> previousOrgDetails =
                    getOrganizationAccountDetailsForPayment(payment.getId(), previousDetails);

            double oldAmount = payment.getAmount();
            if (request.getCustomerPaymentDetails().stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Payment details cannot contain null entries");
            }
            double newAmount = request.getCustomerPaymentDetails().stream()
                    .mapToDouble(CustomerPaymentDetail::getAmount)
                    .sum();
            if (!Double.isFinite(newAmount) || newAmount <= 0
                    || request.getCustomerPaymentDetails().stream()
                    .anyMatch(detail -> !Double.isFinite(detail.getAmount()) || detail.getAmount() <= 0)) {
                throw new IllegalArgumentException("Payment detail amounts must be greater than zero");
            }
            if (customerAccount.getTotalBalanceAmount() + oldAmount < newAmount) {
                throw new IllegalArgumentException("Invalid Amount!");
            }

            List<OrganizationAccountDetail> replacementOrgDetails =
                    request.getOrganizationAccountDetails();
            if (payment.isPaymentAddedToAccount()) {
                if (previousOrgDetails.isEmpty() || replacementOrgDetails == null
                        || replacementOrgDetails.isEmpty()
                        || replacementOrgDetails.stream().anyMatch(Objects::isNull)) {
                    throw new IllegalArgumentException(
                            "Deposited payments must include their organization account details when edited");
                }
                double depositedAmount = replacementOrgDetails.stream()
                        .mapToDouble(OrganizationAccountDetail::getAmount)
                        .sum();
                if (replacementOrgDetails.stream().anyMatch(detail ->
                        !Double.isFinite(detail.getAmount()) || detail.getAmount() <= 0)
                        || Math.abs(depositedAmount - newAmount) > 0.01) {
                    throw new IllegalArgumentException(
                            "Organization account amounts must equal the payment amount");
                }
                for (OrganizationAccountDetail detail : replacementOrgDetails) {
                    if (detail.getOrganizationAcctId() <= 0) {
                        throw new IllegalArgumentException("Organization account is required for each allocation");
                    }
                }
            } else if (replacementOrgDetails != null && !replacementOrgDetails.isEmpty()) {
                throw new IllegalArgumentException("Undeposited payments cannot include organization account details");
            }

            journalEntryService.reverseJournalEntriesForCustomerPayment(
                    customerAccount, payment, loggedInUser);

            customerAccount.setTotalPaidAmount(customerAccount.getTotalPaidAmount() - oldAmount + newAmount);
            customerAccount.setTotalBalanceAmount(customerAccount.getTotalBalanceAmount() + oldAmount - newAmount);
            customerAccount.setUpdatedBy(loggedInUser);
            customerAccountRepo.save(customerAccount);

            for (OrganizationAccountDetail oldDetail : previousOrgDetails) {
                OrganizationAccount oldAccount = organizationAccountRepo.findById(oldDetail.getOrganizationAcctId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid organization account"));
                oldAccount.setTotalAmount(oldAccount.getTotalAmount() - oldDetail.getAmount());
                organizationAccountRepo.save(oldAccount);
            }
            organizationAccountDetailRepo.deleteAll(previousOrgDetails);

            customerPaymentDetailRepo.deleteAll(previousDetails);
            List<CustomerPaymentDetail> updatedDetails = new ArrayList<>();
            for (CustomerPaymentDetail detail : request.getCustomerPaymentDetails()) {
                ValidationService.validate(detail.getPaymentType(), "payment type");
                ValidationService.validate(detail.getCustomerPaymentReason(), "payment reason");
                if (detail.getPaymentType() == PaymentType.CHEQUE) {
                    ValidationService.validate(detail.getChequeNo(), "cheque no");
                    ValidationService.validate(detail.getChequeDate(), "cheque date");
                }
                CustomerPaymentDetail updatedDetail = new CustomerPaymentDetail();
                updatedDetail.setCustomerPaymentId(payment.getId());
                updatedDetail.setAmount(detail.getAmount());
                updatedDetail.setPaymentType(detail.getPaymentType());
                updatedDetail.setCustomerPaymentReason(detail.getCustomerPaymentReason());
                updatedDetail.setChequeNo(detail.getChequeNo());
                updatedDetail.setChequeDate(detail.getChequeDate());
                updatedDetail.setCreatedBy(loggedInUser);
                updatedDetail.setUpdatedBy(loggedInUser);
                updatedDetails.add(customerPaymentDetailRepo.save(updatedDetail));
            }

            payment.setAmount(newAmount);
            payment.setReceivedAmount(newAmount);
            payment.setRemainingAmount(0);
            payment.setPaymentType(updatedDetails.size() == 1
                    ? updatedDetails.get(0).getPaymentType()
                    : PaymentType.CUSTOM);
            payment.setUpdatedBy(loggedInUser);
            payment.setUpdatedDate(LocalDateTime.now());
            customerPaymentRepo.save(payment);

            List<OrganizationAccountDetail> savedOrgDetails = new ArrayList<>();
            if (payment.isPaymentAddedToAccount()) {
                String customerName = customerAccount.getCustomer().getName();
                String projectName = customerAccount.getProject().getName();
                String unitSerial = customerAccount.getUnit().getSerialNo();
                long projectId = customerAccount.getProject().getProjectId();

                for (OrganizationAccountDetail requestedDetail : replacementOrgDetails) {
                    OrganizationAccount orgAccount = organizationAccountRepo
                            .findByIdAndOrganizationId(
                                    requestedDetail.getOrganizationAcctId(),
                                    customerAccount.getCustomer().getOrganizationId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid organization account"));
                    orgAccount.setTotalAmount(orgAccount.getTotalAmount() + requestedDetail.getAmount());
                    organizationAccountRepo.save(orgAccount);

                    OrganizationAccountDetail orgDetail = new OrganizationAccountDetail();
                    orgDetail.setOrganizationAcctId(orgAccount.getId());
                    orgDetail.setTransactionType(TransactionType.DEBIT);
                    orgDetail.setTransactionCategory(TransactionCategory.CUSTOMER_PAYMENT);
                    orgDetail.setAmount(requestedDetail.getAmount());
                    orgDetail.setComments("Paid By " + customerName + " for Unit # "
                            + unitSerial + " of " + projectName);
                    orgDetail.setProjectId(projectId);
                    orgDetail.setProjectName(projectName);
                    orgDetail.setCustomerName(customerName);
                    orgDetail.setUnitSerialNo(unitSerial);
                    orgDetail.setCustomerId(customerAccount.getCustomer().getCustomerId());
                    orgDetail.setCustomerPaymentId(payment.getId());
                    orgDetail.setCustomerAccountId(customerAccount.getId());
                    orgDetail.setCreatedBy(loggedInUser);
                    orgDetail.setUpdatedBy(loggedInUser);
                    savedOrgDetails.add(organizationAccountDetailRepo.save(orgDetail));
                }
            }
            payment.setOrganizationAccountDetails(savedOrgDetails);

            Long bookingId = bookingRepository.findByCustomerIdAndUnitIdAndIsActiveTrue(
                            customerAccount.getCustomer().getCustomerId(), customerAccount.getUnit().getId())
                    .map(booking -> booking.getId())
                    .orElse(0L);
            journalEntryService.createJournalEntryForCustomerPayment(
                    customerAccount,
                    payment,
                    savedOrgDetails,
                    customerAccount.getUnit().getId(),
                    bookingId,
                    loggedInUser);

            return ResponseMapper.buildResponse(Responses.SUCCESS, payment);
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> deleteUnpostedPayment(CustomerPayment request, String loggedInUser) {
        try {
            ValidationService.validate(request.getId(), "customer Payment");


            Optional<CustomerPayment> customerPaymentOptional = customerPaymentRepo.findById(request.getId());

            if (customerPaymentOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Payment");

            CustomerPayment customerPayment = customerPaymentOptional.get();

            Optional<CustomerAccount> customerAccountOptional = customerAccountRepo.findById(customerPayment.getCustomerAccountId());
            if (customerAccountOptional.isEmpty())
                throw new IllegalArgumentException("Invalid Customer Account");

            CustomerAccount customerAccount = customerAccountOptional.get();
            customerAccount.setTotalPaidAmount(customerAccount.getTotalPaidAmount() - customerPayment.getAmount());
            customerAccount.setTotalBalanceAmount(customerAccount.getTotalBalanceAmount() + customerPayment.getAmount());
            customerAccount.setUpdatedBy(loggedInUser);
            customerAccountRepo.save(customerAccount);

            List<CustomerPaymentDetail> customerPaymentDetails =
                    customerPaymentDetailRepo.findByCustomerPaymentId(request.getId());
            List<OrganizationAccountDetail> orgDetails =
                    getOrganizationAccountDetailsForPayment(request.getId(), customerPaymentDetails);
            if (customerPayment.isPaymentAddedToAccount() && orgDetails.isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot delete deposited payment because its organization account records were not found");
            }
            for (OrganizationAccountDetail orgDetail : orgDetails) {
                OrganizationAccount organizationAccount = organizationAccountRepo
                        .findById(orgDetail.getOrganizationAcctId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid organization account"));
                organizationAccount.setTotalAmount(organizationAccount.getTotalAmount() - orgDetail.getAmount());
                organizationAccountRepo.save(organizationAccount);
            }

            customerPaymentDetailRepo.deleteAll(customerPaymentDetails);
            organizationAccountDetailRepo.deleteAll(orgDetails);
            journalEntryService.reverseJournalEntriesForCustomerPayment(
                    customerAccount, customerPayment, loggedInUser);
            customerPaymentRepo.deleteById(request.getId());

            return ResponseMapper.buildResponse(Responses.SUCCESS, "Deleted Successfully!");
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    private List<OrganizationAccountDetail> getOrganizationAccountDetailsForPayment(
            long customerPaymentId,
            List<CustomerPaymentDetail> paymentDetails
    ) {
        Map<Long, OrganizationAccountDetail> detailsById = new LinkedHashMap<>();
        organizationAccountDetailRepo.findByCustomerPaymentId(customerPaymentId)
                .forEach(detail -> detailsById.put(detail.getId(), detail));

        List<Long> paymentDetailIds = paymentDetails.stream()
                .map(CustomerPaymentDetail::getId)
                .filter(id -> id > 0)
                .toList();
        if (!paymentDetailIds.isEmpty()) {
            organizationAccountDetailRepo.findByCustomerPaymentDetailIdIn(paymentDetailIds)
                    .forEach(detail -> detailsById.put(detail.getId(), detail));
        }
        return new ArrayList<>(detailsById.values());
    }

    private void populateOrganizationAccountDetails(List<CustomerPayment> payments) {
        if (payments.isEmpty()) {
            return;
        }

        List<Long> paymentIds = payments.stream()
                .map(CustomerPayment::getId)
                .toList();
        Map<Long, Map<Long, OrganizationAccountDetail>> detailsByPayment = new HashMap<>();
        organizationAccountDetailRepo.findByCustomerPaymentIdIn(paymentIds)
                .forEach(detail -> detailsByPayment
                        .computeIfAbsent(detail.getCustomerPaymentId(), ignored -> new LinkedHashMap<>())
                        .put(detail.getId(), detail));

        List<CustomerPaymentDetail> paymentDetails =
                customerPaymentDetailRepo.findByCustomerPaymentIdIn(paymentIds);
        Map<Long, Long> paymentIdByDetailId = new HashMap<>();
        paymentDetails.forEach(detail ->
                paymentIdByDetailId.put(detail.getId(), detail.getCustomerPaymentId()));

        List<Long> paymentDetailIds = paymentDetails.stream()
                .map(CustomerPaymentDetail::getId)
                .toList();
        if (!paymentDetailIds.isEmpty()) {
            organizationAccountDetailRepo.findByCustomerPaymentDetailIdIn(paymentDetailIds)
                    .forEach(detail -> {
                        Long paymentId = paymentIdByDetailId.get(detail.getCustomerPaymentDetailId());
                        if (paymentId != null) {
                            detailsByPayment
                                    .computeIfAbsent(paymentId, ignored -> new LinkedHashMap<>())
                                    .put(detail.getId(), detail);
                        }
                    });
        }

        for (CustomerPayment payment : payments) {
            Map<Long, OrganizationAccountDetail> paymentDetailsById =
                    detailsByPayment.get(payment.getId());
            payment.setOrganizationAccountDetails(paymentDetailsById == null
                    ? new ArrayList<>()
                    : new ArrayList<>(paymentDetailsById.values()));
        }
    }

}
