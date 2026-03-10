package com.git.hui.springai.ali.mvc.tool;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用 CURD 工具类
 * 支持执行 SELECT、INSERT、UPDATE、DELETE 四种 SQL 操作
 * 
 * @author YiHui
 * @date 2026/3/9
 */
@Component
public class CurdTool {
    
    private JdbcTemplate jdbcTemplate;
    
    // SQL 操作类型枚举
    public enum SqlType {
        SELECT("查询"),
        INSERT("插入"),
        UPDATE("更新"),
        DELETE("删除"),
        UNKNOWN("未知");
        
        private final String description;
        
        SqlType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 执行 SQL 操作
     * 
     * @param jdbcTemplate JdbcTemplate 实例
     * @param sql SQL 语句
     * @param params 参数列表（可选）
     * @return 执行结果
     */
    public Map<String, Object> execute(JdbcTemplate jdbcTemplate, String sql, List<Object> params) {
        this.jdbcTemplate = jdbcTemplate;
        System.out.println("[execute] 执行 SQL：" + sql);
        System.out.println("[execute] 参数列表：" + params);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 预处理 SQL：将 %s 占位符转换为 ?
            String processedSql = preprocessSql(sql);
            if (!processedSql.equals(sql)) {
                System.out.println("[execute] SQL 已预处理：" + processedSql);
            }
            
            // 1. 识别 SQL 类型
            SqlType sqlType = identifySqlType(processedSql);
            result.put("sqlType", sqlType);
            result.put("sql", processedSql);
            
            // 2. 根据 SQL 类型执行不同操作
            switch (sqlType) {
                case SELECT:
                    return executeSelect(sql, params);
                case INSERT:
                    return executeInsert(sql, params);
                case UPDATE:
                    return executeUpdate(sql, params);
                case DELETE:
                    return executeDelete(sql, params);
                default:
                    result.put("success", false);
                    result.put("message", "❌ 不支持的 SQL 操作类型");
                    return result;
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "❌ SQL 执行失败：" + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return result;
        }
    }
    
    /**
     * 预处理 SQL 语句
     * 将 Python 风格的 %s 占位符转换为 JDBC 的 ? 占位符
     */
    private String preprocessSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        
        // 将 %s 替换为 ?
        String processed = sql.replaceAll("%s", "?");
        
        // 清理多余的占位符（如 %(name)s 等）
        processed = processed.replaceAll("%\\([^)]+\\)s", "?");
        
        return processed;
    }
    
    /**
     * 识别 SQL 类型
     */
    private SqlType identifySqlType(String sql) {
        String trimmedSql = sql.trim().toUpperCase();
        
        if (trimmedSql.startsWith("SELECT")) {
            return SqlType.SELECT;
        } else if (trimmedSql.startsWith("INSERT")) {
            return SqlType.INSERT;
        } else if (trimmedSql.startsWith("UPDATE")) {
            return SqlType.UPDATE;
        } else if (trimmedSql.startsWith("DELETE")) {
            return SqlType.DELETE;
        }
        
        return SqlType.UNKNOWN;
    }
    
    /**
     * 执行 SELECT 查询
     */
    private Map<String, Object> executeSelect(String sql, List<Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> data;
        if (params != null && !params.isEmpty()) {
            data = jdbcTemplate.queryForList(sql, params.toArray());
        } else {
            data = jdbcTemplate.queryForList(sql);
        }
        
        result.put("success", true);
        result.put("sqlType", SqlType.SELECT);
        result.put("rowCount", data.size());
        result.put("data", data);
        
        // 构建友好的消息
        StringBuilder message = new StringBuilder();
        message.append("✅ 查询成功！\n");
        message.append("共查询到 ").append(data.size()).append(" 条记录\n\n");
        
        if (!data.isEmpty()) {
            // 显示前 5 条结果
            int displayCount = Math.min(data.size(), 5);
            for (int i = 0; i < displayCount; i++) {
                message.append("【记录 ").append(i + 1).append("】\n");
                Map<String, Object> row = data.get(i);
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    message.append("  ").append(entry.getKey())
                            .append(": ").append(entry.getValue()).append("\n");
                }
                message.append("\n");
            }
            
            if (data.size() > 5) {
                message.append("... 还有 ").append(data.size() - 5)
                        .append(" 条记录，请使用 LIMIT 等条件筛选\n");
            }
        }
        
        result.put("message", message.toString());
        return result;
    }
    
    /**
     * 执行 INSERT 插入
     */
    private Map<String, Object> executeInsert(String sql, List<Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        int rowsAffected;
        if (params != null && !params.isEmpty()) {
            rowsAffected = jdbcTemplate.update(sql, params.toArray());
        } else {
            rowsAffected = jdbcTemplate.update(sql);
        }
        
        result.put("success", true);
        result.put("sqlType", SqlType.INSERT);
        result.put("rowsAffected", rowsAffected);
        result.put("message", String.format("✅ 插入成功！影响了 %d 行记录", rowsAffected));
        
        return result;
    }
    
    /**
     * 执行 UPDATE 更新
     */
    private Map<String, Object> executeUpdate(String sql, List<Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        int rowsAffected;
        if (params != null && !params.isEmpty()) {
            rowsAffected = jdbcTemplate.update(sql, params.toArray());
        } else {
            rowsAffected = jdbcTemplate.update(sql);
        }
        
        result.put("success", true);
        result.put("sqlType", SqlType.UPDATE);
        result.put("rowsAffected", rowsAffected);
        result.put("message", String.format("✅ 更新成功！影响了 %d 行记录", rowsAffected));
        
        return result;
    }
    
    /**
     * 执行 DELETE 删除
     */
    private Map<String, Object> executeDelete(String sql, List<Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        int rowsAffected;
        if (params != null && !params.isEmpty()) {
            rowsAffected = jdbcTemplate.update(sql, params.toArray());
        } else {
            rowsAffected = jdbcTemplate.update(sql);
        }
        
        result.put("success", true);
        result.put("sqlType", SqlType.DELETE);
        result.put("rowsAffected", rowsAffected);
        result.put("message", String.format("✅ 删除成功！影响了 %d 行记录", rowsAffected));
        
        return result;
    }
    
    /**
     * 验证 SQL 安全性（防止 SQL 注入等危险操作）
     * 
     * @param sql SQL 语句
     * @return 是否安全
     */
    public boolean isSqlSafe(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含危险的关键字
        String[] dangerousKeywords = {
            "DROP ", "TRUNCATE ", "ALTER ", "GRANT ", "REVOKE ",
            "CREATE ", "EXEC ", "EXECUTE ", "CALL ", "LOAD_FILE",
            "INTO OUTFILE", "INTO DUMPFILE"
        };
        
        String upperSql = sql.toUpperCase();
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                System.err.println("[SQL 安全检查] 检测到危险操作：" + keyword);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 格式化 SQL 执行结果为前端友好的格式
     */
    public String formatResult(Map<String, Object> result) {
        if (result == null) {
            return "❌ 执行结果为空";
        }
        
        Boolean success = (Boolean) result.get("success");
        if (success == null || !success) {
            return (String) result.getOrDefault("message", "❌ 执行失败");
        }
        
        SqlType sqlType = (SqlType) result.get("sqlType");
        if (sqlType == null) {
            return "❌ 无法识别 SQL 类型";
        }
        
        StringBuilder formatted = new StringBuilder();
        
        switch (sqlType) {
            case SELECT:
                Integer rowCount = (Integer) result.get("rowCount");
                formatted.append("✅ 查询成功！\n");
                formatted.append("共查询到 ").append(rowCount).append(" 条记录\n");
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                if (data != null && !data.isEmpty()) {
                    formatted.append("\n--- 查询结果 ---\n");
                    int displayCount = Math.min(data.size(), 10);
                    for (int i = 0; i < displayCount; i++) {
                        formatted.append("【记录 ").append(i + 1).append("】\n");
                        Map<String, Object> row = data.get(i);
                        if (row != null) {
                            for (Map.Entry<String, Object> entry : row.entrySet()) {
                                formatted.append("  ").append(entry.getKey())
                                        .append(": ").append(entry.getValue()).append("\n");
                            }
                            formatted.append("\n");
                        }
                    }
                    
                    if (data.size() > 10) {
                        formatted.append("... 还有 ").append(data.size() - 10)
                                .append(" 条记录\n");
                    }
                }
                break;
                
            case INSERT:
            case UPDATE:
            case DELETE:
                Integer rowsAffected = (Integer) result.get("rowsAffected");
                formatted.append("✅ ").append(sqlType.getDescription()).append("成功！\n");
                formatted.append("影响了 ").append(rowsAffected).append(" 行记录");
                break;
                
            default:
                formatted.append("⚠️ 未知操作类型");
        }
        
        return formatted.toString();
    }
}
