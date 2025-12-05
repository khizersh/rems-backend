package com.rem.backend.controller;

import com.rem.backend.dto.customerpayable.CustomerPayableDetailListDto;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.service.CustomerPayableService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking/")
@AllArgsConstructor
public class CustomerPayableController {

    private final CustomerPayableService customerPayableService;

    @PostMapping("/{customerPayableId}/addPaymentDetails")
    public ResponseEntity<?> addPaymentDetail(@PathVariable long customerPayableId, @RequestBody CustomerPayableDetailListDto customerPayableDetailDto){
        CustomerPayableDto customerPayableDto = customerPayableService.addPaymentDetail(customerPayableId, customerPayableDetailDto);
        return ResponseEntity.ok(customerPayableDto);
    }
}
