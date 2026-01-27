package com.zbkj.service.service.promotion.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.promotion.BuyGift;
import com.zbkj.common.model.promotion.BuyGiftProduct;
import com.zbkj.common.request.BuyGiftRequest;
import com.zbkj.common.request.BuyGiftSearchRequest;
import com.zbkj.common.response.BuyGiftResponse;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.service.dao.promotion.BuyGiftDao;
import com.zbkj.service.service.promotion.BuyGiftProductService;
import com.zbkj.service.service.promotion.BuyGiftRecordService;
import com.zbkj.service.service.promotion.BuyGiftService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 买赠活动 Service 实现类
 */
@Service
public class BuyGiftServiceImpl extends ServiceImpl<BuyGiftDao, BuyGift>
        implements BuyGiftService {

    @Autowired
    private BuyGiftProductService productService;

    @Autowired
    private BuyGiftRecordService recordService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public PageInfo<BuyGiftResponse> getList(BuyGiftSearchRequest request) {
        PageHelper.startPage(request.getPage(), request.getLimit());
        LambdaQueryWrapper<BuyGift> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGift::getIsDel, false);
        if (StrUtil.isNotBlank(request.getName())) {
            wrapper.like(BuyGift::getName, request.getName());
        }
        if (ObjectUtil.isNotNull(request.getStatus())) {
            wrapper.eq(BuyGift::getStatus, request.getStatus() == 1);
        }
        if (ObjectUtil.isNotNull(request.getGiftType())) {
            wrapper.eq(BuyGift::getGiftType, request.getGiftType());
        }
        wrapper.orderByDesc(BuyGift::getId);
        List<BuyGift> list = list(wrapper);
        PageInfo<BuyGift> pageInfo = new PageInfo<>(list);

        List<BuyGiftResponse> responseList = list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        PageInfo<BuyGiftResponse> resultPage = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, resultPage, "list");
        resultPage.setList(responseList);
        return resultPage;
    }

    @Override
    public BuyGiftResponse getDetail(Integer id) {
        BuyGift gift = getById(id);
        if (ObjectUtil.isNull(gift) || Boolean.TRUE.equals(gift.getIsDel())) {
            throw new CrmebException("买赠活动不存在");
        }
        BuyGiftResponse response = convertToResponse(gift);
        List<BuyGiftProduct> products = productService.getByGiftId(id);
        if (CollUtil.isNotEmpty(products)) {
            response.setBuyProducts(products.stream()
                    .filter(p -> p.getProductType() != null && p.getProductType() == 1)
                    .map(this::convertProductToResponseItem)
                    .collect(Collectors.toList()));
            response.setGiftProducts(products.stream()
                    .filter(p -> p.getProductType() != null && p.getProductType() == 2)
                    .map(this::convertProductToResponseItem)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    @Override
    public Boolean save(BuyGiftRequest request) {
        if (CollUtil.isEmpty(request.getBuyProducts())) {
            throw new CrmebException("请配置购买商品");
        }
        if (CollUtil.isEmpty(request.getGiftProducts())) {
            throw new CrmebException("请配置赠品");
        }
        if (request.getLimitType() != null && request.getLimitType() != 0) {
            if (request.getLimitNum() == null || request.getLimitNum() <= 0) {
                throw new CrmebException("请填写限制次数");
            }
        }

        BuyGift gift = new BuyGift();
        BeanUtils.copyProperties(request, gift);
        gift.setStartTime(CrmebDateUtil.strToDate(request.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
        gift.setEndTime(CrmebDateUtil.strToDate(request.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
        gift.setIsDel(false);
        gift.setStatus(false); // 默认关闭
        gift.setUpdateTime(DateUtil.date());

        return transactionTemplate.execute(status -> {
            if (ObjectUtil.isNotNull(request.getId())) {
                BuyGift old = getById(request.getId());
                if (ObjectUtil.isNull(old) || Boolean.TRUE.equals(old.getIsDel())) {
                    status.setRollbackOnly();
                    throw new CrmebException("买赠活动不存在");
                }
                gift.setId(request.getId());
                updateById(gift);
                productService.deleteByGiftId(request.getId());
            } else {
                gift.setCreateTime(DateUtil.date());
                save(gift);
            }

            List<BuyGiftProduct> buyProducts = request.getBuyProducts().stream().map(item -> {
                BuyGiftProduct p = new BuyGiftProduct();
                p.setGiftId(gift.getId());
                p.setProductId(item.getProductId());
                p.setAttrValueId(item.getAttrValueId() == null ? 0 : item.getAttrValueId());
                p.setProductType(1);
                p.setBuyNum(item.getBuyNum() == null ? 0 : item.getBuyNum());
                p.setGiftNum(0);
                return p;
            }).collect(Collectors.toList());

            List<BuyGiftProduct> giftProducts = request.getGiftProducts().stream().map(item -> {
                BuyGiftProduct p = new BuyGiftProduct();
                p.setGiftId(gift.getId());
                p.setProductId(item.getProductId());
                p.setAttrValueId(item.getAttrValueId() == null ? 0 : item.getAttrValueId());
                p.setProductType(2);
                p.setBuyNum(0);
                p.setGiftNum(item.getGiftNum() == null ? 0 : item.getGiftNum());
                return p;
            }).collect(Collectors.toList());

            productService.saveBatch(buyProducts);
            productService.saveBatch(giftProducts);
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean delete(Integer id) {
        BuyGift gift = getById(id);
        if (ObjectUtil.isNull(gift) || Boolean.TRUE.equals(gift.getIsDel())) {
            throw new CrmebException("买赠活动不存在");
        }
        LambdaUpdateWrapper<BuyGift> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(BuyGift::getId, id);
        wrapper.set(BuyGift::getIsDel, true);
        wrapper.set(BuyGift::getStatus, false);
        wrapper.set(BuyGift::getUpdateTime, DateUtil.date());
        return update(wrapper);
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        BuyGift gift = getById(id);
        if (ObjectUtil.isNull(gift) || Boolean.TRUE.equals(gift.getIsDel())) {
            throw new CrmebException("买赠活动不存在");
        }
        if (gift.getStatus().equals(status)) {
            throw new CrmebException("买赠活动状态无需变更");
        }
        BuyGift update = new BuyGift();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(DateUtil.date());
        return updateById(update);
    }

    @Override
    public BuyGift getAvailableByProductId(Integer productId, Integer uid) {
        if (productId == null || productId <= 0) {
            return null;
        }
        Date now = new Date();
        List<Integer> giftIds = productService.getGiftIdsByProductId(productId);
        if (CollUtil.isEmpty(giftIds)) {
            return null;
        }
        LambdaQueryWrapper<BuyGift> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BuyGift::getIsDel, false);
        wrapper.eq(BuyGift::getStatus, true);
        wrapper.le(BuyGift::getStartTime, now);
        wrapper.ge(BuyGift::getEndTime, now);
        wrapper.in(BuyGift::getId, giftIds);
        wrapper.orderByDesc(BuyGift::getId);
        List<BuyGift> list = list(wrapper);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        for (BuyGift gift : list) {
            if (uid == null || uid <= 0 || checkUserLimit(gift.getId(), uid)) {
                return gift;
            }
        }
        return null;
    }

    @Override
    public Boolean checkUserLimit(Integer giftId, Integer uid) {
        BuyGift gift = getById(giftId);
        if (gift == null || gift.getLimitType() == null || gift.getLimitType() == 0) {
            return true;
        }
        if (uid == null || uid <= 0) {
            return true;
        }
        if (gift.getLimitType() == 1) {
            Integer count = recordService.countByGiftAndUser(giftId, uid);
            return count < (gift.getLimitNum() == null ? 0 : gift.getLimitNum());
        }
        if (gift.getLimitType() == 2) {
            Integer count = recordService.countTodayByGiftAndUser(giftId, uid);
            return count < (gift.getLimitNum() == null ? 0 : gift.getLimitNum());
        }
        return true;
    }

    private BuyGiftResponse.ProductItem convertProductToResponseItem(BuyGiftProduct product) {
        BuyGiftResponse.ProductItem item = new BuyGiftResponse.ProductItem();
        item.setProductId(product.getProductId());
        item.setAttrValueId(product.getAttrValueId());
        item.setBuyNum(product.getBuyNum());
        item.setGiftNum(product.getGiftNum());
        return item;
    }

    private BuyGiftResponse convertToResponse(BuyGift gift) {
        BuyGiftResponse response = new BuyGiftResponse();
        BeanUtils.copyProperties(gift, response);
        String[] typeNames = {"", "同商品买N送M", "跨商品买A送B"};
        if (gift.getGiftType() != null && gift.getGiftType() >= 1 && gift.getGiftType() < typeNames.length) {
            response.setGiftTypeName(typeNames[gift.getGiftType()]);
        }
        String[] limitNames = {"不限", "总次数限制", "每日次数限制"};
        if (gift.getLimitType() != null && gift.getLimitType() >= 0 && gift.getLimitType() < limitNames.length) {
            response.setLimitTypeName(limitNames[gift.getLimitType()]);
        }
        Date now = new Date();
        if (gift.getStartTime() != null && now.before(gift.getStartTime())) {
            response.setActivityStatus(0);
        } else if (gift.getEndTime() != null && now.after(gift.getEndTime())) {
            response.setActivityStatus(2);
        } else {
            response.setActivityStatus(1);
        }
        return response;
    }
}

