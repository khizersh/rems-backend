package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.RoleType;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.UnitRepo;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoleMapper;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.usermanagement.repository.UserRoleRepository;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.rem.backend.utility.EndPointUtils.GET_PROJECTS_BY_ORG_ID;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final UserRepo userRepo;
    private final UserRoleRepository userRoleRepo;
    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;
    private final UnitRepo unitRepo;

    public Map<String, Object> getCustomerById(long id) {
        try {
            ValidationService.validate(id, "id");
            Optional<Customer> customerOptional = customerRepo.findById(id);
            if (customerOptional.isPresent()) {
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
    public Map<String, Object> createCustomer(Customer customer) {
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
            ValidationService.validate(customer.getCreatedBy(), "Created By");
            ValidationService.validate(customer.getUpdatedBy(), "Updated By");

            boolean unitAlreadyAssigned = customerRepo.existsByUnitId(customer.getUnitId());
            if (unitAlreadyAssigned) {
                return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "This unit is already assigned to another customer.");
            }



            if(customer.getUserId() != null){
              Optional<User> userOptional   = userRepo.findById(customer.getUserId());
              if (userOptional.isEmpty()) return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, "Invalid user selected!");
            }else{
                User user = new User();
                user.setActive(true);
                user.setEmail(customer.getEmail());
                user.setPassword(customer.getPassword());
                user.setUsername(customer.getUsername());
                user.setEmail(customer.getEmail());
                user.setOrganizationId(customer.getOrganizationId());
                User userSaved = userRepo.save(user);
                customer.setUserId(userSaved.getId());

                UserRoles roles = new UserRoles();
                roles.setRoleCode(RoleType.USER_ROLE);
                roles.setUserId(userSaved.getId());
                userRoleRepo.save(roles);

            }
            // Save customer
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
}
