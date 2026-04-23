package com.tianji.learning.mq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author hazard
 * @version 1.0
 * @description 课程监听器
 * @date 2025/7/9 22:56
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {

    private final ILearningLessonService lessonService;

    // 监听支付成功消息,绑定交换机、队列、路由键
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "learning.lesson.pay.queue",declare = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type =ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenPaySuccess(OrderBasicDTO order) {

        //1.健壮性检测
        if (order == null || order.getOrderId() == null || order.getUserId() == null ||
                            CollUtils.isEmpty(order.getCourseIds())) {
            log.debug("接受到的MQ消息有误，订单数据为空");
            return;
        }
        //2.添加课程
        log.debug("监听到用户{}的订单{}需要添加课程{}到课表中", order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.addUserLesson(order.getUserId(), order.getCourseIds());


    }


    /**
     * 用户退款成功，取消课程的监听器
     *
     * @param orderBasicDTO
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.return"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void canalLesson(OrderBasicDTO orderBasicDTO) {
        log.info("ApplyLessonMqListener canalLesson ：{}", orderBasicDTO.toString());
        if (ObjectUtil.isNull(orderBasicDTO)
                || ObjectUtil.isNull(orderBasicDTO.getOrderId())
                || ObjectUtil.isNull(orderBasicDTO.getUserId())
                || CollUtil.isEmpty(orderBasicDTO.getCourseIds())) {
            log.error("删除课表失败,参数异常! {}", orderBasicDTO.toString());
            return;
        }
        //进行加入课表操作
        log.debug("监听到用户{}的订单{}，需要删除课程{}", orderBasicDTO.getUserId(), orderBasicDTO.getOrderId(), orderBasicDTO.getCourseIds());
        lessonService.deleteUserLesson(orderBasicDTO.getUserId(), orderBasicDTO.getCourseIds());
    }

}
