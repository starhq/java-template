package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.auditlog.AuditLogPageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
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
 * Provides endpoints for querying audit logs.
 */
@RestController
@RequestMapping(value = "/{version}/audit-logs", version = "v1")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService; // Service for handling audit log operations

    /**
     * Queries audit logs with pagination and filtering based on the provided
     * request.
     *
     * @param request the request containing pagination and filtering information
     * @return a ResponseEntity containing the paginated audit logs
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