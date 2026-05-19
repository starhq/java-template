package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.model.vo.token.TokenPageVO;
import com.github.starhq.template.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing authentication tokens.
 * Provides endpoints for revoking tokens and querying token lists.
 */
@RestController
@RequestMapping(value = "/{version}/tokens", version = "v1")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    /**
     * Revokes a specific token by its ID.
     *
     * @param userId The ID of the token to revoke.
     * @return A ResponseEntity with HTTP status 200 (OK) on successful revocation.
     * Assumes that if the service method completes without throwing an
     * exception,
     * the revocation was successful.
     */
    @PutMapping("/{userId}/revoked")
    public ResponseEntity<Void> revoked(@PathVariable("userId") Long userId) {
        tokenService.removeByUserId(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Queries a paginated list of tokens.
     * Pagination, sorting, and keyword filtering parameters are expected as query
     * parameters.
     *
     * @param request The request object containing pagination (page, size, sort,
     *                isAsc)
     *                and keyword filtering parameters.
     * @return A ResponseEntity with HTTP status 200 (OK) and a RestResponse
     * containing the total count and the list of paginated tokens.
     */
    @GetMapping
    public ResponseEntity<Result<List<TokenPageVO>>> queryTokens(@Valid KeyWordPageRequest request) {
        IPage<TokenPageVO> paginatedTokens = tokenService.page(request);

        Result<List<TokenPageVO>> response = Result.success(paginatedTokens.getRecords(), paginatedTokens.getTotal());
        return ResponseEntity.ok(response);
    }

}
