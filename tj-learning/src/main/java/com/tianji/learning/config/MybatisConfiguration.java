package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hazard
 * @version 1.0
 * @description 实现拦截器注册功能
 * @date 2025/7/28 0:21
 */
@Configuration
public class MybatisConfiguration {
    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        Map<String, TableNameHandler> map = new HashMap<>(1);
        //创建内部类，完成替换表名替换
/*        map.put("points_board", new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                return TableInfoContext.getInfo();
            }
        });*/
        //简化，定义的表名处理器只有一个方法，属于函数式接口，可以lambda表达式替换
        map.put("points_board", (sql, tableName) -> TableInfoContext.getInfo());

        return new DynamicTableNameInnerInterceptor(map);
    }
}
