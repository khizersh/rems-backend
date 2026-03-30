package com.rem.backend.repository;

import com.rem.backend.entity.sidebar.GrandChildSidebar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrandChildSidebarRepository extends JpaRepository<GrandChildSidebar, Long> {
}
