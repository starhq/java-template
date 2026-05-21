package com.github.starhq.template.controller;

import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.model.dto.user.LoginDTO;
import com.github.starhq.template.model.dto.user.ResetPasswordDTO;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.service.AuthService;
import com.github.starhq.template.service.CaptchaService;
import com.github.starhq.template.service.LoginService;
import com.github.starhq.template.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling authentication, token management, and captcha generation.
 *
 * <p>This controller exposes the unauthenticated endpoints (public API) for user identity verification and token lifecycle management.
 * All successful responses strictly follow a standardized {@link Result} wrapper to ensure a uniform JSON response structure.
 *
 * <p><b>Architecture Note:</b> The path variable {@code /v1/auth} is intentionally hardcoded here. While dynamic versioning is possible via {@code /{version}/auth},
 * in a modern microservice architecture, versioning should be handled by API Gateways (like Nginx or Spring Cloud Gateway), not inside the application code.
 *
 * @author starhq
 */
@RestController
@RequestMapping(value = "/{version}/auth", version = "v1")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final TokenService tokenService;
    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * Authenticates user credentials and returns JWT tokens.
     *
     * <p>This is the primary entry point for standard username/password login. If successful,
     * it returns both an Access Token (for API calls) and a Refresh Token (for silent refresh).
     *
     * @param loginDTO the login credentials containing username, password, and optional captcha UUID
     * @return a standardized {@link Result} containing the generated JWT tokens
     */
    @PostMapping(value = "/login")
    public ResponseEntity<Result<JwtToken>> login(@Valid @RequestBody LoginDTO loginDTO) {
        JwtToken token = loginService.login(loginDTO);
        Result<JwtToken> result = Result.success(token);
        return ResponseEntity.ok(result);
    }

    /**
     * Exchanges a valid refresh token for a new Access Token.
     *
     * <p>This endpoint is typically used by frontend SPA (Single Page Applications) in the background to keep the
     * user logged in without forcing a redirect to the login page.
     *
     * @return a {@link ResponseEntity} containing the newly generated Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Result<JwtToken>> refresh() {
        JwtToken jwtToken = tokenService.refresh();
        Result<JwtToken> result = Result.success(jwtToken);
        return ResponseEntity.ok(result);
    }

    /**
     * Resets a user's password.
     *
     * <p><b>HTTP Method Note:</b> Uses {@code @Patch} instead of {@code @Put}.
     * While RESTful purists often prefer PUT for full updates, password resets typically involve comparing old vs new passwords.
     * If the old password is wrong, an exception is thrown, effectively acting like a conditional PUT but implemented as PATCH.
     *
     * @param request the DTO containing the old password, new password, and optional captcha UUID
     * @return a standardized {@link ResponseEntity}
     */
    @PatchMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Generates a graphical CAPTCHA image and writes it directly to the HTTP response.
     *
     * <p>This method bypasses Spring Security and is typically called by the frontend before the login request.
     * The `uuid` parameter binds to the generated image and the captcha logic to prevent blind SQL injection attacks.
     *
     * @param response the HTTP response object to write the image binary data to
     * @param uuid     the unique identifier linking the CAPTCHA image to the login request
     * @return a {@link ResponseEntity} with an empty body (image is written directly to the response stream)
     */
    @GetMapping("/captcha")
    public ResponseEntity<Void> captcha(HttpServletResponse response, @RequestParam(value = "uuid") String uuid) {
        captchaService.generateCode(uuid, response);
        return ResponseEntity.ok().build();
    }
}
