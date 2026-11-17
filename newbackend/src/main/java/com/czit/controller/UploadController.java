package com.czit.controller;

import com.czit.common.BizException;
import com.czit.common.ErrorCode;
import com.czit.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传：保存到工作目录下的 uploads/yyyyMM/，返回可直接访问的相对 URL。
 *
 * <p>文件通过 WebMvcConfig 的静态资源映射以 {@code /uploads/**} 暴露。</p>
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif");

    @PostMapping
    public Result upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("仅支持 PNG / JPG / WEBP / GIF 图片");
        }

        String month = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        Path directory = Paths.get("uploads", month).toAbsolutePath().normalize();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = directory.resolve(fileName);
        try {
            Files.createDirectories(directory);
            file.transferTo(target.toFile());
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "图片保存失败，请稍后重试");
        }
        return Result.ok(Map.of("url", "/uploads/" + month + "/" + fileName));
    }
}
