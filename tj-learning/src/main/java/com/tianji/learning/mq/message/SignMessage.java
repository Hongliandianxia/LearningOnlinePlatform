package com.tianji.learning.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hazard
 * @version 1.0
 * @description 签到积分信息表
 * @date 2025/7/24 23:32
 */
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SignMessage {
    private Long userId;
    private Integer points;
}
