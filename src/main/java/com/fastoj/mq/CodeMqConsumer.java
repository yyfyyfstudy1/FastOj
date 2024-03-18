package com.fastoj.mq;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.fastoj.common.ErrorCode;
import com.fastoj.exception.BusinessException;
import com.fastoj.judge.JudgeService;
import com.fastoj.judge.codesandbox.model.JudgeInfo;
import com.fastoj.model.entity.Question;
import com.fastoj.model.entity.QuestionSubmit;
import com.fastoj.model.enums.JudgeInfoMessageEnum;
import com.fastoj.service.QuestionService;
import com.fastoj.service.QuestionSubmitService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

import static com.fastoj.constant.MqConstant.CODE_QUEUE;

/**
 * Mq 消费者
 *
 * @author Shier
 * CreateTime 2023/6/24 15:53
 */
@Component
@Slf4j
public class CodeMqConsumer {

    @Resource
    private JudgeService judgeService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionService questionService;


    /**
     * 指定程序监听的消息队列和确认机制
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @SneakyThrows
    @RabbitListener(queues = {CODE_QUEUE}, ackMode = "MANUAL")
    private void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("接收到消息 ： {}", message);
        long questionSubmitId = Long.parseLong(message);

        if (message == null) {
            // 消息为空，则拒绝消息（不重试），进入死信队列
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NULL_ERROR, "消息为空");
        }
        try {
            // 判题服务
            judgeService.doJudge(questionSubmitId);
            QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
            // 获取到判题JudgeInfo信息结果
            String getJudgeInfo = questionSubmit.getJudgeInfo();
            Gson gson = new Gson();
            // 转换成JudgeInfo格式
            JudgeInfo judgeInfo = gson.fromJson(getJudgeInfo, JudgeInfo.class);
            String judgeInfoMessage = judgeInfo.getMessage();
            // 是否成功判断
            if (!JudgeInfoMessageEnum.ACCEPTED.getValue().equals(judgeInfoMessage)) {
                // 失败则拒绝消息
                channel.basicNack(deliveryTag, false, false);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败，请参看代码是否正确");
            }
            // 设置通过数
            Long questionId = questionSubmit.getQuestionId();
            log.info("题目ID: {}", questionId);
            Question question = questionService.getById(questionId);
            synchronized (question.getAcceptedNum()) {
                int acceptedNum = question.getAcceptedNum() + 1;
                question.setAcceptedNum(acceptedNum);
                boolean save = questionService.updateById(question);
                if (!save) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存数据失败");
                }
            }
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            // 消息为空，则拒绝消息，进入死信队列
            channel.basicNack(deliveryTag, false, false);
            throw new RuntimeException(e);
        }
    }
}
