package com.git.hui.springai.app.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发票提取请求")
public class InvoiceExtractRequest {

    @Schema(description = "发票图片,如果是http开头表示发票访问链接；如果是 data:image/png;base64, 开头表示为base64格式图片")
    private String image;

    @Schema(description = "图片格式", example = "image/jpeg")
    private String imageType;

    @Schema(description = "是否需要商品明细", example = "true")
    private boolean needItems = true;

    @Schema(description = "提示信息")
    private String msg;
}



