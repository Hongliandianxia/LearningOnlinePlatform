package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author hazard
 * @since 2025-07-18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikedRecordController {

    private final ILikedRecordService likedRecordService;

    /**
     * 添加/取消点赞 记录
     * @param formDTO
     */
    @ApiOperation("添加或取消点赞记录")
    @PostMapping()
    public void addLikedRecord(@Valid @RequestBody LikeRecordFormDTO formDTO){
        likedRecordService.addLikedRecord(formDTO);
    }

    @ApiOperation("批量查询点赞状态")
    @GetMapping("/list")
    public Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds) {
        return likedRecordService.isBizLiked(bizIds);
    }

}
