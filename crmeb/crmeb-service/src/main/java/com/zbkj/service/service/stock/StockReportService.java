package com.zbkj.service.service.stock;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 库存报表服务
 */
public interface StockReportService {

    /**
     * 变动汇总报表（按类型）
     */
    List<Map<String, Object>> summary(Date startTime, Date endTime);

    /**
     * 采购统计报表（按供应商）
     */
    List<Map<String, Object>> purchase(Date startTime, Date endTime);

    /**
     * 盘点差异报表
     */
    List<Map<String, Object>> checkDiff(Date startTime, Date endTime);
}

