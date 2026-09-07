package com.mpc.controller;

import com.mpc.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    @Value("${mpc.upload.avatar-dir}")
    private String avatarDir;

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件大小不能超过10MB"));
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ext.matches("jpg|jpeg|png|gif|webp")) {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件类型"));
        }
        String url = saveFile(file, ext);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/group-avatar")
    public ResponseEntity<Map<String, String>> uploadGroupAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件大小不能超过10MB"));
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ext.matches("jpg|jpeg|png|gif|webp")) {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件类型"));
        }
        String url = saveFile(file, ext);
        return ResponseEntity.ok(Map.of("url", url));
    }

    private String saveFile(MultipartFile file, String ext) throws IOException {
        File dir = new File(avatarDir);
        if (!dir.exists()) dir.mkdirs();
        String filename = UUID.randomUUID() + "." + ext;
        file.transferTo(new File(dir, filename).getAbsoluteFile());
        return "/uploads/avatars/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
