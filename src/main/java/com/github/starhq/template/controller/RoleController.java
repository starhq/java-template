package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.role.RoleDTO;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;
import com.github.starhq.template.model.vo.menu.tree.MenuCheckVO;
import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import com.github.starhq.template.model.vo.role.RolePageVO;
import com.github.starhq.template.model.vo.role.RoleSimpleVO;
import com.github.starhq.template.service.ButtonService;
import com.github.starhq.template.service.MenuService;
import com.github.starhq.template.service.ResourceService;
import com.github.starhq.template.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful API controller for managing role-based access control (RBAC).
 * Provides standardized endpoints for creating, updating, deleting,
 * and querying roles, along with their associated permissions
 * (buttons, resources, and menus) for authorization scenarios.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
@RestController
@RequestMapping(value = "/{version}/roles", version = "v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final ButtonService buttonService;
    private final ResourceService resourceService;
    private final MenuService menuService;

    /**
     * Creates a new role entry with the provided details.
     * Typically used for defining new permission groups in the RBAC system.
     *
     * @param dto the {@link RoleDTO} containing the role creation parameters
     * @return a {@link ResponseEntity} with HTTP status 201 (Created) upon successful creation
     */
    @PostMapping
    public ResponseEntity<Void> createRole(@Valid @RequestBody RoleDTO dto) {
        roleService.createRole(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing role entry by its unique identifier.
     *
     * @param id  the unique identifier of the role to update
     * @param dto the {@link RoleDTO} containing the updated role parameters
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) upon successful update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        roleService.updateRole(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a role entry by its unique identifier.
     * This operation is restricted if the role is assigned to active users
     * or referenced by permission policies.
     *
     * @param id the unique identifier of the role to delete
     * @return a {@link ResponseEntity} with HTTP status 204 (No Content) upon successful deletion
     * @throws com.github.starhq.template.common.exception.BusinessException if the role cannot be deleted due to dependencies
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a single role entry by its unique identifier.
     * Suitable for editing forms or role detail views in admin consoles.
     *
     * @param id the unique identifier of the role to retrieve
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the {@link RoleSimpleVO} details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<RoleSimpleVO>> queryRole(@PathVariable("id") Long id) {
        RoleSimpleVO role = roleService.getRoleById(id);
        Result<RoleSimpleVO> result = Result.success(role);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a paginated list of role entries with optional filtering.
     * Supports sorting and field-based queries for admin console display.
     *
     * @param pageRequest the {@link PageRequest} containing pagination, sorting, and filtering parameters
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with total count and paginated {@link RolePageVO} records
     */
    @GetMapping
    public ResponseEntity<Result<List<RolePageVO>>> queryRoles(@Valid PageRequest pageRequest) {
        IPage<RolePageVO> paginatedRoles = roleService.page(pageRequest);
        Result<List<RolePageVO>> result = Result.success(paginatedRoles.getRecords(), paginatedRoles.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the list of buttons checked/assigned to a specific role.
     * Suitable for rendering permission checkboxes in role configuration UIs.
     *
     * @param roleId the unique identifier of the role
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of {@link ButtonCheckVO}
     */
    @GetMapping("/buttons")
    public ResponseEntity<Result<List<ButtonCheckVO>>> queryRoleButtons(@RequestParam("roleId") Long roleId) {
        List<ButtonCheckVO> buttons = buttonService.selectCheckedButtons(roleId);
        return ResponseEntity.ok(Result.success(buttons));
    }

    /**
     * Retrieves the list of resources checked/assigned to a specific role.
     * Suitable for configuring API endpoint permissions in role management.
     *
     * @param roleId the unique identifier of the role
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of {@link ResourceCheckVO}
     */
    @GetMapping("/resources")
    public ResponseEntity<Result<List<ResourceCheckVO>>> queryRoleResources(@RequestParam("roleId") Long roleId) {
        List<ResourceCheckVO> resources = resourceService.selectCheckedResources(roleId);
        return ResponseEntity.ok(Result.success(resources));
    }

    /**
     * Retrieves the list of menus checked/assigned to a specific role.
     * Suitable for configuring sidebar navigation visibility based on role permissions.
     *
     * @param roleId the unique identifier of the role
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of {@link MenuCheckVO}
     */
    @GetMapping("/menus")
    public ResponseEntity<Result<List<MenuCheckVO>>> queryRoleMenus(@RequestParam("roleId") Long roleId) {
        List<MenuCheckVO> menus = menuService.selectCheckedMenus(roleId);
        return ResponseEntity.ok(Result.success(menus));
    }
}