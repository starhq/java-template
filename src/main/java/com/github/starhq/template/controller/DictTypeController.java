package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;
import com.github.starhq.template.service.DictTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing dictionary types.
 * Provides endpoints for creating, updating, deleting, and querying dictionary
 * type definitions.
 */
@RestController
@RequestMapping(value = "/{version}/dict-types", version = "v1")
@RequiredArgsConstructor
public class DictTypeController {

    private final DictTypeService dictTypeService;

    /**
     * Creates a new dictionary type entry.
     *
     * @param request The request body containing the details for the new dictionary
     *                type.
     * @return A ResponseEntity with HTTP status 201 (Created) on successful
     * creation.
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DictTypeDTO request) {
        dictTypeService.createDictType(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing dictionary type entry.
     * The ID of the dictionary type to update is taken from the path variable.
     *
     * @param id      The ID of the dictionary type to update.
     * @param request The request body containing the updated details for the
     *                dictionary type.
     * @return A ResponseEntity with HTTP status 200 (OK) on successful update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody DictTypeDTO request) {
        dictTypeService.updateDictType(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a dictionary type entry by its ID.
     * Note: This operation will fail if there are dictionary data entries
     * associated with this type.
     *
     * @param id The ID of the dictionary type to delete.
     * @return A ResponseEntity with HTTP status 204 (No Content) on successful
     * deletion.
     * If the type has associated data, a BusinessException will be thrown.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        dictTypeService.removeById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Queries a paginated list of dictionary type entries.
     * Parameters for pagination and filtering are expected as query parameters.
     *
     * @param request The request object containing pagination (page, size, sort,
     *                isAsc) parameters.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the total count and the list of paginated dictionary
     * types.
     */
    @GetMapping
    public ResponseEntity<Result<List<DictTypePageVO>>> queryDictTypes(@Valid PageRequest request) {
        IPage<DictTypePageVO> paginatedDictTypes = dictTypeService.page(request);

        Result<List<DictTypePageVO>> result = Result.success(paginatedDictTypes.getRecords(), paginatedDictTypes.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a single dictionary type entry by its ID.
     *
     * @param id The ID of the dictionary type to retrieve.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the requested dictionary type.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<DictTypeSimpleVO>> queryDictType(@PathVariable("id") Long id) {
        DictTypeSimpleVO dictType = dictTypeService.getDictDataById(id);

        Result<DictTypeSimpleVO> result = Result.success(dictType);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a list of all dictionary types and their associated data.
     *
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the list of dictionary types and their data.
     */
    @GetMapping("/all")
    public ResponseEntity<Result<List<DictTypeWithDataVO>>> queryDictTypeAndData() {
        List<DictTypeWithDataVO> dictTypeAndDataResponses = dictTypeService.selectDictTypeAndDataResponses();

        Result<List<DictTypeWithDataVO>> result = Result.success(dictTypeAndDataResponses);
        return ResponseEntity.ok(result);
    }
}
