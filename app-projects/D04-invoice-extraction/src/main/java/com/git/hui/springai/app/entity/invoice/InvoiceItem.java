package com.git.hui.springai.app.entity.invoice;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;

@JsonClassDescription(value = "发票商品明细")
public record InvoiceItem(
        @JsonPropertyDescription(value = "商品名称")
        String itemName,
        @JsonPropertyDescription(value = "商品规格")
        String specification,
        @JsonPropertyDescription(value = "单位")
        String unit,
        @JsonPropertyDescription(value = "数量")
        BigDecimal quantity,
        @JsonPropertyDescription(value = "单价")
        BigDecimal unitPrice,
        @JsonPropertyDescription(value = "金额")
        BigDecimal amount,
        @JsonPropertyDescription(value = "税率")
        BigDecimal taxRate,
        @JsonPropertyDescription(value = "税额")
        BigDecimal taxAmount,
        @JsonPropertyDescription(value = "价税合计")
        BigDecimal totalAmount) {
}