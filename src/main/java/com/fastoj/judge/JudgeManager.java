package com.fastoj.judge;

import com.fastoj.judge.codesandbox.model.JudgeInfo;
import com.fastoj.judge.strategy.DefaultJudgeStrategy;
import com.fastoj.judge.strategy.JavaLanguageJudgeStrategy;
import com.fastoj.judge.strategy.JudgeContext;
import com.fastoj.judge.strategy.JudgeStrategy;
import com.fastoj.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 * @author Shier
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getSubmitLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}
