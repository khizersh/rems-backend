package com.rem.backend.projectmanagement.service;

import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.paymentschedulemanagement.service.PaymentSchedulerService;
import com.rem.backend.organizationmanagement.repository.OrganizationAccountDetailRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationAccoutRepo;
import com.rem.backend.paymentschedulemanagement.entity.PaymentSchedule;
import com.rem.backend.projectmanagement.entity.Unit;
import com.rem.backend.projectmanagement.entity.Floor;
import com.rem.backend.projectmanagement.entity.Project;
import com.rem.backend.enums.PaymentScheduleType;
import com.rem.backend.enums.ProjectType;
import com.rem.backend.enums.ProjectAcquisitionType;
import com.rem.backend.propertymanagement.service.PropertyPurchaseService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import com.rem.backend.projectmanagement.repository.FloorRepo;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import com.rem.backend.projectmanagement.repository.UnitRepo;

import java.util.*;
import java.util.stream.Collectors;

import static com.rem.backend.utility.Utility.RESPONSE_CODE;
import static com.rem.backend.utility.ValidationService.*;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepo projectRepo;
    private final FloorRepo floorRepo;
    private final UnitRepo unitRepo;
    private final PaymentSchedulerService paymentSchedulerService;
    private final OrganizationAccoutRepo organizationAccoutRepo;
    private final JournalEntryService journalEntryService;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final PropertyPurchaseService propertyPurchaseService;


    public Map<String, Object> getProjectById(long id) {
        try {

            ValidationService.validate(id, "id");
            Project project = getProjectDataById(id);
            return ResponseMapper.buildResponse(Responses.SUCCESS, project);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


    public Project getProjectDataById(long id) {
        try {

            Optional<Project> projectOptional = projectRepo.findByProjectIdAndIsActiveTrue(id);

            if (!projectOptional.isPresent())
                return null;

            Project project = projectOptional.get();

            List<Floor> floorList = floorRepo.findByProjectId(project.getProjectId());

            for (Floor floor : floorList) {
                List<Unit> unitList = unitRepo.findByFloorId(floor.getId());
                for (Unit unit : unitList) {
                    PaymentSchedule paymentSchedule = paymentSchedulerService.getPaymentDetailsByUnitId(unit.getId(),
                            PaymentScheduleType.BUILDER);
                    unit.setPaymentSchedule(paymentSchedule);
                }
                floor.setUnitList(unitList);
            }

            project.setFloorList(floorList);

            return project;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
            ValidationService.validate(project.getAcquisitionType(), "acquisition type");

            double totalAmount = project.getPurchasingAmount() + project.getAdditionalAmount() + project.getRegistrationAmount();
            project.setTotalAmount(totalAmount);

            // Handle acquisition type logic
            if (project.getAcquisitionType() == ProjectAcquisitionType.NEW_PROJECT) {
                // For NEW_PROJECT, propertyPurchaseId is mandatory
                if (project.getPropertyPurchaseId() == null || project.getPropertyPurchaseId() == 0) {
                    throw new IllegalArgumentException("Property Purchase ID is required for new project acquisition. Please create a property purchase first.");
                }

                // Validate that property purchase exists
                Map<String, Object> propertyPurchaseResponse = propertyPurchaseService.getPurchaseById(project.getPropertyPurchaseId());
                if (!propertyPurchaseResponse.get(RESPONSE_CODE).equals(Responses.SUCCESS.getResponseCode())) {
                    throw new IllegalArgumentException("Invalid Property Purchase ID. Property purchase does not exist.");
                }

                project.setMigrated(false);

            } else if (project.getAcquisitionType() == ProjectAcquisitionType.EXISTING_PROJECT) {
                // For EXISTING_PROJECT, propertyPurchaseId is optional (can be null)
                // No financial validation or deduction required
                // Mark as migrated if it's a legacy/onboarded property
                // Note: isMigrated can be set by the client if needed
                if (project.getPropertyPurchaseId() == null) {
                    project.setPropertyPurchaseId(null);
                }
            }

            Project projectSaved = projectRepo.save(project);

            // For NEW_PROJECT, create journal entry for property assignment
            if (project.getAcquisitionType() == ProjectAcquisitionType.NEW_PROJECT) {
                Map<String, Object> journalResponse = journalEntryService.createPropertyAssignmentJournalEntry(
                    projectSaved.getProjectId(), 
                    project.getPropertyPurchaseId(), 
                    project.getTotalAmount(), 
                    loggedInUser
                );
                if (!journalResponse.get(RESPONSE_CODE).equals(Responses.SUCCESS.getResponseCode())) {
                    throw new RuntimeException("Failed to create journal entry for property assignment.");
                }
            }

            if (project.getProjectType().equals(ProjectType.APARTMENT) ||  project.getProjectType().equals(ProjectType.SHOP)) {
                for (Floor floor : project.getFloorList()) {
                    ValidationService.validate(floor.getFloor(), "floor no");
                    floor.setCreatedBy(loggedInUser);
                    floor.setUpdatedBy(loggedInUser);
                    floor.setProjectId(projectSaved.getProjectId());
                    Floor floorSaved = floorRepo.save(floor);

                    for (Unit unit : floor.getUnitList()) {
                        ValidationService.validate(unit.getSerialNo(), "unit serial no");
                        ValidationService.validate(unit.getAmount(), "amount");
                        ValidationService.validate(unit.getSquareFoot(), "square yards");
                        ValidationService.validate(unit.getUnitType(), "unit type");
                        unit.setFloorId(floorSaved.getId());
                        unit.setCreatedBy(loggedInUser);
                        unit.setUpdatedBy(loggedInUser);


                        PaymentSchedule paymentSchedule = unit.getPaymentSchedule();
                        unit.setAmount(paymentSchedule.getActualAmount() + paymentSchedule.getMiscellaneousAmount() + paymentSchedule.getDevelopmentAmount());
                        Unit unitSaved = unitRepo.save(unit);

                        paymentSchedule.setCreatedBy(loggedInUser);
                        paymentSchedule.setUpdatedBy(loggedInUser);

                        validatePaymentScheduler(paymentSchedule);


                        paymentSchedule.setCreatedBy(loggedInUser);
                        paymentSchedule.setUpdatedBy(loggedInUser);
                        paymentSchedule.setUnit(unitSaved);
                        paymentSchedule.setPaymentPlanType(unitSaved.getPaymentPlanType());
                        paymentSchedule.setPaymentScheduleType(PaymentScheduleType.BUILDER);

                        Map<String, Object> createPaymentScheduler = paymentSchedulerService.createSchedule(paymentSchedule, unitSaved.getPaymentPlanType());
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


    @Transactional
    public Map<String, Object> updateProject(Project project, String loggedInUser) {
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

            Project projectSaved = getProjectDataById(project.getProjectId());


            List<Floor> remainingFloors = new ArrayList<>();
            List<Long> deletedFloors = new ArrayList<>();
            for (Floor updatedFloor : project.getFloorList()) {
                boolean isExist = false;
                if (projectSaved.getFloorList().size() > 0) {
                    for (Floor existingFloor : projectSaved.getFloorList()) {
                        if (updatedFloor.getId() == existingFloor.getId() || updatedFloor.getId() == 0L) {
                            isExist = true;
                            break;
                        }
                    }
                } else {
                    isExist = true;
                }
                if (isExist) {
                    remainingFloors.add(updatedFloor);
                }
            }

            List<Long> remainingFloorId = remainingFloors.stream().map(obj -> obj.getId()).toList();

            deletedFloors = projectSaved.getFloorList().stream().filter(savedFloor ->
                    !remainingFloorId.contains(savedFloor.getId())).map(id -> id.getId()).toList();

            Set<Long> unmatchedIds = new HashSet<>();
            for (Floor floor : remainingFloors) {

                List<Unit> savedUnitList = new ArrayList<>();

                if (projectSaved.getFloorList().size() > 0) {
                    for (Floor savedFloor : projectSaved.getFloorList()){
                        if (floor.getId() == savedFloor.getId()){
                            savedUnitList =  savedFloor.getUnitList();
                            break;
                        }
                    }
                }


                Set<Long> list1Ids = floor.getUnitList().stream()
                        .map(obj -> obj.getId())
                        .collect(Collectors.toSet());

                savedUnitList.stream()
                        .map(obj -> obj.getId())
                        .filter(id -> !list1Ids.contains(id))
                        .collect(Collectors.toList()).forEach(unit -> unmatchedIds.add(unit));


            }




            project.setFloorList(remainingFloors);

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
                        ValidationService.validate(unit.getSquareFoot(), "square yards");
                        ValidationService.validate(unit.getUnitType(), "unit type");
                        unit.setFloorId(floorSaved.getId());
                        unit.setCreatedBy(loggedInUser);
                        unit.setUpdatedBy(loggedInUser);

                        PaymentSchedule paymentSchedule = unit.getPaymentSchedule();

                        unit.setAmount(paymentSchedule.getActualAmount() + paymentSchedule.getMiscellaneousAmount() + paymentSchedule.getDevelopmentAmount());

                        Unit unitSaved = unitRepo.save(unit);


                        paymentSchedule.setCreatedBy(loggedInUser);
                        paymentSchedule.setUpdatedBy(loggedInUser);

                        validatePaymentScheduler(paymentSchedule);

                        paymentSchedule.setCreatedBy(loggedInUser);
                        paymentSchedule.setUpdatedBy(loggedInUser);
                        paymentSchedule.setUnit(unitSaved);
                        paymentSchedule.setPaymentScheduleType(PaymentScheduleType.BUILDER);
                        paymentSchedule.setPaymentPlanType(unitSaved.getPaymentPlanType());

                        Map<String, Object> createPaymentScheduler = paymentSchedulerService.createSchedule(paymentSchedule, unitSaved.getPaymentPlanType());
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


            project.setFloors(remainingFloors.size());
            projectRepo.save(project);
            deletedFloors.forEach(floorId -> floorRepo.deleteById(floorId));
            unmatchedIds.forEach(ids -> paymentSchedulerService.deleteByUnitId(ids));
            unmatchedIds.forEach(id -> unitRepo.deleteById(id));


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

    public Map<String, Object> getProjectsByOrganizationId(long organizationId, Pageable pageable) {
        try {
            ValidationService.validate(organizationId, "organization id");
            Page<Project> projects = projectRepo.findByOrganizationId(organizationId, pageable);
            return ResponseMapper.buildResponse(Responses.SUCCESS, projects);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getAllProjectsByOrgId(long organizationId) {
        try {
            ValidationService.validate(organizationId, "organization id");
            List list = projectRepo.findAllByOrganizationId(organizationId);
            return ResponseMapper.buildResponse(Responses.SUCCESS, list);

        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }


}
