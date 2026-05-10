package com.rem.backend.propertymanagement.repository;

import com.rem.backend.propertymanagement.entity.PropertyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyPaymentRepo extends JpaRepository<PropertyPayment, Long> {
    List<PropertyPayment> findByPropertyPurchaseIdOrderByCreatedDateDesc(long propertyPurchaseId);
}

