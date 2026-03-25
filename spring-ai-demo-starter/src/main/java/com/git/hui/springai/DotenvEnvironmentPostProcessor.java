package com.git.hui.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取 .env 文件，自动注入环境变量
 * @author YiHui
 * @date 2026/3/23
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "SpringAI-Dotenv";
    private static final String DOTENV_FILE = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenvPath = Path.of(System.getProperty("user.dir"), DOTENV_FILE);
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        Map<String, Object> properties = loadDotenv(dotenvPath);
        if (properties.isEmpty()) {
            return;
        }

        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            log.info("Replacing existing {} property source with {}", PROPERTY_SOURCE_NAME, DOTENV_FILE);
            environment.getPropertySources().replace(PROPERTY_SOURCE_NAME, new SystemEnvironmentPropertySource(PROPERTY_SOURCE_NAME, properties));
            return;
        }

        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(PROPERTY_SOURCE_NAME, properties));
        log.info("Loaded {} entries from {}", properties.size(), DOTENV_FILE);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Map<String, Object> loadDotenv(Path dotenvPath) {
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }

                properties.put(key, unquote(value));
            }
        } catch (IOException ignored) {
            // Ignore malformed or unreadable .env files and continue with normal environment resolution.
        }
        return properties;
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
