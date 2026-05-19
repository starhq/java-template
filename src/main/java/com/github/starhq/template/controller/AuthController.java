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
 * 认证控制器
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
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping(value = "/login")
    public ResponseEntity<Result<JwtToken>> login(@Valid @RequestBody LoginDTO loginDTO) {
        JwtToken token = loginService.login(loginDTO);
        Result<JwtToken> result = Result.success(token);
        return ResponseEntity.ok(result);
    }

    /**
     * Handles requests to refresh JWT tokens.
     *
     * @return a ResponseEntity containing the new JWT token in the response body
     */
    @PostMapping("/refresh")
    public ResponseEntity<Result<JwtToken>> refresh() {
        JwtToken jwtToken = tokenService.refresh();
        Result<JwtToken> result = Result.success(jwtToken);
        return ResponseEntity.ok(result);
    }

    /**
     * Handles password reset requests.
     *
     * @param request the request containing the old and new passwords
     * @return a ResponseEntity indicating the result of the password reset
     * operation
     */
    @PatchMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Generates and sends a captcha image to the client.
     *
     * @param response the HttpServletResponse object used to send the captcha image
     * @param uuid     the unique identifier for the captcha
     */
    @GetMapping("/captcha")
    public ResponseEntity<Void> captcha(HttpServletResponse response, @RequestParam(value = "uuid") String uuid) {
        captchaService.generateCode(uuid, response);
        return ResponseEntity.ok().build();
    }
}
