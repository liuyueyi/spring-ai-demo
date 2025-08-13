package com.git.hui.springai.app.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.git.hui.springai.app.agents.FoodRecommendAgent;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 旅游推荐结果
 *
 * @author YiHui
 * @date 2025/8/13
 */
@Data
@ToString(callSuper = true)
public class TravelResVo extends SimpleResVo {
    private static final long serialVersionUID = 6808399653463811338L;
    @JsonPropertyDescription("推荐的项目")
    private List<FoodRecommendAgent.TravelFoodRecommends> travels;
}
