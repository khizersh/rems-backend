package com.rem.backend.service;


import com.rem.backend.accountmanagement.service.OrganizationAccountService;
import com.rem.backend.dto.booking.BookingCancellationRequest;
import com.rem.backend.dto.customerpayable.CustomerPayableDetailListDto;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.dto.customerpayable.CustomerPayableFeeDetailListDto;
import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.entity.customerpayable.CustomerPayableDetail;
import com.rem.backend.entity.customerpayable.CustomerPayableFeeDetail;
import com.rem.backend.accountmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.enums.CustomerPayableStatus;
import com.rem.backend.enums.FeeType;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.repository.CustomerPayableDetailRepository;
import com.rem.backend.repository.CustomerPayableFeeDetailRepo;
import com.rem.backend.repository.CustomerPayableRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.Utility;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CustomerPayableService {

    private final CustomerPayableRepository customerPayableRepository;
    private final CustomerPayableDetailRepository customerPayableDetailRepository;
    private final CustomerPayableFeeDetailRepo customerPayableFeeDetailRepo;
    private final OrganizationAccountService organizationAccountService;


    public Map<String, Object> getCustomerPayable(long bookingId, long unitId) {

        try {
            Optional<CustomerPayable> optional =
                    customerPayableRepository.findWithDetails(bookingId, unitId);

            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(
                        Responses.SYSTEM_FAILURE,
                        "Customer payable not found."
                );
            }

            CustomerPayable cp = optional.get();

            CustomerPayableDetailListDto detailDto =
                    CustomerPayableDetailListDto.fromEntityList(cp.getDetails());

            CustomerPayableFeeDetailListDto feeDto =
                    CustomerPayableFeeDetailListDto.fromEntityList(cp.getFeeDetails());

            CustomerPayableDto dto = CustomerPayableDto.map(cp, detailDto, feeDto);

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    "Customer payable not found. System Exception"
            );
        }
    }


    public Map<String, Object> getCustomerPayableById(long customerPayableId) {

        try {
            Optional<CustomerPayable> optional =
                    customerPayableRepository.findById(customerPayableId);

            if (optional.isEmpty()) {
                return ResponseMapper.buildResponse(
                        Responses.SYSTEM_FAILURE,
                        "Customer payable not found."
                );
            }

            CustomerPayable cp = optional.get();

            CustomerPayableDetailListDto detailDto =
                    CustomerPayableDetailListDto.fromEntityList(cp.getDetails());

            CustomerPayableFeeDetailListDto feeDto =
                    CustomerPayableFeeDetailListDto.fromEntityList(cp.getFeeDetails());

            CustomerPayableDto dto = CustomerPayableDto.map(cp, detailDto, feeDto);

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(
                    Responses.SYSTEM_FAILURE,
                    "Customer payable not found. System Exception"
            );
        }
    }


    @Transactional
    public Map<String, Object> addPaymentDetail(
            long customerPayableId,
            CustomerPayableDetailListDto dtoList,
            String loggedInUser) {

        try {
            CustomerPayable customerPayable = customerPayableRepository.findById(customerPayableId)
                    .orElseThrow(() -> new Exception("Customer Payable doesn't exist or is already cancelled"));

            if (customerPayable.getBalanceAmount() <= 0)
                throw new RuntimeException("This booking is already paid!.");


            double totalSumOfAmount = dtoList.getDetails().stream().
                    mapToDouble(detail -> detail.getAmount()).sum();


            double totalPaid = customerPayable.getTotalPaid();

            double balance = customerPayable.getBalanceAmount();

            if (balance - totalSumOfAmount < 0)
                throw new RuntimeException("Amount exceed balance amount!");

            for (CustomerPayableDetailListDto.Detail d : dtoList.getDetails()) {

                CustomerPayableDetail detail = new CustomerPayableDetail();
                detail.setCustomerPayable(customerPayable);
                detail.setPaymentType(d.getPaymentType());
                detail.setAmount(d.getAmount());
                detail.setChequeNo(d.getChequeNo());
                detail.setChequeDate(d.getChequeDate());
                detail.setCreatedBy(loggedInUser);
                detail.setUpdatedBy(loggedInUser);

                OrganizationAccountDetail organizationAccountDetail = getOrganizationAccountDetail(d, customerPayable);

                organizationAccountService.deductFromOrgAcct(organizationAccountDetail, loggedInUser);

                customerPayableDetailRepository.save(detail);

                totalPaid = totalPaid + d.getAmount();
                balance = balance - d.getAmount();
            }


            customerPayable.setTotalPaid(totalPaid);
            customerPayable.setBalanceAmount(balance);

            if (customerPayable.getBalanceAmount() == 0) {
                customerPayable.setStatus(String.valueOf(CustomerPayableStatus.PAID));
            } else {
                customerPayable.setStatus(String.valueOf(CustomerPayableStatus.UNPAID));
            }

            customerPayableRepository.save(customerPayable);

            CustomerPayableFeeDetailListDto feeDto =
                    CustomerPayableFeeDetailListDto.fromEntityList(customerPayable.getFeeDetails());

            return ResponseMapper.buildResponse(Responses.SUCCESS, CustomerPayableDto.map(customerPayable, dtoList, feeDto));

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> editFeesDetail(long feesId, BookingCancellationRequest.CustomerPayableFeesDto customerPayableFeeDetailDto,
                                              String loggedInUser) {

        try {

            Optional<CustomerPayableFeeDetail> feeDetail = customerPayableFeeDetailRepo.findById(feesId);

            if (feeDetail.isEmpty()) {
                return ResponseMapper.buildResponse(
                        Responses.SYSTEM_FAILURE,
                        "Customer payable fee detail not found."
                );
            }

            CustomerPayableFeeDetail customerPayableFeeDetail = feeDetail.get();
            CustomerPayable customerPayable = customerPayableFeeDetail.getCustomerPayable();

            if (customerPayable.getTotalPaid() > 0) {
                return ResponseMapper.buildResponse(
                        Responses.SYSTEM_FAILURE,
                        "Customer payable cannot be edited, payment already made."
                );
            }

            customerPayable.setTotalDeductions(customerPayable.getTotalDeductions()
                    - customerPayableFeeDetail.getCalculatedAmount());

            double calculatedAmount = Utility.calculateFee(customerPayable.getTotalRefund(),
                    customerPayableFeeDetailDto);

            customerPayable.setTotalDeductions(customerPayable.getTotalDeductions()
                    + calculatedAmount);

            customerPayable.setTotalPayable(customerPayable.getTotalRefund()
                    - customerPayable.getTotalDeductions());

            customerPayable.setBalanceAmount(customerPayable.getTotalPayable());

            customerPayableFeeDetail.setTitle(customerPayableFeeDetailDto.getTitle());
            customerPayableFeeDetail.setType(customerPayableFeeDetailDto.getType());
            customerPayableFeeDetail.setInputValue(customerPayableFeeDetailDto.getValue());
            customerPayableFeeDetail.setCalculatedAmount(calculatedAmount);
            customerPayableFeeDetail.setUpdatedBy(loggedInUser);

            customerPayableRepository.save(customerPayable);
            customerPayableFeeDetailRepo.save(customerPayableFeeDetail);


            CustomerPayableDetailListDto detailDto =
                    CustomerPayableDetailListDto.fromEntityList(customerPayable.getDetails());

            CustomerPayableFeeDetailListDto feeDto =
                    CustomerPayableFeeDetailListDto.fromEntityList(customerPayable.getFeeDetails());

            CustomerPayableDto dto = CustomerPayableDto.map(customerPayable, detailDto, feeDto);

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    @Transactional
    public Map<String, Object> editCustomerPayable(BookingCancellationRequest request, String loggedInUser) {

        try {
            ValidationService.validate(request.getCustomerPayableId(), "Invalid Customer Payable!");

            CustomerPayable customerPayable =
                    customerPayableRepository.findById(request.getCustomerPayableId())
                            .orElseThrow(() -> new RuntimeException("CustomerPayable not found"));

            // 1️⃣ Update parent fields
            customerPayable.setReason(request.getReason());
            customerPayable.setUpdatedBy(loggedInUser);

            // 2️⃣ Existing feeDetails indexed by ID
            Map<Long, CustomerPayableFeeDetail> existingMap =
                    customerPayable.getFeeDetails().stream()
                            .filter(f -> f.getId() != null)
                            .collect(Collectors.toMap(
                                    CustomerPayableFeeDetail::getId,
                                    Function.identity()
                            ));

            // 3️⃣ Clear list BUT keep same reference
            customerPayable.getFeeDetails().clear();

            double totalDeductions = 0;
            double totalRefund = 0;

            // 4️⃣ Process incoming fees
            for (BookingCancellationRequest.CustomerPayableFeesDto dto : request.getFees()) {
                ValidationService.validate(dto.getType(), "Invalid Fee Type!");
                ValidationService.validate(dto.getValue(), "Invalid Value!");

                CustomerPayableFeeDetail fee;

                // Update existing
                if (dto.getId() != null && existingMap.containsKey(dto.getId())) {
                    fee = existingMap.get(dto.getId());
                }
                // Add new
                else {
                    fee = new CustomerPayableFeeDetail();
                    fee.setCustomerPayable(customerPayable);
                    fee.setCreatedBy(loggedInUser);
                }

                fee.setType(dto.getType());
                fee.setTitle(dto.getTitle());
                fee.setInputValue(dto.getValue());

                // Calculate amount
                double calculatedAmount;
                if (dto.getType() == FeeType.FIXED) {
                    calculatedAmount = dto.getValue();
                } else {
                    calculatedAmount =
                            (customerPayable.getTotalPayable() / 100) * dto.getValue();
                }

                fee.setCalculatedAmount(calculatedAmount);

                totalDeductions += calculatedAmount;

                fee.setUpdatedBy(loggedInUser);
                customerPayable.getFeeDetails().add(fee);
            }

            // 5️⃣ Update totals
            customerPayable.setTotalRefund(customerPayable.getTotalPayable() - totalDeductions);
            customerPayable.setBalanceAmount(customerPayable.getTotalRefund() - customerPayable.getTotalPaid());
            customerPayable.setTotalDeductions(totalDeductions);

            if (customerPayable.getTotalPaid() > customerPayable.getTotalRefund()) {
                throw new IllegalArgumentException("Refund amount is less than paid amount!");
            }

            // 6️⃣ Save (cascade handles children)
            customerPayableRepository.save(customerPayable);

            // 7️⃣ Return updated response
            CustomerPayableDetailListDto detailDto =
                    CustomerPayableDetailListDto.fromEntityList(customerPayable.getDetails());

            CustomerPayableFeeDetailListDto feeDto =
                    CustomerPayableFeeDetailListDto.fromEntityList(customerPayable.getFeeDetails());

            CustomerPayableDto dto =
                    CustomerPayableDto.map(customerPayable, detailDto, feeDto);

            return ResponseMapper.buildResponse(Responses.SUCCESS, dto);

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    private static OrganizationAccountDetail getOrganizationAccountDetail(CustomerPayableDetailListDto.Detail d,
                                                                          CustomerPayable customerPayable) {
        OrganizationAccountDetail organizationAccountDetail = new OrganizationAccountDetail();
        organizationAccountDetail.setOrganizationAcctId(d.getOrganizationAccountId());
        organizationAccountDetail.setCustomerPaymentDetailId(d.getCustomerPayableId());
        organizationAccountDetail.setAmount(d.getAmount());
        organizationAccountDetail.setTransactionType(TransactionType.CREDIT);
        organizationAccountDetail.setCustomerId(customerPayable.getCustomer().getCustomerId());
        organizationAccountDetail.setComments(d.getComments());
        return organizationAccountDetail;
    }
}
