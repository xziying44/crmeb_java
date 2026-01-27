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
import com.zbkj.common.model.promotion.FullReduction;
import com.zbkj.common.model.promotion.FullReductionLevel;
import com.zbkj.common.model.promotion.FullReductionProduct;
import com.zbkj.common.request.FullReductionRequest;
import com.zbkj.common.request.FullReductionSearchRequest;
import com.zbkj.common.response.FullReductionResponse;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.service.dao.promotion.FullReductionDao;
import com.zbkj.service.service.promotion.FullReductionLevelService;
import com.zbkj.service.service.promotion.FullReductionProductService;
import com.zbkj.service.service.promotion.FullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 满减活动 Service 实现类
 */
@Service
public class FullReductionServiceImpl extends ServiceImpl<FullReductionDao, FullReduction>
        implements FullReductionService {

    @Autowired
    private FullReductionLevelService levelService;

    @Autowired
    private FullReductionProductService productService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public PageInfo<FullReductionResponse> getList(FullReductionSearchRequest request) {
        PageHelper.startPage(request.getPage(), request.getLimit());
        LambdaQueryWrapper<FullReduction> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReduction::getIsDel, false);
        if (StrUtil.isNotBlank(request.getName())) {
            wrapper.like(FullReduction::getName, request.getName());
        }
        if (ObjectUtil.isNotNull(request.getStatus())) {
            wrapper.eq(FullReduction::getStatus, request.getStatus() == 1);
        }
        if (ObjectUtil.isNotNull(request.getScopeType())) {
            wrapper.eq(FullReduction::getScopeType, request.getScopeType());
        }
        wrapper.orderByDesc(FullReduction::getId);
        List<FullReduction> list = list(wrapper);
        PageInfo<FullReduction> pageInfo = new PageInfo<>(list);

        List<FullReductionResponse> responseList = list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        PageInfo<FullReductionResponse> resultPage = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, resultPage, "list");
        resultPage.setList(responseList);
        return resultPage;
    }

    @Override
    public FullReductionResponse getDetail(Integer id) {
        FullReduction reduction = getById(id);
        if (ObjectUtil.isNull(reduction) || Boolean.TRUE.equals(reduction.getIsDel())) {
            throw new CrmebException("满减活动不存在");
        }

        FullReductionResponse response = convertToResponse(reduction);

        List<FullReductionLevel> levels = levelService.getByReductionId(id);
        response.setLevels(levels.stream().map(l -> {
            FullReductionResponse.LevelItem item = new FullReductionResponse.LevelItem();
            item.setFullAmount(l.getFullAmount());
            item.setReduceAmount(l.getReduceAmount());
            return item;
        }).collect(Collectors.toList()));

        List<FullReductionProduct> products = productService.getByReductionId(id);
        response.setRelationIds(products.stream()
                .map(FullReductionProduct::getRelationId)
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    public Boolean save(FullReductionRequest request) {
        if (CollUtil.isEmpty(request.getLevels())) {
            throw new CrmebException("请设置满减阶梯");
        }
        if (request.getScopeType() != 1 && CollUtil.isEmpty(request.getRelationIds())) {
            throw new CrmebException("请选择关联的商品或品类");
        }

        FullReduction reduction = new FullReduction();
        BeanUtils.copyProperties(request, reduction);
        reduction.setStartTime(CrmebDateUtil.strToDate(request.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
        reduction.setEndTime(CrmebDateUtil.strToDate(request.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
        reduction.setIsDel(false);
        reduction.setStatus(false); // 默认关闭
        reduction.setUpdateTime(DateUtil.date());

        return transactionTemplate.execute(status -> {
            if (ObjectUtil.isNotNull(request.getId())) {
                FullReduction old = getById(request.getId());
                if (ObjectUtil.isNull(old) || Boolean.TRUE.equals(old.getIsDel())) {
                    status.setRollbackOnly();
                    throw new CrmebException("满减活动不存在");
                }
                reduction.setId(request.getId());
                updateById(reduction);
                // 删除旧的阶梯和关联
                levelService.deleteByReductionId(request.getId());
                productService.deleteByReductionId(request.getId());
            } else {
                reduction.setCreateTime(DateUtil.date());
                save(reduction);
            }

            List<FullReductionLevel> levels = request.getLevels().stream().map(l -> {
                FullReductionLevel level = new FullReductionLevel();
                level.setReductionId(reduction.getId());
                level.setFullAmount(l.getFullAmount());
                level.setReduceAmount(l.getReduceAmount());
                return level;
            }).collect(Collectors.toList());
            levelService.saveBatch(levels);

            if (request.getScopeType() != 1 && CollUtil.isNotEmpty(request.getRelationIds())) {
                int relationType = request.getScopeType() == 2 ? 1 : 2; // 2-品类 3-商品
                List<FullReductionProduct> products = request.getRelationIds().stream().map(rid -> {
                    FullReductionProduct p = new FullReductionProduct();
                    p.setReductionId(reduction.getId());
                    p.setRelationType(relationType);
                    p.setRelationId(rid);
                    return p;
                }).collect(Collectors.toList());
                productService.saveBatch(products);
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean delete(Integer id) {
        FullReduction reduction = getById(id);
        if (ObjectUtil.isNull(reduction) || Boolean.TRUE.equals(reduction.getIsDel())) {
            throw new CrmebException("满减活动不存在");
        }
        LambdaUpdateWrapper<FullReduction> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(FullReduction::getId, id);
        wrapper.set(FullReduction::getIsDel, true);
        wrapper.set(FullReduction::getStatus, false);
        wrapper.set(FullReduction::getUpdateTime, DateUtil.date());
        return update(wrapper);
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        FullReduction reduction = getById(id);
        if (ObjectUtil.isNull(reduction) || Boolean.TRUE.equals(reduction.getIsDel())) {
            throw new CrmebException("满减活动不存在");
        }
        if (reduction.getStatus().equals(status)) {
            throw new CrmebException("满减活动状态无需变更");
        }
        FullReduction update = new FullReduction();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(DateUtil.date());
        return updateById(update);
    }

    @Override
    public FullReduction getAvailableByProductIds(List<Integer> productIds, List<Integer> categoryIds) {
        Date now = new Date();
        List<Integer> candidateIds = new ArrayList<>();

        if (CollUtil.isNotEmpty(productIds)) {
            for (Integer pid : productIds) {
                candidateIds.addAll(productService.getReductionIdsByProductId(pid));
            }
        }
        if (CollUtil.isNotEmpty(categoryIds)) {
            for (Integer cid : categoryIds) {
                candidateIds.addAll(productService.getReductionIdsByCategoryId(cid));
            }
        }
        final List<Integer> candidateIdList = candidateIds.stream().distinct().collect(Collectors.toList());

        LambdaQueryWrapper<FullReduction> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FullReduction::getIsDel, false);
        wrapper.eq(FullReduction::getStatus, true);
        wrapper.le(FullReduction::getStartTime, now);
        wrapper.ge(FullReduction::getEndTime, now);
        if (CollUtil.isEmpty(candidateIdList)) {
            wrapper.eq(FullReduction::getScopeType, 1);
        } else {
            wrapper.and(i -> i.eq(FullReduction::getScopeType, 1).or().in(FullReduction::getId, candidateIdList));
        }
        wrapper.orderByDesc(FullReduction::getId);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public BigDecimal calculateReduction(FullReduction reduction, BigDecimal totalAmount) {
        if (ObjectUtil.isNull(reduction) || ObjectUtil.isNull(totalAmount) || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        List<FullReductionLevel> levels = levelService.getByReductionId(reduction.getId());
        if (CollUtil.isEmpty(levels)) {
            return BigDecimal.ZERO;
        }

        BigDecimal bestReduce = BigDecimal.ZERO;
        for (FullReductionLevel level : levels) {
            if (ObjectUtil.isNull(level.getFullAmount()) || ObjectUtil.isNull(level.getReduceAmount())) {
                continue;
            }
            if (totalAmount.compareTo(level.getFullAmount()) >= 0
                    && level.getReduceAmount().compareTo(bestReduce) > 0) {
                bestReduce = level.getReduceAmount();
            }
        }
        if (bestReduce.compareTo(BigDecimal.ZERO) < 0) {
            bestReduce = BigDecimal.ZERO;
        }
        if (bestReduce.compareTo(totalAmount) > 0) {
            bestReduce = totalAmount;
        }
        return bestReduce;
    }

    /**
     * 转换为响应对象
     */
    private FullReductionResponse convertToResponse(FullReduction reduction) {
        FullReductionResponse response = new FullReductionResponse();
        BeanUtils.copyProperties(reduction, response);
        String[] scopeNames = {"", "全场", "品类", "指定商品"};
        if (reduction.getScopeType() != null && reduction.getScopeType() >= 1 && reduction.getScopeType() < scopeNames.length) {
            response.setScopeTypeName(scopeNames[reduction.getScopeType()]);
        }
        Date now = new Date();
        if (reduction.getStartTime() != null && now.before(reduction.getStartTime())) {
            response.setActivityStatus(0);
        } else if (reduction.getEndTime() != null && now.after(reduction.getEndTime())) {
            response.setActivityStatus(2);
        } else {
            response.setActivityStatus(1);
        }
        return response;
    }
}
