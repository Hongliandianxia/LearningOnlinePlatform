package com.tianji.learning.utils;

/**
 * @author hazard
 * @version 1.0
 * @description 表名信息传递
 * @date 2025/7/28 0:17
 */
public class TableInfoContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    public static void setInfo(String info){
        TL.set(info);
    }
    public static String getInfo(){
        return TL.get();
    }
    public static void remove(){
        TL.remove();
    }
}
