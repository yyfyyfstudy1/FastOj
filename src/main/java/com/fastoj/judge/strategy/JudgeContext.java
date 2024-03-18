package com.fastoj.judge.strategy;


import com.fastoj.judge.codesandbox.model.JudgeInfo;
import com.fastoj.model.dto.question.JudgeCase;
import com.fastoj.model.entity.Question;
import com.fastoj.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 * @author Shier
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;

}
