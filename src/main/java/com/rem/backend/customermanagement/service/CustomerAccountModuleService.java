package com.rem.backend.customermanagement.service;

import com.rem.backend.customermanagement.dto.*;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.repository.*;
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
public class CustomerAccountModuleService {

    private final UserRepo userRepo;
    private final CustomerRepo customerRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPaymentRepo customerPaymentRepo;
    private final UnitRepo unitRepo;
    private final ProjectRepo projectRepo;

    /**
     * Get all accounts for logged-in customer with pagination
     */
    public Map<String, Object> getCustomerAccounts(String username, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            Page<CustomerAccount> accountPage = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(
                customer.getCustomerId(), pageable);

            Page<CustomerAccountDTO> accountDTOPage = accountPage.map(this::convertToAccountDTO);

            return ResponseMapper.buildResponse(Responses.SUCCESS, accountDTOPage);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get single account details by account ID
     */
    public Map<String, Object> getCustomerAccountById(String username, Long accountId) {
        try {
            Customer customer = getCustomerFromUsername(username);

            Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, "Account not found");
            }

            CustomerAccount account = accountOpt.get();

            // Verify account belongs to the logged-in customer
            if (account.getCustomer().getCustomerId() != customer.getCustomerId()) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Account does not belong to this customer");
            }

            CustomerAccountDTO dto = convertToAccountDTO(account);

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get customer account summary
     */
    public Map<String, Object> getCustomerAccountSummary(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());

            CustomerAccountSummaryDTO summary = new CustomerAccountSummaryDTO();
            summary.setCustomerId(customer.getCustomerId());
            summary.setCustomerName(customer.getName());
            summary.setCustomerContact(customer.getContactNo());

            // Calculate summary stats
            int totalAccounts = accounts.size();
            int activeAccounts = (int) accounts.stream().filter(acc -> acc.getTotalBalanceAmount() > 0).count();
            int closedAccounts = totalAccounts - activeAccounts;

            double totalBookingAmount = accounts.stream().mapToDouble(CustomerAccount::getTotalAmount).sum();
            double totalPaidAmount = accounts.stream().mapToDouble(CustomerAccount::getTotalPaidAmount).sum();
            double totalBalanceAmount = accounts.stream().mapToDouble(CustomerAccount::getTotalBalanceAmount).sum();

            // Get total payments count
            List<Long> accountIds = accounts.stream().map(CustomerAccount::getId).collect(Collectors.toList());
            int totalPaymentsCount = 0;
            if (!accountIds.isEmpty()) {
                List<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountIdIn(accountIds);
                totalPaymentsCount = payments.size();
            }

            double averagePaymentAmount = totalPaymentsCount > 0 ? totalPaidAmount / totalPaymentsCount : 0.0;

            summary.setTotalAccounts(totalAccounts);
            summary.setActiveAccounts(activeAccounts);
            summary.setClosedAccounts(closedAccounts);
            summary.setTotalBookingAmount(totalBookingAmount);
            summary.setTotalPaidAmount(totalPaidAmount);
            summary.setTotalBalanceAmount(totalBalanceAmount);
            summary.setTotalOverdueAmount(0.0); // TODO: Calculate based on payment schedule
            summary.setTotalPaymentsCount(totalPaymentsCount);
            summary.setAveragePaymentAmount(averagePaymentAmount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, summary);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get accounts by project ID
     */
    public Map<String, Object> getCustomerAccountsByProject(String username, Long projectId, Pageable pageable) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // First get all customer accounts, then filter by project
            List<CustomerAccount> allAccounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());
            List<CustomerAccount> projectAccounts = allAccounts.stream()
                .filter(acc -> acc.getProject().getProjectId() == projectId)
                .toList();

            List<CustomerAccountDTO> accountDTOs = projectAccounts.stream()
                .map(this::convertToAccountDTO)
                .collect(Collectors.toList());

            return ResponseMapper.buildResponse(Responses.SUCCESS, accountDTOs);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Convert CustomerAccount entity to DTO
     */
    private CustomerAccountDTO convertToAccountDTO(CustomerAccount account) {
        CustomerAccountDTO dto = new CustomerAccountDTO();
        dto.setId(account.getId());
        dto.setCustomerId(account.getCustomer().getCustomerId());
        dto.setCustomerName(account.getCustomer().getName());
        dto.setCustomerContact(account.getCustomer().getContactNo());
        dto.setCustomerEmail(account.getCustomer().getEmail());
        dto.setProjectId(account.getProject().getProjectId());
        dto.setUnitId(account.getUnit().getId());
        dto.setDurationInMonths(account.getDurationInMonths());
        dto.setActualAmount(account.getActualAmount());
        dto.setMiscellaneousAmount(account.getMiscellaneousAmount());
        dto.setDevelopmentAmount(account.getDevelopmentAmount());
        dto.setDownPayment(account.getDownPayment());
        dto.setTotalAmount(account.getTotalAmount());
        dto.setQuarterlyPayment(account.getQuarterlyPayment());
        dto.setHalfYearly(account.getHalfYearly());
        dto.setOnPossessionAmount(account.getOnPosessionAmount());
        dto.setTotalPaidAmount(account.getTotalPaidAmount());
        dto.setTotalBalanceAmount(account.getTotalBalanceAmount());
        dto.setActive(account.isActive());
        dto.setCreatedBy(account.getCreatedBy());
        dto.setCreatedDate(account.getCreatedDate());
        dto.setUpdatedDate(account.getUpdatedDate());

        // Get project details
        Optional<Project> projectOpt = projectRepo.findById(account.getProject().getProjectId());
        projectOpt.ifPresent(project -> dto.setProjectName(project.getName()));

        // Get unit details
        Optional<Unit> unitOpt = unitRepo.findById(account.getUnit().getId());
        if (unitOpt.isPresent()) {
            Unit unit = unitOpt.get();
            dto.setUnitSerial(unit.getSerialNo());
            dto.setUnitType(unit.getUnitType().name());
        }

        // Get payment summary for this account
        List<CustomerPayment> payments = customerPaymentRepo.findByCustomerAccountId(account.getId());
        dto.setTotalPaymentsCount(payments.size());

        if (!payments.isEmpty()) {
            Optional<CustomerPayment> lastPaymentOpt = payments.stream()
                .max(Comparator.comparing(CustomerPayment::getPaidDate, Comparator.nullsLast(Comparator.naturalOrder())));

            if (lastPaymentOpt.isPresent()) {
                CustomerPayment lastPayment = lastPaymentOpt.get();
                dto.setLastPaymentAmount(lastPayment.getReceivedAmount());
                dto.setLastPaymentDate(lastPayment.getPaidDate());
            }
        }

        // Determine payment status
        if (dto.getTotalBalanceAmount() <= 0) {
            dto.setPaymentStatus("FULLY_PAID");
        } else if (dto.getTotalPaidAmount() > 0) {
            dto.setPaymentStatus("PARTIALLY_PAID");
        } else {
            dto.setPaymentStatus("UNPAID");
        }

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
