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
 * 角色控制器
 *
 * @author starhq
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
     * Creates a new role in the system.
     *
     * @param dto The request body containing the details for the new role.
     * @return A ResponseEntity with HTTP status 201 (Created) on successful
     * creation.
     */
    @PostMapping
    public ResponseEntity<Void> createRole(@Valid @RequestBody RoleDTO dto) {
        roleService.createRole(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing role.
     * The ID of the role to update is taken from the path variable.
     *
     * @param id  The ID of the role to update.
     * @param dto The request body containing the updated details for the role.
     * @return A ResponseEntity with HTTP status 200 (OK) on successful update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        roleService.updateRole(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a role by its ID.
     *
     * @param id The ID of the role to delete.
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
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色VO
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<RoleSimpleVO>> queryRole(@PathVariable("id") Long id) {
        RoleSimpleVO role = roleService.getRoleById(id);
        Result<RoleSimpleVO> result = Result.success(role);
        return ResponseEntity.ok(result);
    }

    /**
     * 分页查询角色
     *
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<Result<List<RolePageVO>>> queryRoles(@Valid PageRequest pageRequest) {
        IPage<RolePageVO> paginatedResources = roleService.page(pageRequest); // Fetch paginated resources
        Result<List<RolePageVO>> result = Result.success(paginatedResources.getRecords(), paginatedResources.getTotal());// Create response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }

    /**
     * Queries the buttons associated with a specific role.
     *
     * @param roleId The ID of the role.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing a list of checked buttons for the specified role.
     */
    @GetMapping("/buttons")
    public ResponseEntity<Result<List<ButtonCheckVO>>> queryRoleButtons(@RequestParam("roleId") Long roleId) {
        List<ButtonCheckVO> buttons = buttonService.selectCheckedButtons(roleId);

        return ResponseEntity.ok(Result.success(buttons)
        );
    }

    /**
     * Queries the resources associated with a specific role.
     *
     * @param roleId The ID of the role.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing a list of checked resources for the specified role.
     */
    @GetMapping("/resources")
    public ResponseEntity<Result<List<ResourceCheckVO>>> queryRoleResources(@RequestParam("roleId") Long roleId) {
        List<ResourceCheckVO> resources = resourceService.selectCheckedResources(roleId);
        return ResponseEntity.ok(Result.success(resources));
    }

    /**
     * Queries the menus associated with a specific role.
     *
     * @param roleId The ID of the role.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing a list of checked menus for the specified role.
     */
    @GetMapping("/menus")
    public ResponseEntity<Result<List<MenuCheckVO>>> queryRoleMenus(@RequestParam("roleId") Long roleId) {
        List<MenuCheckVO> menus = menuService.selectCheckedMenus(roleId);
        return ResponseEntity.ok(Result.success(menus));
    }
}
