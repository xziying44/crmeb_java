package com.zbkj.service.service.impl.stock;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.stock.Supplier;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SupplierRequest;
import com.zbkj.common.request.SupplierSearchRequest;
import com.zbkj.service.dao.SupplierDao;
import com.zbkj.service.service.stock.SupplierService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 供应商管理服务实现
 */
@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierDao, Supplier> implements SupplierService {

    @Resource
    private SupplierDao supplierDao;

    @Override
    public CommonPage<Supplier> getList(SupplierSearchRequest request, PageParamRequest pageParamRequest) {
        SupplierSearchRequest realRequest = request != null ? request : new SupplierSearchRequest();
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        LambdaQueryWrapper<Supplier> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Supplier::getIsDel, false);
        if (StrUtil.isNotBlank(realRequest.getKeywords())) {
            lqw.and(w -> w.like(Supplier::getName, realRequest.getKeywords())
                    .or().like(Supplier::getContact, realRequest.getKeywords())
                    .or().like(Supplier::getPhone, realRequest.getKeywords()));
        }
        if (realRequest.getStatus() != null) {
            lqw.eq(Supplier::getStatus, realRequest.getStatus());
        }
        lqw.orderByDesc(Supplier::getUpdateTime).orderByDesc(Supplier::getId);

        List<Supplier> list = supplierDao.selectList(lqw);
        return CommonPage.restPage(list);
    }

    @Override
    public Supplier getDetail(Integer id) {
        Supplier supplier = supplierDao.selectById(id);
        if (supplier == null || Boolean.TRUE.equals(supplier.getIsDel())) {
            throw new CrmebException("供应商不存在");
        }
        return supplier;
    }

    @Override
    public Boolean create(SupplierRequest request) {
        if (request == null) {
            throw new CrmebException("请求参数不能为空");
        }

        // 名称唯一校验（未删除范围内）
        LambdaQueryWrapper<Supplier> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Supplier::getIsDel, false);
        lqw.eq(Supplier::getName, request.getName());
        Integer count = supplierDao.selectCount(lqw);
        if (count != null && count > 0) {
            throw new CrmebException("供应商名称已存在");
        }

        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(request, supplier);
        supplier.setId(null);
        supplier.setStatus(true);
        supplier.setIsDel(false);
        supplier.setCreateTime(DateUtil.date());
        supplier.setUpdateTime(DateUtil.date());

        return supplierDao.insert(supplier) > 0;
    }

    @Override
    public Boolean update(SupplierRequest request) {
        if (request == null || request.getId() == null) {
            throw new CrmebException("供应商ID不能为空");
        }

        Supplier supplier = getDetail(request.getId());

        // 名称唯一校验（排除自己）
        if (StrUtil.isNotBlank(request.getName())) {
            LambdaQueryWrapper<Supplier> lqw = new LambdaQueryWrapper<>();
            lqw.eq(Supplier::getIsDel, false);
            lqw.eq(Supplier::getName, request.getName());
            lqw.ne(Supplier::getId, request.getId());
            Integer count = supplierDao.selectCount(lqw);
            if (count != null && count > 0) {
                throw new CrmebException("供应商名称已存在");
            }
        }

        BeanUtils.copyProperties(request, supplier);
        supplier.setUpdateTime(DateUtil.date());
        return supplierDao.updateById(supplier) > 0;
    }

    @Override
    public Boolean delete(Integer id) {
        Supplier supplier = getDetail(id);
        if (ObjectUtil.isNull(supplier)) {
            return false;
        }
        supplier.setIsDel(true);
        supplier.setUpdateTime(DateUtil.date());
        return supplierDao.updateById(supplier) > 0;
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        Supplier supplier = getDetail(id);
        supplier.setStatus(status);
        supplier.setUpdateTime(DateUtil.date());
        return supplierDao.updateById(supplier) > 0;
    }

    @Override
    public List<Supplier> getEnabledList() {
        LambdaQueryWrapper<Supplier> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Supplier::getIsDel, false);
        lqw.eq(Supplier::getStatus, true);
        lqw.orderByDesc(Supplier::getUpdateTime).orderByDesc(Supplier::getId);
        return supplierDao.selectList(lqw);
    }
}

