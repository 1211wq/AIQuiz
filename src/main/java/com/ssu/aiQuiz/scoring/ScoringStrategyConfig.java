package com.ssu.aiQuiz.scoring;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ScoringStrategyConfig {

    /**
     * 应用类型
     *
     * @return
     */
    int appType();

    /**
     * 评分类型
     *
     * @return
     */
    int scoringStrategy();

}
