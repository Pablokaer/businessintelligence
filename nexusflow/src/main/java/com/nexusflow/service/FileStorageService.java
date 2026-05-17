package com.nexusflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${nexusflow.upload-dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String original = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "arquivo";
        String filename = UUID.randomUUID() + "_" + original;

        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public Resource load(String filename) {
        try {
            Path path = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) throw new RuntimeException("Arquivo não encontrado.");
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erro ao carregar arquivo.");
        }
    }
}
