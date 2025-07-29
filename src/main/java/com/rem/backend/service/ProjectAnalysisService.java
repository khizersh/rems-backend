package com.rem.backend.service;

import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.project.Project;
import com.rem.backend.repository.BookingRepository;
import com.rem.backend.repository.CustomerAccountRepo;
import com.rem.backend.repository.ExpenseRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ProjectAnalysisService {

    private final ProjectService projectService;
    private final ExpenseRepo expenseRepo;
    private final CustomerAccountRepo customerAccountRepo;
    private final BookingRepository bookingRepository;

    public Map<String, Object> getProjectAnalyticsByid(long projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ValidationService.validate(projectId, "project id");
            Project project = projectService.getProjectDataById(projectId);

            if (project == null)
                throw new IllegalArgumentException("Invalid Project");

            List<Expense> expenseList = expenseRepo.findAllByProjectId(projectId);
            double totalSaleAmount = customerAccountRepo.getTotalAmountSaleByProjectId(projectId);
            double totalRecAmount = customerAccountRepo.getTotalAmountReceivedByProjectId(projectId);
            double totalProfit = 0;
            if (project.getTotalAmount() <= totalRecAmount)
                totalProfit = totalRecAmount - project.getTotalAmount();

            response.put("project", project);
            response.put("expenseList", expenseList);
            response.put("totalSaleAmount", totalSaleAmount);
            response.put("totalReceivedAmount", totalRecAmount);
            response.put("totalProfit", totalProfit);

            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getProjectSalesByid(long projectId) {
        try {
            ValidationService.validate(projectId, "project id");
            List<Map<String, Object>> list = bookingRepository.findMonthlyProjectSales(projectId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getProjectRecievedAmountByid(long projectId) {
        try {
            ValidationService.validate(projectId, "project id");
            List<Map<String, Object>> list = bookingRepository.findMonthlyProjectReceivedAmount(projectId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }



    public Map<String, Object> getProjectClientCountByid(long projectId) {
        try {
            ValidationService.validate(projectId, "project id");
            List<Map<String, Object>> list = bookingRepository.findMonthlyProjectClientCount(projectId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getProjectExpensePurchaseAndPaid(long projectId) {
        Map<String , Object> response = new HashMap<>();

        try {
            ValidationService.validate(projectId, "project id");
            List<Map<String, Object>> purchasedList = expenseRepo.findMonthlyProjectExpensePurchased(projectId);
            List<Map<String, Object>> paidList = expenseRepo.findMonthlyProjectExpensePaid(projectId);
            List<Map<String, Object>> creditList = expenseRepo.findMonthlyProjectExpenseCredit(projectId);

            response.put("purchase", purchasedList);
            response.put("paid", paidList);
            response.put("credit", creditList);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }




}
