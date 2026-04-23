package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.SignMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author hazard
 * @version 1.0
 * @description 学习积分监听器
 * @date 2025/7/24 23:17
 */
@Component
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService pointsRecordService;

    /**
     * 监听写问答消息 一次加5分
     * @param userId 用户id 由前端传递
     */
    @RabbitListener(bindings =@QueueBinding(
            value = @Queue(name = "qa.points.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))

    public void listenWriteRelyMessage(Long userId){
        pointsRecordService.addPointsRecord(userId,5, PointsRecordType.QA);
    }

    /**
     * 监听签到消息 连续签到可获取奖励积分
     * @param message 签到信息
     */

    @RabbitListener(bindings =@QueueBinding(
            value = @Queue(name = "sign.points.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN
    ))

    public void listenSignInMessage(SignMessage message){
        pointsRecordService.addPointsRecord(message.getUserId(),message.getPoints(), PointsRecordType.SIGN);
    }

    /**
     * 监听学习视频消息 一次加10分
     * @param userId 用户id 由前端传递
     */
    @RabbitListener(bindings =@QueueBinding(
            value = @Queue(name = "qa.points.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION
    ))

    public void listenLearningMessage(Long userId){
        pointsRecordService.addPointsRecord(userId,10, PointsRecordType.LEARNING);
    }






}
