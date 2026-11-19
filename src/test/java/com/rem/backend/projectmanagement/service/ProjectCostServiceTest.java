package com.rem.backend.projectmanagement.service;

import com.rem.backend.accountingmanagement.service.JournalEntryService;
import com.rem.backend.dto.projectcost.CreateProjectCostPaymentRequest;
import com.rem.backend.dto.projectcost.CreateProjectCostRequest;
import com.rem.backend.dto.projectcost.CancelProjectCostRequest;
import com.rem.backend.dto.projectcost.ProjectCostResponse;
import com.rem.backend.dto.projectcost.UpdateProjectCostRequest;
import com.rem.backend.organizationmanagement.entity.Organization;
import com.rem.backend.organizationmanagement.entity.OrganizationAccount;
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
import com.rem.backend.vendormanagement.repository.VendorAccountRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectCostServiceTest {
    @Mock ProjectCostRepository costs;
    @Mock ProjectCostPaymentRepository payments;
    @Mock ProjectRepo projects;
    @Mock OrganizationRepo organizations;
    @Mock OrganizationAccoutRepo accounts;
    @Mock OrganizationAccountDetailRepo accountDetails;
    @Mock VendorAccountRepo vendors;
    @Mock JournalEntryService journals;
    @Mock PlatformTransactionManager transactions;

    private ProjectCostService service;
    private Project project;
    private Organization organization;
    private final List<ProjectCost> savedCosts = new ArrayList<>();
    private final List<ProjectCostPayment> savedPayments = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ProjectCostService target = new ProjectCostService(costs, payments, projects, organizations,
                accounts, accountDetails, vendors, journals);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (ProjectCostService) proxy.getProxy();
        lenient().when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());

        organization = new Organization();
        organization.setOrganizationId(1L);
        project = new Project();
        project.setProjectId(4L);
        project.setOrganizationId(1L);
        project.setName("Project");
        project.setPurchasingAmount(300000);
        project.setRegistrationAmount(2000);
        project.setConstructionAmount(7000);
        project.setAdditionalAmount(1000);
        project.setTotalAmount(310000);

        lenient().when(organizations.findByOrganizationIdAndIsActiveTrue(1L)).thenReturn(Optional.of(organization));
        lenient().when(projects.findByProjectIdAndOrganizationIdAndIsActiveTrue(4L, 1L))
                .thenReturn(Optional.of(project));
        lenient().when(projects.findActiveForUpdate(4L, 1L)).thenReturn(Optional.of(project));
        lenient().when(costs.sumActiveByCategory(eq(4L), eq(1L), any(), eq(ProjectCostStatus.CANCELLED)))
                .thenAnswer(invocation -> savedCosts.stream()
                        .filter(c -> c.getCategory() == invocation.getArgument(2))
                        .filter(c -> c.getStatus() != ProjectCostStatus.CANCELLED)
                        .map(ProjectCost::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        lenient().when(costs.save(any())).thenAnswer(invocation -> {
            ProjectCost cost = invocation.getArgument(0);
            if (cost.getId() == null) {
                cost.setId(42L);
                savedCosts.add(cost);
            }
            return cost;
        });
        lenient().when(payments.save(any())).thenAnswer(invocation -> {
            ProjectCostPayment payment = invocation.getArgument(0);
            payment.setId((long) savedPayments.size() + 1);
            savedPayments.add(payment);
            return payment;
        });
        lenient().when(payments.findByProjectCostIdOrderByPaymentDateDescIdDesc(42L))
                .thenAnswer(invocation -> savedPayments);
    }

    @Test
    void createCostRecordsEntireAmountAsLiabilityWithoutPayment() {
        ProjectCostResponse result = createCost();

        assertEquals(ProjectCostStatus.POSTED, result.getStatus());
        assertEquals(ProjectCostPaymentType.CREDIT, result.getPaymentType());
        assertEquals(0, result.getPaidAmount().compareTo(new BigDecimal("0.00")));
        assertEquals(0, result.getPendingAmount().compareTo(new BigDecimal("10000.00")));
        assertEquals("Registrar", result.getPayeeName());
        assertNull(result.getVendorId());
        assertEquals(12000, project.getRegistrationAmount());
        assertEquals(7000, project.getConstructionAmount());
        assertEquals(320000, project.getTotalAmount());
        verify(journals).createJournalEntryForProjectCost(savedCosts.get(0), "user");
        verifyNoInteractions(accounts, accountDetails);
        verify(payments, never()).save(any());
    }

    @Test
    void separateInstallmentsSettleLiabilityWithoutChangingProjectCost() {
        createCost();
        ProjectCost cost = savedCosts.get(0);
        when(costs.findByIdAndOrganizationIdForUpdate(42L, 1L)).thenReturn(Optional.of(cost));
        OrganizationAccount account = new OrganizationAccount();
        account.setId(7);
        account.setOrganizationId(1);
        account.setTotalAmount(20000);
        when(accounts.findByIdAndOrganizationIdForUpdate(7L, 1L)).thenReturn(Optional.of(account));

        CreateProjectCostPaymentRequest payment = new CreateProjectCostPaymentRequest();
        payment.setOrganizationId(1L);
        payment.setOrganizationAccountId(7L);
        payment.setAmount(new BigDecimal("5000.00"));
        Map<String, Object> first = service.createProjectCostPayment(42L, payment, "user");
        assertEquals("0000", first.get("responseCode"));
        assertEquals(ProjectCostStatus.PARTIALLY_PAID, cost.getStatus());
        assertEquals(0, cost.getPendingAmount().compareTo(new BigDecimal("5000.00")));

        Map<String, Object> second = service.createProjectCostPayment(42L, payment, "user");
        assertEquals("0000", second.get("responseCode"));
        assertEquals(ProjectCostStatus.PAID, cost.getStatus());
        assertEquals(0, cost.getPendingAmount().signum());
        assertEquals(0, cost.getPaidAmount().compareTo(new BigDecimal("10000.00")));
        assertEquals(10000, account.getTotalAmount());
        assertEquals(320000, project.getTotalAmount());
        assertEquals(2, savedPayments.size());
        verify(journals, times(2)).createJournalEntryForProjectCostPayment(eq(cost), any(), eq("user"));
        verify(accountDetails, times(2)).save(any());
    }

    @Test
    void rejectsPaymentsAtCreationAndOverpayments() {
        CreateProjectCostRequest request = request();
        request.setPaymentType(ProjectCostPaymentType.PAID);
        request.setPaidAmount(new BigDecimal("10000"));
        Map<String, Object> rejected = service.createProjectCost(request, "user");
        assertEquals("0002", rejected.get("responseCode"));
        verify(costs, never()).save(any());

        createCost();
        ProjectCost cost = savedCosts.get(0);
        when(costs.findByIdAndOrganizationIdForUpdate(42L, 1L)).thenReturn(Optional.of(cost));
        CreateProjectCostPaymentRequest payment = new CreateProjectCostPaymentRequest();
        payment.setOrganizationId(1L);
        payment.setOrganizationAccountId(7L);
        payment.setAmount(new BigDecimal("10000.01"));
        Map<String, Object> overpaid = service.createProjectCostPayment(42L, payment, "user");
        assertEquals("0002", overpaid.get("responseCode"));
        assertEquals(0, cost.getPaidAmount().signum());
        verify(payments, never()).save(any());
        verifyNoInteractions(accounts, accountDetails);
    }

    @Test
    void payableOnlyListsOrdinaryOutstandingCosts() {
        when(costs.findPayableCosts(eq(4L), eq(1L), eq(List.of(
                ProjectCostStatus.POSTED, ProjectCostStatus.PARTIALLY_PAID)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        Map<String, Object> response = service.getProjectCostsByProject(4, 1, 0, 20, true);
        assertEquals("0000", response.get("responseCode"));
        assertTrue(((Page<?>) response.get("data")).isEmpty());
    }

    @Test
    void rejectsAnotherOrganizationsAccountAndInsufficientBalance() {
        createCost();
        ProjectCost cost = savedCosts.get(0);
        when(costs.findByIdAndOrganizationIdForUpdate(42L, 1L)).thenReturn(Optional.of(cost));
        CreateProjectCostPaymentRequest payment = new CreateProjectCostPaymentRequest();
        payment.setOrganizationId(1L);
        payment.setOrganizationAccountId(7L);
        payment.setAmount(new BigDecimal("5000"));
        when(accounts.findByIdAndOrganizationIdForUpdate(7L, 1L)).thenReturn(Optional.empty());

        assertEquals("0002", service.createProjectCostPayment(42L, payment, "user").get("responseCode"));

        OrganizationAccount account = new OrganizationAccount();
        account.setId(7);
        account.setTotalAmount(4999);
        when(accounts.findByIdAndOrganizationIdForUpdate(7L, 1L)).thenReturn(Optional.of(account));
        assertEquals("0002", service.createProjectCostPayment(42L, payment, "user").get("responseCode"));
        assertEquals(0, cost.getPaidAmount().signum());
        assertEquals(4999, account.getTotalAmount());
        verify(payments, never()).save(any());
        verifyNoInteractions(accountDetails);
        verify(journals, never()).createJournalEntryForProjectCostPayment(any(), any(), anyString());
    }

    @Test
    void categoryChangeAndCancellationPreserveOtherProjectCosts() {
        createCost();
        ProjectCost cost = savedCosts.get(0);
        when(costs.findByIdAndOrganizationIdForUpdate(42L, 1L)).thenReturn(Optional.of(cost));

        // A separate existing flow adds construction spending after the cost was created.
        project.setConstructionAmount(10000);
        project.setTotalAmount(323000);
        UpdateProjectCostRequest update = new UpdateProjectCostRequest();
        update.setCategory(ProjectCostCategory.ADDITIONAL);
        assertEquals("0000", service.updateProjectCost(42, 1, update, "user").get("responseCode"));
        assertEquals(2000, project.getRegistrationAmount());
        assertEquals(10000, project.getConstructionAmount());
        assertEquals(11000, project.getAdditionalAmount());
        assertEquals(323000, project.getTotalAmount());

        CancelProjectCostRequest cancellation = new CancelProjectCostRequest();
        cancellation.setOrganizationId(1L);
        cancellation.setReason("Entered in error");
        assertEquals("0000", service.cancelProjectCost(42, cancellation, "user").get("responseCode"));
        assertEquals(1000, project.getAdditionalAmount());
        assertEquals(313000, project.getTotalAmount());
        verify(journals).reverseJournalEntryForProjectCost(cost, "user");
    }

    private ProjectCostResponse createCost() {
        Map<String, Object> response = service.createProjectCost(request(), "user");
        assertEquals("0000", response.get("responseCode"), String.valueOf(response.get("data")));
        return (ProjectCostResponse) response.get("data");
    }

    private CreateProjectCostRequest request() {
        CreateProjectCostRequest request = new CreateProjectCostRequest();
        request.setOrganizationId(1L);
        request.setProjectId(4L);
        request.setTitle("Registration");
        request.setCategory(ProjectCostCategory.REGISTRATION);
        request.setTotalAmount(new BigDecimal("10000"));
        request.setPayeeName("Registrar");
        request.setDescription("Transfer fee");
        return request;
    }
}
