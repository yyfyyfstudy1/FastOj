package com.fastoj.judge.strategy;

import com.fastoj.judge.codesandbox.model.JudgeInfo;

/**
 * 判题策略
 * @author Shier
 */
public interface JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);
}
