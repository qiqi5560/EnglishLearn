package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.AuthDtos;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.User;
import com.englishlearn.repository.UserRepository;
import com.englishlearn.security.JwtService;
import com.englishlearn.service.AuthService;
import com.englishlearn.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账号与鉴权接口（/auth）。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PlanService planService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          PlanService planService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.planService = planService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send-code")
    public ApiResponse sendCode(@RequestBody AuthDtos.SendCodeIn body) {
        if (!authService.phoneValid(body.phone())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "手机号格式不正确");
        }
        authService.issueSmsCode(body.phone());
        return ApiResponse.ok(null, "验证码已发送（mock 固定为 123456，请查看后端日志）");
    }

    @PostMapping("/login")
    @Transactional
    public ApiResponse login(@RequestBody AuthDtos.LoginIn body) {
        if (!authService.phoneValid(body.phone())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "手机号格式不正确");
        }
        User user;
        if (body.code() != null) {
            if (!authService.checkSmsCode(body.phone(), body.code())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "验证码错误或已过期，请重新获取");
            }
            user = authService.getOrCreateUser(body.phone(), "learner", body.nickname());
            if (isAgeGroup(body.ageGroup())) {
                user.ageGroup = body.ageGroup();
            }
        } else if (body.password() != null) {
            user = authService.authenticateByPassword(body.phone(), body.password());
            if (user == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
            }
        } else {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "请提供验证码或密码");
        }
        if (user.status == null || user.status != 1) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该账号已被停用，请联系管理员");
        }
        return ApiResponse.ok(loginPayload(user), "登录成功");
    }

    @PostMapping("/register")
    @Transactional
    public ApiResponse register(@RequestBody AuthDtos.RegisterIn body) {
        if (!authService.phoneValid(body.phone())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "手机号格式不正确");
        }
        User user = userRepository.findByPhone(body.phone()).orElse(null);
        if (user == null) {
            String nickname = (body.nickname() == null || body.nickname().isBlank()) ? null : body.nickname();
            user = authService.getOrCreateUser(body.phone(), "learner", nickname);
            authService.setPassword(user, body.password());
            user.ageGroup = isAgeGroup(body.ageGroup()) ? body.ageGroup() : "adult";
            userRepository.save(user);
        } else if (user.passwordHash == null) {
            authService.setPassword(user, body.password());
            userRepository.save(user);
        } else if (authService.authenticateByPassword(body.phone(), body.password()) == null) {
            throw new ApiException(HttpStatus.CONFLICT, "该手机号已注册，密码错误");
        }
        if (user.status == null || user.status != 1) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该账号已被停用");
        }
        return ApiResponse.ok(loginPayload(user), "注册成功");
    }

    @PostMapping("/admin/login")
    @Transactional
    public ApiResponse adminLogin(@RequestBody AuthDtos.AdminLoginIn body) {
        User user = authService.authenticateByPassword(body.phone(), body.password());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"admin".equals(user.userRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该账号不是管理员");
        }
        if (user.status == null || user.status != 1) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该账号已被停用");
        }
        return ApiResponse.ok(loginPayload(user), "登录成功");
    }

    private Map<String, Object> loginPayload(User user) {
        authService.touchLogin(user);
        userRepository.save(user);
        LearningPlan plan = planService.getActivePlan(user);
        String level = planService.currentLevelOf(plan);
        String token = jwtService.createToken(user.userId, user.userRole);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", token);
        data.put("user", Dtos.userToDict(user, level));
        return data;
    }

    private boolean isAgeGroup(String ageGroup) {
        return ageGroup != null && List.of("child", "k12", "adult", "senior").contains(ageGroup);
    }
}