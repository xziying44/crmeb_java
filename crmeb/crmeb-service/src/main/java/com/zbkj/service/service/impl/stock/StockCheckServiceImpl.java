package com.zbkj.service.service.impl.stock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.StockConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.stock.Stock;
import com.zbkj.common.model.stock.StockCheck;
import com.zbkj.common.model.stock.StockCheckItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StockCheckCreateRequest;
import com.zbkj.common.request.StockCheckUpdateItemRequest;
import com.zbkj.common.response.StockCheckResponse;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.common.vo.LoginUserVo;
import com.zbkj.service.dao.StockCheckDao;
import com.zbkj.service.dao.StockCheckItemDao;
import com.zbkj.service.dao.StockDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.stock.StockCheckService;
import com.zbkj.service.service.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存盘点服务实现
 */
@Service
public class StockCheckServiceImpl extends ServiceImpl<StockCheckDao, StockCheck> implements StockCheckService {

    @Resource
    private StockCheckDao stockCheckDao;

    @Resource
    private StockCheckItemDao stockCheckItemDao;

    @Resource
    private StockDao stockDao;

    @Resource
    private StoreProductService storeProductService;

    @Resource
    private StoreProductAttrValueService storeProductAttrValueService;

    @Resource
    private StockService stockService;

    @Override
    public CommonPage<StockCheckResponse> getList(String checkNo, Integer status, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        LambdaQueryWrapper<StockCheck> lqw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(checkNo)) {
            lqw.like(StockCheck::getCheckNo, checkNo);
        }
        if (status != null) {
            lqw.eq(StockCheck::getStatus, status);
        }
        lqw.orderByDesc(StockCheck::getCreateTime).orderByDesc(StockCheck::getId);

        List<StockCheck> list = stockCheckDao.selectList(lqw);
        List<StockCheckResponse> responses = list.stream().map(this::toResponseSimple).collect(Collectors.toList());
        return CommonPage.restPage(responses);
    }

    @Override
    public StockCheckResponse getDetail(Integer id) {
        StockCheck check = stockCheckDao.selectById(id);
        if (check == null) {
            throw new CrmebException("盘点单不存在");
        }

        List<StockCheckItem> items = getItemsByCheckId(id);
        StockCheckResponse response = toResponseSimple(check);
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(StockCheckCreateRequest request) {
        LoginUserVo loginUserVo = SecurityUtil.getLoginUserVo();
        Integer operatorId = loginUserVo.getUser() != null ? loginUserVo.getUser().getId() : null;
        String operatorName = loginUserVo.getUser() != null ? loginUserVo.getUser().getRealName() : null;

        List<Integer> productIds = request != null ? request.getProductIds() : null;

        LambdaQueryWrapper<Stock> stockLqw = new LambdaQueryWrapper<>();
        if (CollUtil.isNotEmpty(productIds)) {
            stockLqw.in(Stock::getProductId, productIds);
        }
        stockLqw.orderByAsc(Stock::getProductId).orderByAsc(Stock::getAttrValueId);
        List<Stock> stocks = stockDao.selectList(stockLqw);
        if (CollUtil.isEmpty(stocks)) {
            throw new CrmebException("暂无可盘点的库存数据");
        }

        Set<Integer> ids = stocks.stream().map(Stock::getProductId).collect(Collectors.toSet());
        Map<Integer, StoreProduct> productMap = getProductMap(ids);
        Set<Integer> skuIds = stocks.stream().map(Stock::getAttrValueId).filter(i -> i != null && i != 0).collect(Collectors.toSet());
        Map<Integer, StoreProductAttrValue> skuMap = getSkuMap(skuIds);

        StockCheck check = new StockCheck();
        check.setCheckNo(StockConstants.CHECK_NO_PREFIX + CrmebUtil.getOrderNo("check"));
        check.setStatus(StockConstants.CHECK_STATUS_CHECKING);
        check.setTotalProfit(0);
        check.setTotalLoss(0);
        check.setRemark(request != null ? request.getRemark() : null);
        check.setOperatorId(operatorId);
        check.setOperatorName(operatorName);
        check.setCreateTime(DateUtil.date());
        stockCheckDao.insert(check);

        for (Stock stock : stocks) {
            StockCheckItem item = new StockCheckItem();
            item.setCheckId(check.getId());
            item.setProductId(stock.getProductId());
            item.setAttrValueId(Optional.ofNullable(stock.getAttrValueId()).orElse(0));

            StoreProduct product = productMap.get(stock.getProductId());
            item.setProductName(product != null ? product.getStoreName() : "");

            String skuName = "";
            if (item.getAttrValueId() != 0) {
                StoreProductAttrValue sku = skuMap.get(item.getAttrValueId());
                skuName = sku != null ? Optional.ofNullable(sku.getSuk()).orElse("") : "";
            }
            item.setSkuName(skuName);

            item.setSystemStock(Optional.ofNullable(stock.getStock()).orElse(0));
            item.setActualStock(null);
            item.setDiffQuantity(0);
            stockCheckItemDao.insert(item);
        }

        return true;
    }

    @Override
    public Boolean updateItem(StockCheckUpdateItemRequest request) {
        if (request == null || request.getItemId() == null) {
            throw new CrmebException("盘点明细ID不能为空");
        }
        StockCheckItem item = stockCheckItemDao.selectById(request.getItemId());
        if (item == null) {
            throw new CrmebException("盘点明细不存在");
        }
        StockCheck check = stockCheckDao.selectById(item.getCheckId());
        if (check == null) {
            throw new CrmebException("盘点单不存在");
        }
        if (!Objects.equals(check.getStatus(), StockConstants.CHECK_STATUS_CHECKING)) {
            throw new CrmebException("盘点单状态不允许修改");
        }

        int systemStock = Optional.ofNullable(item.getSystemStock()).orElse(0);
        int actualStock = Optional.ofNullable(request.getActualStock()).orElse(0);
        item.setActualStock(actualStock);
        item.setDiffQuantity(actualStock - systemStock);
        stockCheckItemDao.updateById(item);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean confirm(Integer id) {
        StockCheck check = stockCheckDao.selectById(id);
        if (check == null) {
            throw new CrmebException("盘点单不存在");
        }
        if (!Objects.equals(check.getStatus(), StockConstants.CHECK_STATUS_CHECKING)) {
            throw new CrmebException("盘点单状态不允许确认");
        }

        List<StockCheckItem> items = getItemsByCheckId(id);
        if (CollUtil.isEmpty(items)) {
            throw new CrmebException("盘点明细不存在");
        }

        int totalProfit = 0;
        int totalLoss = 0;

        for (StockCheckItem item : items) {
            if (item.getActualStock() == null) {
                throw new CrmebException("请先录入所有盘点明细的实际库存");
            }
            int systemStock = Optional.ofNullable(item.getSystemStock()).orElse(0);
            int actualStock = item.getActualStock();
            int diff = actualStock - systemStock;
            item.setDiffQuantity(diff);
            stockCheckItemDao.updateById(item);

            if (diff > 0) {
                totalProfit += diff;
                stockService.stockIn(item.getProductId(), item.getAttrValueId(), diff,
                        StockConstants.LOG_TYPE_CHECK_PROFIT_IN, StockConstants.RELATION_TYPE_CHECK,
                        check.getId().longValue(), StrUtil.format("盘点单{}盘盈调整", check.getCheckNo()));
            } else if (diff < 0) {
                totalLoss += Math.abs(diff);
                stockService.stockOut(item.getProductId(), item.getAttrValueId(), Math.abs(diff),
                        StockConstants.LOG_TYPE_CHECK_LOSS_OUT, StockConstants.RELATION_TYPE_CHECK,
                        check.getId().longValue(), StrUtil.format("盘点单{}盘亏调整", check.getCheckNo()));
            }
        }

        check.setTotalProfit(totalProfit);
        check.setTotalLoss(totalLoss);
        check.setStatus(StockConstants.CHECK_STATUS_COMPLETED);
        check.setFinishTime(DateUtil.date());
        stockCheckDao.updateById(check);
        return true;
    }

    @Override
    public Boolean cancel(Integer id) {
        StockCheck check = stockCheckDao.selectById(id);
        if (check == null) {
            throw new CrmebException("盘点单不存在");
        }
        if (Objects.equals(check.getStatus(), StockConstants.CHECK_STATUS_CANCELLED)) {
            return true;
        }
        if (!Objects.equals(check.getStatus(), StockConstants.CHECK_STATUS_CHECKING)) {
            throw new CrmebException("盘点单状态不允许取消");
        }
        check.setStatus(StockConstants.CHECK_STATUS_CANCELLED);
        stockCheckDao.updateById(check);
        return true;
    }

    private List<StockCheckItem> getItemsByCheckId(Integer checkId) {
        LambdaQueryWrapper<StockCheckItem> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StockCheckItem::getCheckId, checkId);
        lqw.orderByAsc(StockCheckItem::getId);
        return stockCheckItemDao.selectList(lqw);
    }

    private Map<Integer, StoreProduct> getProductMap(Set<Integer> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<StoreProduct> lqw = new LambdaQueryWrapper<>();
        lqw.in(StoreProduct::getId, productIds);
        List<StoreProduct> products = storeProductService.list(lqw);
        return products.stream().collect(Collectors.toMap(StoreProduct::getId, p -> p, (a, b) -> a));
    }

    private Map<Integer, StoreProductAttrValue> getSkuMap(Set<Integer> skuIds) {
        if (CollUtil.isEmpty(skuIds)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<StoreProductAttrValue> lqw = new LambdaQueryWrapper<>();
        lqw.in(StoreProductAttrValue::getId, skuIds);
        List<StoreProductAttrValue> skus = storeProductAttrValueService.list(lqw);
        return skus.stream().collect(Collectors.toMap(StoreProductAttrValue::getId, s -> s, (a, b) -> a));
    }

    private StockCheckResponse toResponseSimple(StockCheck check) {
        StockCheckResponse resp = new StockCheckResponse();
        resp.setId(check.getId());
        resp.setCheckNo(check.getCheckNo());
        resp.setStatus(check.getStatus());
        resp.setStatusName(getStatusName(check.getStatus()));
        resp.setTotalProfit(check.getTotalProfit());
        resp.setTotalLoss(check.getTotalLoss());
        resp.setRemark(check.getRemark());
        resp.setOperatorName(check.getOperatorName());
        resp.setCreateTime(check.getCreateTime());
        resp.setFinishTime(check.getFinishTime());
        return resp;
    }

    private String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case StockConstants.CHECK_STATUS_CHECKING:
                return "盘点中";
            case StockConstants.CHECK_STATUS_COMPLETED:
                return "已完成";
            case StockConstants.CHECK_STATUS_CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }
}

