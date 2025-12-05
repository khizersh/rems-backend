package com.rem.backend.controller;


import com.rem.backend.service.PaymentSchedulerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paymentSchedule/")
@AllArgsConstructor
public class PaymentScheduleController {

    private final PaymentSchedulerService paymentSchedulerService;

    @PostMapping("/getByUnit")
    public Map getPaymentScheduleByUnit(@RequestBody Map<String , String> request){
        return paymentSchedulerService.getPaymentscheduleByUnitIdAndScheduleType(request);
    }


    @GetMapping("/getByUnit/{unitId}")
    public Map getPaymentScheduleByUnit(@PathVariable long unitId){
        return paymentSchedulerService.getPaymentscheduleByUnitId(unitId);
    }


    @PostMapping("/getByCustomerAccount")
    public Map getPaymentScheduleByCustomerAccount(@RequestBody Map<String , String> request){
        return paymentSchedulerService.getPaymentScheduleByCustomerAccountId(request);
    }

}
