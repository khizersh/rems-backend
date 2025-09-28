package com.rem.backend.service;

import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.enums.PaymentPlanType;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.repository.MonthWisePaymentRepo;
import com.rem.backend.repository.PaymentScheduleRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.rem.backend.utility.Utility.monthlyPaymentSum;
import static com.rem.backend.utility.ValidationService.validateMonthWisePayments;
import static com.rem.backend.utility.ValidationService.validatePaymentSchedule;

@Service
@AllArgsConstructor
public class PaymentSchedulerService {

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final MonthWisePaymentRepo monthWisePaymentRepo;

    public void deleteByUnitId(long unitID) {
        try {
            paymentScheduleRepository.deleteByUnit_Id(unitID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> createSchedule(PaymentSchedule paymentSchedule, PaymentPlanType paymentPlanType) {

        try {

            double totalAmount = paymentSchedule.getActualAmount() + paymentSchedule.getMiscellaneousAmount() + paymentSchedule.getDevelopmentAmount();
            paymentSchedule.setTotalAmount(totalAmount);
            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT)) {
                validateMonthWisePayments(paymentSchedule.getMonthWisePaymentList(), paymentSchedule.getDurationInMonths(), paymentPlanType);
            }
            validatePaymentSchedule(paymentSchedule);
            paymentSchedule.setPaymentPlanType(paymentPlanType);
            PaymentSchedule paymentScheduleSaved = paymentScheduleRepository.save(paymentSchedule);


            if (paymentPlanType.equals(PaymentPlanType.ONE_TIME_PAYMENT)) {
                paymentSchedule.setDownPayment(paymentSchedule.getTotalAmount());
                paymentSchedule.setDurationInMonths(0);
            }

            double monthlySum = monthlyPaymentSum(paymentSchedule);
            double collectedAmount = Math.ceil(paymentSchedule.getDownPayment() +
                    paymentSchedule.getOnPossessionPayment() + monthlySum);


            if (totalAmount != collectedAmount) {
                throw new IllegalArgumentException("Amounts not matched!");
            }

            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT)) {
                for (MonthWisePayment payment : paymentSchedule.getMonthWisePaymentList()) {
                    payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                    monthWisePaymentRepo.save(payment);
                }
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentScheduleSaved);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> updateSchedule(PaymentSchedule paymentSchedule, PaymentPlanType paymentPlanType) {

        try {

            double totalAmount = paymentSchedule.getActualAmount() + paymentSchedule.getMiscellaneousAmount() + paymentSchedule.getDevelopmentAmount();
            paymentSchedule.setTotalAmount(totalAmount);
            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT)) {
                validateMonthWisePayments(paymentSchedule.getMonthWisePaymentList(), paymentSchedule.getDurationInMonths(), paymentPlanType);
            }
            validatePaymentSchedule(paymentSchedule);
            paymentSchedule.setPaymentPlanType(paymentPlanType);
            PaymentSchedule paymentScheduleSaved = paymentScheduleRepository.save(paymentSchedule);


            if (paymentPlanType.equals(PaymentPlanType.ONE_TIME_PAYMENT)) {
                paymentSchedule.setDownPayment(paymentSchedule.getTotalAmount());
                paymentSchedule.setDurationInMonths(0);
            }

            double monthlySum = monthlyPaymentSum(paymentSchedule);
            double collectedAmount = Math.ceil(paymentSchedule.getDownPayment() +
                    paymentSchedule.getOnPossessionPayment() + monthlySum);


            if (totalAmount != collectedAmount) {
                throw new IllegalArgumentException("Amounts not matched!");
            }

            List<MonthWisePayment> existingList = monthWisePaymentRepo.findByPaymentScheduleId(paymentScheduleSaved.getId());

            List<MonthWisePayment> updatedList = paymentSchedule.getMonthWisePaymentList();

            Set<Long> updatedIds = updatedList.stream()
                    .map(MonthWisePayment::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<MonthWisePayment> toDelete = existingList.stream()
                    .filter(existing -> !updatedIds.contains(existing.getId()))
                    .collect(Collectors.toList());

            if (!toDelete.isEmpty()) {
                monthWisePaymentRepo.deleteAll(toDelete);
            }

            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT)) {
                for (MonthWisePayment payment : updatedList) {
                    payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                    monthWisePaymentRepo.save(payment);
                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentScheduleRepository.save(paymentSchedule));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> getPaymentscheduleByUnitId(Map<String, String> request) {

        try {


            ValidationService.validate(request.get("id"), "unitId");
            ValidationService.validate(request.get("paymentScheduleType"), "paymentScheduleType");
            PaymentScheduleType type = PaymentScheduleType.valueOf(request.get("paymentScheduleType").toString());
            Optional<PaymentSchedule> paymentScheduleOptional = paymentScheduleRepository.
                    findByUnitIdAndPaymentScheduleType(Long.valueOf(request.get("id").toString()), type);

            PaymentSchedule paymentSchedule = null;
            if (paymentScheduleOptional.isPresent()) {
                paymentSchedule = paymentScheduleOptional.get();
                List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> getPaymentScheduleByCustomerAccountId(Map<String, String> request) {

        try {


            ValidationService.validate(request.get("id"), "unitId");
            ValidationService.validate(request.get("paymentScheduleType"), "paymentScheduleType");
            PaymentScheduleType type = PaymentScheduleType.valueOf(request.get("paymentScheduleType").toString());
            PaymentSchedule paymentSchedule = paymentScheduleRepository.
                    findByCustomerAccountIdAndPaymentScheduleType(Long.valueOf(request.get("id").toString()), type);

            if (paymentSchedule != null) {
                List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> getUnpaidPaymentscheduleByUnitId(Map<String, String> request) {

        try {


            ValidationService.validate(request.get("id"), "unitId");
            ValidationService.validate(request.get("paymentScheduleType"), "paymentScheduleType");
            PaymentScheduleType type = PaymentScheduleType.valueOf(request.get("paymentScheduleType").toString());
            Optional<PaymentSchedule> paymentScheduleOptional = paymentScheduleRepository.
                    findByUnitIdAndPaymentScheduleType(Long.valueOf(request.get("id").toString()), type);

            PaymentSchedule paymentSchedule = null;
            if (paymentScheduleOptional.isPresent()) {
                paymentSchedule = paymentScheduleOptional.get();
                List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public PaymentSchedule getPaymentDetailsByUnitId(long unitId, PaymentScheduleType type) {

        try {


            ValidationService.validate(unitId, "unitId");
            ValidationService.validate(type, "paymentScheduleType");
            Optional<PaymentSchedule> paymentScheduleOptional = paymentScheduleRepository.
                    findByUnitIdAndPaymentScheduleType(Long.valueOf(unitId), type);

            PaymentSchedule paymentSchedule = null;
            if (paymentScheduleOptional.isPresent()) {
                paymentSchedule = paymentScheduleOptional.get();
                List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
            }


            return paymentSchedule;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


}
