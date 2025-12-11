package com.rem.backend.controller;

import com.rem.backend.dto.customerpayable.CustomerPayableDetailListDto;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.service.CustomerPayableService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/booking/")
@AllArgsConstructor
public class CustomerPayableController {

    private final CustomerPayableService customerPayableService;

    @PostMapping("/{customerPayableId}/addPaymentDetails")
    public ResponseEntity<?> addPaymentDetail(@PathVariable long customerPayableId,
                                              @RequestBody CustomerPayableDetailListDto customerPayableDetailDto,
                                              HttpServletRequest request){
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        Map customerPayableDto = customerPayableService.addPaymentDetail(customerPayableId,
                customerPayableDetailDto, loggedInUser);
        return ResponseEntity.ok(customerPayableDto);
    }

    @GetMapping("/{bookingId}/{unitId}/customerPayable")
    public ResponseEntity<?> getCustomerPayable(@PathVariable long bookingId,
                                                @PathVariable long unitId,
                                                HttpServletRequest request) {

        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);

        Map<String, Object> response =
                customerPayableService.getCustomerPayable(bookingId, unitId);

        return ResponseEntity.ok(response);
    }

}
