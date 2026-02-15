package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.paymentschedule.MonthSpecificPayment;
import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.enums.PaymentPlanType;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.repository.*;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.rem.backend.utility.Utility.monthlyPaymentSum;
import static com.rem.backend.utility.ValidationService.*;

@Service
@AllArgsConstructor
public class PaymentSchedulerService {

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final MonthWisePaymentRepo monthWisePaymentRepo;
    private final MonthSpecificPaymentRepo monthSpecificPaymentRepo;
    private final UnitRepo unitRepo;
    private final CustomerRepo customerRepo;


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

            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                validateMonthWisePayments(paymentSchedule.getMonthWisePaymentList(), paymentSchedule.getDurationInMonths(), paymentPlanType);
            } else if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                validateMonthSpecificPayments(paymentSchedule.getMonthSpecificPaymentList(), paymentSchedule.getDurationInMonths(), paymentPlanType);
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

            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                for (MonthWisePayment payment : paymentSchedule.getMonthWisePaymentList()) {
                    payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                    monthWisePaymentRepo.save(payment);
                }
            } else if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                for (MonthSpecificPayment payment : paymentSchedule.getMonthSpecificPaymentList()) {
                    payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                    monthSpecificPaymentRepo.save(payment);
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
            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                validateMonthWisePayments(paymentSchedule.getMonthWisePaymentList(), paymentSchedule.getDurationInMonths(), paymentPlanType);
            } else if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                validateMonthSpecificPayments(paymentSchedule.getMonthSpecificPaymentList(), paymentSchedule.getDurationInMonths(), paymentPlanType);
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

            // Handle MonthWisePayment updates (for INSTALLMENT_RANGE)
            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_RANGE)) {
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

                for (MonthWisePayment payment : updatedList) {
                    payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                    monthWisePaymentRepo.save(payment);
                }
            }

            // Handle MonthSpecificPayment updates (for INSTALLMENT_SPECIFIC)
            if (paymentPlanType.equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                List<MonthSpecificPayment> existingSpecificList = monthSpecificPaymentRepo.findByPaymentScheduleId(paymentScheduleSaved.getId());
                List<MonthSpecificPayment> updatedSpecificList = paymentSchedule.getMonthSpecificPaymentList();

                Set<Long> updatedSpecificIds = updatedSpecificList.stream()
                        .map(MonthSpecificPayment::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                List<MonthSpecificPayment> toDeleteSpecific = existingSpecificList.stream()
                        .filter(existing -> !updatedSpecificIds.contains(existing.getId()))
                        .collect(Collectors.toList());

                if (!toDeleteSpecific.isEmpty()) {
                    monthSpecificPaymentRepo.deleteAll(toDeleteSpecific);
                }

                for (MonthSpecificPayment payment : updatedSpecificList) {
                    payment.setPaymentScheduleId(paymentScheduleSaved.getId());
                    monthSpecificPaymentRepo.save(payment);
                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentScheduleRepository.save(paymentSchedule));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> getPaymentscheduleByUnitIdAndScheduleType(Map<String, String> request) {

        try {


            ValidationService.validate(request.get("id"), "unitId");
            ValidationService.validate(request.get("paymentScheduleType"), "paymentScheduleType");
            PaymentScheduleType type = PaymentScheduleType.valueOf(request.get("paymentScheduleType").toString());
            Optional<PaymentSchedule> paymentScheduleOptional = paymentScheduleRepository.
                    findByUnitIdAndPaymentScheduleTypeAndIsActiveTrue(Long.valueOf(request.get("id").toString()), type);

            PaymentSchedule paymentSchedule = null;
            if (paymentScheduleOptional.isPresent()) {
                paymentSchedule = paymentScheduleOptional.get();

                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                    List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
                } else {
                    paymentSchedule.setMonthWisePaymentList(Collections.emptyList());
                }
                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                    List<MonthSpecificPayment> monthSpecificPaymentList = monthSpecificPaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthSpecificPaymentList(monthSpecificPaymentList);
                } else {
                    paymentSchedule.setMonthSpecificPaymentList(Collections.emptyList());
                }
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, paymentSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> getPaymentscheduleByUnitId(long unitId) {

        Map<String, Object> response = new HashMap<>();
        try {


            ValidationService.validate(unitId, "unitId");

            Map unitOptional = unitRepo.findUnitDetails(unitId);
            response.put("unit", unitOptional);

            if (unitOptional == null)
                throw new IllegalArgumentException("Invalid Unit");

            if (Boolean.valueOf(unitOptional.get("booked").toString()) == true) {
                Optional<Customer> customerOptional = customerRepo.getCustomerByUnitId(unitId);
                if (customerOptional.isEmpty())
                    throw new IllegalArgumentException("Customer not found against this booking!");

                response.put("customerData", customerOptional.get());
            }





            Optional<PaymentSchedule> paymentScheduleOptionalBuilder = paymentScheduleRepository.
                    findByUnitIdAndPaymentScheduleTypeAndIsActiveTrue(unitId, PaymentScheduleType.BUILDER);

            if (paymentScheduleOptionalBuilder.isPresent()) {
                PaymentSchedule paymentSchedule = paymentScheduleOptionalBuilder.get();

                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                    List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
                }
                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                    List<MonthSpecificPayment> monthSpecificPaymentList = monthSpecificPaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthSpecificPaymentList(monthSpecificPaymentList);
                }
                response.put("builder", paymentSchedule);
            }

            Optional<PaymentSchedule> paymentScheduleOptionalCustomer = paymentScheduleRepository.
                    findByUnitIdAndPaymentScheduleTypeAndIsActiveTrue(unitId, PaymentScheduleType.CUSTOMER);

            if (paymentScheduleOptionalCustomer.isPresent()) {
                PaymentSchedule paymentSchedule = paymentScheduleOptionalCustomer.get();

                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                    List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
                }
                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                    List<MonthSpecificPayment> monthSpecificPaymentList = monthSpecificPaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthSpecificPaymentList(monthSpecificPaymentList);
                }
                response.put("customer", paymentSchedule);
            }


            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
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


                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                    List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
                }
                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                    List<MonthSpecificPayment> monthSpecificPaymentList = monthSpecificPaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthSpecificPaymentList(monthSpecificPaymentList);
                }
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
                    findByUnitIdAndPaymentScheduleTypeAndIsActiveTrue(Long.valueOf(request.get("id").toString()), type);

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
                    findByUnitIdAndPaymentScheduleTypeAndIsActiveTrue(Long.valueOf(unitId), type);

            PaymentSchedule paymentSchedule = null;
            if (paymentScheduleOptional.isPresent()) {
                paymentSchedule = paymentScheduleOptional.get();

                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_RANGE)) {
                    List<MonthWisePayment> monthWisePaymentList = monthWisePaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthWisePaymentList(monthWisePaymentList);
                }
                if (paymentSchedule.getPaymentPlanType().equals(PaymentPlanType.INSTALLMENT_SPECIFIC)) {
                    List<MonthSpecificPayment> monthSpecificPaymentList = monthSpecificPaymentRepo.findByPaymentScheduleId(paymentSchedule.getId());
                    paymentSchedule.setMonthSpecificPaymentList(monthSpecificPaymentList);
                }

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
