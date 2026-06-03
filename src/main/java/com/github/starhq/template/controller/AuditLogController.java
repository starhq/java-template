package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.AuditLogPageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.AuditLogPageVO;
import com.github.starhq.template.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for handling audit log operations.
 * Provides endpoints for querying historical audit trails.
 */
@RestController
@RequestMapping(value = "/{version}/audit-logs", version = "v1")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService; // Service for handling audit log operations

    /**
     * Queries paginated audit logs based on filtering criteria.
     *
     * <p>This endpoint is typically called by frontend admin dashboards to display the system's operation history.
     * It uses MyBatis-Plus {@link IPage} for efficient database pagination.
     *
     * <p><b>Data Transformation Note:</b>
     * This method explicitly extracts {@code records} from {@link IPage} and passes them to {@link Result#success(Object)}.
     * We do not return the raw {@code IPage} object to avoid exposing internal pagination metadata
     * (like total pages, current page index) directly to the frontend JSON structure.
     *
     * @param request the query parameters (page size, current page, search criteria)
     * @return a standardized {@link ResponseEntity} containing the data list and total count
     */
    @GetMapping
    public ResponseEntity<Result<List<AuditLogPageVO>>> queryAuditLogs(
            @Valid AuditLogPageRequest request) {

        // Retrieve paginated audit logs from the service
        IPage<AuditLogPageVO> paginatedLogs = auditLogService.page(request);

        // Create a response containing total count and records
        Result<List<AuditLogPageVO>> result = Result.success(paginatedLogs.getRecords(), paginatedLogs.getTotal());
        return ResponseEntity.ok(result);
    }
}