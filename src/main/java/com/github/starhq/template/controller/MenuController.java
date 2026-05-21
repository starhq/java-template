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
 * RESTful API controller for managing menu resources.
 * Provides standardized endpoints for creating, updating, deleting,
 * and querying menu definitions, including hierarchical tree structures.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
@RestController
@RequestMapping(value = "/{version}/menus", version = "v1")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * Creates a new menu entry with the provided details.
     *
     * @param request the {@link MenuDTO} containing the menu creation parameters
     * @return a {@link ResponseEntity} with HTTP status 201 (Created) upon successful creation
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody MenuDTO request) {
        menuService.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing menu entry by its unique identifier.
     *
     * @param id      the unique identifier of the menu to update
     * @param request the {@link MenuDTO} containing the updated menu parameters
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) upon successful update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody MenuDTO request) {
        menuService.updateMenu(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a menu entry by its unique identifier.
     * This operation may cascade to child menus or be restricted if the menu
     * is referenced by roles/permissions.
     *
     * @param id the unique identifier of the menu to delete
     * @return a {@link ResponseEntity} with HTTP status 204 (No Content) upon successful deletion
     * @throws com.github.starhq.template.common.exception.BusinessException if the menu cannot be deleted due to dependencies
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        menuService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all menus organized as a hierarchical tree structure.
     * Suitable for rendering sidebar navigation or permission trees.
     *
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the list of {@link MenuListVO} in tree order
     */
    @GetMapping
    public ResponseEntity<Result<List<MenuListVO>>> queryMenus() {
        List<MenuListVO> menus = menuService.selectList(new PageRequest());
        Result<List<MenuListVO>> response = Result.success(menus);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single menu entry by its unique identifier.
     *
     * @param id the unique identifier of the menu to retrieve
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the {@link MenuSimpleVO} details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<MenuSimpleVO>> queryMenuById(@PathVariable("id") Long id) {
        MenuSimpleVO menu = menuService.getMenuById(id);
        Result<MenuSimpleVO> response = Result.success(menu);
        return ResponseEntity.ok(response);
    }
}