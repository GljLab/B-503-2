package com.example.permission.controller;

import com.example.permission.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));

        String filename = UUID.randomUUID().toString() + extension;

        String relativePath = UPLOAD_DIR + "/" + datePath;
        File dir = new File(relativePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败");
        }

        String url = "/" + UPLOAD_DIR + "/" + datePath + "/" + filename;
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success("上传成功", data);
    }
}
