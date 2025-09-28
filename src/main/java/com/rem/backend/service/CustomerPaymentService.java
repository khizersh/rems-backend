package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.organizationAccount.OrganizationAccountDetail;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.customer.CustomerPaymentDetail;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.repository.*;
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
import java.util.stream.Collectors;

import static com.rem.backend.enums.PaymentStatus.PAID;
import static com.rem.backend.enums.PaymentStatus.PENDING;

@Service
@AllArgsConstructor
public class CustomerPaymentService {
    private final CustomerPaymentRepo customerPaymentRepo;
    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerRepo customerRepo;
    private final UnitRepo unitRepo;
    private final OrganizationAccountService organizationAccountService;

    public Map<String, Object> getPaymentsByCustomerAccountId(long customerAccountId, Pageable pageable) {
        try {
            ValidationService.validate(customerAccountId, "customerAccountId");
            Page<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountId(customerAccountId, pageable);
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

            if (customerPayment.getCustomerPaymentDetails() == null ||
                    customerPayment.getCustomerPaymentDetails().size() == 0)
                throw new IllegalArgumentException("Invalid Payment");

            CustomerAccount customerAccount = customerAccountOp.get();
            if (customerAccountOp.isEmpty())
                throw new IllegalArgumentException("Invalid Account!");

            Double totalReceivedAmount = customerAccount.getTotalBalanceAmount();

            double currentPaidAmount = customerPayment.getCustomerPaymentDetails().stream().mapToDouble(p -> p.getAmount()).sum();


            if (totalReceivedAmount <= currentPaidAmount)
                throw new IllegalArgumentException("Invalid Amount!");

            customerPayment.setSerialNo(0);

            if (customerPayment.getCustomerPaymentDetails().size() > 0)
                customerPayment.setPaymentType(PaymentType.CUSTOM);

            if (customerPayment.getCustomerPaymentDetails().size() == 1)
                customerPayment.setPaymentType(customerPayment.getCustomerPaymentDetails().get(0).getPaymentType());

            customerPayment.setPaymentStatus(PAID);
            customerPayment.setCustomerAccountId(customerPayment.getCustomerAccountId());


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
            if (customerData != null) {
                customerName = customerData.get("customerName").toString();
                projectName = customerData.get("projectName").toString();
                unitSerial = customerData.get("unitSerial").toString();
                projectId = Long.valueOf(customerData.get("projectId").toString());

            }

            double receivingAccountAmount = customerPayment.getOrganizationAccountDetails().stream().
                    mapToDouble(OrganizationAccountDetail::getAmount).sum();

            if (receivingAccountAmount > 0) {
                if (receivingAccountAmount != currentPaidAmount)
                    throw new IllegalArgumentException("Receiving Amount is not matched!");

                for (OrganizationAccountDetail organizationAccountDetail : customerPayment.getOrganizationAccountDetails()) {
                    organizationAccountDetail.setCustomerName(customerName);
                    organizationAccountDetail.setProjectName(projectName);
                    organizationAccountDetail.setUnitSerialNo(unitSerial);
                    organizationAccountDetail.setProjectId(projectId);
                    organizationAccountDetail.setComments("Paid By " + customerName + " for Unit # " + unitSerial + " of " + projectName);
                    organizationAccountService.addOrgAcctDetail(organizationAccountDetail, loggedInUser);
                }
            }


            customerAccount.setTotalPaidAmount(customerAccount.getTotalPaidAmount() + currentPaidAmount);
            customerAccount.setTotalBalanceAmount( customerAccount.getTotalBalanceAmount() - currentPaidAmount);
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
////                        partially amount deductable
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


            if (customerPayment.getPaymentType().equals(PaymentType.CHEQUE)) {
                ValidationService.validate(customerPayment.getChequeNo(), "cheque no");
                ValidationService.validate(customerPayment.getChequeDate(), "cheque date");
            }

            if (!customerPaymentDetailRepo.existsById(customerPayment.getId()))
                throw new IllegalArgumentException("Invalid Payment!");


            customerPayment.setUpdatedBy(loggedInUser);
            customerPayment.setUpdatedDate(LocalDateTime.now());




            return ResponseMapper.buildResponse(Responses.SUCCESS, customerPaymentDetailRepo.save(customerPayment));
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
