package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;
import com.github.starhq.template.service.ButtonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing system-level button definitions.
 *
 * <p>This controller provides standard CRUD operations for UI button metadata (e.g., menu buttons like "Add", "Edit", "Delete"). These APIs are typically
 * consumed by backend admin dashboards for dynamic menu generation.
 *
 * <p><b>Architecture Note:</b> The path is strictly hard-coded to the active version (e.g., /v1/buttons).
 * Dynamic version control (e.g., /v1/buttons, /v2/buttons) should be handled by the API Gateway
 * to prevent developers from accidentally exposing old API versions without proper testing.
 *
 * @author starhq
 */
@RestController
@RequestMapping(value = "/{version}/buttons", version = "v1")
@RequiredArgsConstructor
public class ButtonController {

    private final ButtonService buttonService; // Service for handling button operations

    /**
     * Creates a new button definition.
     *
     * <p><b>HTTP Status 201 Created:</b> In strict RESTful design, creating a resource must return 201. Simply returning 200 OK makes it
     * impossible for clients to determine if their creation request actually succeeded or silently failed.
     *
     * @param request the DTO containing the button details
     * @return a standardized {@link ResponseEntity} with HTTP status code 201
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ButtonDTO request) {
        buttonService.createButton(request); // Call service to create button
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing button definition by its unique ID.
     *
     * <p><b>HTTP Status 200 OK:</b> A successful full update should return 200 OK. If the provided ID does not exist,
     * the service layer should throw a {@link com.github.starhq.template.common.exception.NotFoundException}, which will be
     * translated to a 404 Not Found response by the GlobalExceptionHandler.
     * <p><b>RESTful Idempotency Note:</b>
     * While pure RESTful standards (RFC 9.1.2. Put requests should be idempotent (calling multiple times
     * should have the same effect as calling once). This is the standard RESTful approach.
     *
     * @param id      the unique identifier of the button
     * @param request the DTO containing updated details
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody ButtonDTO request) {
        buttonService.updateButton(id, request); // Call service to update button
        return ResponseEntity.ok().build(); // Return 200 OK status
    }

    /**
     * Permanently deletes a button definition by its unique ID.
     *
     * <p><b>HTTP Status 204 No Content:</b> A successful deletion must return 204. Returning 200 OK is technically valid
     * but 204 No Content is the standard HTTP convention for successful operations that have no response body.
     *
     * @param id the unique identifier of the button to delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        buttonService.removeById(id); // Call service to remove button
        return ResponseEntity.noContent().build(); // Return 204 No Content status
    }

    /**
     * Queries a paginated list of button definitions based on search criteria.
     *
     * <p><b>Design Note:</b> This method returns {@code List<ButtonPageVO>} instead of raw Entities.
     * Returning raw Entities exposes internal table structures and MyBatis-Plus pagination metadata (like total pages)
     * directly to the frontend, which breaks the abstraction layer.
     *
     * @param request the query parameters (page index, size, filters)
     * @return a standardized {@link ResponseEntity} containing the paginated data and total count
     */
    @GetMapping
    public ResponseEntity<Result<List<ButtonPageVO>>> queryButtons(@Valid ButtonPageRequest request) {
        IPage<ButtonPageVO> paginatedButtons = buttonService.page(request); // Fetch paginated buttons

        Result<List<ButtonPageVO>> result = Result.success(paginatedButtons.getRecords(), paginatedButtons.getTotal());// Create
// response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }

    /**
     * Retrieves a single button definition by its unique ID.
     *
     * <p><b>Design Consideration:</b> Returning an Entity directly forces the frontend to understand the database schema.
     * Here we extract the necessary fields into a lightweight VO to prevent structural leakage.
     *
     * @param id the unique identifier of the button
     * @return a standardized {@link ResponseEntity} containing the entity details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<ButtonSimpleVO>> queryButtonById(@PathVariable("id") Long id) {
        ButtonSimpleVO button = buttonService.getButtonById(id); // Fetch the button by ID

        Result<ButtonSimpleVO> result = Result.success(button); // Create response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }
}
