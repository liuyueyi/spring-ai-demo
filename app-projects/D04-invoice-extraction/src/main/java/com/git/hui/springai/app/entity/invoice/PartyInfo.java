package com.git.hui.springai.app.entity.invoice;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription(value = "发票方信息")
public record PartyInfo(
        @JsonPropertyDescription(value = "名称")
        String name,
        @JsonPropertyDescription(value = "纳税人识别号")
        String taxId,
        @JsonPropertyDescription(value = "地址")
        String address,
        @JsonPropertyDescription(value = "电话")
        String phone,
        @JsonPropertyDescription(value = "开户银行")
        String bank,
        @JsonPropertyDescription(value = "银行账号")
        String account) {
}