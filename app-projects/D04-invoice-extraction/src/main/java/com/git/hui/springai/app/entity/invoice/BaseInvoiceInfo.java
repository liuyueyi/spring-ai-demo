package com.git.hui.springai.app.entity.invoice;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonClassDescription(value = "发票基本信息，不包含商品行信息")
public class BaseInvoiceInfo {

    // 发票基本信息
    @JsonPropertyDescription(value = "发票类型，如：增值税专用发票")
    private String invoiceType;

    @JsonPropertyDescription(value = "发票代码，如：044001800111")
    private String invoiceCode;

    @JsonPropertyDescription(value = "发票号码，如：12345678")
    private String invoiceNumber;

    // 机器编号
    @JsonPropertyDescription(value = "机器编号，如：66173206007")
    private String machineNumber;

    @JsonPropertyDescription(value = "校验码，如：12345 67890 12345 67890")
    private String checkCode;

    @JsonPropertyDescription(value = "密码区，如：0-TZ099+/8<0*8+T98Z/<6T</L9>0-260<*L6/6T/S->>00998¥//<L82Z099*+/8<0*8+T9*/Z<¥<696<E9200-896+/8T*8-")
    private String passwordArea;

    @JsonPropertyDescription(value = "开票日期，如：2024-01-15")
    private LocalDate issueDate;

    // 购销双方信息
    @JsonPropertyDescription(value = "销售方信息")
    private PartyInfo seller;

    @JsonPropertyDescription(value = "购买方信息")
    private PartyInfo buyer;

    // 金额信息
    @JsonPropertyDescription(value = "不含税金额")
    private BigDecimal amountWithoutTax;

    @JsonPropertyDescription(value = "税额")
    private BigDecimal taxAmount;

    @JsonPropertyDescription(value = "价税合计")
    private BigDecimal totalAmount;

    @JsonPropertyDescription(value = "税率，如：0.13")
    private BigDecimal taxRate;

    @JsonPropertyDescription(value = "备注")
    private String remark;

    @JsonPropertyDescription(value = "收款人")
    private String payee;

    @JsonPropertyDescription(value = "复核人")
    private String reviewer;

    @JsonPropertyDescription(value = "开票人")
    private String issuer;

    // 系统信息
    @JsonPropertyDescription(value = "发票图片MD5")
    private String imageHash;

    @JsonPropertyDescription(value = "提取置信度，如：0.95")
    private Double confidence;

    @JsonPropertyDescription(value = "提取时间")
    private LocalDateTime extractTime;
}
