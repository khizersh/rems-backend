package com.rem.backend.service;

import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.repository.CustomerAccountRepo;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CustomerAccountService {

    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerRepo customerRepo;
    private final FloorRepo floorRepo;



    public Page<CustomerAccount> getByProjectId(Long projectId, Pageable pageable) {
        ValidationService.validate(projectId, "Project ID");
        return customerAccountRepo.findByProject_ProjectIdAndIsActiveTrue(projectId, pageable);
    }


    public Map<String , Object> getById(Long accountId) {
        try{
        ValidationService.validate(accountId, "Account");
        Optional<CustomerAccount>  customerAccount = customerAccountRepo.findById(accountId);
        if (customerAccount.isEmpty())
            throw new IllegalArgumentException("Invalid Account");

            return ResponseMapper.buildResponse(Responses.SUCCESS , customerAccount.get());
        }catch (IllegalArgumentException e){
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER , e.getMessage());
        }catch (Exception e){
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE , e.getMessage());
        }

    }

    public Map<String, Object> getNameIdByOrganizationId(Long organizationId) {
        try {
            ValidationService.validate(organizationId, "Organization ID");

            return ResponseMapper.buildResponse(Responses.SUCCESS, customerAccountRepo.findNameIdOrganization(organizationId));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }


    public Map<String, Object> getByCustomerId(long customerId, Pageable pageable) {
        try {


            ValidationService.validate(customerId, "customer ID");

            Page<CustomerAccount> customerAccountOptional = customerAccountRepo.findByCustomer_CustomerIdAndIsActiveTrue(customerId , pageable);
            if(customerAccountOptional.isEmpty()){
                throw new IllegalArgumentException("Customer Account Not Found!");
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, customerAccountOptional);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

    public Map<String, Object> getNameIdByProjectId(Long projectId) {
        try {
            ValidationService.validate(projectId, "Project ID");

            return ResponseMapper.buildResponse(Responses.SUCCESS, customerAccountRepo.findNameIdProject(projectId));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }

    }

//    public Map<String , Object> getByCustomerId(Long customerId) {
//        ValidationService.validate(customerId, "Customer ID");
//        return  ResponseMapper.buildResponse(Responses.SUCCESS , customerAccountRepo.findByCustomer_CustomerId(customerId));
//    }

    public Page<CustomerAccount> getByUnitId(Long unitId, Pageable pageable) {
        ValidationService.validate(unitId, "Unit ID");
        return customerAccountRepo.findByUnit_IdAndIsActiveTrue(unitId, pageable);
    }

    public Page<CustomerAccount> getByOrganizationId(Long organizationId, Pageable pageable) {
        return customerAccountRepo.findByProject_OrganizationIdAndIsActiveTrue(organizationId, pageable);
    }


    public Page<CustomerAccount> getAllOrderedByCreatedDateDesc(Pageable pageable) {
        return customerAccountRepo.findAllByOrderByCreatedDateDesc(pageable);
    }


    public Map<String, Object> getCustomerAccountsByIds(long id, String filteredBy, Pageable pageable) {
        try {
            Page<CustomerAccount> customers = null;
            ValidationService.validate(id, "id");
            switch (filteredBy) {
                case "organization":
                    customers = customerAccountRepo.findByProject_OrganizationIdAndIsActiveTrue(id, pageable);
                    break;
                case "project":
                    customers = customerAccountRepo.findByProject_ProjectIdAndIsActiveTrue(id, pageable);
                    break;
                case "floor":
                    customers = customerAccountRepo.findByUnit_FloorIdAndIsActiveTrue(id, pageable);
                    break;
                default:
                    customers = customerAccountRepo.findByProject_OrganizationIdAndIsActiveTrue(id, pageable);
            }

            customers.getContent().forEach(customerAccount -> {

               String floorNo = floorRepo.findFloorNoById(customerAccount.getUnit().getFloorId());

               customerAccount.getUnit().setFloorNo(Integer.valueOf(floorNo));

//                double grandTotal = customerPaymentDetails.stream()
//                        .mapToDouble(CustomerPaymentDetail::getAmount)
//                        .sum();
//                double totalAmount = customerAccount.get().getTotalAmount();
//                double balanceAmount = totalAmount - grandTotal;
//
//                response.put("totalAmount", totalAmount);
//                response.put("grandTotal", grandTotal);
//                response.put("balanceAmount", balanceAmount);
//                customerAccount.setTotalPaidAmount();
//                customerAccount.setTotalBalanceAmount();
            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, customers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getCustomerAccountsNameIdByIds(long id, String filteredBy) {
        try {

            Map<String, Object> customers = null;
            ValidationService.validate(id, "id");
            switch (filteredBy) {
                case "organization":
                    customers = getNameIdByOrganizationId(id);
                    break;
                case "project":
                    customers = getNameIdByProjectId(id);
                    break;
                default:
                    customers = getNameIdByOrganizationId(id);
            }

            return customers;
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

}
