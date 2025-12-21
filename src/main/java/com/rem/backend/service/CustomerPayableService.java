package com.rem.backend.service;


import com.rem.backend.dto.customerpayable.CustomerPayableDetailListDto;
import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.dto.customerpayable.CustomerPayableFeeDetailListDto;
import com.rem.backend.entity.customerpayable.CustomerPayable;
import com.rem.backend.entity.customerpayable.CustomerPayableDetail;
import com.rem.backend.entity.organization.OrganizationAccountDetail;
import com.rem.backend.enums.CustomerPayableStatus;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.repository.CustomerPayableDetailRepository;
import com.rem.backend.repository.CustomerPayableRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class CustomerPayableService {

    private final CustomerPayableRepository customerPayableRepository;
    private final CustomerPayableDetailRepository customerPayableDetailRepository;
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

        } catch (Exception e){
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

        } catch (Exception e){
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

           if (balance - totalSumOfAmount < 0  )
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

                organizationAccountService.deductFromOrgAcct(organizationAccountDetail,loggedInUser);

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

            return ResponseMapper.buildResponse(Responses.SUCCESS,CustomerPayableDto.map(customerPayable, dtoList,feeDto));

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
