package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignRecordVO;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author hazard
 * @version 1.0
 * @description
 * @date 2025/7/23 23:18
 */
@Api(tags = "签到记录接口")
@RestController
@RequestMapping("/sign-records")
@RequiredArgsConstructor
public class SignRecordController {

    private final ISignRecordService signRecordService;

    @ApiOperation("签到接口")
    @PostMapping
    public SignResultVO sign() {
        return signRecordService.sign();
    }

    @ApiOperation("查询签到结果")
    @GetMapping
    public List<Byte> querySignRecord() {
        return signRecordService.getSignRecord();
    }
}
