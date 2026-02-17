package com.rem.backend.customermanagement.service;

import com.rem.backend.customermanagement.dto.*;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.entity.customer.CustomerPaymentDetail;
import com.rem.backend.entity.project.Project;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.repository.*;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

    private final UserRepo userRepo;
    private final CustomerRepo customerRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPaymentRepo customerPaymentRepo;
    private final CustomerPaymentDetailRepo customerPaymentDetailRepo;
    private final UnitRepo unitRepo;
    private final ProjectRepo projectRepo;

    /**
     * Get customer summary dashboard data
     */
    public Map<String, Object> getCustomerSummary(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            // Get aggregated data
            Integer totalBookings = customerAccountRepo.countBookingsByCustomerId(customer.getCustomerId());
            Double totalAmountPayable = customerAccountRepo.getTotalAmountByCustomerId(customer.getCustomerId());
            Double totalAmountPaid = customerAccountRepo.getTotalPaidAmountByCustomerId(customer.getCustomerId());
            Double totalRemainingAmount = customerAccountRepo.getTotalRemainingAmountByCustomerId(customer.getCustomerId());

            CustomerSummaryDTO summaryDTO = new CustomerSummaryDTO();
            summaryDTO.setCustomerName(customer.getName());
            summaryDTO.setNationalId(customer.getNationalId());
            summaryDTO.setContactNo(customer.getContactNo());
            summaryDTO.setEmail(customer.getEmail());
            summaryDTO.setTotalBookings(totalBookings != null ? totalBookings : 0);
            summaryDTO.setTotalUnitsBooked(totalBookings != null ? totalBookings : 0); // Same as bookings in this context
            summaryDTO.setTotalAmountPayable(totalAmountPayable != null ? totalAmountPayable : 0.0);
            summaryDTO.setTotalAmountPaid(totalAmountPaid != null ? totalAmountPaid : 0.0);
            summaryDTO.setTotalRemainingAmount(totalRemainingAmount != null ? totalRemainingAmount : 0.0);
            summaryDTO.setOverdueAmount(0.0); // TODO: Calculate overdue based on payment schedule if available

            return ResponseMapper.buildResponse(Responses.SUCCESS, summaryDTO);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get monthly payment chart data
     */
    public Map<String, Object> getPaymentChartData(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<Map<String, Object>> monthlyData = customerPaymentRepo.getMonthlyPaymentsByCustomerId(customer.getCustomerId());

            List<PaymentChartDTO> chartData = new ArrayList<>();

            for (Map<String, Object> data : monthlyData) {
                PaymentChartDTO dto = new PaymentChartDTO();
                dto.setMonth(data.get("month") != null ? ((Number) data.get("month")).intValue() : 0);
                dto.setYear(data.get("year") != null ? ((Number) data.get("year")).intValue() : 0);
                dto.setTotalPaidAmount(data.get("totalPaid") != null ? ((Number) data.get("totalPaid")).doubleValue() : 0.0);
                dto.setTotalDueAmount(0.0); // Calculate based on payment schedule if needed
                dto.setCumulativeRemaining(0.0); // Can be calculated if needed

                // Set month name
                if (dto.getMonth() > 0 && dto.getMonth() <= 12) {
                    dto.setMonthName(java.time.Month.of(dto.getMonth()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                }

                chartData.add(dto);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, chartData);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get payment mode distribution
     */
    public Map<String, Object> getPaymentModeDistribution(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<Map<String, Object>> modeData = customerPaymentDetailRepo.getPaymentModeDistributionByCustomerId(customer.getCustomerId());

            List<PaymentModeDistributionDTO> distributionList = new ArrayList<>();

            for (Map<String, Object> data : modeData) {
                PaymentModeDistributionDTO dto = new PaymentModeDistributionDTO();
                dto.setPaymentMode(data.get("paymentMode") != null ? data.get("paymentMode").toString() : "UNKNOWN");
                dto.setTotalAmount(data.get("totalAmount") != null ? ((Number) data.get("totalAmount")).doubleValue() : 0.0);
                dto.setTransactionCount(data.get("transactionCount") != null ? ((Number) data.get("transactionCount")).intValue() : 0);
                distributionList.add(dto);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, distributionList);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get recent payment transactions
     */
    public Map<String, Object> getRecentPayments(String username, int limit) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerPayment> recentPayments = customerPaymentRepo.getRecentPaymentsByCustomerId(customer.getCustomerId(), limit);

            List<RecentPaymentDTO> recentPaymentDTOs = new ArrayList<>();

            for (CustomerPayment payment : recentPayments) {
                RecentPaymentDTO dto = new RecentPaymentDTO();
                dto.setPaymentId(payment.getId());
                dto.setAccountId(payment.getCustomerAccountId());
                dto.setTotalPaymentAmount(payment.getAmount());
                dto.setReceivedAmount(payment.getReceivedAmount());
                dto.setPaidDate(payment.getPaidDate());
                dto.setPaymentStatus(payment.getPaymentStatus().name());

                // Get account details for project and unit info
                Optional<CustomerAccount> accountOpt = customerAccountRepo.findById(payment.getCustomerAccountId());
                if (accountOpt.isPresent()) {
                    CustomerAccount account = accountOpt.get();

                    Optional<Unit> unitOpt = unitRepo.findById(account.getUnit().getId());
                    unitOpt.ifPresent(unit -> dto.setUnitSerial(unit.getSerialNo()));

                    Optional<Project> projectOpt = projectRepo.findById(account.getProject().getProjectId());
                    projectOpt.ifPresent(project -> dto.setProjectName(project.getName()));
                }

                // Get payment details
                List<CustomerPaymentDetail> details = customerPaymentDetailRepo.findByCustomerPaymentId(payment.getId());
                List<RecentPaymentDTO.PaymentDetailDTO> paymentDetails = details.stream()
                        .map(detail -> new RecentPaymentDTO.PaymentDetailDTO(
                                detail.getPaymentType().name(),
                                detail.getAmount(),
                                detail.getChequeNo(),
                                detail.getChequeDate()
                        ))
                        .collect(Collectors.toList());
                dto.setPaymentDetails(paymentDetails);

                recentPaymentDTOs.add(dto);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, recentPaymentDTOs);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Get all customer accounts with status
     */
    public Map<String, Object> getAccountsStatus(String username) {
        try {
            Customer customer = getCustomerFromUsername(username);

            List<CustomerAccount> accounts = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customer.getCustomerId());

            List<AccountStatusDTO> accountStatusList = new ArrayList<>();

            for (CustomerAccount account : accounts) {
                AccountStatusDTO dto = new AccountStatusDTO();
                dto.setAccountId(account.getId());
                dto.setTotalAmount(account.getTotalAmount());
                dto.setTotalPaidAmount(account.getTotalPaidAmount());
                dto.setTotalBalanceAmount(account.getTotalBalanceAmount());
                dto.setDurationInMonths(account.getDurationInMonths());

                // Get unit details
                Optional<Unit> unitOpt = unitRepo.findById(account.getUnit().getId());
                if (unitOpt.isPresent()) {
                    Unit unit = unitOpt.get();
                    dto.setUnitSerial(unit.getSerialNo());
                    dto.setUnitType(unit.getUnitType().name());
                }

                // Get project details
                Optional<Project> projectOpt = projectRepo.findById(account.getProject().getProjectId());
                projectOpt.ifPresent(project -> dto.setProjectName(project.getName()));

                // Determine status
                String status = "ACTIVE";
                if (account.getTotalBalanceAmount() <= 0) {
                    status = "CLOSED";
                }
                // TODO: Add overdue logic based on payment schedule if available
                dto.setStatus(status);

                accountStatusList.add(dto);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, accountStatusList);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    /**
     * Helper method to get customer from username in JWT token
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
