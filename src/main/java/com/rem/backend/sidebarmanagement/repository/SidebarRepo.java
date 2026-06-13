package com.rem.backend.sidebarmanagement.repository;

import com.rem.backend.sidebarmanagement.entity.Sidebar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SidebarRepo extends JpaRepository<Sidebar , Long> {
}
