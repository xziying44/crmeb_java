package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.request.FullReductionRequest;
import com.zbkj.common.request.FullReductionSearchRequest;
import com.zbkj.common.response.FullReductionResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 满减活动 Service 接口
 */
public interface FullReductionService extends IService<FullReduction> {

    /**
     * 分页列表
     */
    PageInfo<FullReductionResponse> getList(FullReductionSearchRequest request);

    /**
     * 详情
     */
    FullReductionResponse getDetail(Integer id);

    /**
     * 新增/编辑
     */
    Boolean save(FullReductionRequest request);

    /**
     * 删除
     */
    Boolean delete(Integer id);

    /**
     * 更新状态
     */
    Boolean updateStatus(Integer id, Boolean status);

    /**
     * 根据商品ID列表查询可用的满减活动
     */
    FullReduction getAvailableByProductIds(List<Integer> productIds, List<Integer> categoryIds);

    /**
     * 计算满减金额
     */
    BigDecimal calculateReduction(FullReduction reduction, BigDecimal totalAmount);
}

