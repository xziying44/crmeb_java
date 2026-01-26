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
import com.zbkj.common.model.stock.Purchase;
import com.zbkj.common.model.stock.PurchaseItem;
import com.zbkj.common.model.stock.Supplier;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.PurchaseAddRequest;
import com.zbkj.common.request.PurchaseInStockRequest;
import com.zbkj.common.response.PurchaseResponse;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.common.vo.LoginUserVo;
import com.zbkj.service.dao.PurchaseDao;
import com.zbkj.service.dao.PurchaseItemDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.stock.PurchaseService;
import com.zbkj.service.service.stock.StockService;
import com.zbkj.service.service.stock.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购管理服务实现
 */
@Service
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, Purchase> implements PurchaseService {

    @Resource
    private PurchaseDao purchaseDao;

    @Resource
    private PurchaseItemDao purchaseItemDao;

    @Resource
    private SupplierService supplierService;

    @Resource
    private StoreProductService storeProductService;

    @Resource
    private StoreProductAttrValueService storeProductAttrValueService;

    @Resource
    private StockService stockService;

    @Override
    public CommonPage<PurchaseResponse> getList(String purchaseNo, Integer supplierId, Integer status, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        LambdaQueryWrapper<Purchase> lqw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(purchaseNo)) {
            lqw.like(Purchase::getPurchaseNo, purchaseNo);
        }
        if (supplierId != null) {
            lqw.eq(Purchase::getSupplierId, supplierId);
        }
        if (status != null) {
            lqw.eq(Purchase::getStatus, status);
        }
        lqw.orderByDesc(Purchase::getUpdateTime).orderByDesc(Purchase::getId);

        List<Purchase> list = purchaseDao.selectList(lqw);
        if (CollUtil.isEmpty(list)) {
            return CommonPage.restPage(new ArrayList<>());
        }

        Map<Integer, Supplier> supplierMap = getSupplierMap(list.stream().map(Purchase::getSupplierId).collect(Collectors.toSet()));
        List<PurchaseResponse> responses = list.stream().map(p -> toResponse(p, supplierMap.get(p.getSupplierId()), null)).collect(Collectors.toList());
        return CommonPage.restPage(responses);
    }

    @Override
    public PurchaseResponse getDetail(Integer id) {
        Purchase purchase = purchaseDao.selectById(id);
        if (purchase == null) {
            throw new CrmebException("采购单不存在");
        }

        Supplier supplier = supplierService.getDetail(purchase.getSupplierId());
        List<PurchaseItem> items = getItemsByPurchaseId(id);
        return toResponse(purchase, supplier, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(PurchaseAddRequest request) {
        if (request == null) {
            throw new CrmebException("请求参数不能为空");
        }
        if (CollUtil.isEmpty(request.getItems())) {
            throw new CrmebException("采购商品不能为空");
        }

        Supplier supplier = supplierService.getDetail(request.getSupplierId());
        if (!Boolean.TRUE.equals(supplier.getStatus())) {
            throw new CrmebException("供应商已禁用");
        }

        LoginUserVo loginUserVo = SecurityUtil.getLoginUserVo();
        Integer operatorId = loginUserVo.getUser() != null ? loginUserVo.getUser().getId() : null;
        String operatorName = loginUserVo.getUser() != null ? loginUserVo.getUser().getRealName() : null;

        String purchaseNo = StockConstants.PURCHASE_NO_PREFIX + CrmebUtil.getOrderNo("purchase");

        Purchase purchase = new Purchase();
        purchase.setPurchaseNo(purchaseNo);
        purchase.setSupplierId(request.getSupplierId());
        purchase.setStatus(StockConstants.PURCHASE_STATUS_PENDING);
        purchase.setRemark(request.getRemark());
        purchase.setOperatorId(operatorId);
        purchase.setOperatorName(operatorName);
        purchase.setCreateTime(DateUtil.date());
        purchase.setUpdateTime(DateUtil.date());

        List<PurchaseItem> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseAddRequest.PurchaseItemRequest itemReq : request.getItems()) {
            StoreProduct product = storeProductService.getById(itemReq.getProductId());
            if (product == null || Boolean.TRUE.equals(product.getIsDel())) {
                throw new CrmebException("商品不存在");
            }

            Integer attrValueId = Optional.ofNullable(itemReq.getAttrValueId()).orElse(0);
            String skuName = "";
            if (attrValueId != 0) {
                StoreProductAttrValue attrValue = storeProductAttrValueService.getById(attrValueId);
                if (attrValue == null || Boolean.TRUE.equals(attrValue.getIsDel())) {
                    throw new CrmebException("商品规格不存在");
                }
                skuName = Optional.ofNullable(attrValue.getSuk()).orElse("");
            }

            Integer quantity = Optional.ofNullable(itemReq.getQuantity()).orElse(0);
            if (quantity <= 0) {
                throw new CrmebException("采购数量必须大于0");
            }

            BigDecimal price = Optional.ofNullable(itemReq.getPrice()).orElse(BigDecimal.ZERO);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new CrmebException("采购单价不能小于0");
            }

            PurchaseItem item = new PurchaseItem();
            item.setProductId(product.getId());
            item.setAttrValueId(attrValueId);
            item.setProductName(product.getStoreName());
            item.setSkuName(skuName);
            item.setQuantity(quantity);
            item.setInQuantity(0);
            item.setPrice(price);
            item.setAmount(price.multiply(new BigDecimal(quantity)));
            items.add(item);

            totalQuantity += quantity;
            totalAmount = totalAmount.add(item.getAmount());
        }

        purchase.setTotalQuantity(totalQuantity);
        purchase.setTotalAmount(totalAmount);

        purchaseDao.insert(purchase);

        for (PurchaseItem item : items) {
            item.setPurchaseId(purchase.getId());
            purchaseItemDao.insert(item);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Integer id) {
        Purchase purchase = purchaseDao.selectById(id);
        if (purchase == null) {
            throw new CrmebException("采购单不存在");
        }
        if (Objects.equals(purchase.getStatus(), StockConstants.PURCHASE_STATUS_COMPLETED)) {
            throw new CrmebException("采购单已入库，不能取消");
        }
        if (Objects.equals(purchase.getStatus(), StockConstants.PURCHASE_STATUS_CANCELLED)) {
            return true;
        }

        purchase.setStatus(StockConstants.PURCHASE_STATUS_CANCELLED);
        purchase.setUpdateTime(DateUtil.date());
        purchaseDao.updateById(purchase);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean inStock(PurchaseInStockRequest request) {
        if (request == null || request.getPurchaseId() == null) {
            throw new CrmebException("采购单ID不能为空");
        }
        if (CollUtil.isEmpty(request.getItems())) {
            throw new CrmebException("入库明细不能为空");
        }

        Purchase purchase = purchaseDao.selectById(request.getPurchaseId());
        if (purchase == null) {
            throw new CrmebException("采购单不存在");
        }
        if (Objects.equals(purchase.getStatus(), StockConstants.PURCHASE_STATUS_CANCELLED)) {
            throw new CrmebException("采购单已取消，不能入库");
        }

        List<PurchaseItem> items = getItemsByPurchaseId(purchase.getId());
        if (CollUtil.isEmpty(items)) {
            throw new CrmebException("采购单明细不存在");
        }
        Map<Integer, PurchaseItem> itemMap = items.stream().collect(Collectors.toMap(PurchaseItem::getId, i -> i));

        // 逐条入库并更新明细已入库数量
        for (PurchaseInStockRequest.InStockItemRequest inReq : request.getItems()) {
            PurchaseItem item = itemMap.get(inReq.getItemId());
            if (item == null) {
                throw new CrmebException("入库明细不存在");
            }
            int inQty = Optional.ofNullable(inReq.getQuantity()).orElse(0);
            if (inQty <= 0) {
                throw new CrmebException("入库数量必须大于0");
            }
            int remain = Optional.ofNullable(item.getQuantity()).orElse(0) - Optional.ofNullable(item.getInQuantity()).orElse(0);
            if (inQty > remain) {
                throw new CrmebException("入库数量不能大于剩余待入库数量");
            }

            // 库存增加（采购入库）
            stockService.stockIn(item.getProductId(), item.getAttrValueId(), inQty,
                    StockConstants.LOG_TYPE_PURCHASE_IN, StockConstants.RELATION_TYPE_PURCHASE,
                    purchase.getId().longValue(), StrUtil.format("采购入库：{}，明细ID={}", purchase.getPurchaseNo(), item.getId()));

            item.setInQuantity(Optional.ofNullable(item.getInQuantity()).orElse(0) + inQty);
            purchaseItemDao.updateById(item);
        }

        // 更新采购单状态：部分入库 / 已入库
        List<PurchaseItem> latestItems = getItemsByPurchaseId(purchase.getId());
        boolean allIn = latestItems.stream().allMatch(i ->
                Optional.ofNullable(i.getInQuantity()).orElse(0) >= Optional.ofNullable(i.getQuantity()).orElse(0));
        purchase.setStatus(allIn ? StockConstants.PURCHASE_STATUS_COMPLETED : StockConstants.PURCHASE_STATUS_PARTIAL);
        purchase.setUpdateTime(DateUtil.date());
        purchaseDao.updateById(purchase);
        return true;
    }

    private List<PurchaseItem> getItemsByPurchaseId(Integer purchaseId) {
        LambdaQueryWrapper<PurchaseItem> lqw = new LambdaQueryWrapper<>();
        lqw.eq(PurchaseItem::getPurchaseId, purchaseId);
        lqw.orderByAsc(PurchaseItem::getId);
        return purchaseItemDao.selectList(lqw);
    }

    private Map<Integer, Supplier> getSupplierMap(Set<Integer> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<Supplier> lqw = new LambdaQueryWrapper<>();
        lqw.in(Supplier::getId, supplierIds);
        List<Supplier> suppliers = supplierService.list(lqw);
        return suppliers.stream().collect(Collectors.toMap(Supplier::getId, s -> s, (a, b) -> a));
    }

    private PurchaseResponse toResponse(Purchase purchase, Supplier supplier, List<PurchaseItem> items) {
        PurchaseResponse resp = new PurchaseResponse();
        resp.setId(purchase.getId());
        resp.setPurchaseNo(purchase.getPurchaseNo());
        resp.setSupplierId(purchase.getSupplierId());
        resp.setSupplierName(supplier != null ? supplier.getName() : "");
        resp.setTotalQuantity(purchase.getTotalQuantity());
        resp.setTotalAmount(purchase.getTotalAmount());
        resp.setStatus(purchase.getStatus());
        resp.setStatusName(getStatusName(purchase.getStatus()));
        resp.setRemark(purchase.getRemark());
        resp.setOperatorName(purchase.getOperatorName());
        resp.setCreateTime(purchase.getCreateTime());
        if (items != null) {
            resp.setItems(items);
        }
        return resp;
    }

    private String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case StockConstants.PURCHASE_STATUS_PENDING:
                return "待入库";
            case StockConstants.PURCHASE_STATUS_PARTIAL:
                return "部分入库";
            case StockConstants.PURCHASE_STATUS_COMPLETED:
                return "已入库";
            case StockConstants.PURCHASE_STATUS_CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }
}
