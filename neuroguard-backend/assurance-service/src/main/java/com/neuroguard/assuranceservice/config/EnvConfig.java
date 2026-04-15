package com.neuroguard.assuranceservice.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import java.io.File;

@Configuration
@Slf4j
public class EnvConfig {

    public EnvConfig() {
        // Try multiple potential locations for .env file
        String[] possiblePaths = {
            // Project root (when running from IDE or root)
            System.getProperty("user.dir") + File.separator + ".env",
            // Parent of assurance-service (when running from assurance-service folder)
            System.getProperty("user.dir") + File.separator + ".." + File.separator + ".env",
            // Try to find NeuroGuard-saif root
            findNeuroGuardRoot()
        };

        File envFile = null;
        for (String path : possiblePaths) {
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    envFile = f;
                    break;
                }
            }
        }

        if (envFile != null && envFile.exists()) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(envFile.getParent())
                        .filename(".env")
                        .load();

                // Load all variables into System properties
                dotenv.entries().forEach(entry -> {
                    if (System.getProperty(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });

                log.info(".env file loaded successfully from: {}", envFile.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Error loading .env file: {}", e.getMessage());
            }
        } else {
            log.warn(".env file not found at:");
            for (String path : possiblePaths) {
                if (path != null && !path.isEmpty()) {
                    log.warn("   - {}", path);
                }
            }
            log.info("Using environment variables from system");
        }
    }

    private String findNeuroGuardRoot() {
        String userDir = System.getProperty("user.dir");
        File current = new File(userDir);

        // Traverse up the directory tree looking for NeuroGuard-saif folder
        while (current != null && current.exists()) {
            if (current.getName().equals("NeuroGuard-saif")) {
                return current.getAbsolutePath() + File.separator + ".env";
            }
            current = current.getParentFile();
        }

        return null;
    }
}
