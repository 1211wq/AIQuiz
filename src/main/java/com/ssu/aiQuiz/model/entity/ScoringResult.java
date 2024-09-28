package com.ssu.aiQuiz.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 评分结果
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scoring_result")
public class ScoringResult implements Serializable {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评分结果名称
     */
    private String resultName;

    /**
     * 评分结果描述
     */
    private String resultDesc;

    /**
     * 评分结果图片
     */
    private String resultPicture;

    /**
     * 评分结果属性集合（json格式，如[I,S,T,J]）
     */
    private String resultProp;

    /**
     * 评分结果分数区间，如80，表示80及以上得分数命中此结果
     */
    private int resultScoreRange;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}
