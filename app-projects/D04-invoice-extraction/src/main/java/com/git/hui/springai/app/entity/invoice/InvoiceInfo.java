package com.git.hui.springai.app.entity.invoice;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

@Data
@JsonClassDescription(value = "发票完整信息")
public class InvoiceInfo extends BaseInvoiceInfo {
    @JsonPropertyDescription(value = "商品/服务明细列表")
    private List<InvoiceItem> items;
}
