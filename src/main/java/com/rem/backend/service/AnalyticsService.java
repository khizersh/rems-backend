package com.rem.backend.service;

import com.rem.backend.dto.analytic.CountStateByTenureRequest;
import com.rem.backend.dto.analytic.CountStatsByTenureResponse;
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

import java.text.DecimalFormat;
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
           LocalDateTime lastTenureDate = Utility.getDateInLastDays(request.getTenure() * 2);

           double recentCount = 0;
            double totalCount = 0;



           switch (request.getRequestBy().toLowerCase()){
               case "project":
                   recentCount = projectRepo.countByCreatedDateAfterAndOrganizationId(tenureDate, request.getOrgId());
                   totalCount = projectRepo.countByCreatedDateAfterAndOrganizationId(lastTenureDate, request.getOrgId());
                   break;
               case "user":
                   recentCount = customerRepo.countByCreatedDateAfterAndOrganizationId(tenureDate, request.getOrgId());
                   totalCount = customerRepo.countByCreatedDateAfterAndOrganizationId(lastTenureDate, request.getOrgId());
                   break;
               case "sale":
                   Double recentCountDouble = customerAccountRepo.getTotalAmountByOrganizationIdAndCreatedAfter(request.getOrgId() , tenureDate);
                   if (recentCountDouble == null) recentCount = 0; else recentCount = recentCountDouble;

                   Double totalCountDouble = customerAccountRepo.getTotalAmountByOrganizationIdAndCreatedAfter(request.getOrgId() , tenureDate);
                   if (totalCountDouble == null) totalCount = 0; else totalCount = totalCountDouble;

                   break;
               case "received_payment":

                   recentCountDouble = customerAccountRepo.getTotalReceivedAmountByOrganizationIdAndDate(request.getOrgId() , tenureDate);
                   if (recentCountDouble == null) recentCount = 0; else recentCount = recentCountDouble;

                   totalCountDouble = customerAccountRepo.getTotalReceivedAmountByOrganizationIdAndDate(request.getOrgId() , lastTenureDate);
                   if (totalCountDouble == null) totalCount = 0; else totalCount = totalCountDouble;

                   break;
               default:
                   recentCount = 0;
                   totalCount = 0;
                   break;

           }
            DecimalFormat df = new DecimalFormat("#.##");
            double percentageFormatted = Double.valueOf(df.format((recentCount * 100) / totalCount));

            if (totalCount > 0){
                countStatsByTenureResponse.setPercentage(percentageFormatted );
            }

            countStatsByTenureResponse.setTitle(request.getRequestBy());
            countStatsByTenureResponse.setValue(recentCount);
            countStatsByTenureResponse.setTenure("last " + request.getTenure() + " days");

            return ResponseMapper.buildResponse(Responses.SUCCESS , countStatsByTenureResponse);

        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }catch (Exception e){
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE , e.getMessage());
        }
    }



 }
