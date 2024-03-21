package com.fastoj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@TableName(value = "comment")
@Data
public class Comment implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建题目用户 id
     */
    private Long userId;

    private Long parentCommentId;
    private Long questionId;
    private String content;

    @JsonFormat(pattern = "YYYY-MM-DD HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    // 子评论列表
    @TableField(exist = false)
    private List<Comment> children = new ArrayList<>();

}
