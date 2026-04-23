package com.tianji.api.dto.remark;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * @author hazard
 * @version 1.0
 * @description 点赞
 * @date 2025/7/18 21:47
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class LikedTimesDTO {
    private Long bizId;
    private Integer likedTimes;
}
