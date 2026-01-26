package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.stock.StockLog;
import com.zbkj.common.response.StockLogResponse;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.Map;
import java.util.List;

/**
 * 库存流水 Mapper 接口
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
public interface StockLogDao extends BaseMapper<StockLog> {

    /**
     * 库存流水分页列表（包含商品/规格信息）
     */
    List<StockLogResponse> selectLogList(@Param("productId") Integer productId,
                                         @Param("type") Integer type);

    /**
     * 变动汇总报表（按类型汇总）
     */
    List<Map<String, Object>> reportSummary(@Param("startTime") Date startTime,
                                            @Param("endTime") Date endTime);
}
