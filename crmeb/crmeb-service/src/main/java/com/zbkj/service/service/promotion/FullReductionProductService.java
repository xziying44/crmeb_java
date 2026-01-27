package com.zbkj.service.service.promotion;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.promotion.FullReductionProduct;

import java.util.List;

/**
 * 满减关联 Service 接口
 */
public interface FullReductionProductService extends IService<FullReductionProduct> {

    /**
     * 根据满减活动ID获取关联列表
     */
    List<FullReductionProduct> getByReductionId(Integer reductionId);

    /**
     * 删除满减活动的关联
     */
    Boolean deleteByReductionId(Integer reductionId);

    /**
     * 根据商品ID查询关联的满减活动ID列表
     */
    List<Integer> getReductionIdsByProductId(Integer productId);

    /**
     * 根据品类ID查询关联的满减活动ID列表
     */
    List<Integer> getReductionIdsByCategoryId(Integer categoryId);
}

