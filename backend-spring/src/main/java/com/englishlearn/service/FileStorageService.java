package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 上传文件存储：目前只服务「自定义头像」。
 *
 * <p>图片存到 {@code app.upload-dir} 指定的目录（默认 ./uploads/avatars），
 * 通过 {@code /files/**} 静态资源映射对外提供访问，数据库里只存 URL 字符串，
 * 因此 users 表的 avatar_url 字段无需改动。
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    /** 头像 URL 里属于本站的路径片段，用于识别「旧头像要不要删」 */
    private static final String OWNED_MARKER = "/files/avatars/";

    private final Path root;
    private final String urlPrefix;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String dir,
                              @Value("${server.servlet.context-path:}") String contextPath) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.urlPrefix = contextPath == null || contextPath.isBlank() ? "" : contextPath;
    }

    /**
     * 保存头像图片。
     *
     * @return 可直接给前端 img src 使用的相对 URL，例如 {@code /api/files/avatars/12_1695...jpg}
     */
    public String saveAvatar(int userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择一张图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "图片不能超过 5MB");
        }
        String ext = extOf(file);
        try {
            Path dir = root.resolve("avatars");
            Files.createDirectories(dir);
            String name = userId + "_" + System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path target = dir.resolve(name).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return urlPrefix + OWNED_MARKER + name;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败，请稍后重试");
        }
    }

    /** 删除本站生成的旧头像；外部 URL（如默认头像）不做处理 */
    public void deleteIfOwned(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        int idx = url.lastIndexOf(OWNED_MARKER);
        if (idx < 0) {
            return;
        }
        String name = url.substring(idx + OWNED_MARKER.length());
        // 只取文件名，杜绝路径穿越
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve("avatars").resolve(name));
        } catch (IOException ignored) {
            // 旧文件删不掉不影响新头像
        }
    }

    /** 取扩展名：优先用原始文件名，缺失时按 MIME 推断，都不是图片则报错 */
    private String extOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        if (ALLOWED_EXT.contains(ext)) {
            return ext;
        }
        String type = file.getContentType();
        if ("image/png".equals(type)) {
            return "png";
        }
        if ("image/gif".equals(type)) {
            return "gif";
        }
        if ("image/webp".equals(type)) {
            return "webp";
        }
        if (type != null && type.startsWith("image/")) {
            return "jpg";
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持 JPG / PNG / GIF / WebP 图片");
    }
}
