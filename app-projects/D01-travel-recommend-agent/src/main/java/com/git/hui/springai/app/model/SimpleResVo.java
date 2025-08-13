package com.git.hui.springai.app.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.io.Serializable;

/**
 * 旅游推荐结果
 *
 * @author YiHui
 * @date 2025/8/13
 */
@Data
public class SimpleResVo implements Serializable {
    private static final long serialVersionUID = 8147413953959189557L;
    @JsonPropertyDescription("推荐标题")
    private String title;

    @JsonPropertyDescription("推荐内容")
    private String content;
}
