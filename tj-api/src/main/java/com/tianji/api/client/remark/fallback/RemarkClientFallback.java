package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * @author hazard
 */
@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {

    @Override
    //兜底方案,采用springboot自动装配，定义factory配置config
    public RemarkClient create(Throwable cause) {
        log.error("查询学习服务异常", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> isBizLiked(Iterator<Long> bizIds) {
                return CollUtils.emptySet();
            }
        };
    }
}
