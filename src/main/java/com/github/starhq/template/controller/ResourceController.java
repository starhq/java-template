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
 * Controller for managing resource operations.
 * Provides endpoints for creating, updating, deleting, and querying resources.
 */
@RestController
@RequestMapping(value = "/{version}/resources", version = "v1")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService; // Service for handling resource operations

    /**
     * Creates a new resource.
     *
     * @param request the request containing resource creation details
     * @return a ResponseEntity indicating the result of the creation
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ResourceDTO request) {
        resourceService.createResource(request); // Call service to create resource
        return ResponseEntity.status(HttpStatus.CREATED).build(); // Return 201 Created status
    }

    /**
     * Updates an existing resource by its ID.
     *
     * @param id      the ID of the resource to update
     * @param request the request containing updated resource details
     * @return a ResponseEntity indicating the result of the update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody ResourceDTO request) {
        resourceService.updateResource(id, request); // Call service to update resource
        return ResponseEntity.ok().build(); // Return 200 OK status
    }

    /**
     * Deletes a resource by its ID.
     *
     * @param id the ID of the resource to delete
     * @return a ResponseEntity indicating the result of the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        resourceService.removeById(id); // Call service to remove resource
        return ResponseEntity.noContent().build(); // Return 204 No Content status
    }

    /**
     * Retrieves resources with pagination.
     *
     * @param pageRequest the pagination request parameters
     * @return a ResponseEntity containing the paginated resources
     */
    @GetMapping
    public ResponseEntity<Result<List<ResourcePageVO>>> queryResources(@Valid PageRequest pageRequest) {
        IPage<ResourcePageVO> paginatedResources = resourceService.page(pageRequest); // Fetch paginated resources
        Result<List<ResourcePageVO>> result = Result.success(paginatedResources.getRecords(), paginatedResources.getTotal());// Create response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }

    /**
     * Retrieves a resource by its ID.
     *
     * @param id the ID of the resource to retrieve
     * @return a ResponseEntity containing the resource details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<ResourceSimpleVO>> queryResourceById(@PathVariable("id") Long id) {
        ResourceSimpleVO resource = resourceService.getResourceById(id); // Fetch the resource by ID
        Result<ResourceSimpleVO> result = Result.success(resource); // Create response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }

}
