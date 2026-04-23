package com.tianji.learning.constants;

/**
 * @author hazard
 * @version 1.0
 * @description Redis签到key格式: sign:uid:年月
 * @date 2025/7/23 23:15
 */
public interface RedisConstants {

    //签到积分记录
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";

    //排行榜
    String POINTS_RECORD_PREFIX = "boards:";



}
