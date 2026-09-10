package com.englishlearn.security;

import com.englishlearn.common.ApiException;
import com.englishlearn.entity.User;
import com.englishlearn.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 手动鉴权门面：从 Authorization: Bearer <token> 解析当前用户并执行角色校验。
 * 错误码与提示与原 FastAPI 后端一一对应。
 */
@Component
public class AuthFacade {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthFacade(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public User requireUser() {
        String token = bearerToken();
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录凭证无效，请重新登录");
        }
        Integer userId;
        try {
            userId = Integer.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录凭证无效，请重新登录");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        if (user.status == null || user.status != 1) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已被停用");
        }
        return user;
    }

    public User requireAdmin() {
        User user = requireUser();
        if (!"admin".equals(user.userRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }

    private String bearerToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}