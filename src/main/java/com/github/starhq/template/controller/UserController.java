package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;
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
 * 用户控制器
 *
 * @author starhq
 */
@RestController
@RequestMapping(value = "/{version}/users", version = "v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService; // Service for user-related operations
    private final ButtonService buttonService; // Service for button-related operations
    private final MenuService menuService; // Service for menu-related operations
    private final RoleService roleService; // Service for role-related operations


    /**
     * Creates a new user in the system.
     *
     * @param dto The request body containing the details for the new user.
     * @return A ResponseEntity with HTTP status 201 (Created) on successful
     * creation.
     */
    @PostMapping
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserDTO dto) {
        userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing user's information.
     * The ID of the user to update is taken from the path variable.
     *
     * @param id      The ID of the user to update.
     * @param request The request body containing the updated details for the user.
     * @return A ResponseEntity with HTTP status 200 (OK) on successful update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody UserDTO request) {
        userService.updateUser(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id The ID of the user to delete.
     * @return A ResponseEntity with HTTP status 204 (No Content) on successful
     * deletion.
     * Assumes that if the service method completes without throwing an
     * exception,
     * the deletion was successful. If the resource was not found, the
     * service
     * layer or global exception handler should return an appropriate error
     * status (e.g., 404).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        userService.removeById(id);
        return ResponseEntity.noContent().build(); // 204 No Content is appropriate for successful deletion
    }

    /**
     * Queries a paginated list of users.
     * Pagination, sorting, and keyword filtering parameters are expected as query
     * parameters.
     *
     * @param request The request object containing pagination (page, size, sort,
     *                isAsc)
     *                and keyword filtering parameters.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the total count and the list of paginated users.
     */
    @GetMapping
    public ResponseEntity<Result<List<UserPageVO>>> queryUsers(@Valid KeyWordPageRequest request) {
        IPage<UserPageVO> paginatedUsers = userService.page(request);
        Result<List<UserPageVO>> response = Result.success(paginatedUsers.getRecords(), paginatedUsers.getTotal());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single user by their ID.
     *
     * @param id The ID of the user to retrieve.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the requested user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<UserSimpleVO>> queryUser(@PathVariable("id") Long id) {
        UserSimpleVO user = userService.getUserById(id);
        Result<UserSimpleVO> response = Result.success(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the roles associated with a specific user by their ID.
     *
     * @param userId The ID of the user for whom roles are to be retrieved.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing a list of roles associated with the user.
     */
    @GetMapping("/roles")
    public ResponseEntity<Result<List<RoleCheckVO>>> queryUserRoles(@RequestParam("userId") Long userId) {
        List<RoleCheckVO> selectCheckedRoles = roleService.selectCheckedRoles(userId);
        Result<List<RoleCheckVO>> response = Result.success(selectCheckedRoles);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * This endpoint typically returns details about the user, such as their name,
     * email, and other relevant information.
     *
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the user's profile information.
     */
    @GetMapping("/profile")
    public ResponseEntity<Result<UserSimpleVO>> queryUserProfile() {
        Long id = SecurityContextUtils.getRequiredUserId();
        UserSimpleVO user = userService.getUserById(id);
        Result<UserSimpleVO> response = Result.success(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Queries the buttons associated with the currently authenticated user.
     * This endpoint typically returns buttons relevant to the user's
     * roles/permissions.
     *
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing a list of buttons accessible by the current user.
     */
    @GetMapping("/buttons")
    public ResponseEntity<Result<List<String>>> queryUserButton() {
        Long id = SecurityContextUtils.getRequiredUserId();
        List<String> buttons = buttonService.select(id); // Assuming this method selects buttons for the current user

        Result<List<String>> response = Result.success(buttons);
        return ResponseEntity.ok(response);
    }

    /**
     * Queries the navigation menus associated with the currently authenticated
     * user.
     * This endpoint typically returns menus relevant to the user's
     * roles/permissions.
     *
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing a list of navigation menus accessible by the current user.
     */
    @GetMapping("/menus")
    public ResponseEntity<Result<List<LeftNavVO>>> queryUserMenus() {
        Long id = SecurityContextUtils.getRequiredUserId();
        List<LeftNavVO> menus = menuService.selectSidebar(id); // Assuming this method selects navigation menus for the
        // current user

        Result<List<LeftNavVO>> response = Result.success(menus);
        return ResponseEntity.ok(response);
    }
}
