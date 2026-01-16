package com.git.hui.springai.app.entity.req;

import com.git.hui.springai.app.entity.invoice.InvoiceInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(description = "发票提取响应")
public class InvoiceExtractResponse {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "处理状态")
    private ProcessStatus status;

    @Schema(description = "提取结果")
    private InvoiceInfo invoiceInfo;

    @Schema(description = "处理耗时(ms)")
    private Long processTime;

    @Schema(description = "错误信息")
    private String errorMessage;
}