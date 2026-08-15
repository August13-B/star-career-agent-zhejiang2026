package org.example.web.entity.request;

import lombok.Data;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Data
public class AbilityAnalysisRequest {
    @NotNull(message = "用户ID(userId)不能为空")
    private Long userId;

    private String message;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Float temperature = 0.3f;
}