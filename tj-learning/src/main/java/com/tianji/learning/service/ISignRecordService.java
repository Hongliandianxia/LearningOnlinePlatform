package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignRecordVO;
import com.tianji.learning.domain.vo.SignResultVO;
import io.lettuce.core.StrAlgoArgs;

import java.util.List;

/**
 * @author hazard
 * @version 1.0
 * @description
 * @date 2025/7/23 23:21
 */
public interface ISignRecordService {
    SignResultVO sign();

    List<Byte> getSignRecord();
}
