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
 * 用户答题记录
 * </p>
 *
 * @author ssu
 * @since 2024-09-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_answer")
public class UserAnswer implements Serializable {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 应用类型（1-得分类，2-测试类）
     */
    private Integer appType;

    /**
     * 评分策略（1-自定义，2-AI）
     */
    private Integer scoringStrategy;

    /**
     * 用户答案（sjon数组）
     */
    private String choices;

    /**
     * 评分结果id
     */
    private Long resultId;

    /**
     * 评分结果名称（如物流师）
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
     * 得分
     */
    private Integer resultScore;

    /**
     * 用户id
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
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}
