package com.rem.backend.service;

import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.customer.CustomerAccount;
import com.rem.backend.entity.customer.CustomerPayment;
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
import java.util.stream.Collectors;

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
    private final BookingRepository bookingRepository;
    private final CustomerAccountRepo customerAccountRepo;
    private final CustomerPaymentRepo customerPaymentRepo;

    public Map<String, Object> getCustomerById(long id) {
        try {
            ValidationService.validate(id, "id");
            Optional<Customer> customerOptional = customerRepo.findById(id);


            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
//                customer.setProjectName(projectRepo.findProjectNameById(customer.getProjectId()));
//                customer.setFloorNo(floorRepo.findFloorNoById(customer.getFloorId()));
//                customer.setUnitSerialNo(unitRepo.findUnitSerialById(customer.getUnitId()));
                return ResponseMapper.buildResponse(Responses.SUCCESS, customer);

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

            Optional<CustomerPayment> customerPaymentOptional = customerPaymentRepo.findById(Long.valueOf(customerPaymentId));
            Map<String, Object> response = customerPaymentService.getPaymentDetailsByPaymentIdOnlyData(Long.valueOf(customerPaymentId) , customerPaymentOptional.get());
            if (response == null)
                response = new HashMap<>();

            Optional<CustomerAccount> customerAccount =
                    customerAccountRepo.findById(customerPaymentOptional.get().getCustomerAccountId());

            Map<String, Object> customer = customerRepo.getAllDetailsByCustomerId(Long.valueOf(customerId) ,
                    customerAccount.get().getUnit().getId());

            response.put("customer", customer);
            return ResponseMapper.buildResponse(Responses.SUCCESS, response);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }



    public Map<String, Object> getFullDetailByCustomerAccountId(long customerAccountId) {

        Map<String, Object> response = new HashMap<>();
        try {
            ValidationService.validate(customerAccountId, "customerAccountId");




            Optional<CustomerAccount> customerAccount =
                    customerAccountRepo.findById(customerAccountId);

            if (customerAccount.isEmpty())
                throw new IllegalArgumentException("Invalid Account");

            Map<String, Object> customer = customerRepo.getAllDetailsByCustomerId(customerAccount.get().getCustomer().getCustomerId() ,
                    customerAccount.get().getUnit().getId());

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

            Map<String, Object> response = customerRepo.getAllDetailsByCustomerId(Long.valueOf(customerId) , 1);
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
                    customers = bookingRepository.findCustomersByProjectId(id, pageable);
                    break;
                case "floor":
                    customers = bookingRepository.findCustomersByUnitId(id, pageable);
                    break;
                case "unit":
                    customers = bookingRepository.findCustomersByFloorId(id, pageable);
                    break;
                default:
                    customers = customerRepo.findByOrganizationId(id, pageable);
            }

//            customers.getContent().forEach(customer -> {
//                customer.setProjectName(projectRepo.findProjectNameById(customer.getProjectId()));
//                customer.setFloorNo(floorRepo.findFloorNoById(customer.getFloorId()));
//                customer.setUnitSerialNo(unitRepo.findUnitSerialById(customer.getUnitId()));
//            });

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
            ValidationService.validate(customer.getOrganizationId(), "Organization");
//            ValidationService.validate(customer.getGuardianName(), "Guardian Name");
//            ValidationService.validate(customer.getProjectId(), "Project");
//            ValidationService.validate(customer.getFloorId(), "Floor");
//            ValidationService.validate(customer.getUnitId(), "Unit");
            ValidationService.validate(customer.getCreatedBy(), "Created By");
            ValidationService.validate(customer.getUpdatedBy(), "Updated By");
            ValidationService.validate(customer.getContactNo(), "Contact No");

//            boolean unitAlreadyAssigned = customerRepo.existsByUnitId(customer.getUnitId());
//            if (unitAlreadyAssigned) {
//                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "This unit is already assigned to another customer.");
//            }


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
//            ValidationService.validate(customer.getProjectId(), "Project ID");
//            ValidationService.validate(customer.getFloorId(), "Floor ID");
//            ValidationService.validate(customer.getUnitId(), "Unit ID");
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

    public Map<String, Object> getUnitListByCustomerId(long customerId) {
        try {
            List<Booking> bookingList = bookingRepository.findByCustomerId(customerId);
            List<Map<String, Object>> result = bookingList.stream()
                    .map(booking -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("Project", projectRepo.findProjectNameById(booking.getProjectId()));
                        data.put("Floor", floorRepo.findFloorNoById(booking.getFloorId()));
                        data.put("Unit", unitRepo.findUnitSerialById(booking.getUnitId()));
                        return data;
                    })
                    .collect(Collectors.toList());

            return ResponseMapper.buildResponse(Responses.SUCCESS, result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
