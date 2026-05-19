package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.dictData.DictDataDTO;
import com.github.starhq.template.model.dto.dictData.DictDataPageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.dictData.DictDataPageVO;
import com.github.starhq.template.model.vo.dictData.DictDataSimpleVO;
import com.github.starhq.template.service.DictDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing dictionary data.
 * Provides endpoints for creating, updating, deleting, and querying dictionary
 * entries.
 */
@RestController
@RequestMapping(value = "/{version}/dict-datas", version = "v1")
@RequiredArgsConstructor
public class DictDataController {

    private final DictDataService dictDataService;

    /**
     * Creates a new dictionary data entry.
     *
     * @param request The request body containing the details for the new dictionary
     *                data.
     * @return A ResponseEntity with HTTP status 201 (Created) on successful
     * creation.
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DictDataDTO request) {
        dictDataService.createDictData(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing dictionary data entry.
     * The ID of the dictionary data to update is taken from the path variable.
     *
     * @param id      The ID of the dictionary data to update.
     * @param request The request body containing the updated details for the
     *                dictionary data.
     * @return A ResponseEntity with HTTP status 200 (OK) on successful update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody DictDataDTO request) {
        dictDataService.updateDictData(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a dictionary data entry by its ID.
     *
     * @param id The ID of the dictionary data to delete.
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
        dictDataService.removeById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Queries a paginated list of dictionary data entries.
     * Parameters for pagination and filtering are expected as query parameters.
     *
     * @param request The request object containing pagination (page, size, sort,
     *                isAsc)
     *                and keyword filtering parameters.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the total count and the list of paginated dictionary data.
     */
    @GetMapping
    public ResponseEntity<Result<List<DictDataPageVO>>> queryDictDatum(@Valid DictDataPageRequest request) {
        // Changed @RequestBody to @ModelAttribute for GET requests to bind query
        // parameters.
        IPage<DictDataPageVO> paginatedDictData = dictDataService.page(request);

        Result<List<DictDataPageVO>> result = Result.success(paginatedDictData.getRecords(), paginatedDictData.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a single dictionary data entry by its ID.
     *
     * @param id The ID of the dictionary data to retrieve.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the requested dictionary data.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<DictDataSimpleVO>> queryDictData(@PathVariable("id") Long id) {
        DictDataSimpleVO dictData = dictDataService.getDictDataById(id);

        Result<DictDataSimpleVO> result = Result.success(dictData);
        return ResponseEntity.ok(result);
    }

}
