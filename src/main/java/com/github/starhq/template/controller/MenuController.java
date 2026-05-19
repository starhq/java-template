package com.github.starhq.template.controller;

import com.github.starhq.template.model.dto.menu.MenuDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.menu.MenuSimpleVO;
import com.github.starhq.template.model.vo.menu.tree.MenuListVO;
import com.github.starhq.template.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing menu operations.
 * Provides endpoints for creating, updating, deleting, and querying menus.
 */
@RestController
@RequestMapping(value = "/{version}/menus",version = "v1")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService; // Service for handling menu operations

    /**
     * Creates a new menu.
     *
     * @param request the request containing menu creation details
     * @return a ResponseEntity indicating the result of the creation
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody MenuDTO request) {
        menuService.createMenu(request); // Call service to create menu
        return ResponseEntity.status(HttpStatus.CREATED).build(); // Return 201 Created status
    }

    /**
     * Updates an existing menu by its ID.
     *
     * @param id      the ID of the menu to update
     * @param request the request containing updated menu details
     * @return a ResponseEntity indicating the result of the update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody MenuDTO request) {
        menuService.updateMenu(id, request); // Call service to update menu
        return ResponseEntity.ok().build(); // Return 200 OK status
    }

    /**
     * Deletes a menu by its ID.
     *
     * @param id the ID of the menu to delete
     * @return a ResponseEntity indicating the result of the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        menuService.removeById(id); // Call service to remove menu
        return ResponseEntity.noContent().build(); // Return 204 No Content status
    }

    /**
     * Retrieves all menus in a tree structure.
     *
     * @return a ResponseEntity containing the menu tree
     */
    @GetMapping
    public ResponseEntity<Result<List<MenuListVO>>> queryMenus() {
        List<MenuListVO> menus = menuService.selectList(new PageRequest()); // Fetch menu tree
        Result<List<MenuListVO>> response = Result.success(menus);// Create response
        return ResponseEntity.ok(response); // Return response with 200 OK status
    }

    /**
     * Retrieves a menu by its ID.
     *
     * @param id the ID of the menu to retrieve
     * @return a ResponseEntity containing the menu details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<MenuSimpleVO>> queryMenuById(@PathVariable("id") Long id) {
        MenuSimpleVO menu = menuService.getMenuById(id); // Fetch the menu by ID
        Result<MenuSimpleVO> response = Result.success(menu); // Create response
        return ResponseEntity.ok(response); // Return response with 200 OK status
    }
}

