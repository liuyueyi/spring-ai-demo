# H2 数据库集成说明

## 数据库配置

### 1. 数据库连接信息
- **URL**: `jdbc:h2:mem:testdb`
- **用户名**: `sa`
- **密码**: (空)
- **控制台路径**: `/h2-console`

### 2. 访问 H2 控制台
启动应用后，访问：`http://localhost:8080/h2-console`

使用以下信息登录：
- JDBC URL: `jdbc:h2:mem:testdb`
- 用户名：`sa`
- 密码：留空

## 数据表结构

### 订单表 (t_order)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 订单 ID（主键，自增） |
| order_no | VARCHAR(64) | 订单号 |
| customer_name | VARCHAR(128) | 客户姓名 |
| customer_email | VARCHAR(128) | 客户邮箱 |
| product_name | VARCHAR(256) | 产品名称 |
| quantity | INT | 数量 |
| unit_price | DECIMAL(10,2) | 单价 |
| total_amount | DECIMAL(10,2) | 总金额 |
| status | VARCHAR(32) | 订单状态 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

## 测试数据

系统启动时会自动初始化 10 条订单数据：

1. 张三 - iPhone 15 Pro - ¥8,999
2. 李四 - MacBook Pro 14 - ¥14,999
3. 王五 - AirPods Pro 2 (2 个) - ¥3,798
4. 赵六 - iPad Air - ¥4,799
5. 孙七 - Apple Watch S9 - ¥3,199
6. 周八 - Magic Keyboard - ¥2,499
7. 吴九 - Studio Display - ¥11,499
8. 郑十 - Mac mini (2 个) - ¥8,998
9. 小明 - HomePod mini (3 个) - ¥2,247
10. 小红 - Apple Pencil 2 - ¥999

## 使用示例

### 1. 访问前端页面
```
http://localhost:8080/sql/chat
```

### 2. 示例查询语句

#### 查询所有订单
```
查询所有订单信息
```

#### 查询特定客户的订单
```
查询张三的所有订单
```

#### 查询特定状态的订单
```
查询状态为"已完成"的订单
```

#### 统计查询
```
查询每个订单状态的订单数量
```

#### 金额统计
```
查询总销售额
```

#### 产品排名
```
查询销量最高的产品
```

#### 插入新订单
```
插入一个新订单：订单号 ORD20260309011，客户王五，产品 Apple TV，数量 1，价格 1499，状态待支付
```

#### 更新订单状态
```
将订单 ORD20260309004 的状态改为已发货
```

#### 删除订单
```
删除订单号为 ORD20260309008 的订单
```

## 安全限制

✅ **允许的 SQL 操作**：
- SELECT - 查询数据
- INSERT - 插入数据
- UPDATE - 更新数据
- DELETE - 删除数据

❌ **禁止的 SQL 操作**：
- DROP
- TRUNCATE
- ALTER
- CREATE
- GRANT
- REVOKE
- EXEC / EXECUTE
- CALL
- LOAD_FILE
- INTO OUTFILE / INTO DUMPFILE
- 其他危险操作

## 功能特性

1. **完整 CURD 支持**：支持 SELECT/INSERT/UPDATE/DELETE 四种 SQL 操作
2. **真实数据库查询**：使用 H2 内存数据库，执行真实的 SQL 查询
3. **人工审批机制**：所有 SQL 执行前需要用户确认
4. **安全性检查**：自动拦截危险 SQL 操作（DROP/TRUNCATE/ALTER 等）
5. **结果格式化**：清晰展示查询结果，最多显示 10 条详细记录
6. **错误处理**：SQL 执行失败时返回详细错误信息
7. **参数化查询**：支持参数化查询防止 SQL 注入
8. **影响行数统计**：INSERT/UPDATE/DELETE 操作返回影响的行数

## 技术栈

- **数据库**: H2 Database (内存模式)
- **ORM**: Spring Data JPA
- **JDBC**: Spring JdbcTemplate
- **AI 框架**: Spring AI Alibaba
- **Agent**: ReAct Agent with Human-in-the-Loop

## 注意事项

1. 数据仅在内存中，重启应用后数据会重置
2. 建议查询时使用 LIMIT 限制返回数量
3. SQL 语句由大模型生成，请仔细审查后再批准执行
4. 可以通过 H2 控制台查看表结构和数据
