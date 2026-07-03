package com.example.freshfarm3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Exposes files saved by FileUploadService at {@code app.upload.base-url}
 * (default {@code /uploads/**}) so the URLs it returns are directly usable
 * as <img src="..."> targets.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:/uploads}")
    private String baseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "**";
        String location = "file:" + Paths.get(uploadDir).toAbsolutePath() + "/";

        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
