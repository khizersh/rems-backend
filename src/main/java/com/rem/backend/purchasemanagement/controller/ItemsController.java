package com.rem.backend.purchasemanagement.controller;

import com.rem.backend.dto.commonRequest.CommonPaginationRequest;
import com.rem.backend.purchasemanagement.entity.items.Items;
import com.rem.backend.purchasemanagement.entity.items.ItemsUnit;
import com.rem.backend.purchasemanagement.service.ItemsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.rem.backend.usermanagement.utillity.JWTUtils.LOGGED_IN_USER;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemsController {

    private final ItemsService itemsService;

    // Get paginated items for organization
    @PostMapping("/{organizationId}/getAll")
    public Map getAllItemsPaginated(@PathVariable long organizationId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return itemsService.getAllItems(organizationId, pageable);
    }

    // Get all items (no pagination)
    @GetMapping("/{organizationId}/list")
    public Map getAllItems(@PathVariable long organizationId) {
        return itemsService.getAllItems(organizationId);
    }

    // Get item by id
    @GetMapping("/getById/{id}")
    public Map getItemById(@PathVariable long id) {
        return itemsService.getItemById(id);
    }

    // Create item (unitId passed as query param)
    @PostMapping("/create")
    public Map createItem(@RequestBody Items item, @RequestParam Long unitId, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return itemsService.addItem(item, unitId, loggedInUser);
    }

    // Update item (unitId passed as query param)
    @PostMapping("/update")
    public Map updateItem(@RequestBody Items item, @RequestParam Long unitId, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return itemsService.updateItem(item, unitId, loggedInUser);
    }

    // -------------------- Item Units --------------------

    // Get all units (no pagination)
    @GetMapping("/unit/{organizationId}/list")
    public Map getAllUnits(@PathVariable long organizationId) {
        return itemsService.getAllUnits(organizationId);
    }

    // Get paginated units
    @PostMapping("/unit/{organizationId}/getAll")
    public Map getAllUnitsPaginated(@PathVariable long organizationId, @RequestBody CommonPaginationRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.by(request.getSortBy()).ascending()
                        : Sort.by(request.getSortBy()).descending());

        return itemsService.getAllUnits(organizationId, pageable);
    }

    // Create or update unit
    @PostMapping("/unit/createOrUpdate")
    public Map createOrUpdateUnit(@RequestBody ItemsUnit unit, HttpServletRequest request) {
        String loggedInUser = (String) request.getAttribute(LOGGED_IN_USER);
        return itemsService.createOrUpdateUnit(unit, loggedInUser);
    }
}
