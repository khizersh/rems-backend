package com.rem.backend.repository;

import com.rem.backend.entity.sidebar.ChildSidebar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildSidebarRepository extends JpaRepository<ChildSidebar , Long> {
}
