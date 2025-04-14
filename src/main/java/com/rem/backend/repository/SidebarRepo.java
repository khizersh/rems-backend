package com.rem.backend.repository;

import com.rem.backend.entity.sidebar.Sidebar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SidebarRepo extends JpaRepository<Sidebar , Long> {
}
