package com.zbkj.service.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.salesman.SalesmanInfo;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.system.SystemRole;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.CustomerTransferRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SalesmanAddRequest;
import com.zbkj.common.request.SalesmanSearchRequest;
import com.zbkj.common.request.SalesmanUpdateRequest;
import com.zbkj.common.request.SystemAdminAddRequest;
import com.zbkj.common.request.SystemAdminLoginRequest;
import com.zbkj.common.request.SystemAdminUpdateRequest;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.vo.CustomerConsumeStatsVo;
import com.zbkj.common.vo.SalesmanDashboardVo;
import com.zbkj.service.service.SalesmanInfoService;
import com.zbkj.service.service.SalesmanService;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.SystemAdminService;
import com.zbkj.service.service.SystemRoleService;
import com.zbkj.service.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务员管理服务实现类
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Slf4j
@Service
public class SalesmanServiceImpl implements SalesmanService {

    @Autowired
    private SalesmanInfoService salesmanInfoService;

    @Autowired
    private SystemAdminService systemAdminService;

    @Autowired
    private SystemRoleService systemRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Override
    public CommonPage<SalesmanResponse> getList(SalesmanSearchRequest request, PageParamRequest pageRequest) {
        SalesmanSearchRequest realRequest = request != null ? request : new SalesmanSearchRequest();

        // 1. 获取业务员角色ID
        Integer salesmanRoleId = getSalesmanRoleId();
        if (salesmanRoleId == null) {
            return CommonPage.restPage(new ArrayList<>());
        }

        // 2. 查询具有业务员角色的管理员
        Page<SystemAdmin> adminPage = PageHelper.startPage(pageRequest.getPage(), pageRequest.getLimit());
        LambdaQueryWrapper<SystemAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemAdmin::getIsDel, false);
        // 角色包含业务员角色ID
        wrapper.apply("FIND_IN_SET({0}, roles)", salesmanRoleId);

        // 关键字搜索
        if (StrUtil.isNotBlank(realRequest.getKeywords())) {
            wrapper.and(w -> w.like(SystemAdmin::getRealName, realRequest.getKeywords())
                    .or().like(SystemAdmin::getPhone, realRequest.getKeywords())
                    .or().like(SystemAdmin::getAccount, realRequest.getKeywords()));
        }
        if (realRequest.getStatus() != null) {
            wrapper.eq(SystemAdmin::getStatus, realRequest.getStatus());
        }
        wrapper.orderByDesc(SystemAdmin::getId);

        List<SystemAdmin> adminList = systemAdminService.list(wrapper);
        if (adminList.isEmpty()) {
            PageInfo<SalesmanResponse> emptyPageInfo = CommonPage.copyPageInfo(adminPage, new ArrayList<>());
            return CommonPage.restPage(emptyPageInfo);
        }

        // 3. 组装响应数据
        List<Integer> adminIds = adminList.stream().map(SystemAdmin::getId).collect(Collectors.toList());

        // 获取业务员扩展信息
        Map<Integer, SalesmanInfo> infoMap = getSalesmanInfoMap(adminIds);

        // 获取客户数量统计
        Map<Integer, Integer> customerCountMap = getCustomerCountMap(adminIds);
        Map<Integer, Integer> monthCustomerCountMap = getMonthNewCustomerCountMap(adminIds);

        List<SalesmanResponse> responseList = adminList.stream().map(admin -> {
            SalesmanResponse response = new SalesmanResponse();
            response.setId(admin.getId());
            response.setAccount(admin.getAccount());
            response.setRealName(admin.getRealName());
            response.setPhone(admin.getPhone());
            response.setStatus(admin.getStatus());
            response.setCreateTime(admin.getCreateTime());

            SalesmanInfo info = infoMap.get(admin.getId());
            if (info != null) {
                response.setSalesmanCode(info.getSalesmanCode());
                response.setSalesmanQrcode(info.getSalesmanQrcode());
                response.setBindable(info.getBindable());
            }

            response.setCustomerCount(customerCountMap.getOrDefault(admin.getId(), 0));
            response.setMonthNewCustomerCount(monthCustomerCountMap.getOrDefault(admin.getId(), 0));

            return response;
        }).collect(Collectors.toList());

        PageInfo<SalesmanResponse> pageInfo = CommonPage.copyPageInfo(adminPage, responseList);
        return CommonPage.restPage(pageInfo);
    }

    @Override
    public SalesmanResponse getDetail(Integer id) {
        SystemAdmin admin = systemAdminService.getById(id);
        if (admin == null || admin.getIsDel()) {
            throw new CrmebException("业务员不存在");
        }

        SalesmanResponse response = new SalesmanResponse();
        response.setId(admin.getId());
        response.setAccount(admin.getAccount());
        response.setRealName(admin.getRealName());
        response.setPhone(admin.getPhone());
        response.setStatus(admin.getStatus());
        response.setCreateTime(admin.getCreateTime());

        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info != null) {
            response.setSalesmanCode(info.getSalesmanCode());
            response.setSalesmanQrcode(info.getSalesmanQrcode());
            response.setBindable(info.getBindable());
        }

        // 客户统计
        response.setCustomerCount(getCustomerCount(id));
        response.setMonthNewCustomerCount(getMonthNewCustomerCount(id));

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(SalesmanAddRequest request) {
        // 1. 检查账号是否存在
        if (systemAdminService.checkAccount(request.getAccount())) {
            throw new CrmebException("账号已存在");
        }

        // 2. 获取业务员角色ID
        Integer salesmanRoleId = getSalesmanRoleId();
        if (salesmanRoleId == null) {
            throw new CrmebException("业务员角色未配置，请先创建业务员角色");
        }

        // 3. 创建管理员账号
        SystemAdminAddRequest adminRequest = new SystemAdminAddRequest();
        adminRequest.setAccount(request.getAccount());
        adminRequest.setPwd(request.getPwd());
        adminRequest.setRealName(request.getRealName());
        adminRequest.setPhone(request.getPhone());
        adminRequest.setRoles(salesmanRoleId.toString());
        adminRequest.setStatus(request.getStatus());

        systemAdminService.saveAdmin(adminRequest);

        // 4. 获取新创建的管理员ID
        SystemAdmin newAdmin = systemAdminService.getByAccount(request.getAccount());
        if (newAdmin == null) {
            throw new CrmebException("创建业务员失败");
        }

        // 5. 创建业务员扩展信息
        SalesmanInfo info = new SalesmanInfo();
        info.setAdminId(newAdmin.getId());
        info.setSalesmanCode(salesmanInfoService.generateUniqueCode());
        info.setBindable(true);
        info.setCreateTime(new Date());

        return salesmanInfoService.save(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SalesmanUpdateRequest request) {
        SystemAdmin admin = systemAdminService.getById(request.getId());
        if (admin == null || admin.getIsDel()) {
            throw new CrmebException("业务员不存在");
        }

        // 更新管理员信息
        SystemAdminUpdateRequest adminRequest = new SystemAdminUpdateRequest();
        adminRequest.setId(request.getId());
        if (StrUtil.isNotBlank(request.getRealName())) {
            adminRequest.setRealName(request.getRealName());
        }
        if (StrUtil.isNotBlank(request.getPhone())) {
            adminRequest.setPhone(request.getPhone());
        }
        if (StrUtil.isNotBlank(request.getPwd())) {
            adminRequest.setPwd(request.getPwd());
        }
        systemAdminService.updateAdmin(adminRequest);

        // 更新业务员扩展信息
        if (request.getBindable() != null) {
            SalesmanInfo info = salesmanInfoService.getByAdminId(request.getId());
            if (info != null) {
                info.setBindable(request.getBindable());
                salesmanInfoService.updateById(info);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        // 检查是否有绑定的客户
        int customerCount = getCustomerCount(id);
        if (customerCount > 0) {
            throw new CrmebException("该业务员还有" + customerCount + "个客户，请先转移客户");
        }

        // 删除业务员扩展信息
        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info != null) {
            salesmanInfoService.removeById(info.getId());
        }

        // 删除管理员账号
        return systemAdminService.removeById(id);
    }

    @Override
    public Boolean updateStatus(Integer id, Boolean status) {
        return systemAdminService.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String regenerateCode(Integer id) {
        SalesmanInfo info = salesmanInfoService.getByAdminId(id);
        if (info == null) {
            throw new CrmebException("业务员信息不存在");
        }

        String newCode = salesmanInfoService.generateUniqueCode();
        info.setSalesmanCode(newCode);
        // 清除旧的二维码
        info.setSalesmanQrcode(null);
        info.setQrcodeScene(null);
        salesmanInfoService.updateById(info);

        return newCode;
    }

    @Override
    public CommonPage<CustomerBindRecordVo> getBindList(Integer salesmanId, String keywords, PageParamRequest pageRequest) {
        Page<User> userPage = PageHelper.startPage(pageRequest.getPage(), pageRequest.getLimit());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(User::getSalesmanId, 0);

        if (salesmanId != null && salesmanId > 0) {
            wrapper.eq(User::getSalesmanId, salesmanId);
        }

        if (StrUtil.isNotBlank(keywords)) {
            wrapper.and(w -> w.like(User::getNickname, keywords)
                    .or().like(User::getPhone, keywords));
        }

        wrapper.orderByDesc(User::getSalesmanBindTime);

        List<User> userList = userService.list(wrapper);
        if (userList.isEmpty()) {
            PageInfo<CustomerBindRecordVo> emptyPageInfo = CommonPage.copyPageInfo(userPage, new ArrayList<>());
            return CommonPage.restPage(emptyPageInfo);
        }

        // 获取业务员信息
        Set<Integer> salesmanIds = userList.stream()
                .map(User::getSalesmanId)
                .collect(Collectors.toSet());
        Map<Integer, SystemAdmin> adminMap = getAdminMap(new ArrayList<>(salesmanIds));

        // 获取用户消费统计（批量查询）
        List<Integer> userIds = userList.stream().map(User::getUid).collect(Collectors.toList());
        Map<Integer, CustomerConsumeStatsVo> consumeStatsMap = storeOrderService.getConsumeStatsByUids(userIds);

        // 组装响应
        List<CustomerBindRecordVo> voList = userList.stream().map(user -> {
            CustomerBindRecordVo vo = new CustomerBindRecordVo();
            vo.setUid(user.getUid());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
            vo.setAvatar(user.getAvatar());
            vo.setSalesmanId(user.getSalesmanId());
            vo.setBindTime(user.getSalesmanBindTime());

            SystemAdmin admin = adminMap.get(user.getSalesmanId());
            if (admin != null) {
                vo.setSalesmanName(admin.getRealName());
            }

            // 填充消费统计
            CustomerConsumeStatsVo stats = consumeStatsMap.get(user.getUid());
            if (stats != null) {
                vo.setTotalAmount(stats.getTotalAmount());
                vo.setOrderCount(stats.getOrderCount());
                vo.setLastOrderTime(stats.getLastOrderTime());
            } else {
                vo.setTotalAmount(BigDecimal.ZERO);
                vo.setOrderCount(0);
            }

            return vo;
        }).collect(Collectors.toList());

        PageInfo<CustomerBindRecordVo> pageInfo = CommonPage.copyPageInfo(userPage, voList);
        return CommonPage.restPage(pageInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transferCustomer(CustomerTransferRequest request) {
        User user = userService.getById(request.getUid());
        if (user == null) {
            throw new CrmebException("客户不存在");
        }

        // 验证目标业务员
        SalesmanInfo targetInfo = salesmanInfoService.getByAdminId(request.getTargetSalesmanId());
        if (targetInfo == null) {
            throw new CrmebException("目标业务员不存在");
        }

        user.setSalesmanId(request.getTargetSalesmanId());
        user.setSalesmanBindTime(new Date());

        return userService.updateById(user);
    }

    @Override
    public Object getStatistics() {
        // TODO: 实现业绩统计汇总
        return new HashMap<>();
    }

    @Override
    public List<SalesmanResponse> getRanking(Integer limit) {
        // TODO: 实现业务员排行榜
        return new ArrayList<>();
    }

    @Override
    public Object login(SystemAdminLoginRequest request) {
        // TODO: 实现业务员登录（阶段一先复用管理员登录逻辑，后续在业务员端接口补齐）
        return null;
    }

    @Override
    public SalesmanDashboardVo getDashboard(Integer adminId, String dateType) {
        // TODO: 实现数据看板
        return new SalesmanDashboardVo();
    }

    @Override
    public SalesmanResponse getMyCode(Integer adminId) {
        return getDetail(adminId);
    }

    @Override
    public Boolean checkCode(String code) {
        SalesmanInfo info = salesmanInfoService.getByCode(code);
        return info != null && Boolean.TRUE.equals(info.getBindable());
    }

    @Override
    public List<SalesmanResponse> getAllList() {
        SalesmanSearchRequest request = new SalesmanSearchRequest();
        request.setStatus(true);
        PageParamRequest pageRequest = new PageParamRequest();
        pageRequest.setPage(1);
        pageRequest.setLimit(1000);
        return getList(request, pageRequest).getList();
    }

    // ==================== 私有方法 ====================

    /**
     * 获取业务员角色ID
     */
    private Integer getSalesmanRoleId() {
        LambdaQueryWrapper<SystemRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRole::getIsSalesmanRole, true);
        wrapper.eq(SystemRole::getStatus, true);
        wrapper.last("LIMIT 1");
        SystemRole role = systemRoleService.getOne(wrapper);
        return role != null ? role.getId() : null;
    }

    /**
     * 获取业务员扩展信息Map
     */
    private Map<Integer, SalesmanInfo> getSalesmanInfoMap(List<Integer> adminIds) {
        if (adminIds.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<SalesmanInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SalesmanInfo::getAdminId, adminIds);
        List<SalesmanInfo> list = salesmanInfoService.list(wrapper);
        return list.stream().collect(Collectors.toMap(SalesmanInfo::getAdminId, info -> info));
    }

    /**
     * 获取客户数量Map（批量查询优化，解决 N+1 问题）
     */
    private Map<Integer, Integer> getCustomerCountMap(List<Integer> salesmanIds) {
        if (salesmanIds.isEmpty()) {
            return new HashMap<>();
        }
        // 使用批量 GROUP BY 查询替代 N 次单独查询
        return userService.countBySalesmanIds(salesmanIds);
    }

    /**
     * 获取本月新增客户数量Map（批量查询优化，解决 N+1 问题）
     */
    private Map<Integer, Integer> getMonthNewCustomerCountMap(List<Integer> salesmanIds) {
        if (salesmanIds.isEmpty()) {
            return new HashMap<>();
        }
        // 使用批量 GROUP BY 查询替代 N 次单独查询
        return userService.countMonthNewBySalesmanIds(salesmanIds);
    }

    /**
     * 获取单个业务员的客户数量
     */
    private int getCustomerCount(Integer salesmanId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getSalesmanId, salesmanId);
        return (int) userService.count(wrapper);
    }

    /**
     * 获取单个业务员本月新增客户数量
     */
    private int getMonthNewCustomerCount(Integer salesmanId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getSalesmanId, salesmanId);
        // 本月开始时间
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        wrapper.ge(User::getSalesmanBindTime, cal.getTime());
        return (int) userService.count(wrapper);
    }

    /**
     * 获取管理员Map
     */
    private Map<Integer, SystemAdmin> getAdminMap(List<Integer> adminIds) {
        if (adminIds.isEmpty()) {
            return new HashMap<>();
        }
        List<SystemAdmin> list = systemAdminService.listByIds(adminIds);
        return list.stream().collect(Collectors.toMap(SystemAdmin::getId, admin -> admin));
    }
}

