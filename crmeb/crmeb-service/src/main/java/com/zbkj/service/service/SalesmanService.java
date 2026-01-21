package com.zbkj.service.service;

import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.CustomerTransferRequest;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SalesmanAddRequest;
import com.zbkj.common.request.SalesmanSearchRequest;
import com.zbkj.common.request.SalesmanUpdateRequest;
import com.zbkj.common.request.SystemAdminLoginRequest;
import com.zbkj.common.response.SalesmanResponse;
import com.zbkj.common.vo.CustomerBindRecordVo;
import com.zbkj.common.vo.SalesmanDashboardVo;

import java.util.List;

/**
 * 业务员管理服务接口
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
public interface SalesmanService {

    /**
     * 获取业务员分页列表
     */
    CommonPage<SalesmanResponse> getList(SalesmanSearchRequest request, PageParamRequest pageRequest);

    /**
     * 获取业务员详情
     */
    SalesmanResponse getDetail(Integer id);

    /**
     * 创建业务员
     */
    Boolean create(SalesmanAddRequest request);

    /**
     * 更新业务员
     */
    Boolean update(SalesmanUpdateRequest request);

    /**
     * 删除业务员
     */
    Boolean delete(Integer id);

    /**
     * 更新业务员状态
     */
    Boolean updateStatus(Integer id, Boolean status);

    /**
     * 重新生成邀请码
     */
    String regenerateCode(Integer id);

    /**
     * 获取客户绑定记录列表
     */
    CommonPage<CustomerBindRecordVo> getBindList(Integer salesmanId, String keywords, PageParamRequest pageRequest);

    /**
     * 转移客户
     */
    Boolean transferCustomer(CustomerTransferRequest request);

    /**
     * 获取业绩统计
     */
    Object getStatistics();

    /**
     * 获取业务员排行榜
     */
    List<SalesmanResponse> getRanking(Integer limit);

    /**
     * 业务员登录
     */
    Object login(SystemAdminLoginRequest request);

    /**
     * 获取数据看板（业务员端）
     */
    SalesmanDashboardVo getDashboard(Integer adminId, String dateType);

    /**
     * 获取我的邀请码和二维码
     */
    SalesmanResponse getMyCode(Integer adminId);

    /**
     * 检查邀请码是否有效
     */
    Boolean checkCode(String code);

    /**
     * 获取所有业务员列表（下拉选择用）
     */
    List<SalesmanResponse> getAllList();
}

