package com.rem.backend.service;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.dto.analytic.CountStatsByTenureResponse;
import com.rem.backend.entity.customer.CustomerPayment;
import com.rem.backend.repository.BookingRepository;
import com.rem.backend.repository.CustomerAccountRepo;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@AllArgsConstructor
public class AnalyticsService {

    private final ProjectRepo projectRepo;
    private final CustomerRepo customerRepo;
    private final BookingRepository bookingRepository;
    private final CustomerAccountRepo customerAccountRepo;

    public Map<String , Object> getCountByState(CountStateByTenureRequest request){
        try{
            CountStatsByTenureResponse countStatsByTenureResponse = new CountStatsByTenureResponse();
            ValidationService.validate(request.getRequestBy() , "request by");
            ValidationService.validate(request.getTenure() , "tenure");

           LocalDateTime tenureDate = Utility.getDateInLastDays(request.getTenure());

           double recentCount = 0;
            double totalCount = 0;



           switch (request.getRequestBy().toLowerCase()){
               case "project":
                   recentCount = projectRepo.countByCreatedDateAfterAndOrganizationId(tenureDate, request.getOrgId());
                   totalCount = projectRepo.countByOrganizationId(request.getOrgId());
               case "user":
                   recentCount = customerRepo.countByCreatedDateAfterAndOrganizationId(tenureDate, request.getOrgId());
                   totalCount = customerRepo.countByOrganizationId(request.getOrgId());
               case "sale":
                   recentCount = customerAccountRepo.getTotalAmountByOrganizationIdAndCreatedAfter(request.getOrgId() , tenureDate);
                   totalCount = customerAccountRepo.getTotalAmountByOrganizationId(request.getOrgId());
               case "received_payment":
                   recentCount = customerAccountRepo.getTotalReceivedAmountByOrganizationIdAndDate(request.getOrgId() , tenureDate);
                   totalCount = customerAccountRepo.getTotalReceivedAmountByOrganizationId(request.getOrgId());
               default:
                   recentCount = projectRepo.countByCreatedDateAfterAndOrganizationId(tenureDate, request.getOrgId());
                   totalCount = projectRepo.countByOrganizationId(request.getOrgId());


           }


            if (totalCount > 0){
                countStatsByTenureResponse.setPercentage((recentCount * 100) / totalCount);
            }

            countStatsByTenureResponse.setTitle("Project");
            countStatsByTenureResponse.setValue(recentCount);
            countStatsByTenureResponse.setTenure("last " + request.getTenure() + " days");

            return ResponseMapper.buildResponse(Responses.SUCCESS , countStatsByTenureResponse);

        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }catch (Exception e){
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE , e.getMessage());
        }
    }



 }
