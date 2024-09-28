package com.ssu.aiQuiz.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 应用类型枚举（0-得分类， 1-测试类）
 */
public enum AppTypeEnum {
    SCORE(0, "得分类"),
    TEST(1, "测试类");

    @Getter
    private final int value;
    @Getter
    private final String desc;

    AppTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据值获取枚举类型
     */
    public static AppTypeEnum getEnumByValue(int value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (AppTypeEnum appTypeEnum : AppTypeEnum.values()) {
            if (appTypeEnum.value == value) {
                return appTypeEnum;
            }
        }
        return null;
    }
}
