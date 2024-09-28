package com.ssu.aiQuiz.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审核状态枚举
 */
public enum ReviewStatusEnum {
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    @Getter
    private final String text;
    @Getter
    private final int value;

    ReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举值
     *
     * @Param value
     * @return
     */
    public static ReviewStatusEnum getEnumByValue(int value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (ReviewStatusEnum anEnum : ReviewStatusEnum.values()) {
            if (anEnum.value == value)
                return anEnum;
        }
        return null;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

}
