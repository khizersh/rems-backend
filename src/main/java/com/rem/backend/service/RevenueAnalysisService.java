package com.rem.backend.service;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.dto.analytic.DateRangeRequest;
import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.entity.organization.OrganizationAccountDetail;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class RevenueAnalysisService {

    private final ProjectService projectService;
    private final ExpenseRepo expenseRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final BookingRepository bookingRepository;
    private final OrganizationAccoutRepo organizationAccoutRepo;
    private final VendorAccountRepo vendorAccountRepo;

    public Map<String, Object> getOrgAccountRevenue(FilterPaginationRequest filterPaginationRequest) {
        Map<String, Object> response = new HashMap<>();
        double sum = 0.0;
        try {
            ValidationService.validate(filterPaginationRequest.getFilteredBy(), "project id");

            switch (filterPaginationRequest.getFilteredBy()) {
                case "organization":
                    sum = organizationAccoutRepo.getTotalAmountByOrganizationId(filterPaginationRequest.getId());
                    break;
                case "account":
                    sum = organizationAccoutRepo.getTotalAmountByOrganizationIdAndAccountId(filterPaginationRequest.getId(), filterPaginationRequest.getId2());
                    break;
                default:
                    sum = organizationAccoutRepo.getTotalAmountByOrganizationId(filterPaginationRequest.getId());
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, sum);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getExpenseDetailByTenure(CountStateByTenureRequest request) {
        try {
            ValidationService.validate(request.getTenure(), "tenure");
            ValidationService.validate(request.getOrgId(), "org id");


            Map<String, Object> data = expenseRepo.getExpenseSumsByOrgAndDays(request.getOrgId(), request.getTenure());
            return ResponseMapper.buildResponse(Responses.SUCCESS, data);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getRevenueDetails(long organizationId) {

        Map<String, Object> response = new HashMap<>();
        try {
            ValidationService.validate(organizationId, "organizationId");
            double receivableAmount = customerAccountRepo.getTotalReceiveableAmountByOrganizationId(organizationId);
            double payableAmount = vendorAccountRepo.findTotalPayableByOrgId(organizationId);
            response.put("totalReceivableAmount", receivableAmount);
            response.put("totalPayableAmount", payableAmount);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getRevenueDetailByDateRangeAndTransactionType(DateRangeRequest request , Pageable pageable) {
       Map<String , Object>  response = new HashMap<>();
        try {
            ValidationService.validate(request.getOrganizationId(), "organization id");
            ValidationService.validate(request.getTransactionType(), "transaction type");
            ValidationService.validate(request.getStartDate(), "start date");
            ValidationService.validate(request.getEndDate(), "end date");

            LocalDateTime startDate = Utility.getStartOfDay(request.getStartDate());
            LocalDateTime endDate = Utility.getEndOfDay(request.getEndDate());

            Double sum = 0.0;

            Page<OrganizationAccountDetail> list = null;
            if (request.getTransactionType() == null || request.getTransactionType().equals(TransactionType.DEBIT_CREDIT)) {
                list = organizationAccoutRepo.findTransactionsByOrgIdAndDateRange(
                        request.getOrganizationId(),
                        startDate,
                        endDate,
                         pageable);
                sum = organizationAccoutRepo.findSumBetweenDate(request.getOrganizationId(),
                        startDate,
                        endDate);
            } else {
                list = organizationAccoutRepo.
                        findTransactionsByOrgIdAndTypeAndDateRange(
                                request.getOrganizationId(),
                                request.getTransactionType().toString(),
                                startDate,
                                endDate,
                                pageable
                        );
                sum = organizationAccoutRepo.findSumBetweenDateByTransactionType(  request.getOrganizationId(),
                        request.getTransactionType().toString(),
                        startDate,
                        endDate);

            }

            response.put("pageData" , list);
            response.put("sum" , sum);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }



    public Map<String, Object> getOrganizationSalesByDateRange(DateRangeRequest request) {
        try {
            ValidationService.validate(request.getOrganizationId(), "organization id");
            ValidationService.validate(request.getStartDate(), "start date");
            ValidationService.validate(request.getEndDate(), "end date");

            LocalDateTime startDate = Utility.getStartOfDay(request.getStartDate());
            LocalDateTime endDate = Utility.getEndOfDay(request.getEndDate());
            List<Map<String, Object>> list = bookingRepository.findMonthlySalesByOrganizationAndDateRange(request.getOrganizationId() , startDate , endDate);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
