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
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.stock.Stock;
import com.zbkj.common.model.stock.StockLog;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.StockInRequest;
import com.zbkj.common.request.StockOutRequest;
import com.zbkj.common.response.StockLogResponse;
import com.zbkj.common.response.StockResponse;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.common.vo.LoginUserVo;
import com.zbkj.service.dao.StockDao;
import com.zbkj.service.dao.StockLogDao;
import com.zbkj.service.service.StoreOrderInfoService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.stock.StockService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 库存核心服务实现（统一入口）
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockDao, Stock> implements StockService {

    @Resource
    private StockDao stockDao;

    @Resource
    private StockLogDao stockLogDao;

    @Resource
    private StoreProductService storeProductService;

    @Resource
    private StoreProductAttrValueService storeProductAttrValueService;

    @Resource
    private StoreOrderService storeOrderService;

    @Resource
    private StoreOrderInfoService storeOrderInfoService;

    @Override
    public CommonPage<StockResponse> getStockList(String keywords, Boolean warning, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        List<StockResponse> list = stockDao.selectStockList(keywords, warning);
        return CommonPage.restPage(list);
    }

    @Override
    public CommonPage<StockLogResponse> getLogList(Integer productId, Integer type, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        List<StockLogResponse> list = stockLogDao.selectLogList(productId, type);
        list.forEach(l -> l.setTypeName(getLogTypeName(l.getType())));
        return CommonPage.restPage(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean manualIn(StockInRequest request) {
        if (request == null) {
            throw new CrmebException("请求参数不能为空");
        }
        if (!Objects.equals(request.getType(), StockConstants.LOG_TYPE_RETURN_IN)
                && !Objects.equals(request.getType(), StockConstants.LOG_TYPE_OTHER_IN)) {
            throw new CrmebException("入库类型不正确");
        }

        int attrValueId = Optional.ofNullable(request.getAttrValueId()).orElse(0);
        return stockIn(request.getProductId(), attrValueId, request.getQuantity(), request.getType(),
                StockConstants.RELATION_TYPE_MANUAL, null, request.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean manualOut(StockOutRequest request) {
        if (request == null) {
            throw new CrmebException("请求参数不能为空");
        }
        if (!Objects.equals(request.getType(), StockConstants.LOG_TYPE_DAMAGE_OUT)
                && !Objects.equals(request.getType(), StockConstants.LOG_TYPE_OTHER_OUT)) {
            throw new CrmebException("出库类型不正确");
        }

        int attrValueId = Optional.ofNullable(request.getAttrValueId()).orElse(0);
        return stockOut(request.getProductId(), attrValueId, request.getQuantity(), request.getType(),
                StockConstants.RELATION_TYPE_MANUAL, null, request.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean stockIn(Integer productId, Integer attrValueId, Integer quantity, Integer type,
                           String relationType, Long relationId, String remark) {
        int realAttrValueId = Optional.ofNullable(attrValueId).orElse(0);
        return addStockWithLog(productId, realAttrValueId, quantity, type, relationType, relationId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean stockOut(Integer productId, Integer attrValueId, Integer quantity, Integer type,
                            String relationType, Long relationId, String remark) {
        int realAttrValueId = Optional.ofNullable(attrValueId).orElse(0);
        return deductStockWithLog(productId, realAttrValueId, quantity, type, relationType, relationId, remark);
    }

    @Override
    public Boolean updateWarningStock(Integer productId, Integer attrValueId, Integer warningStock) {
        if (productId == null) {
            throw new CrmebException("商品ID不能为空");
        }
        if (attrValueId == null) {
            attrValueId = 0;
        }
        if (warningStock == null || warningStock < 0) {
            throw new CrmebException("预警库存不能小于0");
        }

        Stock stock = getOrCreateStockRecord(productId, attrValueId);
        stock.setWarningStock(warningStock);
        stock.setUpdateTime(DateUtil.date());
        return stockDao.updateById(stock) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean salesOut(Integer orderId) {
        if (orderId == null) {
            throw new CrmebException("订单ID不能为空");
        }
        StoreOrder storeOrder = storeOrderService.getById(orderId);
        if (storeOrder == null || Boolean.TRUE.equals(storeOrder.getIsDel())) {
            throw new CrmebException("订单不存在");
        }

        LambdaQueryWrapper<StoreOrderInfo> infoLqw = new LambdaQueryWrapper<>();
        infoLqw.eq(StoreOrderInfo::getOrderId, orderId);
        List<StoreOrderInfo> orderInfos = storeOrderInfoService.list(infoLqw);
        if (CollUtil.isEmpty(orderInfos)) {
            return true;
        }

        for (StoreOrderInfo info : orderInfos) {
            Integer productId = info.getProductId();
            Integer attrValueId = Optional.ofNullable(info.getAttrValueId()).orElse(0);
            Integer payNum = Optional.ofNullable(info.getPayNum()).orElse(0);
            if (payNum <= 0) {
                continue;
            }

            deductStockWithLog(productId, attrValueId, payNum,
                    StockConstants.LOG_TYPE_SALES_OUT, StockConstants.RELATION_TYPE_ORDER,
                    storeOrder.getId().longValue(), StrUtil.format("订单发货出库：{}", storeOrder.getOrderId()));
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean initStockFromProduct() {
        // 1) 先同步规格库存（普通商品 type=0）
        LambdaQueryWrapper<StoreProductAttrValue> attrLqw = new LambdaQueryWrapper<>();
        attrLqw.eq(StoreProductAttrValue::getType, 0);
        attrLqw.eq(StoreProductAttrValue::getIsDel, false);
        List<StoreProductAttrValue> attrValues = storeProductAttrValueService.list(attrLqw);

        Set<Integer> hasSkuProductIds = new HashSet<>();
        for (StoreProductAttrValue attrValue : attrValues) {
            hasSkuProductIds.add(attrValue.getProductId());
            upsertStock(attrValue.getProductId(), attrValue.getId(), Optional.ofNullable(attrValue.getStock()).orElse(0));
        }

        // 2) 再同步无规格商品（若该商品没有任何规格记录）
        LambdaQueryWrapper<StoreProduct> productLqw = new LambdaQueryWrapper<>();
        productLqw.eq(StoreProduct::getIsDel, false);
        List<StoreProduct> products = storeProductService.list(productLqw);
        for (StoreProduct product : products) {
            if (hasSkuProductIds.contains(product.getId())) {
                continue;
            }
            upsertStock(product.getId(), 0, Optional.ofNullable(product.getStock()).orElse(0));
        }

        return true;
    }

    private void upsertStock(Integer productId, Integer attrValueId, Integer stockNum) {
        Stock exist = getStockRecord(productId, attrValueId);
        if (exist == null) {
            Stock stock = new Stock();
            stock.setProductId(productId);
            stock.setAttrValueId(attrValueId);
            stock.setStock(stockNum);
            stock.setWarningStock(0);
            stock.setCreateTime(DateUtil.date());
            stock.setUpdateTime(DateUtil.date());
            try {
                stockDao.insert(stock);
            } catch (DuplicateKeyException e) {
                // 并发情况下可能已插入，忽略并走更新
            }
        }

        Stock update = getOrCreateStockRecord(productId, attrValueId);
        update.setStock(stockNum);
        update.setUpdateTime(DateUtil.date());
        stockDao.updateById(update);
    }

    private Stock getStockRecord(Integer productId, Integer attrValueId) {
        LambdaQueryWrapper<Stock> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Stock::getProductId, productId);
        lqw.eq(Stock::getAttrValueId, attrValueId);
        return stockDao.selectOne(lqw);
    }

    private Stock getOrCreateStockRecord(Integer productId, Integer attrValueId) {
        Stock stock = getStockRecord(productId, attrValueId);
        if (stock != null) {
            return stock;
        }

        Stock create = new Stock();
        create.setProductId(productId);
        create.setAttrValueId(attrValueId);
        create.setStock(0);
        create.setWarningStock(0);
        create.setCreateTime(DateUtil.date());
        create.setUpdateTime(DateUtil.date());
        try {
            stockDao.insert(create);
        } catch (DuplicateKeyException e) {
            // 并发情况下可能已插入，忽略
        }
        return Optional.ofNullable(getStockRecord(productId, attrValueId))
                .orElseThrow(() -> new CrmebException("库存记录创建失败"));
    }

    private Boolean addStockWithLog(Integer productId, Integer attrValueId, Integer quantity, Integer type,
                                    String relationType, Long relationId, String remark) {
        if (productId == null) {
            throw new CrmebException("商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new CrmebException("数量必须大于0");
        }

        Stock stock = getOrCreateStockRecord(productId, attrValueId);
        Integer before = Optional.ofNullable(stock.getStock()).orElse(0);

        // 增加库存
        if (before == 0) {
            // 确保记录存在后再增量更新
            stockDao.addStock(productId, attrValueId, quantity);
        } else {
            stockDao.addStock(productId, attrValueId, quantity);
        }

        Stock afterStock = getStockRecord(productId, attrValueId);
        Integer after = Optional.ofNullable(afterStock.getStock()).orElse(before + quantity);

        createLog(productId, attrValueId, type, quantity, before, after, relationType, relationId, remark);
        syncProductStock(productId);
        return true;
    }

    private Boolean deductStockWithLog(Integer productId, Integer attrValueId, Integer quantity, Integer type,
                                       String relationType, Long relationId, String remark) {
        if (productId == null) {
            throw new CrmebException("商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new CrmebException("数量必须大于0");
        }

        Stock stock = getStockRecord(productId, attrValueId);
        if (stock == null) {
            throw new CrmebException("库存记录不存在");
        }
        Integer before = Optional.ofNullable(stock.getStock()).orElse(0);
        int update = stockDao.deductStock(productId, attrValueId, quantity);
        if (update <= 0) {
            throw new CrmebException("库存不足");
        }

        Stock afterStock = getStockRecord(productId, attrValueId);
        Integer after = Optional.ofNullable(afterStock.getStock()).orElse(before - quantity);

        // 出库记录数量为负数
        createLog(productId, attrValueId, type, -quantity, before, after, relationType, relationId, remark);
        syncProductStock(productId);
        return true;
    }

    private void createLog(Integer productId, Integer attrValueId, Integer type, Integer quantity,
                           Integer beforeStock, Integer afterStock,
                           String relationType, Long relationId, String remark) {
        LoginUserVo loginUserVo = SecurityUtil.getLoginUserVo();
        Integer operatorId = loginUserVo.getUser() != null ? loginUserVo.getUser().getId() : null;
        String operatorName = loginUserVo.getUser() != null ? loginUserVo.getUser().getRealName() : null;

        StockLog log = new StockLog();
        log.setProductId(productId);
        log.setAttrValueId(attrValueId);
        log.setType(type);
        log.setQuantity(quantity);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setRelationType(relationType);
        log.setRelationId(relationId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        log.setCreateTime(DateUtil.date());
        stockLogDao.insert(log);
    }

    /**
     * 同步商品与规格库存（将 eb_stock 的库存写回商品/规格表）
     */
    private void syncProductStock(Integer productId) {
        if (productId == null) {
            return;
        }

        Integer sum = stockDao.sumStockByProductId(productId);
        if (sum == null) {
            sum = 0;
        }

        // 同步主商品库存
        storeProductService.update().setSql("stock = " + sum).eq("id", productId).update();

        // 同步规格库存（只同步存在规格ID的记录）
        LambdaQueryWrapper<Stock> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Stock::getProductId, productId);
        lqw.ne(Stock::getAttrValueId, 0);
        List<Stock> skuStocks = stockDao.selectList(lqw);
        if (CollUtil.isEmpty(skuStocks)) {
            return;
        }
        for (Stock skuStock : skuStocks) {
            storeProductAttrValueService.update().setSql("stock = " + Optional.ofNullable(skuStock.getStock()).orElse(0))
                    .eq("id", skuStock.getAttrValueId()).update();
        }
    }

    private String getLogTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case StockConstants.LOG_TYPE_PURCHASE_IN:
                return "采购入库";
            case StockConstants.LOG_TYPE_RETURN_IN:
                return "退货入库";
            case StockConstants.LOG_TYPE_CHECK_PROFIT_IN:
                return "盘盈入库";
            case StockConstants.LOG_TYPE_OTHER_IN:
                return "其他入库";
            case StockConstants.LOG_TYPE_SALES_OUT:
                return "销售出库";
            case StockConstants.LOG_TYPE_DAMAGE_OUT:
                return "报损出库";
            case StockConstants.LOG_TYPE_CHECK_LOSS_OUT:
                return "盘亏出库";
            case StockConstants.LOG_TYPE_OTHER_OUT:
                return "其他出库";
            default:
                return "未知";
        }
    }
}
