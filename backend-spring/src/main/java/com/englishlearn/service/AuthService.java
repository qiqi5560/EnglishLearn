package com.englishlearn.service;

import com.englishlearn.entity.User;
import com.englishlearn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号服务：手机号自动注册/登录、密码注册、登录态维护（对齐 FastAPI auth_service）。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Map<String, String> SMS_STORE = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String mockSmsCode;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       @Value("${app.sms.mock-code:123456}") String mockSmsCode) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mockSmsCode = mockSmsCode;
    }

    public boolean phoneValid(String phone) {
        return phone != null && phone.chars().allMatch(Character::isDigit)
                && phone.length() >= 6 && phone.length() <= 20;
    }

    public String issueSmsCode(String phone) {
        SMS_STORE.put(phone, mockSmsCode);
        log.info("[mock-sms] 向 {} 发送验证码：{}", phone, mockSmsCode);
        return mockSmsCode;
    }

    public boolean checkSmsCode(String phone, String code) {
        String expected = SMS_STORE.get(phone);
        return expected != null && expected.equals(code);
    }

    @Transactional
    public User getOrCreateUser(String phone, String role, String nickname) {
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = new User();
            user.phone = phone;
            user.userRole = role;
            user.ageGroup = "adult";
            user.nickname = (nickname == null || nickname.isBlank())
                    ? "学习者" + phone.substring(phone.length() - 4)
                    : nickname;
            user.status = 1;
            user.registerTime = LocalDateTime.now();
            user = userRepository.save(user);
            log.info("注册新账号 phone={} role={} id={}", phone, role, user.userId);
        }
        return user;
    }

    public User authenticateByPassword(String phone, String password) {
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null || user.passwordHash == null) {
            return null;
        }
        return passwordEncoder.matches(password, user.passwordHash) ? user : null;
    }

    public boolean verifyPassword(String plain, String hashed) {
        return hashed != null && passwordEncoder.matches(plain, hashed);
    }

    public String hashPassword(String plain) {
        return passwordEncoder.encode(plain);
    }

    public void setPassword(User user, String password) {
        user.passwordHash = passwordEncoder.encode(password);
    }

    public void touchLogin(User user) {
        user.lastLoginTime = LocalDateTime.now();
    }
}