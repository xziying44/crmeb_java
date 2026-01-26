package com.zbkj.service.service.stock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.stock.Supplier;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SupplierRequest;
import com.zbkj.common.request.SupplierSearchRequest;

import java.util.List;

/**
 * 供应商管理服务
 */
public interface SupplierService extends IService<Supplier> {

    /**
     * 供应商分页列表
     */
    CommonPage<Supplier> getList(SupplierSearchRequest request, PageParamRequest pageParamRequest);

    /**
     * 供应商详情
     */
    Supplier getDetail(Integer id);

    /**
     * 新增供应商
     */
    Boolean create(SupplierRequest request);

    /**
     * 编辑供应商
     */
    Boolean update(SupplierRequest request);

    /**
     * 删除供应商（软删除）
     */
    Boolean delete(Integer id);

    /**
     * 启用/禁用供应商
     */
    Boolean updateStatus(Integer id, Boolean status);

    /**
     * 获取启用状态的供应商列表（下拉用）
     */
    List<Supplier> getEnabledList();
}

