package com.rem.backend.service;

import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.repository.MonthWisePaymentRepo;
import com.rem.backend.repository.PaymentScheduleRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.rem.backend.utility.ValidationService.validateMonthWisePayments;
import static com.rem.backend.utility.ValidationService.validatePaymentSchedule;

@Service
@AllArgsConstructor
public class PaymentSchedulerService {

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final MonthWisePaymentRepo monthWisePaymentRepo;

    public Map<String, Object> createSchedule(PaymentSchedule paymentSchedule) {

        try {
            validatePaymentSchedule(paymentSchedule);
            validateMonthWisePayments(paymentSchedule.getMonthWisePaymentList(), paymentSchedule.getDurationInMonths());
            PaymentSchedule paymentScheduleSaved  = paymentScheduleRepository.save(paymentSchedule);

            for (MonthWisePayment payment : paymentSchedule.getMonthWisePaymentList()) {
                payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                monthWisePaymentRepo.save(payment);
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentScheduleRepository.save(paymentSchedule));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }



}
