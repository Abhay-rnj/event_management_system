package com.eventmanager.config;

import com.eventmanager.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;

/** Serves uploaded banner images from the uploads folder at /uploads/... */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService files;

    public WebConfig(FileStorageService files) {
        this.files = files;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Files.createDirectories(files.directory());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload folder " + files.directory(), e);
        }
        String location = files.directory().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
