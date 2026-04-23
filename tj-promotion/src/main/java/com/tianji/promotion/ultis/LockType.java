package com.tianji.promotion.ultis;

/**
 * @author hazard
 * @version 1.0
 * @description redis中锁的类型
 * @date 2025/8/6 22:32
 */
public enum LockType {
    RE_ENTRANT_LOCK,
    FAIR_LOCK,
    READ_LOCK,
    WRITE_LOCk,
    ;
}
