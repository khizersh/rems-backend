package com.rem.backend.projectmanagement.service;

import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.dto.projectcost.*;
import com.rem.backend.enums.TransactionType;
import com.rem.backend.organizationmanagement.entity.Organization;
import com.rem.backend.organizationmanagement.entity.OrganizationAccount;
import com.rem.backend.organizationmanagement.entity.OrganizationAccountDetail;
import com.rem.backend.organizationmanagement.enums.TransactionCategory;
import com.rem.backend.organizationmanagement.repository.OrganizationAccountDetailRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationAccoutRepo;
import com.rem.backend.organizationmanagement.repository.OrganizationRepo;
import com.rem.backend.projectmanagement.entity.Project;
import com.rem.backend.projectmanagement.entity.ProjectCost;
import com.rem.backend.projectmanagement.entity.ProjectCostPayment;
import com.rem.backend.projectmanagement.enums.ProjectCostCategory;
import com.rem.backend.projectmanagement.enums.ProjectCostPaymentType;
import com.rem.backend.projectmanagement.enums.ProjectCostStatus;
import com.rem.backend.projectmanagement.repository.ProjectCostPaymentRepository;
import com.rem.backend.projectmanagement.repository.ProjectCostRepository;
import com.rem.backend.projectmanagement.repository.ProjectRepo;
import com.rem.backend.vendormanagement.entity.VendorAccount;
import com.rem.backend.vendormanagement.repository.VendorAccountRepo;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProjectCostService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final ProjectCostRepository projectCostRepository;
    private final ProjectCostPaymentRepository projectCostPaymentRepository;
    private final ProjectRepo projectRepo;
    private final OrganizationRepo organizationRepo;
    private final OrganizationAccoutRepo organizationAccountRepo;
    private final OrganizationAccountDetailRepo organizationAccountDetailRepo;
    private final VendorAccountRepo vendorAccountRepo;
    private final JournalEntryService journalEntryService;

    @Transactional
    public Map<String, Object> createProjectCost(CreateProjectCostRequest request, String loggedInUser) {
        try {
            validateRequest(request, loggedInUser);
            validateOrganizationAndProject(request.getOrganizationId(), request.getProjectId());
            validateVendor(request.getVendorId(), request.getOrganizationId());
            BigDecimal total = amount(request.getTotalAmount(), "totalAmount");
            String title = required(request.getTitle(), "title");
            String payeeName = required(request.getPayeeName(), "payeeName");
            Project project = lockProject(request.getOrganizationId(), request.getProjectId());
            Map<ProjectCostCategory, BigDecimal> previousCosts = categoryTotals(project);

            ProjectCost cost = new ProjectCost();
            cost.setOrganizationId(request.getOrganizationId());
            cost.setProjectId(project.getProjectId());
            cost.setTitle(title);
            cost.setDescription(request.getDescription());
            cost.setCategory(request.getCategory());
            cost.setExpenseTypeId(request.getExpenseTypeId());
            cost.setVendorId(request.getVendorId());
            cost.setPayeeName(payeeName);
            cost.setTotalAmount(total);
            cost.setPaidAmount(ZERO);
            cost.setPendingAmount(total);
            cost.setPaymentType(ProjectCostPaymentType.CREDIT);
            cost.setStatus(ProjectCostStatus.POSTED);
            cost.setReferenceNumber(request.getReferenceNumber());
            cost.setCostDate(request.getCostDate() == null ? LocalDate.now() : request.getCostDate());
            cost.setSupportingDocumentPath(request.getSupportingDocumentPath());
            cost.setCreatedBy(loggedInUser);
            cost.setUpdatedBy(loggedInUser);
            cost = projectCostRepository.save(cost);

            journalEntryService.createJournalEntryForProjectCost(cost, loggedInUser);
            recalculateProjectFinancialSummary(project, previousCosts, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, toResponse(cost, true));
        } catch (IllegalArgumentException e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getProjectCostById(long id, long organizationId) {
        try {
            validateIds(id, organizationId);
            ProjectCost cost = projectCostRepository.findByIdAndOrganizationId(id, organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Project cost was not found."));
            return ResponseMapper.buildResponse(Responses.SUCCESS, toResponse(cost, true));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getProjectCostsByProject(
            long projectId, long organizationId, int page, int size, boolean payableOnly) {
        try {
            validateOrganizationAndProject(organizationId, projectId);
            validatePage(page, size);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "costDate")
                    .and(Sort.by(Sort.Direction.DESC, "id")));
            Page<ProjectCost> costs = payableOnly
                    ? projectCostRepository.findPayableCosts(projectId, organizationId,
                            List.of(ProjectCostStatus.POSTED, ProjectCostStatus.PARTIALLY_PAID), pageable)
                    : projectCostRepository.findByProjectIdAndOrganizationId(projectId, organizationId, pageable);
            Page<ProjectCostResponse> results = costs.map(cost -> toResponse(cost, false));
            return ResponseMapper.buildResponse(Responses.SUCCESS, results);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> filterProjectCosts(ProjectCostFilterRequest request) {
        try {
            if (request == null) throw new IllegalArgumentException("Filter request is required.");
            validateOrganizationAndProject(request.getOrganizationId(), request.getProjectId());
            validatePage(request.getPage(), request.getSize());
            String sortField = validateSortField(request.getSortBy());
            Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDir())
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                    Sort.by(direction, sortField));
            Page<ProjectCostResponse> results = projectCostRepository.filter(
                    request.getProjectId(), request.getOrganizationId(), request.getCategory(),
                    request.getStatus(), request.getVendorId(), pageable).map(cost -> toResponse(cost, false));
            return ResponseMapper.buildResponse(Responses.SUCCESS, results);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateProjectCost(
            long id, long organizationId, UpdateProjectCostRequest request, String loggedInUser) {
        try {
            validateIds(id, organizationId);
            required(loggedInUser, "loggedInUser");
            if (request == null) throw new IllegalArgumentException("Update request is required.");
            ProjectCost cost = projectCostRepository.findByIdAndOrganizationIdForUpdate(id, organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Project cost was not found."));
            if (cost.getStatus() == ProjectCostStatus.CANCELLED) {
                throw new IllegalArgumentException("Cancelled project costs cannot be updated.");
            }
            validateOrganizationAndProject(cost.getOrganizationId(), cost.getProjectId());
            boolean categoryChanged = request.getCategory() != null && request.getCategory() != cost.getCategory();
            Project project = categoryChanged ? lockProject(cost.getOrganizationId(), cost.getProjectId()) : null;
            Map<ProjectCostCategory, BigDecimal> previousCosts = categoryChanged ? categoryTotals(project) : null;
            if (request.getTitle() != null) cost.setTitle(required(request.getTitle(), "title"));
            if (request.getDescription() != null) cost.setDescription(request.getDescription());
            if (request.getReferenceNumber() != null) cost.setReferenceNumber(request.getReferenceNumber());
            if (request.getCostDate() != null) cost.setCostDate(request.getCostDate());
            if (request.getSupportingDocumentPath() != null) {
                cost.setSupportingDocumentPath(request.getSupportingDocumentPath());
            }
            if (request.getCategory() != null) cost.setCategory(request.getCategory());
            cost.setUpdatedBy(loggedInUser);
            projectCostRepository.save(cost);
            if (categoryChanged) {
                recalculateProjectFinancialSummary(project, previousCosts, loggedInUser);
            }
            return ResponseMapper.buildResponse(Responses.SUCCESS, toResponse(cost, true));
        } catch (IllegalArgumentException e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> createProjectCostPayment(
            long projectCostId, CreateProjectCostPaymentRequest request, String loggedInUser) {
        try {
            if (request == null) throw new IllegalArgumentException("Payment request is required.");
            validateCostIds(projectCostId, request.getOrganizationId());
            required(loggedInUser, "loggedInUser");
            ProjectCost cost = projectCostRepository
                    .findByIdAndOrganizationIdForUpdate(projectCostId, request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Project cost was not found."));
            Project project = validateOrganizationAndProject(cost.getOrganizationId(), cost.getProjectId());
            if (cost.getStatus() == ProjectCostStatus.CANCELLED
                    || cost.getStatus() == ProjectCostStatus.PAID
                    || cost.getStatus() == ProjectCostStatus.PDC_PENDING) {
                throw new IllegalArgumentException("This project cost is not eligible for a regular payment.");
            }
            BigDecimal paymentAmount = amount(request.getAmount(), "amount");
            if (paymentAmount.compareTo(cost.getPendingAmount()) > 0) {
                throw new IllegalArgumentException("Payment cannot exceed the outstanding project cost amount.");
            }
            OrganizationAccount account = getLockedBankAccount(
                    requiredAccountId(request.getOrganizationAccountId()), request.getOrganizationId());
            validateSufficientBalance(account, paymentAmount);

            ProjectCostPayment payment = createPaymentRecord(cost, paymentAmount, account.getId(),
                    request.getPaymentDate(), request.getPaymentMethod(), request.getReferenceNumber(),
                    request.getRemarks(), loggedInUser);
            payment = projectCostPaymentRepository.save(payment);
            deductAccount(account, paymentAmount, project, "Project cost payment #" + cost.getId(), loggedInUser);

            cost.setPaidAmount(cost.getPaidAmount().add(paymentAmount).setScale(2, RoundingMode.HALF_UP));
            cost.setPendingAmount(cost.getPendingAmount().subtract(paymentAmount).setScale(2, RoundingMode.HALF_UP));
            cost.setStatus(cost.getPendingAmount().signum() == 0
                    ? ProjectCostStatus.PAID : ProjectCostStatus.PARTIALLY_PAID);
            cost.setUpdatedBy(loggedInUser);
            projectCostRepository.save(cost);
            journalEntryService.createJournalEntryForProjectCostPayment(cost, payment, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, toResponse(cost, true));
        } catch (IllegalArgumentException e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> clearProjectCostPdc(
            long projectCostId, ClearProjectCostPdcRequest request, String loggedInUser) {
        try {
            if (request == null) throw new IllegalArgumentException("PDC clearing request is required.");
            validateCostIds(projectCostId, request.getOrganizationId());
            required(loggedInUser, "loggedInUser");
            ProjectCost cost = projectCostRepository
                    .findByIdAndOrganizationIdForUpdate(projectCostId, request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Project cost was not found."));
            Project project = validateOrganizationAndProject(cost.getOrganizationId(), cost.getProjectId());
            if (cost.getStatus() != ProjectCostStatus.PDC_PENDING) {
                throw new IllegalArgumentException("Only a pending project cost PDC can be cleared.");
            }
            if (projectCostPaymentRepository.existsByProjectCostIdAndReversedFalse(projectCostId)) {
                throw new IllegalArgumentException("This project cost PDC has already been cleared.");
            }
            OrganizationAccount account = getLockedBankAccount(
                    requiredAccountId(request.getOrganizationAccountId()), request.getOrganizationId());
            BigDecimal clearingAmount = cost.getPendingAmount();
            validateSufficientBalance(account, clearingAmount);
            ProjectCostPayment payment = createPaymentRecord(cost, clearingAmount, account.getId(),
                    request.getPaymentDate(), "PDC_CLEARING", request.getReferenceNumber(),
                    request.getRemarks(), loggedInUser);
            payment = projectCostPaymentRepository.save(payment);
            deductAccount(account, clearingAmount, project, "Clear project cost PDC #" + cost.getId(), loggedInUser);

            cost.setPaidAmount(cost.getPaidAmount().add(clearingAmount).setScale(2, RoundingMode.HALF_UP));
            cost.setPendingAmount(ZERO);
            cost.setStatus(ProjectCostStatus.PAID);
            cost.setUpdatedBy(loggedInUser);
            projectCostRepository.save(cost);
            journalEntryService.createJournalEntryForProjectCostPdcClearing(cost, payment, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, toResponse(cost, true));
        } catch (IllegalArgumentException e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> cancelProjectCost(
            long id, CancelProjectCostRequest request, String loggedInUser) {
        try {
            if (request == null) throw new IllegalArgumentException("Cancellation request is required.");
            validateCostIds(id, request.getOrganizationId());
            required(loggedInUser, "loggedInUser");
            required(request.getReason(), "reason");
            ProjectCost cost = projectCostRepository.findByIdAndOrganizationIdForUpdate(
                            id, request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Project cost was not found."));
            validateOrganizationAndProject(cost.getOrganizationId(), cost.getProjectId());
            if (cost.getStatus() == ProjectCostStatus.CANCELLED) {
                throw new IllegalArgumentException("Project cost is already cancelled.");
            }
            if (cost.getPaidAmount().signum() > 0
                    || projectCostPaymentRepository.existsByProjectCostIdAndReversedFalse(id)) {
                throw new IllegalArgumentException(
                        "Paid project costs require a separate refund or recovery transaction before cancellation.");
            }
            if (cost.getStatus() != ProjectCostStatus.POSTED
                    && cost.getStatus() != ProjectCostStatus.PDC_PENDING) {
                throw new IllegalArgumentException("Only unpaid project costs can be cancelled.");
            }
            Project project = lockProject(cost.getOrganizationId(), cost.getProjectId());
            Map<ProjectCostCategory, BigDecimal> previousCosts = categoryTotals(project);
            journalEntryService.reverseJournalEntryForProjectCost(cost, loggedInUser);
            cost.setStatus(ProjectCostStatus.CANCELLED);
            cost.setPendingAmount(ZERO);
            cost.setUpdatedBy(loggedInUser);
            projectCostRepository.save(cost);
            recalculateProjectFinancialSummary(project, previousCosts, loggedInUser);
            return ResponseMapper.buildResponse(Responses.SUCCESS, toResponse(cost, true));
        } catch (IllegalArgumentException e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            markRollback();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Map<String, Object> getProjectCostSummary(long projectId, long organizationId) {
        try {
            Project project = validateOrganizationAndProject(organizationId, projectId);
            ProjectCostSummaryResponse summary = summaryFor(project);
            return ResponseMapper.buildResponse(Responses.SUCCESS, summary);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    private void recalculateProjectFinancialSummary(
            Project project, Map<ProjectCostCategory, BigDecimal> previousCosts, String loggedInUser) {
        Map<ProjectCostCategory, BigDecimal> currentCosts = categoryTotals(project);
        BigDecimal registration = updatedSummaryAmount(project.getRegistrationAmount(), previousCosts,
                currentCosts, ProjectCostCategory.REGISTRATION);
        BigDecimal construction = updatedSummaryAmount(project.getConstructionAmount(), previousCosts,
                currentCosts, ProjectCostCategory.CONSTRUCTION);
        BigDecimal additional = updatedSummaryAmount(project.getAdditionalAmount(), previousCosts,
                currentCosts, ProjectCostCategory.ADDITIONAL);
        project.setRegistrationAmount(registration.doubleValue());
        project.setConstructionAmount(construction.doubleValue());
        project.setAdditionalAmount(additional.doubleValue());
        project.setTotalAmount(BigDecimal.valueOf(project.getPurchasingAmount())
                .add(registration).add(construction).add(additional).doubleValue());
        project.setUpdatedBy(loggedInUser);
        projectRepo.save(project);
    }

    private Map<ProjectCostCategory, BigDecimal> categoryTotals(Project project) {
        Map<ProjectCostCategory, BigDecimal> totals = new EnumMap<>(ProjectCostCategory.class);
        for (ProjectCostCategory category : ProjectCostCategory.values()) {
            totals.put(category, projectCostRepository.sumActiveByCategory(project.getProjectId(),
                    project.getOrganizationId(), category, ProjectCostStatus.CANCELLED));
        }
        return totals;
    }

    private BigDecimal updatedSummaryAmount(double currentSummary,
            Map<ProjectCostCategory, BigDecimal> previousCosts,
            Map<ProjectCostCategory, BigDecimal> currentCosts, ProjectCostCategory category) {
        return BigDecimal.valueOf(currentSummary).setScale(2, RoundingMode.HALF_UP)
                .subtract(previousCosts.get(category)).add(currentCosts.get(category));
    }

    private ProjectCostSummaryResponse summaryFor(Project project) {
        BigDecimal paid = projectCostRepository.sumPaidByProject(
                project.getProjectId(), project.getOrganizationId(), ProjectCostStatus.CANCELLED);
        BigDecimal outstanding = projectCostRepository.sumOutstandingByProject(
                project.getProjectId(), project.getOrganizationId(), ProjectCostStatus.CANCELLED);
        BigDecimal pendingPdc = Optional.ofNullable(projectCostRepository
                .sumPendingAmountByProjectIdAndOrganizationIdAndStatus(
                        project.getProjectId(), project.getOrganizationId(), ProjectCostStatus.PDC_PENDING))
                .orElse(ZERO);

        ProjectCostSummaryResponse response = new ProjectCostSummaryResponse();
        response.setProjectId(project.getProjectId());
        response.setPurchasingAmount(BigDecimal.valueOf(project.getPurchasingAmount()).setScale(2, RoundingMode.HALF_UP));
        response.setRegistrationAmount(BigDecimal.valueOf(project.getRegistrationAmount()).setScale(2, RoundingMode.HALF_UP));
        response.setConstructionAmount(BigDecimal.valueOf(project.getConstructionAmount()).setScale(2, RoundingMode.HALF_UP));
        response.setAdditionalAmount(BigDecimal.valueOf(project.getAdditionalAmount()).setScale(2, RoundingMode.HALF_UP));
        response.setTotalProjectCost(BigDecimal.valueOf(project.getTotalAmount()).setScale(2, RoundingMode.HALF_UP));
        response.setTotalPaidProjectCosts(paid);
        response.setTotalOutstandingProjectCosts(outstanding);
        response.setPendingPdcAmount(pendingPdc);
        Map<String, BigDecimal> breakdown = new java.util.LinkedHashMap<>();
        breakdown.put(ProjectCostCategory.REGISTRATION.name(), response.getRegistrationAmount());
        breakdown.put(ProjectCostCategory.CONSTRUCTION.name(), response.getConstructionAmount());
        breakdown.put(ProjectCostCategory.ADDITIONAL.name(), response.getAdditionalAmount());
        response.setCategoryBreakdown(breakdown);
        Page<ProjectCost> recent = projectCostRepository.findByProjectIdAndOrganizationId(
                project.getProjectId(), project.getOrganizationId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "costDate")));
        response.setRecentCosts(recent.getContent().stream().map(cost -> toResponse(cost, false)).toList());
        return response;
    }

    private Project validateOrganizationAndProject(Long organizationId, Long projectId) {
        if (organizationId == null || projectId == null || organizationId <= 0 || projectId <= 0) {
            throw new IllegalArgumentException("Organization ID and project ID are required.");
        }
        Organization organization = organizationRepo.findByOrganizationIdAndIsActiveTrue(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization was not found or is inactive."));
        return projectRepo.findByProjectIdAndOrganizationIdAndIsActiveTrue(projectId, organization.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Project was not found for this organization."));
    }

    private Project lockProject(long organizationId, long projectId) {
        return projectRepo.findActiveForUpdate(projectId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Project was not found for this organization."));
    }

    private VendorAccount validateVendor(Long vendorId, Long organizationId) {
        if (vendorId == null) {
            return null;
        }
        VendorAccount vendor = vendorAccountRepo.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor was not found."));
        if (vendor.getOrganizationId() != organizationId) {
            throw new IllegalArgumentException("Vendor does not belong to this organization.");
        }
        return vendor;
    }

    private OrganizationAccount getLockedBankAccount(long accountId, long organizationId) {
        return organizationAccountRepo.findByIdAndOrganizationIdForUpdate(accountId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization account was not found for this organization."));
    }

    private void validateSufficientBalance(OrganizationAccount account, BigDecimal amount) {
        if (BigDecimal.valueOf(account.getTotalAmount()).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient organization account balance.");
        }
    }

    private void deductAccount(
            OrganizationAccount account, BigDecimal amount, Project project, String comments, String loggedInUser) {
        BigDecimal balance = BigDecimal.valueOf(account.getTotalAmount()).subtract(amount);
        account.setTotalAmount(balance.doubleValue());
        account.setUpdatedBy(loggedInUser);
        organizationAccountRepo.save(account);

        OrganizationAccountDetail detail = new OrganizationAccountDetail();
        detail.setOrganizationAcctId(account.getId());
        detail.setTransactionType(TransactionType.CREDIT);
        detail.setTransactionCategory(TransactionCategory.CONSTRUCTION);
        detail.setAmount(amount.doubleValue());
        detail.setComments(comments);
        detail.setProjectId(project.getProjectId());
        detail.setProjectName(project.getName());
        detail.setCreatedBy(loggedInUser);
        detail.setUpdatedBy(loggedInUser);
        organizationAccountDetailRepo.save(detail);
    }

    private ProjectCostPayment createPaymentRecord(
            ProjectCost cost, BigDecimal amount, long organizationAccountId, LocalDate date,
            String paymentMethod, String referenceNumber, String remarks, String loggedInUser) {
        ProjectCostPayment payment = new ProjectCostPayment();
        payment.setProjectCostId(cost.getId());
        payment.setOrganizationId(cost.getOrganizationId());
        payment.setProjectId(cost.getProjectId());
        payment.setVendorId(cost.getVendorId());
        payment.setOrganizationAccountId(organizationAccountId);
        payment.setAmount(amount);
        payment.setPaymentDate(date == null ? LocalDate.now() : date);
        payment.setPaymentMethod(paymentMethod);
        payment.setReferenceNumber(referenceNumber);
        payment.setRemarks(remarks);
        payment.setCreatedBy(loggedInUser);
        payment.setUpdatedBy(loggedInUser);
        return payment;
    }

    private ProjectCostResponse toResponse(ProjectCost cost, boolean includePayments) {
        ProjectCostResponse response = new ProjectCostResponse();
        response.setId(cost.getId());
        response.setOrganizationId(cost.getOrganizationId());
        response.setProjectId(cost.getProjectId());
        response.setTitle(cost.getTitle());
        response.setDescription(cost.getDescription());
        response.setCategory(cost.getCategory());
        response.setExpenseTypeId(cost.getExpenseTypeId());
        response.setVendorId(cost.getVendorId());
        response.setPayeeName(cost.getPayeeName());
        response.setTotalAmount(cost.getTotalAmount());
        response.setPaidAmount(cost.getPaidAmount());
        response.setPendingAmount(cost.getPendingAmount());
        response.setPaymentType(cost.getPaymentType());
        response.setStatus(cost.getStatus());
        response.setOrganizationAccountId(cost.getOrganizationAccountId());
        response.setReferenceNumber(cost.getReferenceNumber());
        response.setCostDate(cost.getCostDate());
        response.setChequeDate(cost.getChequeDate());
        response.setChequeNumber(cost.getChequeNumber());
        response.setSupportingDocumentPath(cost.getSupportingDocumentPath());
        response.setCreatedDate(cost.getCreatedDate());
        if (includePayments) {
            response.setPayments(projectCostPaymentRepository
                    .findByProjectCostIdOrderByPaymentDateDescIdDesc(cost.getId()).stream()
                    .map(payment -> {
                        ProjectCostPaymentResponse item = new ProjectCostPaymentResponse();
                        item.setId(payment.getId());
                        item.setAmount(payment.getAmount());
                        item.setOrganizationAccountId(payment.getOrganizationAccountId());
                        item.setPaymentDate(payment.getPaymentDate());
                        item.setPaymentMethod(payment.getPaymentMethod());
                        item.setReferenceNumber(payment.getReferenceNumber());
                        item.setRemarks(payment.getRemarks());
                        return item;
                    }).toList());
        }
        return response;
    }

    private void validateRequest(CreateProjectCostRequest request, String loggedInUser) {
        if (request == null) throw new IllegalArgumentException("Project cost request is required.");
        validateIds(request.getOrganizationId(), request.getProjectId());
        required(loggedInUser, "loggedInUser");
        required(request.getCategory(), "category");
        if ((request.getPaymentType() != null && request.getPaymentType() != ProjectCostPaymentType.CREDIT)
                || (request.getPaidAmount() != null && request.getPaidAmount().signum() != 0)
                || request.getOrganizationAccountId() != null
                || (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                || request.getChequeDate() != null
                || (request.getChequeNumber() != null && !request.getChequeNumber().isBlank())) {
            throw new IllegalArgumentException(
                    "Create costs unpaid; record payments separately after creating the cost.");
        }
    }

    private static BigDecimal amount(BigDecimal value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required.");
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.signum() <= 0) throw new IllegalArgumentException(field + " must be greater than zero.");
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    private static <T> T required(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required.");
        return value;
    }

    private static long requiredAccountId(Long value) {
        if (value == null || value <= 0) throw new IllegalArgumentException("Organization account is required.");
        return value;
    }

    private static void validateIds(long id, long organizationId) {
        if (id <= 0 || organizationId <= 0) throw new IllegalArgumentException("IDs must be positive.");
    }

    private static void validateCostIds(long id, Long organizationId) {
        if (id <= 0 || organizationId == null || organizationId <= 0) {
            throw new IllegalArgumentException("IDs must be positive.");
        }
    }

    private static void validateIds(Long organizationId, Long projectId) {
        if (organizationId == null || organizationId <= 0 || projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("Organization ID and project ID are required.");
        }
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Pagination must have a non-negative page and size between 1 and 100.");
        }
    }

    private static String validateSortField(String sortBy) {
        List<String> fields = List.of("id", "title", "category", "status", "totalAmount", "costDate", "createdDate");
        return fields.contains(sortBy) ? sortBy : "costDate";
    }

    private static void markRollback() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
