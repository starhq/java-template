package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.DictTypeDTO;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.DictTypePageVO;
import com.github.starhq.template.model.vo.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.DictTypeWithDataVO;
import com.github.starhq.template.service.DictTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful API controller for managing dictionary types.
 * Provides standardized endpoints for creating, updating, deleting,
 * and querying dictionary type definitions.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
@RestController
@RequestMapping(value = "/{version}/dict-types", version = "v1")
@RequiredArgsConstructor
public class DictTypeController {

    private final DictTypeService dictTypeService;

    /**
     * Creates a new dictionary type entry.
     *
     * @param request the DTO containing the details for the new dictionary type
     * @return a {@link ResponseEntity} with HTTP status 201 (Created) upon successful creation
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DictTypeDTO request) {
        dictTypeService.createDictType(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing dictionary type entry by its unique identifier.
     *
     * @param id      the unique identifier of the dictionary type to update
     * @param request the DTO containing the updated details for the dictionary type
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) upon successful update
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id, @Valid @RequestBody DictTypeDTO request) {
        dictTypeService.updateDictType(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a dictionary type entry by its unique identifier.
     * This operation is restricted if there are active dictionary data entries
     * associated with the specified type.
     *
     * @param id the unique identifier of the dictionary type to delete
     * @return a {@link ResponseEntity} with HTTP status 204 (No Content) upon successful deletion
     * @throws com.github.starhq.template.common.exception.BusinessException if the dictionary type has associated data entries
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        dictTypeService.removeById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Retrieves a paginated list of dictionary type entries with optional filtering.
     * Pagination and sorting parameters are provided via query parameters.
     *
     * @param request the pagination and filtering request object
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the total count and paginated records
     */
    @GetMapping
    public ResponseEntity<Result<List<DictTypePageVO>>> queryDictTypes(@Valid PageRequest request) {
        IPage<DictTypePageVO> paginatedDictTypes = dictTypeService.page(request);
        Result<List<DictTypePageVO>> result = Result.success(paginatedDictTypes.getRecords(), paginatedDictTypes.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a single dictionary type entry by its unique identifier.
     *
     * @param id the unique identifier of the dictionary type to retrieve
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with the requested dictionary type data
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<DictTypeSimpleVO>> queryDictType(@PathVariable("id") Long id) {
        DictTypeSimpleVO dictType = dictTypeService.getDictDataById(id);
        Result<DictTypeSimpleVO> result = Result.success(dictType);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves all dictionary types along with their associated dictionary data entries.
     * Suitable for initializing dropdown menus or caching dictionary structures.
     *
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with a list of dictionary types and their nested data
     */
    @GetMapping("/all")
    public ResponseEntity<Result<List<DictTypeWithDataVO>>> queryDictTypeAndData() {
        List<DictTypeWithDataVO> dictTypeAndDataResponses = dictTypeService.selectDictTypeAndDataResponses();
        Result<List<DictTypeWithDataVO>> result = Result.success(dictTypeAndDataResponses);
        return ResponseEntity.ok(result);
    }
}