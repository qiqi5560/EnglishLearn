package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.common.TimeUtil;
import com.englishlearn.dto.Dtos;
import com.englishlearn.dto.UserDtos;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.User;
import com.englishlearn.entity.UserPartner;
import com.englishlearn.repository.UserPartnerRepository;
import com.englishlearn.repository.UserRepository;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.AuthService;
import com.englishlearn.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习者个人中心接口（/users）。
 */
@RestController
@RequestMapping("/users")
public class UsersController {

    private final AuthFacade authFacade;
    private final AuthService authService;
    private final PlanService planService;
    private final UserRepository userRepository;
    private final UserPartnerRepository partnerRepository;

    public UsersController(AuthFacade authFacade,
                           AuthService authService,
                           PlanService planService,
                           UserRepository userRepository,
                           UserPartnerRepository partnerRepository) {
        this.authFacade = authFacade;
        this.authService = authService;
        this.planService = planService;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
    }

    @GetMapping("/me")
    public ApiResponse getMe() {
        User user = authFacade.requireUser();
        return ApiResponse.ok(userPayload(user));
    }

    @PutMapping("/me")
    @Transactional
    public ApiResponse updateMe(@RequestBody UserDtos.UpdateMeIn body) {
        User user = authFacade.requireUser();
        if (body.nickname() != null) {
            if (body.nickname().isBlank()) {
                throw new ApiException(422, "昵称不能为空");
            }
            user.nickname = body.nickname().strip();
        }
        if (body.avatarUrl() != null) {
            user.avatarUrl = body.avatarUrl();
        }
        if (body.ageGroup() != null) {
            if (!List.of("child", "k12", "adult", "senior").contains(body.ageGroup())) {
                throw new ApiException(422, "年龄段取值不合法");
            }
            user.ageGroup = body.ageGroup();
        }
        if (body.guardianId() != null) {
            User guardian = userRepository.findById(body.guardianId()).orElse(null);
            if (guardian == null || !"guardian".equals(guardian.userRole)) {
                throw new ApiException(422, "指定的监护人不存在");
            }
            user.guardianId = guardian.userId;
        }
        userRepository.save(user);
        return ApiResponse.ok(userPayload(user), "资料已更新");
    }

    @PutMapping("/me/password")
    @Transactional
    public ApiResponse changePassword(@RequestBody UserDtos.PasswordIn body) {
        User user = authFacade.requireUser();
        if (body.newPassword() == null || body.newPassword().length() < 6) {
            throw new ApiException(422, "新密码至少 6 位");
        }
        if (user.passwordHash != null && !authService.verifyPassword(body.oldPassword(), user.passwordHash)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "原密码错误");
        }
        user.passwordHash = authService.hashPassword(body.newPassword());
        userRepository.save(user);
        return ApiResponse.ok(null, "密码修改成功");
    }

    @PostMapping("/me/bind-guardian")
    @Transactional
    public ApiResponse bindGuardian(@RequestBody UserDtos.BindGuardianIn body) {
        User user = authFacade.requireUser();
        User guardian = authService.getOrCreateUser(body.guardianPhone(), "guardian", "监护人");
        if (!authService.checkSmsCode(body.guardianPhone(), body.code())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "监护人验证码错误或已过期");
        }
        guardian.userRole = "guardian";
        userRepository.save(guardian);
        user.guardianId = guardian.userId;
        userRepository.save(user);
        return ApiResponse.ok(userPayload(user), "绑定监护人成功");
    }

    @GetMapping("/me/partners")
    public ApiResponse listPartners() {
        User user = authFacade.requireUser();
        List<Map<String, Object>> list = partnerRepository.findByUserIdOrderByIdAsc(user.userId)
                .stream().map(this::partnerPayload).toList();
        return ApiResponse.ok(list);
    }

    @PostMapping("/me/partners")
    @Transactional
    public ApiResponse addPartner(@RequestBody UserDtos.AddPartnerIn body) {
        User user = authFacade.requireUser();
        if (!authService.phoneValid(body.phone())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "手机号格式不正确");
        }
        User partner = userRepository.findByPhone(body.phone()).orElse(null);
        if (partner == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "该手机号尚未注册");
        }
        if (partner.userId.equals(user.userId)) {
            throw new ApiException(422, "不能添加自己为搭子");
        }
        if (partnerRepository.existsByUserIdAndPartnerUserId(user.userId, partner.userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "该搭子已在列表中");
        }
        UserPartner rel = new UserPartner();
        rel.userId = user.userId;
        rel.partnerUserId = partner.userId;
        partnerRepository.save(rel);
        return ApiResponse.ok(partnerPayload(rel), "添加成功");
    }

    @DeleteMapping("/me/partners/{partnerUserId}")
    @Transactional
    public ApiResponse deletePartner(@PathVariable Integer partnerUserId) {
        User user = authFacade.requireUser();
        if (!partnerRepository.existsByUserIdAndPartnerUserId(user.userId, partnerUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "该搭子不存在");
        }
        partnerRepository.deleteByUserIdAndPartnerUserId(user.userId, partnerUserId);
        return ApiResponse.ok(null, "已删除");
    }

    private Map<String, Object> partnerPayload(UserPartner rel) {
        User partner = userRepository.findById(rel.partnerUserId).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", rel.partnerUserId);
        m.put("nickname", partner != null && partner.nickname != null ? partner.nickname : "用户");
        m.put("phone", partner != null ? partner.phone : "");
        m.put("avatarUrl", partner != null ? partner.avatarUrl : null);
        String level = partner != null ? planService.currentLevelOf(planService.getActivePlan(partner)) : null;
        m.put("level", level);
        m.put("addedTime", TimeUtil.iso(rel.createTime));
        return m;
    }

    private Map<String, Object> userPayload(User user) {
        LearningPlan plan = planService.getActivePlan(user);
        return Dtos.userToDict(user, planService.currentLevelOf(plan));
    }
}