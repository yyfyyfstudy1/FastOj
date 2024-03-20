package com.fastoj.model.dto.questionsumbit;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加题目
 *
* @author Shier
 */
@Data
public class QuestionStatusCheck implements Serializable {
    /**
     * 题目 id
     */
    private Long questionId;

}
