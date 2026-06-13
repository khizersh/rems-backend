package com.rem.backend.sidebarmanagement.service;

import com.rem.backend.customermanagement.entity.Customer;
import com.rem.backend.sidebarmanagement.entity.ChildSidebar;
import com.rem.backend.sidebarmanagement.entity.GrandChildSidebar;
import com.rem.backend.sidebarmanagement.entity.Sidebar;
import com.rem.backend.sidebarmanagement.repository.ChildSidebarRepository;
import com.rem.backend.customermanagement.repository.CustomerRepo;
import com.rem.backend.sidebarmanagement.repository.GrandChildSidebarRepository;
import com.rem.backend.sidebarmanagement.repository.SidebarRepo;
import com.rem.backend.usermanagement.entity.Role;
import com.rem.backend.usermanagement.entity.User;
import com.rem.backend.usermanagement.entity.UserRoles;
import com.rem.backend.usermanagement.repository.UserRepo;
import com.rem.backend.usermanagement.service.RoleService;
import com.rem.backend.utility.ResponseMapper;
import com.rem.backend.utility.Responses;
import com.rem.backend.utility.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SidebarService {

    private final SidebarRepo sidebarRepo;
    private final ChildSidebarRepository childSidebarRepository;
    private final GrandChildSidebarRepository grandChildSidebarRepository;
    private final UserRepo userRepo;
    private final RoleService roleService;
    private final CustomerRepo customerRepo;

    // Add new Sidebar with children and grand children
    public Map<String, Object> addSidebar(Sidebar sidebar, String loggedInUser) {
        try {
            ValidationService.validate(sidebar.getIcon(), "icon");
            ValidationService.validate(sidebar.getUrl(), "url");
            ValidationService.validate(sidebar.getTitle(), "title");
            ValidationService.validate(sidebar.getRoles(), "roles");

            if (sidebar.getChildList() != null) {
                for (ChildSidebar child : sidebar.getChildList()) {
                    ValidationService.validate(child.getIcon(), "icon");
                    ValidationService.validate(child.getUrl(), "url");
                    ValidationService.validate(child.getTitle(), "title");
                    ValidationService.validate(child.getRoles(), "roles");
                    child.setUpdatedBy(loggedInUser);
                    child.setCreatedBy(loggedInUser);

                    if (child.getGrandChildList() != null) {
                        for (GrandChildSidebar grandChild : child.getGrandChildList()) {
                            ValidationService.validate(grandChild.getIcon(), "icon");
                            ValidationService.validate(grandChild.getUrl(), "url");
                            ValidationService.validate(grandChild.getTitle(), "title");
                            ValidationService.validate(grandChild.getRoles(), "roles");
                            grandChild.setUpdatedBy(loggedInUser);
                            grandChild.setCreatedBy(loggedInUser);
                        }
                    }
                }
            }
            sidebar.setUpdatedBy(loggedInUser);
            sidebar.setCreatedBy(loggedInUser);

            Sidebar sidebarSaved = sidebarRepo.save(sidebar);
            return ResponseMapper.buildResponse(Responses.SUCCESS, sidebarSaved);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        }
    }

    // Get all Sidebars with children and grand children
    public Map<String, Object> getAllSidebars() {
        try {
            List<Sidebar> sidebarList = sidebarRepo.findAll();
            return ResponseMapper.buildResponse(Responses.SUCCESS, sidebarList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // Update Sidebar and its children and grand children
    public Map<String, Object> updateSidebar(Long id, Sidebar updatedSidebar, String loggedInUser) {
        try {
            ValidationService.validate(id, "id");
            ValidationService.validate(updatedSidebar.getIcon(), "icon");
            ValidationService.validate(updatedSidebar.getUrl(), "url");
            ValidationService.validate(updatedSidebar.getTitle(), "title");

            Sidebar existing = sidebarRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Sidebar not found with ID: " + id));

            existing.setIcon(updatedSidebar.getIcon());
            existing.setUrl(updatedSidebar.getUrl());
            existing.setTitle(updatedSidebar.getTitle());
            existing.setPage(updatedSidebar.isPage());
            existing.setUpdatedBy(loggedInUser);

            // Clear and replace children (orphanRemoval handles grand children cascaded)
            existing.getChildList().clear();

            if (updatedSidebar.getChildList() != null) {
                for (ChildSidebar child : updatedSidebar.getChildList()) {
                    ValidationService.validate(child.getIcon(), "icon");
                    ValidationService.validate(child.getUrl(), "url");
                    ValidationService.validate(child.getTitle(), "title");
                    child.setUpdatedBy(loggedInUser);

                    if (child.getGrandChildList() != null) {
                        for (GrandChildSidebar grandChild : child.getGrandChildList()) {
                            ValidationService.validate(grandChild.getIcon(), "icon");
                            ValidationService.validate(grandChild.getUrl(), "url");
                            ValidationService.validate(grandChild.getTitle(), "title");
                            grandChild.setUpdatedBy(loggedInUser);
                        }
                    }
                    existing.getChildList().add(child);
                }
            }

            return ResponseMapper.buildResponse(Responses.SUCCESS, sidebarRepo.save(existing));
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.INVALID_PARAMETER, e.getMessage());
        }
    }

    // Get sidebar by ID
    public Map<String, Object> getSidebarById(Long id) {
        try {
            Sidebar sidebar = sidebarRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Sidebar not found with ID: " + id));
            return ResponseMapper.buildResponse(Responses.SUCCESS, sidebar);
        } catch (IllegalArgumentException e) {
            return ResponseMapper.buildResponse(Responses.NO_DATA_FOUND, e.getMessage());
        } catch (Exception e) {
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // Get sidebar filtered by user roles — 3-level hierarchy
    public List<Sidebar> getSidebarByRole(String username) {
        List<Sidebar> finalSidebarList = new ArrayList<>();

        try {
            Optional<User> userOptional = userRepo.findByUsernameAndIsActiveTrue(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Optional<Customer> customerOptional = customerRepo.findByUserId(user.getId());
                Set<UserRoles> userRoles = roleService.getUserRoles(user.getId());

                List<Sidebar> allSidebars = sidebarRepo.findAll();
                List<Role> roleList = roleService.getAll();

                for (Sidebar sidebar : allSidebars) {
                    boolean parentMatched = false;

                    for (UserRoles role : userRoles) {
                        Optional<Role> roleOptional = roleList.stream()
                                .filter(singleRole -> singleRole.getId() == role.getRoleId())
                                .findFirst();
                        if (roleOptional.isPresent()) {
                            if (sidebar.getRoles().toLowerCase().contains(roleOptional.get().getName().toLowerCase())) {
                                parentMatched = true;
                                break;
                            }
                        }
                    }

                    if (parentMatched) {
                        List<ChildSidebar> matchingChildren = new ArrayList<>();

                        for (ChildSidebar child : sidebar.getChildList()) {
                            boolean childMatched = false;

                            for (UserRoles role : userRoles) {
                                Optional<Role> roleOptional = roleList.stream()
                                        .filter(singleRole -> singleRole.getId() == role.getRoleId())
                                        .findFirst();
                                if (roleOptional.isPresent()) {
                                    if (child.getRoles().toLowerCase().contains(roleOptional.get().getName().toLowerCase())) {
                                        childMatched = true;
                                        if (customerOptional.isPresent()) {
                                            child.setUrl(child.getUrl().replace("{cId}", String.valueOf(customerOptional.get().getCustomerId())));
                                        }
                                        break;
                                    }
                                }
                            }

                            if (childMatched) {
                                // Filter grand children by role
                                List<GrandChildSidebar> matchingGrandChildren = new ArrayList<>();

                                for (GrandChildSidebar grandChild : child.getGrandChildList()) {
                                    for (UserRoles role : userRoles) {
                                        Optional<Role> roleOptional = roleList.stream()
                                                .filter(singleRole -> singleRole.getId() == role.getRoleId())
                                                .findFirst();
                                        if (roleOptional.isPresent()) {
                                            if (grandChild.getRoles().toLowerCase().contains(roleOptional.get().getName().toLowerCase())) {
                                                if (customerOptional.isPresent()) {
                                                    grandChild.setUrl(grandChild.getUrl().replace("{cId}", String.valueOf(customerOptional.get().getCustomerId())));
                                                }
                                                matchingGrandChildren.add(grandChild);
                                                break;
                                            }
                                        }
                                    }
                                }

                                child.setGrandChildList(matchingGrandChildren);
                                matchingChildren.add(child);
                            }
                        }

                        sidebar.setChildList(matchingChildren);

                        if (customerOptional.isPresent()) {
                            sidebar.setUrl(sidebar.getUrl().replace("{cId}", String.valueOf(customerOptional.get().getCustomerId())));
                        }
                        finalSidebarList.add(sidebar);
                    }
                }
            }

            return finalSidebarList;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
