package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.resource.ResourceDTO;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.resource.ResourcePageVO;
import com.github.starhq.template.model.vo.resource.ResourceSimpleVO;
import com.github.starhq.template.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful API controller for managing resource permissions.
 * Provides standardized endpoints for creating, updating, deleting,
 * and querying system resources (e.g., API endpoints, buttons, menus)
 * used in role-based access control (RBAC) scenarios.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
@RestController
@RequestMapping(value = "/{version}/resources", version = "v1")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * Creates a new resource entry with the provided details.
     * Typically used for registering new API endpoints or UI elements
     * into the permission system.
     *
     * @param request the {@link ResourceDTO} containing the resource creation parameters
     * @return a {@link ResponseEntity} with HTTP status 201 (Created) upon successful creation
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ResourceDTO request) {
        resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing resource entry by its unique identifier.
     *
     * @param id      the unique identifier of the resource to update
     * @param request the {@link ResourceDTO} containing the updated resource parameters
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) upon successful update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody ResourceDTO request) {
        resourceService.updateResource(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a resource entry by its unique identifier.
     * This operation is restricted if the resource is referenced by
     * active roles, permissions, or menu configurations.
     *
     * @param id the unique identifier of the resource to delete
     * @return a {@link ResponseEntity} with HTTP status 204 (No Content) upon successful deletion
     * @throws com.github.starhq.template.common.exception.BusinessException if the resource cannot be deleted due to dependencies
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        resourceService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of resource entries with optional filtering.
     * Supports sorting and field-based queries for admin console display.
     *
     * @param pageRequest the {@link PageRequest} containing pagination, sorting, and filtering parameters
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with total count and paginated {@link ResourcePageVO} records
     */
    @GetMapping
    public ResponseEntity<Result<List<ResourcePageVO>>> queryResources(@Valid PageRequest pageRequest) {
        IPage<ResourcePageVO> paginatedResources = resourceService.page(pageRequest);
        Result<List<ResourcePageVO>> result = Result.success(paginatedResources.getRecords(), paginatedResources.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a single resource entry by its unique identifier.
     * Suitable for editing forms or permission detail views.
     *
     * @param id the unique identifier of the resource to retrieve
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the {@link ResourceSimpleVO} details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<ResourceSimpleVO>> queryResourceById(@PathVariable("id") Long id) {
        ResourceSimpleVO resource = resourceService.getResourceById(id);
        Result<ResourceSimpleVO> result = Result.success(resource);
        return ResponseEntity.ok(result);
    }
}