package com.tianji.api.client.remark;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author hazard
 * @version 1.0
 * @description 点赞api
 * @date 2025/7/18 23:51
 */
@FeignClient("remark-service")
public interface RemarkClient {
    @ApiOperation("批量查询点赞状态")
    @GetMapping("/likes/list")
    //不一定需要和接口声明一致，只要是可迭代，get请求，都会发出一个get请求
    Set<Long> isBizLiked(@RequestParam("bizIds") Iterator<Long> bizIds);
}
