package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.enums.RoleType;
import com.rem.backend.repository.*;
import com.rem.backend.usermanagement.entity.Role;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.usermanagement.repository.UserRoleRepository;
import com.rem.backend.usermanagement.service.RoleService;
import com.rem.backend.utility.PasswordGenerator;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final UserRepo userRepo;
    private final UserRoleRepository userRoleRepo;
    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;
    private final UnitRepo unitRepo;
    private final EmailService emailService;
    private final CustomerPaymentService customerPaymentService;
    private final RoleService roleService;

    public Map<String, Object> getCustomerById(long id) {
        try {
            ValidationService.validate(id, "id");
            Optional<Customer> customerOptional = customerRepo.findById(id);


            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
                customer.setProjectName(projectRepo.findProjectNameById(customer.getProjectId()));
                customer.setFloorNo(floorRepo.findFloorNoById(customer.getFloorId()));
                customer.setUnitSerialNo(unitRepo.findUnitSerialById(customer.getUnitId()));
                return ResponseMapper.buildResponse(Responses.SUCCESS, customerOptional.get());

            }
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getFullDetailByCustomerId(Map<String, String> request) {
        try {
            String customerId = request.get("customerId");
            String customerPaymentId = request.get("customerPaymentId");
            ValidationService.validate(customerId, "customerId");
            ValidationService.validate(customerPaymentId, "customerPaymentId");
            Map<String, Object> response = customerPaymentService.getPaymentDetailsByPaymentIdOnlyData(Long.valueOf(customerPaymentId));
            if (response == null)
                response = new HashMap<>();
            Map<String, Object> customer = customerRepo.getAllDetailsByCustomerId(Long.valueOf(customerId));

            response.put("customer", customer);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Map<String, Object> getFullDetailByCustomer(long customerId) {
        try {

            ValidationService.validate(customerId, "customerId");

            Map<String, Object> response = customerRepo.getAllDetailsByCustomerId(Long.valueOf(customerId));
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getCustomerByIds(long id, String filteredBy, Pageable pageable) {
        try {
            Page<Customer> customers = null;
            ValidationService.validate(id, "id");
            switch (filteredBy) {
                case "organization":
                    customers = customerRepo.findByOrganizationId(id, pageable);
                    break;
                case "project":
                    customers = customerRepo.findByProjectId(id, pageable);
                    break;
                case "floor":
                    customers = customerRepo.findByFloorId(id, pageable);
                    break;
                case "unit":
                    customers = customerRepo.findByUnitId(id, pageable);
                    break;
                default:
                    customers = customerRepo.findByOrganizationId(id, pageable);
            }

            customers.getContent().forEach(customer -> {
                customer.setProjectName(projectRepo.findProjectNameById(customer.getProjectId()));
                customer.setFloorNo(floorRepo.findFloorNoById(customer.getFloorId()));
                customer.setUnitSerialNo(unitRepo.findUnitSerialById(customer.getUnitId()));
            });

            return ResponseMapper.buildResponse(Responses.SUCCESS, customers);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> createCustomer(Customer customer, String loggedInUser) {
        try {
            ValidationService.validate(customer.getName(), "Customer name");
            ValidationService.validate(customer.getNationalId(), "National ID");
//            ValidationService.validate(customer.getNextOFKinName(), "Next of kin name");
//            ValidationService.validate(customer.getNextOFKinNationalId(), "Next of kin National ID");
//            ValidationService.validate(customer.getRelationShipWithKin(), "Relation with kin");
            ValidationService.validate(customer.getOrganizationId(), "Organization ID");
//            ValidationService.validate(customer.getGuardianName(), "Guardian Name");
            ValidationService.validate(customer.getProjectId(), "Project ID");
            ValidationService.validate(customer.getFloorId(), "Floor ID");
            ValidationService.validate(customer.getUnitId(), "Unit ID");
            ValidationService.validate(customer.getCreatedBy(), "Created By");
            ValidationService.validate(customer.getUpdatedBy(), "Updated By");
            ValidationService.validate(customer.getContactNo(), "Contact No");

            boolean unitAlreadyAssigned = customerRepo.existsByUnitId(customer.getUnitId());
            if (unitAlreadyAssigned) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "This unit is already assigned to another customer.");
            }


            if (customer.getUserId() != null) {
                Optional<User> userOptional = userRepo.findById(customer.getUserId());
                if (userOptional.isEmpty())
                    return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid user selected!");
            } else {
                User user = new User();
                if (!ValidationService.isValidEmail(customer.getEmail())) {
                    throw new IllegalArgumentException("Invalid Email!");
                }
                user.setActive(true);
                user.setEmail(customer.getEmail());
                user.setPassword(PasswordGenerator.generateRandomPassword());
                user.setUsername(customer.getUsername());
                user.setEmail(customer.getEmail());
                user.setOrganizationId(customer.getOrganizationId());
                user.setCreatedBy(loggedInUser);
                user.setUpdatedBy(loggedInUser);
                User userSaved = userRepo.save(user);

                if (user.getEmail() != null || !user.getEmail().isBlank())
                    emailService.sendEmailAsync(user.getEmail(), user.getUsername(), user.getPassword());

                customer.setUserId(userSaved.getId());

                UserRoles roles = new UserRoles();
                Role role = roleService.getRoleByName(RoleType.USER_ROLE.toString());
                roles.setRoleId(role.getId());
                roles.setUserId(userSaved.getId());
                roles.setCreatedBy(loggedInUser);
                roles.setUpdatedBy(loggedInUser);
                userRoleRepo.save(roles);

            }
            // Save customer
            customer.setCreatedBy(loggedInUser);
            customer.setUpdatedBy(loggedInUser);

            Customer savedCustomer = customerRepo.save(customer);

            return ResponseMapper.buildResponse(Responses.SUCCESS, savedCustomer);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> updateCustomer(Customer customer, String loggedInUser) {
        try {
            ValidationService.validate(customer.getName(), "Customer name");
            ValidationService.validate(customer.getNationalId(), "National ID");
            ValidationService.validate(customer.getNextOFKinName(), "Next of kin name");
            ValidationService.validate(customer.getNextOFKinNationalId(), "Next of kin National ID");
            ValidationService.validate(customer.getRelationShipWithKin(), "Relation with kin");
            ValidationService.validate(customer.getOrganizationId(), "Organization ID");
            ValidationService.validate(customer.getProjectId(), "Project ID");
            ValidationService.validate(customer.getFloorId(), "Floor ID");
            ValidationService.validate(customer.getUnitId(), "Unit ID");
            ValidationService.validate(loggedInUser, "Updated By");
            ValidationService.validate(customer.getContactNo(), "Contact No");
            ValidationService.validate(customer.getGuardianName(), "Guardian Name");


            customer.setUpdatedBy(loggedInUser);
            Customer savedCustomer = customerRepo.save(customer);
            return ResponseMapper.buildResponse(Responses.SUCCESS, savedCustomer);

        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> searchCustomersByName(Map<String, String> request) {
        try {

            String name = "";
            if (request.containsKey("name")) {
                name = request.get("name").toString();
            }
            List<Map<String, Object>> customers;
            if (name != null && !name.trim().isEmpty()) {
                customers = customerRepo.searchByName(name);
            } else {
                customers = customerRepo.findTop20ByOrderByCreatedDateDesc();
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, customers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
