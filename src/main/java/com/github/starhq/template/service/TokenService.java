package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.token.TokenSimpleDTO;
import com.github.starhq.template.model.vo.token.TokenPageVO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Token服务接口
 *
 * @author starhq
 */
public interface TokenService extends IService<SysToken> {

    /**
     * Removes the token associated with the current user.
     *
     * @param userId the user's token to revoke
     * @return true if the removal was successful
     * @throws BusinessException if the removal fails
     */
    boolean removeByUserId(Long userId);

    /**
     * Retrieves the token associated with the current user.
     *
     * @return the token response containing the user's token details
     */
    TokenSimpleDTO getByUserId(Long userId);

    /**
     * Retrieves a paginated list of tokens based on the provided pagination info.
     *
     * @param pageInfo the pagination and sorting information
     * @return a paginated response of tokens
     */
    IPage<TokenPageVO> page(KeyWordPageRequest pageInfo);

    /**
     * Builds a JWT token for the user and saves the session token.
     *
     * @param user              the user for whom the token is being built
     * @return the generated JWT token
     */
    JwtToken build(UserDetails user);

    /**
     * Refreshes the JWT token for the currently authenticated user.
     *
     * @return the new JWT token
     * @throws BadCredentialsException if the user is not found
     * @throws DisabledException       if the user account is not active
     * @throws AccessDeniedException   if the user has no roles assigned
     */
    JwtToken refresh();

}
