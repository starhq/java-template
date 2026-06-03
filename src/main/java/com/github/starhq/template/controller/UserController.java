package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.model.dto.KeyWordPageRequest;
import com.github.starhq.template.model.dto.UserDTO;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.LeftNavVO;
import com.github.starhq.template.model.vo.RoleCheckVO;
import com.github.starhq.template.model.vo.UserPageVO;
import com.github.starhq.template.model.vo.UserSimpleVO;
import com.github.starhq.template.service.ButtonService;
import com.github.starhq.template.service.MenuService;
import com.github.starhq.template.service.RoleService;
import com.github.starhq.template.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful API controller for managing user accounts and profile operations.
 * Provides standardized endpoints for creating, updating, deleting, and querying
 * user information, along with role assignments and permission-based resources
 * (buttons, menus) for personalized UI rendering in RBAC systems.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
@RestController
@RequestMapping(value = "/{version}/users", version = "v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ButtonService buttonService;
    private final MenuService menuService;
    private final RoleService roleService;

    /**
     * Creates a new user entry with the provided details.
     * Typically used for user registration or admin-initiated account creation.
     *
     * @param dto the {@link UserDTO} containing the user creation parameters
     * @return a {@link ResponseEntity} with HTTP status 201 (Created) upon successful creation
     * @throws com.github.starhq.template.common.exception.BusinessException if the username/email already exists or validation fails
     */
    @PostMapping
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserDTO dto) {
        userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing user entry by its unique identifier.
     * Sensitive fields (e.g., password) should be handled via dedicated endpoints.
     *
     * @param id      the unique identifier of the user to update
     * @param request the {@link UserDTO} containing the updated user parameters
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) upon successful update
     * @throws com.github.starhq.template.common.exception.BusinessException if the user ID is invalid or update conflicts occur
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody UserDTO request) {
        userService.updateUser(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a user entry by its unique identifier.
     * This operation is restricted if the user is referenced by active sessions,
     * audit logs, or business data. Soft deletion is recommended for auditability.
     *
     * @param id the unique identifier of the user to delete
     * @return a {@link ResponseEntity} with HTTP status 204 (No Content) upon successful deletion
     * @throws com.github.starhq.template.common.exception.BusinessException if the user cannot be deleted due to dependencies
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        userService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of user entries with optional keyword filtering.
     * Supports filtering by username, email, status, or time range for admin console display.
     *
     * @param request the {@link KeyWordPageRequest} containing pagination, sorting, and keyword filter parameters
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with total count and paginated {@link UserPageVO} records
     */
    @GetMapping
    public ResponseEntity<Result<List<UserPageVO>>> queryUsers(@Valid KeyWordPageRequest request) {
        IPage<UserPageVO> paginatedUsers = userService.page(request);
        Result<List<UserPageVO>> response = Result.success(paginatedUsers.getRecords(), paginatedUsers.getTotal());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single user entry by its unique identifier.
     * Suitable for user detail views or editing forms in admin consoles.
     *
     * @param id the unique identifier of the user to retrieve
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the {@link UserSimpleVO} details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<UserSimpleVO>> queryUser(@PathVariable("id") Long id) {
        UserSimpleVO user = userService.getUserById(id);
        Result<UserSimpleVO> response = Result.success(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the list of roles assigned to a specific user.
     * Suitable for rendering role selection checkboxes in user management UIs.
     *
     * @param userId the unique identifier of the user
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of {@link RoleCheckVO}
     */
    @GetMapping("/roles")
    public ResponseEntity<Result<List<RoleCheckVO>>> queryUserRoles(@RequestParam("userId") Long userId) {
        List<RoleCheckVO> selectCheckedRoles = roleService.selectCheckedRoles(userId);
        Result<List<RoleCheckVO>> response = Result.success(selectCheckedRoles);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the profile information of the currently authenticated user.
     * The user ID is extracted from the security context (e.g., JWT token or session).
     * Suitable for displaying user avatar, name, or personal settings in frontend apps.
     *
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the {@link UserSimpleVO} profile details
     */
    @GetMapping("/profile")
    public ResponseEntity<Result<UserSimpleVO>> queryUserProfile() {
        Long id = SecurityContextUtils.getRequiredUserId();
        UserSimpleVO user = userService.getUserById(id);
        Result<UserSimpleVO> response = Result.success(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the list of button permissions accessible to the currently authenticated user.
     * The result is typically used for dynamic UI rendering (e.g., showing/hiding action buttons)
     * based on the user's role-based permissions.
     *
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of button permission codes
     */
    @GetMapping("/buttons")
    public ResponseEntity<Result<List<String>>> queryUserButton() {
        Long id = SecurityContextUtils.getRequiredUserId();
        List<String> buttons = buttonService.select(id);
        Result<List<String>> response = Result.success(buttons);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the navigation menu tree accessible to the currently authenticated user.
     * The result is typically used for rendering sidebar navigation structures
     * based on the user's role-based menu permissions.
     *
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of {@link LeftNavVO} in tree order
     */
    @GetMapping("/menus")
    public ResponseEntity<Result<List<LeftNavVO>>> queryUserMenus() {
        Long id = SecurityContextUtils.getRequiredUserId();
        List<LeftNavVO> menus = menuService.selectSidebar(id);
        Result<List<LeftNavVO>> response = Result.success(menus);
        return ResponseEntity.ok(response);
    }
}