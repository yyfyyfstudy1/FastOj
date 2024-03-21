package com.fastoj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastoj.model.dto.questionsumbit.CommentDto;
import com.fastoj.model.dto.questionsumbit.QuestionStatusCheck;
import com.fastoj.model.entity.Comment;
import com.fastoj.service.CommentService;
import com.google.gson.Gson;
import com.fastoj.annotation.AuthCheck;
import com.fastoj.common.BaseResponse;
import com.fastoj.common.DeleteRequest;
import com.fastoj.common.ErrorCode;
import com.fastoj.common.ResultUtils;
import com.fastoj.constant.UserConstant;
import com.fastoj.exception.BusinessException;
import com.fastoj.exception.ThrowUtils;
import com.fastoj.manager.RedisLimiterManager;
import com.fastoj.model.dto.question.*;
import com.fastoj.model.dto.questionsumbit.QuestionSubmitAddRequest;
import com.fastoj.model.dto.questionsumbit.QuestionSubmitQueryRequest;
import com.fastoj.model.entity.Question;
import com.fastoj.model.entity.QuestionSubmit;
import com.fastoj.model.entity.User;
import com.fastoj.model.vo.QuestionSubmitVO;
import com.fastoj.model.vo.QuestionVO;
import com.fastoj.service.QuestionService;
import com.fastoj.service.QuestionSubmitService;
import com.fastoj.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目管理
 *
 * @author Shier
 */
@RestController
@RequestMapping("/question")
@Slf4j
@CrossOrigin(originPatterns = {"http://localhost:8080","http://oj.kongshier.top"}, allowCredentials = "true")
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private RedisLimiterManager redissonLimiter;

    @Resource
    private CommentService commentService;

    private final static Gson GSON = new Gson();


    /**
     * 提交题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加题目")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        questionService.validQuestion(question, true);
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除题目")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "管理员更新信息")
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取 (脱敏)
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "获取题目信息")
    public BaseResponse<QuestionVO> getQuestionVoById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @ApiOperation(value = "获取题目信息")
    public BaseResponse<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 用户权限判断
        User loginUser = userService.getLoginUser(request);
        // 非本人或者管理员，不能获取到题目所有信息
        if (!question.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        return ResultUtils.success(question);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取题目列表")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "管理员分页获取题目列表（所有信息）")
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                           HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    @ApiOperation(value = "分页获取当前用户创建的题目列表")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation(value = "编辑题目")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(GSON.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录 id
     */
    @PostMapping("/question_submit/do")
    @ApiOperation(value = "题目提交")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交
        final User loginUser = userService.getLoginUser(request);
        // 限流
        boolean rateLimit = redissonLimiter.doRateLimit(loginUser.getId().toString());
        if (!rateLimit) {
            return ResultUtils.error(ErrorCode.TOO_MANY_REQUEST, "提交过于频繁,请稍后重试");
        }
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }

    /**
     * 查看question提交的状态
     */
    @PostMapping("/question_submit/getQuestionSubmitStatus")
    @ApiOperation(value = "题目提交状态查询")
    public BaseResponse<Integer> getQuestionSubmitStatus(@RequestBody QuestionStatusCheck questionStatusCheck,
                                               HttpServletRequest request) {

        QuestionSubmit questionSubmit = questionSubmitService.getById(questionStatusCheck.getQuestionId());


        return ResultUtils.success(questionSubmit.getSubmitState());
    }



    /**
     * 分页获取题目提交列表（除了管理员外，其他普通用户只能看到非答案、提交的代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/question_submit/list/page")
    @ApiOperation(value = "分页获取题目提交列表")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long pageSize = questionSubmitQueryRequest.getPageSize();

        // 从数据库中查询到原始的题目提交信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, pageSize),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final User loginUer = userService.getLoginUser(request);
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUer));
    }

    @PostMapping("/question_submit/listUserSubmission/page")
    @ApiOperation(value = "分页获取用户题目提交列表")
    public BaseResponse<Page<QuestionSubmitVO>> listUserSubmission(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                 HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long pageSize = questionSubmitQueryRequest.getPageSize();
        final User loginUer = userService.getLoginUser(request);


        LambdaQueryWrapper<QuestionSubmit> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionSubmit::getUserId, loginUer.getId());
        lambdaQueryWrapper.eq(QuestionSubmit::getQuestionId, questionSubmitQueryRequest.getQuestionId());
        lambdaQueryWrapper.orderByDesc(QuestionSubmit::getCreateTime);

        // 从数据库中查询到原始的题目提交信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, pageSize),
                lambdaQueryWrapper);

        // 转换Page<QuestionSubmit>到Page<QuestionSubmitVO>
        List<QuestionSubmitVO> vos = questionSubmitPage.getRecords().stream()
                .map(QuestionSubmitVO::objToVo) // 使用转换方法
                .collect(Collectors.toList());

        // 创建一个新的Page对象来包含VOs
        Page<QuestionSubmitVO> voPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        voPage.setRecords(vos);

        return ResultUtils.success(voPage);
    }



    @PostMapping("/question/comment/tree")
    @ApiOperation(value = "获取评论列表")
    public BaseResponse<List<Comment>> listComment(@RequestBody CommentDto useDto,
                                                                   HttpServletRequest request) {

        LambdaQueryWrapper<Comment> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Comment::getQuestionId, useDto.getQuestionId());
        List<Comment> commentList = commentService.list(lambdaQueryWrapper);


        Map<Long, Comment> commentMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        // 首先，将所有评论放入Map中，便于后续快速查找
        for (Comment comment : commentList) {
            User user = userService.getById(comment.getUserId());
            comment.setUserName(user.getUserName());
            comment.setUserAvatar(user.getUserAvatar());
            commentMap.put(comment.getId(), comment);
        }

        // 然后，组织评论的树状结构
        for (Comment comment : commentList) {
            if (comment.getParentCommentId() != null && comment.getParentCommentId() != 0) {
                Comment parent = commentMap.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.getChildren().add(comment);
                }
            } else {
                rootComments.add(comment); // 没有父评论的是根评论
            }
        }

        return ResultUtils.success(rootComments);
    }


    @PostMapping("/addComment")
    @ApiOperation(value = "添加评论")
    public BaseResponse<Boolean> addComment(@RequestBody Comment comment, HttpServletRequest request) {
        final User loginUer = userService.getLoginUser(request);
        comment.setCreateTime(new Date());
        comment.setUserId(loginUer.getId());

        if(comment.getParentCommentId()!=0){
            Comment parentComment = commentService.getById(comment.getParentCommentId());
            comment.setQuestionId(parentComment.getQuestionId());
        }
        return ResultUtils.success(commentService.save(comment));
    }
}
