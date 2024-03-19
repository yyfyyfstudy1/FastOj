package com.fastoj.judge;

import cn.hutool.json.JSONUtil;
import com.fastoj.common.ErrorCode;
import com.fastoj.exception.BusinessException;
import com.fastoj.judge.codesandbox.CodeSandBox;
import com.fastoj.judge.codesandbox.CodeSandBoxProxy;
import com.fastoj.judge.codesandbox.CodeSandboxFactory;
import com.fastoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.fastoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.fastoj.judge.codesandbox.model.JudgeInfo;
import com.fastoj.judge.strategy.JudgeContext;
import com.fastoj.model.dto.question.JudgeCase;
import com.fastoj.model.entity.Question;
import com.fastoj.model.entity.QuestionSubmit;
import com.fastoj.model.enums.QuestionSubmitStatusEnum;
import com.fastoj.service.QuestionService;
import com.fastoj.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Shier
 * @createTime 2023/8/30 星期三 12:09
 * 判题服务实现类
 */
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionService questionService;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String judgeType;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1、传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        // 通过提交的信息中的题目id 获取到题目的全部信息
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (questionId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2、如果题目提交状态不为等待中
        if (!questionSubmit.getSubmitState().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        // 3、更改判题（题目提交）的状态为 “判题中”，防止重复执行，也能让用户即时看到状态
        QuestionSubmit updateQuestionSubmit = new QuestionSubmit();
        updateQuestionSubmit.setId(questionSubmitId);
        updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean updateState = questionSubmitService.updateById(updateQuestionSubmit);
        if (!updateState) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新失败");
        }
        //4、调用沙箱
        CodeSandBox codeSandbox = CodeSandboxFactory.newInstance(judgeType);
        codeSandbox = new CodeSandBoxProxy(codeSandbox);
        String submitLanguage = questionSubmit.getSubmitLanguage();

        // 用户提交的代码
        String submitCode = questionSubmit.getSubmitCode();

        // TODO 拼接主函数和包
        String imports =
                "import java.util.*;\n" +
                        "import java.util.concurrent.*;\n" + // 包含并发集合
                        "import java.util.function.*;\n" + // 包含函数式接口
                        "import java.util.stream.*;\n" + // 包含Stream API
                        "import java.util.regex.*;\n"; // 包含正则表达式类
        String mainFunction = question.getMainFunction();

        // 使用正则表达式查找类定义的结束大括号，并在其前插入 main 方法
        String fullCode = imports + submitCode.replaceAll("(\\}\\s*$)", mainFunction + "$1");


        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCasesList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        // 通过Lambda表达式获取到每个题目的输入用例
        List<String> inputList = judgeCasesList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        // 调用沙箱
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(fullCode)
                .language(submitLanguage)
                .inputList(inputList)
                .mainFunction(mainFunction)
                .build();
        // 获取到代码沙箱执行结果
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();
        // 5、根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCasesList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);

        // 获取判题结果
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 6、修改判题结果
        updateQuestionSubmit = new QuestionSubmit();
        updateQuestionSubmit.setId(questionSubmitId);
        updateQuestionSubmit.setSubmitState(QuestionSubmitStatusEnum.SUCCEED.getValue());
        updateQuestionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        updateState = questionSubmitService.updateById(updateQuestionSubmit);
        if (!updateState) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新失败");
        }
        // 再次查询数据库，返回最新提交信息
        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionId);
        return questionSubmitResult;
    }
}
