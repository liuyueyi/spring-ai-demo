package com.git.hui.springai.ali.mvc.config;

import com.git.hui.springai.ali.mvc.entity.Order;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据库初始化配置
 * 
 * @author YiHui
 * @date 2026/3/9
 */
@Component
public class DatabaseInitializer {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void init() {
        System.out.println("=== 初始化订单数据 ===");
        
        // 插入测试订单数据
        String sql = """
            INSERT INTO t_order (order_no, customer_name, customer_email, product_name, quantity, unit_price, total_amount, status, create_time, update_time) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        // 订单 1
        jdbcTemplate.update(sql, 
            "ORD20260309001", "张三", "zhangsan@example.com", 
            "iPhone 15 Pro", 1, new BigDecimal("8999.00"), new BigDecimal("8999.00"),
            "已完成", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 2
        jdbcTemplate.update(sql,
            "ORD20260309002", "李四", "lisi@example.com",
            "MacBook Pro 14", 1, new BigDecimal("14999.00"), new BigDecimal("14999.00"),
            "待发货", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 3
        jdbcTemplate.update(sql,
            "ORD20260309003", "王五", "wangwu@example.com",
            "AirPods Pro 2", 2, new BigDecimal("1899.00"), new BigDecimal("3798.00"),
            "已完成", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 4
        jdbcTemplate.update(sql,
            "ORD20260309004", "赵六", "zhaoliu@example.com",
            "iPad Air", 1, new BigDecimal("4799.00"), new BigDecimal("4799.00"),
            "待支付", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 5
        jdbcTemplate.update(sql,
            "ORD20260309005", "孙七", "sunqi@example.com",
            "Apple Watch S9", 1, new BigDecimal("3199.00"), new BigDecimal("3199.00"),
            "已完成", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 6
        jdbcTemplate.update(sql,
            "ORD20260309006", "周八", "zhouba@example.com",
            "Magic Keyboard", 1, new BigDecimal("2499.00"), new BigDecimal("2499.00"),
            "待发货", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 7
        jdbcTemplate.update(sql,
            "ORD20260309007", "吴九", "wujiu@example.com",
            "Studio Display", 1, new BigDecimal("11499.00"), new BigDecimal("11499.00"),
            "已完成", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 8
        jdbcTemplate.update(sql,
            "ORD20260309008", "郑十", "zhengshi@example.com",
            "Mac mini", 2, new BigDecimal("4499.00"), new BigDecimal("8998.00"),
            "待支付", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 9
        jdbcTemplate.update(sql,
            "ORD20260309009", "小明", "xiaoming@example.com",
            "HomePod mini", 3, new BigDecimal("749.00"), new BigDecimal("2247.00"),
            "已完成", LocalDateTime.now(), LocalDateTime.now());
        
        // 订单 10
        jdbcTemplate.update(sql,
            "ORD20260309010", "小红", "xiaohong@example.com",
            "Apple Pencil 2", 1, new BigDecimal("999.00"), new BigDecimal("999.00"),
            "待发货", LocalDateTime.now(), LocalDateTime.now());
        
        System.out.println("=== 订单数据初始化完成，共 10 条记录 ===");
    }
}
