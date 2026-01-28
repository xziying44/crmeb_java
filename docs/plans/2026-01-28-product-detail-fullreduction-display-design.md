# 商品详情页满减活动展示设计方案

## 背景

当前商品详情页没有显示满减活动信息，用户只有在提交订单时才能看到满减优惠，这是一个糟糕的用户体验。本方案旨在在商品详情页展示适用的满减活动信息，让用户在浏览商品时就能了解可享受的优惠。

## 需求分析

### 设计原则
1. **保持价格准确性**：满减基于订单金额，单件商品页面不显示"满减后价格"，避免误导
2. **展示满减门槛**：以标签形式显示所有满减阶梯（如"满99减10"、"满199减30"）
3. **只显示最优活动**：一个商品可能命中多个满减活动，只展示一个（后端已通过 `LIMIT 1` 实现）
4. **横向滚动展示**：类似淘宝，一行展示所有阶梯标签，可左右滑动

### 展示效果
```
┌─────────────────────────────────────────┐
│  ￥99.00                                │  ← 商品价格
│  ┌─────────────────────────────────┐    │
│  │ 满99减10 │ 满199减30 │ 满299减60 │    │  ← 满减标签（横向滚动）
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

## 技术方案

### 1. 数据结构

#### 新增 VO 对象
**文件**：`crmeb/crmeb-common/src/main/java/com/zbkj/common/vo/FullReductionDisplayVO.java`

```java
package com.zbkj.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 满减活动展示信息
 */
@Data
@ApiModel(value = "FullReductionDisplayVO", description = "满减活动展示信息")
public class FullReductionDisplayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID")
    private Integer id;

    @ApiModelProperty(value = "活动名称")
    private String name;

    @ApiModelProperty(value = "阶梯列表")
    private List<LevelItem> levels;

    @Data
    public static class LevelItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "满足金额")
        private BigDecimal fullAmount;

        @ApiModelProperty(value = "减免金额")
        private BigDecimal reduceAmount;
    }
}
```

#### 修改 ProductDetailResponse
**文件**：`crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ProductDetailResponse.java`

新增字段：
```java
@ApiModelProperty(value = "满减活动信息")
private FullReductionDisplayVO fullReduction;
```

### 2. 后端服务实现

#### 新增服务方法
**文件**：`crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/FullReductionService.java`

```java
/**
 * 根据商品ID获取可用的满减活动展示信息
 * @param productId 商品ID
 * @param categoryIds 商品所属品类ID列表
 * @return 满减活动展示信息（如无活动返回null）
 */
FullReductionDisplayVO getDisplayInfoByProduct(Integer productId, List<Integer> categoryIds);
```

#### 服务实现
**文件**：`crmeb/crmeb-service/src/main/java/com/zbkj/service/service/promotion/impl/FullReductionServiceImpl.java`

```java
@Override
public FullReductionDisplayVO getDisplayInfoByProduct(Integer productId, List<Integer> categoryIds) {
    // 1. 复用现有方法查找适用的满减活动
    FullReduction reduction = getAvailableByProductIds(
        Collections.singletonList(productId),
        categoryIds
    );

    if (ObjectUtil.isNull(reduction)) {
        return null;
    }

    // 2. 查询该活动的所有阶梯
    List<FullReductionLevel> levels = levelService.getByReductionId(reduction.getId());
    if (CollUtil.isEmpty(levels)) {
        return null;
    }

    // 3. 按满足金额升序排列（方便前端展示）
    levels.sort(Comparator.comparing(FullReductionLevel::getFullAmount));

    // 4. 组装返回对象
    FullReductionDisplayVO vo = new FullReductionDisplayVO();
    vo.setId(reduction.getId());
    vo.setName(reduction.getName());
    vo.setLevels(levels.stream().map(l -> {
        FullReductionDisplayVO.LevelItem item = new FullReductionDisplayVO.LevelItem();
        item.setFullAmount(l.getFullAmount());
        item.setReduceAmount(l.getReduceAmount());
        return item;
    }).collect(Collectors.toList()));

    return vo;
}
```

#### 修改商品详情服务
**文件**：`crmeb/crmeb-front/src/main/java/com/zbkj/front/service/impl/ProductServiceImpl.java`

在 `getDetail()` 方法中增加：
```java
// 查询满减活动信息
List<Integer> categoryIds = storeProductService.getProductAllCategoryIdByProductIds(
    Collections.singletonList(storeProduct.getId())
);
FullReductionDisplayVO fullReduction = fullReductionService.getDisplayInfoByProduct(
    storeProduct.getId(),
    categoryIds
);
productDetailResponse.setFullReduction(fullReduction);
```

### 3. 前端实现

#### 新增满减标签组件
**文件**：`app/components/fullReductionTags/index.vue`

```vue
<template>
  <view class="full-reduction-wrap" v-if="reduction && reduction.levels && reduction.levels.length">
    <view class="reduction-label">满减</view>
    <scroll-view scroll-x class="tags-scroll">
      <view class="tags-inner">
        <view
          class="tag-item"
          v-for="(level, index) in reduction.levels"
          :key="index"
        >
          满{{ level.fullAmount }}减{{ level.reduceAmount }}
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  name: 'fullReductionTags',
  props: {
    reduction: {
      type: Object,
      default: () => null
    }
  }
}
</script>

<style lang="scss" scoped>
.full-reduction-wrap {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #FFF5F5;

  .reduction-label {
    flex-shrink: 0;
    padding: 4rpx 12rpx;
    background: #FF6B6B;
    color: #fff;
    font-size: 22rpx;
    border-radius: 4rpx;
    margin-right: 16rpx;
  }

  .tags-scroll {
    flex: 1;
    white-space: nowrap;
  }

  .tags-inner {
    display: inline-flex;
    gap: 16rpx;
  }

  .tag-item {
    display: inline-block;
    padding: 8rpx 16rpx;
    background: #FFF;
    color: #FF6B6B;
    font-size: 24rpx;
    border-radius: 8rpx;
    border: 1rpx solid #FF6B6B;
  }
}
</style>
```

#### 修改商品详情页
**文件**：`app/pages/goods/goods_details/index.vue`

1. 引入组件：
```javascript
import fullReductionTags from '@/components/fullReductionTags/index.vue';

export default {
  components: {
    // ... 现有组件
    fullReductionTags,
  },
  data() {
    return {
      // ... 现有数据
      fullReduction: null,
    }
  }
}
```

2. 在模板中使用（价格区域下方）：
```vue
<!-- 满减活动标签 -->
<fullReductionTags :reduction="fullReduction" />
```

3. 在 `getGoodsDetails` 方法中赋值：
```javascript
this.fullReduction = res.data.fullReduction;
```

## 涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `crmeb-common/.../vo/FullReductionDisplayVO.java` | 新增 | 满减展示 VO |
| `crmeb-common/.../response/ProductDetailResponse.java` | 修改 | 增加 fullReduction 字段 |
| `crmeb-service/.../promotion/FullReductionService.java` | 修改 | 增加接口方法 |
| `crmeb-service/.../promotion/impl/FullReductionServiceImpl.java` | 修改 | 实现查询逻辑 |
| `crmeb-front/.../impl/ProductServiceImpl.java` | 修改 | 商品详情增加满减查询 |
| `app/components/fullReductionTags/index.vue` | 新增 | 前端满减标签组件 |
| `app/pages/goods/goods_details/index.vue` | 修改 | 引入并使用组件 |

## 后续优化（可选）

1. **最优活动选择**：当前按创建时间选择活动，可优化为按"最大优惠力度"选择
2. **凑单引导**：显示"再买 ¥XX 可减 ¥XX"的提示
3. **活动倒计时**：展示活动剩余时间
