package com.ssu.aiQuiz.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 评分策略（0-自定义， 1-AI）
 */
public enum AppScoringStategyEnum {
    CUSTOM(0, "自定义"),
    AI(1, "AI");

    @Getter
    private final int value;
    @Getter
    private final String desc;

    AppScoringStategyEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据value获取枚举值
     */
    public static AppScoringStategyEnum getEnumByValue(int value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (AppScoringStategyEnum e : AppScoringStategyEnum.values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
