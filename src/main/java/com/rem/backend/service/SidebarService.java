package com.rem.backend.service;

import com.rem.backend.entity.customer.Customer;
import com.rem.backend.entity.sidebar.ChildSidebar;
import com.rem.backend.entity.sidebar.Sidebar;
import com.rem.backend.repository.ChildSidebarRepository;
import com.rem.backend.repository.CustomerRepo;
import com.rem.backend.repository.SidebarRepo;
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
    private final UserRepo userRepo;
    private final RoleService roleService;
    private final CustomerRepo customerRepo;

    // Add new Sidebar with children
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

    // Get all Sidebars with children
    public Map<String, Object> getAllSidebars() {
        try {
            List<Sidebar> sidebarList = sidebarRepo.findAll();
            return ResponseMapper.buildResponse(Responses.SUCCESS, sidebarList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMapper.buildResponse(Responses.SYSTEM_FAILURE, e.getMessage());
        }
    }

    // Update Sidebar and its children
    public Map<String, Object> updateSidebar(Long id, Sidebar updatedSidebar, String loggedInUser) {
        try {
            // Validate top-level sidebar fields
            ValidationService.validate(id, "id");
            ValidationService.validate(updatedSidebar.getIcon(), "icon");
            ValidationService.validate(updatedSidebar.getUrl(), "url");
            ValidationService.validate(updatedSidebar.getTitle(), "title");

            // Fetch existing sidebar
            Sidebar existing = sidebarRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Sidebar not found with ID: " + id));

            // Update fields
            existing.setIcon(updatedSidebar.getIcon());
            existing.setUrl(updatedSidebar.getUrl());
            existing.setTitle(updatedSidebar.getTitle());
            existing.setUpdatedBy(loggedInUser);

            // Clear and replace children
            existing.getChildList().clear();

            if (updatedSidebar.getChildList() != null) {
                for (ChildSidebar child : updatedSidebar.getChildList()) {
                    ValidationService.validate(child.getIcon(), "icon");
                    ValidationService.validate(child.getUrl(), "url");
                    ValidationService.validate(child.getTitle(), "title");
                    child.setUpdatedBy(loggedInUser);
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

    // Optional: Get sidebar by ID
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


    // Optional: Get sidebar by ID
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

                    // Check if parent sidebar matches any of the user roles

                    for (UserRoles role : userRoles) {

                        Optional<Role> roleOptional = roleList.stream().filter(singleRole -> singleRole.getId() == role.getRoleId()).findFirst();
                        if (roleOptional.isPresent()) {
                            if (sidebar.getRoles().toLowerCase().contains(roleOptional.get().getName().toString().toLowerCase())) {
                                parentMatched = true;
                                break;
                            }
                        }

                    }

                    if (parentMatched) {
                        List<ChildSidebar> matchingChildren = new ArrayList<>();

                        for (ChildSidebar child : sidebar.getChildList()) {
                            for (UserRoles role : userRoles) {
                                Optional<Role> roleOptional = roleList.stream().filter(singleRole -> singleRole.getId() == role.getRoleId()).findFirst();
                                if (roleOptional.isPresent()) {
                                    if (child.getRoles().toLowerCase().contains(roleOptional.get().getName().toString().toLowerCase())) {
                                        if (customerOptional.isPresent()) {
                                            child.setUrl(child.getUrl().replace("{cId}", String.valueOf(customerOptional.get().getCustomerId())));
                                        }
                                        matchingChildren.add(child);
                                        break;
                                    }
                                }
                            }
                        }

                        sidebar.setChildList(matchingChildren);

                        if (customerOptional.isPresent()) {
                            sidebar.setUrl(sidebar.getUrl().replace("{cId}", String.valueOf(customerOptional.get().getCustomerId()))); ;
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
