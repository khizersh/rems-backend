package com.rem.backend.sidebarmanagement.repository;

import com.rem.backend.sidebarmanagement.entity.ChildSidebar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildSidebarRepository extends JpaRepository<ChildSidebar , Long> {
}
