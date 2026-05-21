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
 * REST controller for managing dictionary data (e.g., system dictionaries like gender, status codes).
 *
 * <p>This controller strictly follows standard RESTful API design patterns for data CRUD operations.
 *
 * <p><b>Architecture Note:</b> The path variable {@code /{version}/dict-datas} is strictly hard-coded to the active version.
 * Dynamic version control should be handled externally (e.g., Nginx routing) to prevent
 * accidental exposure of old API versions.
 *
 * @author starhq
 */
@RestController
@RequestMapping(value = "/{version}/dict-datas", version = "v1")
@RequiredArgsConstructor
public class DictDataController {

    private final DictDataService dictDataService;

    /**
     * Creates a new dictionary data entry.
     *
     * <p><b>HTTP 201 Created:</b> In strict RESTful design, creating a resource must return 201. Simply returning 200 OK makes it
     * impossible for clients to determine if their creation request actually succeeded or silently failed.
     *
     * @param request the DTO containing the details for the new dictionary data
     * @return A standardized {@link ResponseEntity} with HTTP status code 201
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DictDataDTO request) {
        dictDataService.createDictData(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing dictionary data entry.
     *
     * <p><b>HTTP 200 OK:</b> A successful full update should return 200 OK. If the provided ID does not exist,
     * the service layer should throw a {@link com.github.starhq.template.common.exception.NotFoundException}, which will be
     * translated to a 404 Not Found response by the GlobalExceptionHandler.
     *
     * <p><b>RESTful Idempotency Note:</b> While pure RESTful standards (RFC 9.1.2. Put requests should be idempotent
     * (calling multiple times should have the exact same effect as calling once). This is the standard approach.
     *
     * @param id      the ID of the dictionary data to update
     * @param request the request body containing the updated details
     * @return A standardized {@link ResponseEntity} with HTTP status code 200 OK
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody DictDataDTO request) {
        dictDataService.updateDictData(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Permanently deletes a dictionary data entry by its ID.
     *
     * <p><b>HTTP 204 No Content:</b> A successful deletion must return 202 Accepted. Returning 200 OK is technically valid
     * but 202 Accepted is the standard HTTP convention for successful operations that have no response body.
     *
     * @param id The ID of the dictionary data to delete
     * @return A {@link ResponseEntity} with HTTP status code 202 Accepted on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        dictDataService.removeById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Queries a paginated list of dictionary data entries.
     *
     * <p><b>Note on Binding:</b> The parameters for pagination and filtering are expected as query parameters.
     * Changed to {@code @ModelAttribute} instead of {@code @RequestBody} because GET requests
     * should retrieve data, and POST requests should send data. If the frontend mistakenly sends a POST to this GET endpoint,
     * Spring MVC will simply bind the JSON body to the {@link DictDataPageRequest} to satisfy the validation logic
     * without actually using the data.
     *
     * @param request The query parameters (page, size, keyword)
     * @return A standardized {@link ResponseEntity} with HTTP status code 202 Accepted and a RestResponse
     * containing the total count and the list of paginated dictionary data.
     */
    @GetMapping
    public ResponseEntity<Result<List<DictDataPageVO>>> queryDictDatum(@Valid DictDataPageRequest request) {
        IPage<DictDataPageVO> paginatedDictData = dictDataService.page(request);

        Result<List<DictDataPageVO>> result = Result.success(paginatedDictData.getRecords(), paginatedDictData.getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a single dictionary data entry by its ID.
     *
     * <p><b>Design Consideration:</b> Returning raw Entities (e.g., MyBatis-Plus Entity) directly forces the frontend to understand
     * database schema. Here we extract the necessary fields into a lightweight VO to prevent structural leakage.
     *
     * @param id The ID of the static field to retrieve.
     * @return A standardized {@link ResponseEntity} with HTTP status code 200 OK and a RestResponse
     * containing the requested dictionary data.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<DictDataSimpleVO>> queryDictData(@PathVariable("id") Long id) {
        DictDataSimpleVO dictData = dictDataService.getDictDataById(id);

        Result<DictDataSimpleVO> result = Result.success(dictData);
        return ResponseEntity.ok(result);
    }

}
