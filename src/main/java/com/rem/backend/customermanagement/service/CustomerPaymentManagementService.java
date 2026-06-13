package com.rem.backend.customermanagement.service;

import com.rem.backend.customermanagement.dto.*;
import com.rem.backend.customermanagement.entity.Customer;
import com.rem.backend.customermanagement.entity.CustomerAccount;
import com.rem.backend.customermanagement.entity.CustomerPayment;
import com.rem.backend.customermanagement.entity.CustomerPaymentDetail;
import com.rem.backend.projectmanagement.entity.Project;
import com.rem.backend.projectmanagement.entity.Unit;
import com.rem.backend.enums.PaymentStatus;
import com.rem.backend.customermanagement.repository.CustomerAccountRepo;
import com.rem.backend.customermanagement.repository.CustomerPaymentDetailRepo;
import com.rem.backend.customermanagement.repository.CustomerPaymentRepo;
import com.rem.backend.customermanagement.repository.CustomerRepo;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import com.rem.backend.projectmanagement.repository.UnitRepo;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerPaymentManagementService {

    private final UserRepo userRepo;
    private final CustomerRepo customerRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPaymentRepo customerPaymentRepo;
    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;
    private final UnitRepo unitRepo;
    private final ProjectRepo projectRepo;

    /**
     * Get all payments for logged-in customer across all accounts
     */
    public Map<String, Object> getAllCustomerPayments(String username, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // Get all customer account IDs
            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).collect(Collectors.toList());

            if (accountIds.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, Page.empty());
            }

            // Get all payments for these accounts
            List<CustomerPayment> allPayments = customerPaymentRepo.findByCustomerAccountIdIn(accountIds);

            // Convert to DTOs
            List<CustomerPaymentDTO> paymentDTOs = allPayments.stream()
                .map(this::convertToPaymentDTO)
                .sorted((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()))
                .collect(Collectors.toList());

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentDTOs);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payments by account ID
     */
    public Map<String, Object> getPaymentsByAccount(String username, Long accountId, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // Verify account belongs to customer
            Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Account not found");
            }

            CustomerAccount account = accountOpt.get();
            if (account.getCustomer().getCustomerId() != customer.getCustomerId()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Account does not belong to this customer");
            }

            Page<CustomerPayment> paymentPage = customerPaymentRepo.findByCustomerAccountId(accountId, pageable);
            Page<CustomerPaymentDTO> paymentDTOPage = paymentPage.map(this::convertToPaymentDTO);

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentDTOPage);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payment details by payment ID
     */
    public Map<String, Object> getPaymentDetails(String username, Long paymentId) {
        try {
            Customer customer = getCustomerFromUsername(username);

            Optional<CustomerPayment> paymentOpt = customerPaymentRepo.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Payment not found");
            }

            CustomerPayment payment = paymentOpt.get();

            // Verify payment belongs to customer's account
            Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(payment.getCustomerAccountId());
            if (accountOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Payment does not belong to this customer");
            }

            CustomerPaymentDTO dto = convertToPaymentDTO(payment);

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payments by status
     */
    public Map<String, Object> getPaymentsByStatus(String username, PaymentStatus status, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // Get all customer account IDs
            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).collect(Collectors.toList());

            if (accountIds.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, Page.empty());
            }

            // Get payments by status for these accounts
            List<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountIdIn(accountIds)
                .stream()
                .filter(p -> p.getPaymentStatus().equals(status))
                .collect(Collectors.toList());

            List<CustomerPaymentDTO> paymentDTOs = payments.stream()
                .map(this::convertToPaymentDTO)
                .sorted((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()))
                .collect(Collectors.toList());

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentDTOs);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payments by date range
     */
    public Map<String, Object> getPaymentsByDateRange(String username, String startDate, String endDate, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // Get all customer account IDs
            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).collect(Collectors.toList());

            if (accountIds.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.SUCCESS, Page.empty());
            }

            // Note: You would need to add a repository method for date range filtering
            // For now, returning all payments
            List<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountIdIn(accountIds);

            List<CustomerPaymentDTO> paymentDTOs = payments.stream()
                .map(this::convertToPaymentDTO)
                .sorted((p1, p2) -> p2.getCreatedDate().compareTo(p1.getCreatedDate()))
                .collect(Collectors.toList());

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentDTOs);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payment summary for customer
     */
    public Map<String, Object> getCustomerPaymentSummary(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).collect(Collectors.toList());

            Map<String, Object> summary = new HashMap<>();

            if (!accountIds.isEmpty()) {
                List<CustomerPayment> allPayments = customerPaymentRepo.findByCustomerAccountIdIn(accountIds);

                // Calculate summary statistics
                int totalPayments = allPayments.size();
                int paidPayments = (int) allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.PAID).count();
                int pendingPayments = (int) allPayments.stream().filter(p -> p.getPaymentStatus() == PaymentStatus.UNPAID).count();

                double totalAmount = allPayments.stream().mapToDouble(CustomerPayment::getAmount).sum();
                double receivedAmount = allPayments.stream().mapToDouble(CustomerPayment::getReceivedAmount).sum();
                double remainingAmount = allPayments.stream().mapToDouble(CustomerPayment::getRemainingAmount).sum();

                summary.put("totalPayments", totalPayments);
                summary.put("paidPayments", paidPayments);
                summary.put("pendingPayments", pendingPayments);
                summary.put("totalAmount", totalAmount);
                summary.put("receivedAmount", receivedAmount);
                summary.put("remainingAmount", remainingAmount);
            } else {
                summary.put("totalPayments", 0);
                summary.put("paidPayments", 0);
                summary.put("pendingPayments", 0);
                summary.put("totalAmount", 0.0);
                summary.put("receivedAmount", 0.0);
                summary.put("remainingAmount", 0.0);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, summary);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Convert CustomerPayment entity to DTO
     */
    private CustomerPaymentDTO convertToPaymentDTO(CustomerPayment payment) {
        CustomerPaymentDTO dto = new CustomerPaymentDTO();
        dto.setId(payment.getId());
        dto.setSerialNo(payment.getSerialNo());
        dto.setCustomerAccountId(payment.getCustomerAccountId());
        dto.setAmount(payment.getAmount());
        dto.setReceivedAmount(payment.getReceivedAmount());
        dto.setRemainingAmount(payment.getRemainingAmount());
        dto.setPaymentType(payment.getPaymentType().name());
        dto.setPaymentStatus(payment.getPaymentStatus().name());
        dto.setPaidDate(payment.getPaidDate());
        dto.setPaymentAddedToAccount(payment.isPaymentAddedToAccount());
        dto.setCreatedBy(payment.getCreatedBy());
        dto.setUpdatedBy(payment.getUpdatedBy());
        dto.setCreatedDate(payment.getCreatedDate());
        dto.setUpdatedDate(payment.getUpdatedDate());

        // Get account details
        Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(payment.getCustomerAccountId());
        if (accountOpt.isPresent()) {
            CustomerAccount account = accountOpt.get();
            dto.setCustomerName(account.getCustomer().getName());

            // Get unit details
            Optional<Unit> unitOpt = unitRepo.findById(account.getUnit().getId());
            unitOpt.ifPresent(unit -> dto.setUnitSerial(unit.getSerialNo()));

            // Get project details
            Optional<Project> projectOpt = projectRepo.findById(account.getProject().getProjectId());
            projectOpt.ifPresent(project -> dto.setProjectName(project.getName()));
        }

        // Get payment details
        List<CustomerPaymentDetail> details = customerPaymentDetailRepo.findByCustomerPaymentId(payment.getId());
        List<CustomerPaymentDTO.CustomerPaymentDetailDTO> detailDTOs = details.stream()
            .map(detail -> new CustomerPaymentDTO.CustomerPaymentDetailDTO(
                detail.getId(),
                detail.getCustomerPaymentId(),
                detail.getAmount(),
                detail.getPaymentType().name(),
                detail.getCustomerPaymentReason().name(),
                detail.getChequeNo(),
                detail.getChequeDate(),
                detail.getCreatedBy(),
                detail.getCreatedDate()
            ))
            .collect(Collectors.toList());

        dto.setPaymentDetails(detailDTOs);

        return dto;
    }

    /**
     * Helper method to get customer from username
     */
    private Customer getCustomerFromUsername(String username) {
        ValidationService.validate(username, "username");

        Optional<User> userOpt = userRepo.findByUsernameAndIsActiveTrue(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        Optional<Customer> customerOpt = customerRepo.findByUserId(user.getId());
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Customer profile not found for this user");
        }

        return customerOpt.get();
    }
}
