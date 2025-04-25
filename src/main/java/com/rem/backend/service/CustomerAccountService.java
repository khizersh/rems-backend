package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.repository.CustomerAccountRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomerAccountService {

    private final CustomerAccountRepo customerAccountRepo;

    @Autowired
    public CustomerAccountService(CustomerAccountRepo customerAccountRepo) {
        this.customerAccountRepo = customerAccountRepo;
    }

    public Page<CustomerAccount> getByProjectId(Long projectId, Pageable pageable) {
        ValidationService.validate(projectId, "Project ID");
        return customerAccountRepo.findByProject_ProjectId(projectId, pageable);
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

    public Page<CustomerAccount> getByCustomerId(Long customerId, Pageable pageable) {
        ValidationService.validate(customerId, "Customer ID");
        return customerAccountRepo.findByCustomer_CustomerId(customerId, pageable);
    }

    public Page<CustomerAccount> getByUnitId(Long unitId, Pageable pageable) {
        ValidationService.validate(unitId, "Unit ID");
        return customerAccountRepo.findByUnit_Id(unitId, pageable);
    }

    public Page<CustomerAccount> getByOrganizationId(Long organizationId, Pageable pageable) {
        return customerAccountRepo.findByProject_OrganizationId(organizationId, pageable);
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
                    customers = customerAccountRepo.findByProject_OrganizationId(id, pageable);
                    break;
                case "project":
                    customers = customerAccountRepo.findByProject_OrganizationId(id, pageable);
                    break;
                case "floor":
                    customers = customerAccountRepo.findByProject_OrganizationId(id, pageable);
                    break;
                default:
                    customers = customerAccountRepo.findByProject_OrganizationId(id, pageable);
            }

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
