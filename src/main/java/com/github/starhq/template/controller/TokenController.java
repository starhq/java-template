package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.KeyWordPageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.TokenPageVO;
import com.github.starhq.template.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful API controller for managing authentication token lifecycle.
 * Provides standardized endpoints for revoking user tokens and querying
 * token audit logs, supporting security operations and session management
 * in JWT/OAuth2-based authentication systems.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 */
@RestController
@RequestMapping(value = "/{version}/tokens", version = "v1")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    /**
     * Revokes all active tokens associated with a specific user.
     * Typically used for forced logout, password reset, or account suspension
     * scenarios to invalidate existing sessions.
     *
     * @param userId the unique identifier of the user whose tokens should be revoked
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) upon successful revocation
     * @throws com.github.starhq.template.common.exception.BusinessException if the user ID is invalid or revocation fails
     */
    @PutMapping("/{userId}/revoked")
    public ResponseEntity<Void> revoked(@PathVariable("userId") Long userId) {
        tokenService.removeByUserId(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a paginated list of token audit records with optional keyword filtering.
     * Supports filtering by user info, token status, or time range for security auditing.
     *
     * @param request the {@link KeyWordPageRequest} containing pagination, sorting, and keyword filter parameters
     * @return a {@link ResponseEntity} containing a {@link Result} wrapper with total count and paginated {@link TokenPageVO} records
     */
    @GetMapping
    public ResponseEntity<Result<List<TokenPageVO>>> queryTokens(@Valid KeyWordPageRequest request) {
        IPage<TokenPageVO> paginatedTokens = tokenService.page(request);
        Result<List<TokenPageVO>> response = Result.success(paginatedTokens.getRecords(), paginatedTokens.getTotal());
        return ResponseEntity.ok(response);
    }
}