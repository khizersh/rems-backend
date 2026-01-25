package com.rem.backend.controller;

import com.rem.backend.dto.commonRequest.FilterPaginationRequest;
import com.rem.backend.entity.expense.Expense;
import com.rem.backend.entity.expense.ExpenseDetail;
import com.rem.backend.entity.expense.ExpenseType;
import com.rem.backend.service.ExpenseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@AllArgsConstructor
@RequestMapping("/api/expense/")
public class ExpenseController {


    private final ExpenseService expenseService;

    @PostMapping("/addExpense")
    public Map addExpense(@RequestBody Expense expense , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return expenseService.addExpense(expense , loggedInUser);
    }

    @PostMapping("/addExpenseType")
    public Map addExpenseType(@RequestBody ExpenseType expense , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return expenseService.addExpenseType(expense , loggedInUser);
    }

    @PostMapping("/updateExpenseType")
    public Map updateExpenseType(@RequestBody ExpenseType expense , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return expenseService.updateExpenseType(expense , loggedInUser);
    }

    @PostMapping("/updateExpense")
    public Map updateExpense(@RequestBody Expense expense , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return expenseService.updateExpense(expense , loggedInUser);
    }


    @GetMapping("/deleteById/{expenseId}")
    public Map updateExpense(@PathVariable long expenseId , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return expenseService.deleteExpense(expenseId , loggedInUser);
    }


    @PostMapping("/addExpenseDetail")
    public Map addExpenseDetail(@RequestBody ExpenseDetail expense , HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return expenseService.addExpenseDetail(expense , loggedInUser);
    }

    @PostMapping("/getAllExpenseTypeByOrgId")
    public Map getAllExpenseTypeByOrgId(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return expenseService.getAllExpenseType(request.getId() , pageable);
    }


    @GetMapping("/getAllExpenseTypeByOrgId/{orgId}")
    public Map getAllExpenseTypeByOrgIdList(@PathVariable long orgId) {
        return expenseService.getAllExpenseType(orgId);
    }


    @GetMapping("/getExpenseTypeById/{orgId}")
    public Map getExpenseTypeById(@PathVariable long orgId){
        return expenseService.getExpenseTypeById(orgId);
    }

    @PostMapping("/getAllExpensesByIds")
    public ResponseEntity<?> getExpensesByIds(@RequestBody FilterPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        Map<String , Object> expensePage = expenseService.getExpenseList(request.getId(), request.getId2(), request.getFilteredBy(), pageable);
        return ResponseEntity.ok(expensePage);
    }

    @GetMapping("/getExpenseDetailByExpenseId/{expenseId}")
    public ResponseEntity<?> getExpensesByIds(@PathVariable long expenseId) {
        Map<String , Object> expensePage = expenseService.getExpenseDetails(expenseId);
        return ResponseEntity.ok(expensePage);
    }


    @GetMapping("/geExpenseById/{expenseId}")
    public ResponseEntity<?> geExpenseById(@PathVariable long expenseId) {
        Map<String , Object> expensePage = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expensePage);
    }

}
