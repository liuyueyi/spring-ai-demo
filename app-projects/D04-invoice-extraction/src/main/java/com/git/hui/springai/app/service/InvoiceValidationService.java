package com.git.hui.springai.app.service;

import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceValidationService {
    @Tool(
            name = "validateInvoiceFields",
            description = "验证发票字段的合法性和一致性"
    )
    public ValidationResult validateInvoice(
            @ToolParam(description = "发票代码") String invoiceCode,
            @ToolParam(description = "发票号码") String invoiceNumber,
            @ToolParam(description = "开票日期") String issueDate,
            @ToolParam(description = "不含税金额") BigDecimal amountWithoutTax,
            @ToolParam(description = "税额") BigDecimal taxAmount,
            @ToolParam(description = "价税合计") BigDecimal totalAmount
    ) {
        ValidationResult result = new ValidationResult();
        List<ValidationError> errors = new ArrayList<>();

        // 1. 验证发票代码格式（12位数字）
        if (invoiceCode != null && !invoiceCode.matches("\\d{12}")) {
            errors.add(new ValidationError("invoiceCode", "发票代码必须是12位数字"));
        }

        // 2. 验证发票号码格式（8位数字）
        if (invoiceNumber != null && !invoiceNumber.matches("\\d{8}")) {
            errors.add(new ValidationError("invoiceNumber", "发票号码必须是8位数字"));
        }

        // 3. 验证日期格式
        if (issueDate != null) {
            try {
                LocalDate.parse(issueDate);
            } catch (Exception e) {
                errors.add(new ValidationError("issueDate", "开票日期格式错误"));
            }
        }

        // 4. 验证金额关系
        if (amountWithoutTax != null && taxAmount != null && totalAmount != null) {
            BigDecimal calculatedTotal = amountWithoutTax.add(taxAmount);
            BigDecimal diff = totalAmount.subtract(calculatedTotal).abs();

            if (diff.compareTo(new BigDecimal("0.01")) > 0) {
                errors.add(new ValidationError("amounts",
                        String.format("金额计算不一致: 不含税(%.2f) + 税额(%.2f) ≠ 合计(%.2f)",
                                amountWithoutTax, taxAmount, totalAmount)));
            }
        }

        // 5. 验证纳税人识别号（15-20位）
        // 这里可以添加更复杂的验证逻辑

        result.setErrors(errors);
        result.setValid(errors.isEmpty());
        result.setValidationTime(LocalDateTime.now());

        return result;
    }

    @Data
    public static class ValidationResult {
        private boolean valid;
        private List<ValidationError> errors;
        private LocalDateTime validationTime;
    }

    public record ValidationError(String field, String message) {
    }
}