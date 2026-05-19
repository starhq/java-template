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
 * Controller for managing button operations.
 * Provides endpoints for creating, updating, deleting, and querying buttons.
 */
@RestController
@RequestMapping(value = "/{version}/buttons", version = "v1")
@RequiredArgsConstructor
public class ButtonController {

    private final ButtonService buttonService; // Service for handling button operations

    /**
     * Creates a new button.
     *
     * @param request the request containing button creation details
     * @return a ResponseEntity indicating the result of the creation
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ButtonDTO request) {
        buttonService.createButton(request); // Call service to create button
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing button by its ID.
     *
     * @param id      the ID of the button to update
     * @param request the request containing updated button details
     * @return a ResponseEntity indicating the result of the update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                               @Valid @RequestBody ButtonDTO request) {
        buttonService.updateButton(id, request); // Call service to update button
        return ResponseEntity.ok().build(); // Return 200 OK status
    }

    /**
     * Deletes a button by its ID.
     *
     * @param id the ID of the button to delete
     * @return a ResponseEntity indicating the result of the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        buttonService.removeById(id); // Call service to remove button
        return ResponseEntity.noContent().build(); // Return 204 No Content status
    }

    /**
     * Retrieves a paginated list of buttons based on the provided request.
     *
     * @param request the request containing pagination and sorting information
     * @return a ResponseEntity containing the paginated button responses
     */
    @GetMapping
    public ResponseEntity<Result<List<ButtonPageVO>>> queryButtons(@Valid ButtonPageRequest request) {
        IPage<ButtonPageVO> paginatedButtons = buttonService.page(request); // Fetch paginated buttons

        Result<List<ButtonPageVO>> result = Result.success(paginatedButtons.getRecords(), paginatedButtons.getTotal());// Create
// response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }

    /**
     * Retrieves a button by its ID.
     *
     * @param id the ID of the button to retrieve
     * @return a ResponseEntity containing the button details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<ButtonSimpleVO>> queryButtonById(@PathVariable("id") Long id) {
        ButtonSimpleVO button = buttonService.getButtonById(id); // Fetch the button by ID

        Result<ButtonSimpleVO> result = Result.success(button); // Create response
        return ResponseEntity.ok(result); // Return response with 200 OK status
    }
}
