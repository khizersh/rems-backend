package com.rem.backend.entity.customerpayable;

import com.rem.backend.dto.customerpayable.CustomerPayableDto;
import com.rem.backend.entity.booking.Booking;
import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.project.Unit;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "customer_payable",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"booking_id", "customer_id", "unit_id"}
        )
)
@Data
public class CustomerPayable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false)
    private double totalPayable;    // Sum of all details (refunds + deductions)

    @Column(nullable = false)
    private double totalRefund;     // Optional → refundable amount to customer

    @Column(nullable = false)
    private double totalDeductions; // Cancellation charges etc.

    @Column(nullable = false)
    private double totalPaid; // Cancellation charges etc.

    @Column(nullable = false)
    private double balanceAmount; // Cancellation charges etc.

    @Column(nullable = false)
    private String reason;              // e.g. "Customer Requested", "Default", etc.

    @Column(nullable = false)
    private String status;              // PENDING, PROCESSED, CANCELLED

    private String createdBy;

    private String updatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "customerPayable",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<CustomerPayableDetail> details;

    @OneToMany(
            mappedBy = "customerPayable",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<CustomerPayableFeeDetail> feeDetails;

    public static CustomerPayable map(CustomerPayable customerPayable, CustomerPayableDto customerPayableDto, Booking booking){
        customerPayable.setCustomer(booking.getCustomer());
        customerPayable.setBooking(booking);
        customerPayable.setUnit(booking.getUnit());
        customerPayable.setTotalPayable(customerPayableDto.getTotalPayable());
        customerPayable.setTotalRefund(customerPayableDto.getTotalRefund());
        customerPayable.setTotalDeductions(customerPayableDto.getTotalDeductions());
        customerPayable.setTotalPaid(customerPayableDto.getTotalPaid());
        customerPayable.setBalanceAmount(customerPayableDto.getBalanceAmount());
        customerPayable.setReason(customerPayableDto.getReason());
        customerPayable.setStatus(customerPayableDto.getStatus());

        return customerPayable;
    }
}
