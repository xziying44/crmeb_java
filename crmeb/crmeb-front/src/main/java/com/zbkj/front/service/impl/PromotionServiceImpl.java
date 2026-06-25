package com.zbkj.front.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.model.promotion.FullReductionProduct;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.IndexProductResponse;
import com.zbkj.front.service.PromotionService;
import com.zbkj.service.service.ActivityStyleService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.promotion.BuyGiftProductService;
import com.zbkj.service.service.promotion.BuyGiftService;
import com.zbkj.service.service.promotion.FullReductionProductService;
import com.zbkj.service.service.promotion.FullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 促销活动商品 Service 实现
 */
@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private BuyGiftService buyGiftService;

    @Autowired
    private BuyGiftProductService buyGiftProductService;

    @Autowired
    private FullReductionService fullReductionService;

    @Autowired
    private FullReductionProductService fullReductionProductService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private ActivityStyleService activityStyleService;

    @Override
    public CommonPage<IndexProductResponse> getBuyGiftProducts(PageParamRequest pageParamRequest) {
        // 查询所有有效的买赠活动
        Date now = new Date();
        LambdaQueryWrapper<BuyGift> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGift::getIsDel, false);
        wrapper.eq(BuyGift::getStatus, true);
        wrapper.le(BuyGift::getStartTime, now);
        wrapper.ge(BuyGift::getEndTime, now);
        List<BuyGift> giftList = buyGiftService.list(wrapper);
        if (CollUtil.isEmpty(giftList)) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 收集所有买赠活动关联的购买商品ID（productType=1）
        Set<Integer> productIdSet = new LinkedHashSet<>();
        for (BuyGift gift : giftList) {
            List<BuyGiftProduct> products = buyGiftProductService.getByGiftId(gift.getId());
            for (BuyGiftProduct p : products) {
                if (p.getProductType() != null && p.getProductType() == 1) {
                    productIdSet.add(p.getProductId());
                }
            }
        }
        if (CollUtil.isEmpty(productIdSet)) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 对商品ID列表做分页查询
        List<Integer> allProductIds = new ArrayList<>(productIdSet);
        return getProductPageByIds(allProductIds, pageParamRequest);
    }

    @Override
    public CommonPage<IndexProductResponse> getFullReductionProducts(PageParamRequest pageParamRequest) {
        // 查询所有有效的满减活动
        Date now = new Date();
        LambdaQueryWrapper<FullReduction> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReduction::getIsDel, false);
        wrapper.eq(FullReduction::getStatus, true);
        wrapper.le(FullReduction::getStartTime, now);
        wrapper.ge(FullReduction::getEndTime, now);
        List<FullReduction> reductionList = fullReductionService.list(wrapper);
        if (CollUtil.isEmpty(reductionList)) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 检查是否有全场满减活动（scopeType=1）
        boolean hasGlobalReduction = reductionList.stream()
                .anyMatch(r -> r.getScopeType() != null && r.getScopeType() == 1);

        if (hasGlobalReduction) {
            // 全场满减：分页查询所有上架商品
            return getAllOnSaleProducts(pageParamRequest);
        }

        // 非全场：收集指定商品ID
        Set<Integer> productIdSet = new LinkedHashSet<>();
        for (FullReduction reduction : reductionList) {
            List<FullReductionProduct> relProducts = fullReductionProductService.getByReductionId(reduction.getId());
            for (FullReductionProduct rp : relProducts) {
                if (rp.getRelationType() != null && rp.getRelationType() == 2) {
                    // 直接关联商品
                    productIdSet.add(rp.getRelationId());
                }
                // 品类关联（relationType=1）暂不展开品类下所有商品，避免数据量过大
            }
        }
        if (CollUtil.isEmpty(productIdSet)) {
            return CommonPage.restPage(new ArrayList<>());
        }

        List<Integer> allProductIds = new ArrayList<>(productIdSet);
        return getProductPageByIds(allProductIds, pageParamRequest);
    }

    /**
     * 根据商品ID列表手动分页，查询商品详情并转换为 IndexProductResponse
     */
    private CommonPage<IndexProductResponse> getProductPageByIds(List<Integer> productIds, PageParamRequest pageParamRequest) {
        int page = pageParamRequest.getPage();
        int limit = pageParamRequest.getLimit();
        int total = productIds.size();
        int fromIndex = (page - 1) * limit;
        if (fromIndex >= total) {
            CommonPage<IndexProductResponse> result = new CommonPage<>();
            result.setPage(page);
            result.setLimit(limit);
            result.setTotal((long) total);
            result.setTotalPage((int) Math.ceil((double) total / limit));
            result.setList(new ArrayList<>());
            return result;
        }
        int toIndex = Math.min(fromIndex + limit, total);
        List<Integer> pageIds = productIds.subList(fromIndex, toIndex);

        // 查询商品详情
        List<StoreProduct> products = storeProductService.findByIds(pageIds, "front");
        if (CollUtil.isEmpty(products)) {
            CommonPage<IndexProductResponse> result = new CommonPage<>();
            result.setPage(page);
            result.setLimit(limit);
            result.setTotal((long) total);
            result.setTotalPage((int) Math.ceil((double) total / limit));
            result.setList(new ArrayList<>());
            return result;
        }

        // 填充活动边框样式
        products = activityStyleService.makeActivityBorderStyle(products);

        // 转换为 IndexProductResponse
        List<IndexProductResponse> responseList = products.stream().map(product -> {
            IndexProductResponse response = new IndexProductResponse();
            BeanUtils.copyProperties(product, response);
            return response;
        }).collect(Collectors.toList());

        CommonPage<IndexProductResponse> result = new CommonPage<>();
        result.setPage(page);
        result.setLimit(limit);
        result.setTotal((long) total);
        result.setTotalPage((int) Math.ceil((double) total / limit));
        result.setList(responseList);
        return result;
    }

    /**
     * 全场满减时，分页查询所有上架商品
     */
    private CommonPage<IndexProductResponse> getAllOnSaleProducts(PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        // 查询上架且未删除的商品
        LambdaQueryWrapper<StoreProduct> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreProduct::getIsDel, false);
        lqw.eq(StoreProduct::getIsRecycle, false);
        lqw.eq(StoreProduct::getIsShow, true);
        lqw.orderByDesc(StoreProduct::getId);
        List<StoreProduct> products = storeProductService.list(lqw);
        if (CollUtil.isEmpty(products)) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 填充活动边框样式
        products = activityStyleService.makeActivityBorderStyle(products);

        CommonPage<StoreProduct> productPage = CommonPage.restPage(products);
        List<IndexProductResponse> responseList = products.stream().map(product -> {
            IndexProductResponse response = new IndexProductResponse();
            BeanUtils.copyProperties(product, response);
            return response;
        }).collect(Collectors.toList());

        CommonPage<IndexProductResponse> result = CommonPage.restPage(responseList);
        BeanUtils.copyProperties(productPage, result, "list");
        return result;
    }
}
