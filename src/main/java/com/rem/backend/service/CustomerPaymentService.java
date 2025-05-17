package com.rem.backend.service;

import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.customer.CustomerPaymentDetail;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.enums.PaymentType;
import com.rem.backend.repository.CustomerPaymentDetailRepo;
import com.rem.backend.repository.CustomerPaymentRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.rem.backend.enums.PaymentStatus.PAID;
import static com.rem.backend.enums.PaymentStatus.PENDING;

@Service
@AllArgsConstructor
public class CustomerPaymentService {
    private final CustomerPaymentRepo customerPaymentRepo;
    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;


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


    public Map<String, Object> getPaymentDetailsByPaymentIdOnlyData(long customerPaymentId) {
        try {
            ValidationService.validate(customerPaymentId, "customerPaymentId");

            Optional<CustomerPayment> customerPaymentOptional = customerPaymentRepo.findById(customerPaymentId);
            if (customerPaymentOptional.isEmpty()) {
                return null;
            }

            CustomerPayment customerPayment = customerPaymentOptional.get();
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
            Optional<CustomerPayment> payments = customerPaymentRepo.findById(customerPayment.getId());

            if (payments.isEmpty())
                throw new IllegalArgumentException("Invalid Payment!");

            if (payments.get().getPaymentStatus().equals(PAID))
                throw new IllegalArgumentException("Payment already paid!");

            CustomerPayment payment = payments.get();
            double totalAmount = payment.getRemainingAmount();


            double customerPaidAmount = 0;

            PaymentType paymentType = customerPayment.getPaymentType();


            List<CustomerPaymentDetail> customerPaymentDetails = customerPaymentDetailRepo.findByCustomerPaymentId(customerPayment.getId());

            double sumOfPaidAmount = customerPaymentDetails.stream()
                    .mapToDouble(CustomerPaymentDetail::getAmount)
                    .sum();

            if (customerPayment.getCustomerPaymentDetails().size() > 0) {


                for (CustomerPaymentDetail customerPaymentDetail : customerPayment.getCustomerPaymentDetails()) {

                    ValidationService.validate(customerPaymentDetail.getPaymentType(), "paymentType");
                    ValidationService.validate(customerPaymentDetail.getAmount(), "detail amount");
                    customerPaymentDetail.setCustomerPaymentId(customerPayment.getId());
                    customerPaymentDetail.setCreatedBy(loggedInUser);
                    customerPaymentDetail.setUpdatedBy(loggedInUser);
                    customerPaymentDetailRepo.save(customerPaymentDetail);

                    customerPaidAmount += customerPaymentDetail.getAmount();
                }

                paymentType = PaymentType.CUSTOM;
            }

            double remainingAmount = totalAmount - customerPaidAmount;

            if (remainingAmount == 0) {
//                WHEN PAID AMOUNT IS EQUAL TO INSTALLMENT AMOUNT
                payment.setReceivedAmount(sumOfPaidAmount + customerPaidAmount);
                payment.setRemainingAmount(remainingAmount);
                payment.setPaymentStatus(PAID);
                payment.setPaymentType(paymentType);
                payment.setUpdatedBy(loggedInUser);
                customerPaymentRepo.save(payment);
            } else if (remainingAmount > 0) {
//                WHEN PAID AMOUNT IS LESS THAN INSTALLMENT AMOUNT
                payment.setReceivedAmount(sumOfPaidAmount + customerPaidAmount);
                payment.setRemainingAmount(remainingAmount);
                payment.setPaymentStatus(PENDING);
                payment.setPaymentType(paymentType);
                payment.setUpdatedBy(loggedInUser);
                customerPaymentRepo.save(payment);
            } else {
//                WHEN PAID AMOUNT IS GREATER THAN INSTALLMENT AMOUNT

                List<CustomerPaymentDetail> paymentDetails = customerPayment.getCustomerPaymentDetails();
                List<CustomerPayment> customerPayments = customerPaymentRepo
                        .findByCustomerAccountId(customerPayment.getCustomerAccountId()).stream()
                        .filter(customerPaymentSaved -> customerPaymentSaved.getSerialNo() >= customerPayment.getSerialNo())
                        .collect(Collectors.toList());

                for (CustomerPayment defaultPayment : customerPayments) {
                    if (defaultPayment.getRemainingAmount() - customerPaidAmount <= 0) {
                        // amount fully deductable
                        defaultPayment.setReceivedAmount(defaultPayment.getAmount());
                        defaultPayment.setRemainingAmount(0);
                        defaultPayment.setPaymentStatus(PAID);
                        payment.setPaymentType(paymentType);
                        payment.setUpdatedBy(loggedInUser);
                        customerPaymentRepo.save(defaultPayment);
                        customerPaidAmount -= defaultPayment.getRemainingAmount();

                    } else {
//                        partially amount deductable
                        defaultPayment.setReceivedAmount(customerPaidAmount);
                        defaultPayment.setRemainingAmount(defaultPayment.getRemainingAmount() - customerPaidAmount);
                        defaultPayment.setPaymentStatus(PENDING);
                        payment.setPaymentType(paymentType);
                        payment.setUpdatedBy(loggedInUser);
                        customerPaymentRepo.save(defaultPayment);
                        customerPaidAmount -= defaultPayment.getAmount();
                    }
                    if (customerPaidAmount <= 0) break;
                }
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, payments);
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
