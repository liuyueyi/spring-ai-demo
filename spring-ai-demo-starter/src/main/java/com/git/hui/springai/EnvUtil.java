package com.git.hui.springai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从项目根目录的 .env 文件中读取配置的工具类
 *
 * @author YiHui
 * @date 2026/7/22
 */
public class EnvUtil {

    private static final String DOTENV_FILE = ".env";

    /** 懒加载缓存，启动时解析一次 */
    private static volatile Map<String, String> cached = null;

    /**
     * 获取指定 key 的值
     *
     * @param key 配置键
     * @return 对应的值，不存在则返回 null
     */
    public static String get(String key) {
        return getEnvMap().get(key);
    }

    /**
     * 获取指定 key 的值，带默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 对应的值，不存在则返回 defaultValue
     */
    public static String get(String key, String defaultValue) {
        String value = getEnvMap().get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取硅基流动 API Key
     */
    public static String getSiliconApiKey() {
        return get("silicon-api-key");
    }

    /**
     * 获取智谱 API Key
     */
    public static String getZhipuApiKey() {
        return get("zhipuai-api-key");
    }

    /**
     * 获取讯飞 API Key
     */
    public static String getSparkApiKey() {
        return get("spark-api-key");
    }

    /**
     * 获取阿里百炼 API Key
     */
    public static String getDashboardApiKey() {
        return get("dashboard-api-key");
    }

    /**
     * 获取英伟达 API Key
     */
    public static String getNvidiaApiKey() {
        return get("nvidia-api-key");
    }

    // -------------------- 内部实现 --------------------

    private static Map<String, String> getEnvMap() {
        if (cached == null) {
            synchronized (EnvUtil.class) {
                if (cached == null) {
                    cached = loadDotenv();
                }
            }
        }
        return cached;
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> properties = new LinkedHashMap<>();
        // 从项目根目录读取 .env（通过 user.dir 定位）
        Path dotenvPath = Path.of(System.getProperty("user.dir"), DOTENV_FILE);
        if (!Files.isRegularFile(dotenvPath)) {
            System.err.println("[EnvUtil] .env file not found at: " + dotenvPath.toAbsolutePath());
            return properties;
        }

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
            System.out.println("[EnvUtil] Loaded " + properties.size() + " entries from " + DOTENV_FILE);
        } catch (IOException e) {
            System.err.println("[EnvUtil] Failed to read .env file: " + e.getMessage());
        }
        return properties;
    }

    private static String unquote(String value) {
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
