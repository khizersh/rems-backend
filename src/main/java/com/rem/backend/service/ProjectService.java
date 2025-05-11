package com.rem.backend.service;

import com.rem.backend.entity.paymentschedule.MonthWisePayment;
import com.rem.backend.entity.paymentschedule.PaymentSchedule;
import com.rem.backend.entity.project.Unit;
import com.rem.backend.entity.project.Floor;
import com.rem.backend.entity.project.Project;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.enums.ProjectType;
import com.rem.backend.repository.FloorRepo;
import com.rem.backend.repository.PaymentScheduleRepository;
import com.rem.backend.repository.ProjectRepo;
import com.rem.backend.repository.UnitRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rem.backend.utility.Utility.DATA;
import static com.rem.backend.utility.Utility.RESPONSE_CODE;
import static com.rem.backend.utility.ValidationService.*;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;
    private final UnitRepo unitRepo;
    private final PaymentSchedulerService paymentSchedulerService;

    public Map<String, Object> getProjectById(@PathVariable long id) {
        try {

            ValidationService.validate(id, "id");
            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(id);
            if (projectOptional.isPresent())
                return ResponseMapper.buildResponse(Responses.SUCCESS, projectOptional.get());

            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }



    @Transactional
    public Map<String, Object> createProject(Project project, String loggedInUser) {
        try {
            // Set user information for project, floors, and units
            project.setCreatedBy(loggedInUser);
            project.setUpdatedBy(loggedInUser);

            // Validate project details
            ValidationService.validate(project.getName(), "name");
            ValidationService.validate(project.getFloors(), "floors");
            ValidationService.validate(project.getAddress(), "address");
            ValidationService.validate(project.getProjectType(), "project type");

            double totalAmount = project.getPurchasingAmount() + project.getAdditionalAmount() + project.getRegistrationAmount();
            project.setTotalAmount(totalAmount);

            Project projectSaved = projectRepo.save(project);

            if (project.getProjectType().equals(ProjectType.APARTMENT)) {
                for (Floor floor : project.getFloorList()) {
                    ValidationService.validate(floor.getFloor(), "floor no");
                    floor.setCreatedBy(loggedInUser);
                    floor.setUpdatedBy(loggedInUser);
                    floor.setProjectId(projectSaved.getProjectId());
                    Floor floorSaved = floorRepo.save(floor);

                    for (Unit unit : floor.getUnitList()) {
                        ValidationService.validate(unit.getSerialNo(), "unit serial no");
                        ValidationService.validate(unit.getAmount(), "amount");
                        ValidationService.validate(unit.getSquareYards(), "square yards");
                        ValidationService.validate(unit.getUnitType(), "unit type");
                        unit.setFloorId(floorSaved.getId());
                        unit.setCreatedBy(loggedInUser);
                        unit.setUpdatedBy(loggedInUser);
//                        unit.setAmount(loggedInUser);

                        Unit unitSaved = unitRepo.save(unit);

                        PaymentSchedule paymentSchedule = unit.getPaymentSchedule();
                        paymentSchedule.setCreatedBy(loggedInUser);
                        paymentSchedule.setUpdatedBy(loggedInUser);

                        validatePaymentScheduler(paymentSchedule);



                        if(paymentSchedule != null){

                        }
                        paymentSchedule.setCreatedBy(loggedInUser);
                        paymentSchedule.setUpdatedBy(loggedInUser);
                        paymentSchedule.setUnit(unitSaved);
                        paymentSchedule.setPaymentScheduleType(PaymentScheduleType.BUILDER);

                        Map<String, Object> createPaymentScheduler = paymentSchedulerService.createSchedule(paymentSchedule);
                        if (createPaymentScheduler != null) {
                            // Set type to CUSTOMER and associate with unit
                            PaymentSchedule savedSchedule = null;
                            if (!createPaymentScheduler.get(RESPONSE_CODE).equals(Responses.SUCCESS.getResponseCode())) {
                                return createPaymentScheduler;
                            }

                        }
                    }

                }

            }



            return ResponseMapper.buildResponse(Responses.SUCCESS, "Project added successfully!");
        } catch (IllegalArgumentException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> deActivate(long id, String loggedInUser) {
        try {
            ValidationService.validate(id, "id");
            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(id);
            if (projectOptional.isPresent()) {
                Project project = projectOptional.get();
                project.setUpdatedBy(loggedInUser);
                project.setActive(false);
                return ResponseMapper.buildResponse(Responses.SUCCESS, projectRepo.save(project));
            }
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, null);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getProjectsByOrganizationId(long organizationId , Pageable pageable) {
        try {
            ValidationService.validate(organizationId, "organization id");
            Page<Project> projects = projectRepo.findByOrganizationId(organizationId , pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, projects);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllProjectsByOrgId(long organizationId ) {
        try {
            ValidationService.validate(organizationId, "organization id");
            List list = projectRepo.findAllByOrganizationId(organizationId );
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
