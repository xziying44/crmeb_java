package com.zbkj.service.service.impl.stock;

import com.zbkj.common.constants.StockConstants;
import com.zbkj.service.dao.PurchaseDao;
import com.zbkj.service.dao.StockCheckItemDao;
import com.zbkj.service.dao.StockLogDao;
import com.zbkj.service.service.stock.StockReportService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 库存报表服务实现
 */
@Service
public class StockReportServiceImpl implements StockReportService {

    @Resource
    private StockLogDao stockLogDao;

    @Resource
    private PurchaseDao purchaseDao;

    @Resource
    private StockCheckItemDao stockCheckItemDao;

    @Override
    public List<Map<String, Object>> summary(Date startTime, Date endTime) {
        List<Map<String, Object>> list = stockLogDao.reportSummary(startTime, endTime);
        // 补充类型名称，便于前端直接展示
        list.forEach(m -> {
            Object typeObj = m.get("type");
            Integer type = typeObj instanceof Number ? ((Number) typeObj).intValue() : null;
            m.put("typeName", getTypeName(type));
        });
        return list;
    }

    @Override
    public List<Map<String, Object>> purchase(Date startTime, Date endTime) {
        return purchaseDao.reportPurchase(startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> checkDiff(Date startTime, Date endTime) {
        return stockCheckItemDao.reportCheckDiff(startTime, endTime);
    }

    private String getTypeName(Integer type) {
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

