# 分类宫格 (category_grid) 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增首页装修组件「分类宫格」，管理员选择二级分类后自动展示分类图标和名称的 5 列宫格。

**Architecture:** 新增独立装修组件，遵循现有 DIY 系统的三文件模式（mobilePage 预览 + mobileConfig 配置面板 + homeIndex 移动端渲染）。管理端通过分类选择器存储 categoryIds，移动端根据 IDs 调用后端 API 获取分类详情后渲染宫格。

**Tech Stack:** Vue 2 + Element UI（管理端）, uni-app（移动端）, Spring Boot + MyBatis-Plus（后端）

---

## Task 1: 后端 - 新增移动端分类批量查询 API

**Files:**
- Modify: `crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/ProductController.java:77-81`（在 `getCategoryByPid` 方法后添加）

**Step 1: 在 ProductController 中添加 listByIds 端点**

在 `getCategoryByPid` 方法后（约第 81 行之后）添加：

```java
@ApiOperation(value = "根据ID集合获取分类列表")
@RequestMapping(value = "/category/listByIds", method = RequestMethod.GET)
@ApiImplicitParam(name = "ids", value = "分类ID集合，逗号分隔", required = true)
public CommonResult<List<Category>> getCategoryByIds(@RequestParam String ids) {
    List<Integer> idList = CrmebUtil.stringToArray(ids);
    return CommonResult.success(categoryService.getByIds(idList));
}
```

**关键参考：**
- `CrmebUtil.stringToArray(ids)` 是项目已有的工具方法，将逗号分隔字符串转为 `List<Integer>`
- `categoryService.getByIds(idList)` 是 `CategoryService` 已有方法（签名：`List<Category> getByIds(List<Integer> ids)`）
- `ProductController` 已注入 `categoryService`（第 48 行：`@Autowired private CategoryService categoryService`）
- 此端点无需认证（前端模块的 API 默认配置）

**Step 2: 验证后端编译**

运行：`cd crmeb && mvn clean compile -pl crmeb-front -am -DskipTests`
预期：BUILD SUCCESS

**Step 3: 提交**

```bash
git add crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/ProductController.java
git commit -m "添加移动端分类批量查询API /category/listByIds"
```

---

## Task 2: 移动端 API - 添加分类批量查询接口封装

**Files:**
- Modify: `app/api/api.js`（在 `getCategoryTwo` 函数附近添加）

**Step 1: 在 api.js 中添加 API 函数**

在 `getCategoryTwo` 函数（约第 334 行）之后添加：

```javascript
/**
 * 根据ID集合获取分类列表
 * @param {string} ids 分类ID集合，逗号分隔
*/
export function getCategoryByIds(ids)
{
  return request.get(`category/listByIds`, { ids }, { noAuth : true });
}
```

**关键参考：**
- 遵循 `getCategoryTwo` 的代码风格
- `noAuth: true` 表示无需用户登录
- `request.get` 是项目封装的请求方法，第二个参数为查询参数

**Step 2: 提交**

```bash
git add app/api/api.js
git commit -m "添加移动端分类批量查询API封装 getCategoryByIds"
```

---

## Task 3: 管理端 - 创建 mobilePage 预览组件

**Files:**
- Create: `admin/src/views/design/components/mobilePage/home_category_grid.vue`

**Step 1: 创建预览组件文件**

此文件定义组件在左侧面板中的元信息（名称、图标、类型）和中间预览区的显示效果。

```vue
<template>
  <div v-if="configObj" :style="boxStyle">
    <div class="category-grid" :style="gridStyle">
      <template v-for="(item, index) in categoryList">
        <div class="grid-item" :key="index">
          <div class="img-box">
            <img :src="item.extra" alt="" v-if="item.extra" :style="contentStyle" />
            <div class="empty-box" v-else :style="contentStyle">
              <span class="iconfont-diy iconfont icontupian"></span>
            </div>
          </div>
          <p :style="titleColor">{{ item.name }}</p>
        </div>
      </template>
      <!-- 未选择分类时的占位提示 -->
      <template v-if="!categoryList.length">
        <div class="grid-item" v-for="n in 5" :key="'empty-'+n">
          <div class="img-box">
            <div class="empty-box" :style="contentStyle">
              <span class="iconfont-diy iconfont icontupian"></span>
            </div>
          </div>
          <p :style="titleColor">分类{{ n }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import { categroyByIds } from '@/api/categoryApi';
export default {
  name: 'home_category_grid',
  cname: '分类宫格',
  icon: 't-icon-zujian-shangpinfenlei',
  configName: 'c_home_category_grid',
  type: 0, // 0 基础组件
  defaultName: 'categoryGrid', // 移动端匹配名称
  props: {
    index: {
      type: null,
    },
    num: {
      type: null,
    },
  },
  computed: {
    ...mapState('mobildConfig', ['defaultArray']),
    boxStyle() {
      return [
        { 'border-radius': this.configObj.bgStyle.val ? this.configObj.bgStyle.val + 'px' : '0' },
        {
          background: `linear-gradient(${this.configObj.bgColor.color[0].item}, ${this.configObj.bgColor.color[1].item})`,
        },
        { margin: this.configObj.mbConfig.val + 'px' + ' ' + this.configObj.lrConfig.val + 'px' + ' ' + 0 },
        { padding: this.configObj.upConfig.val + 'px' + ' ' + 0 + ' ' + this.configObj.downConfig.val + 'px' },
      ];
    },
    gridStyle() {
      return [
        { 'grid-row-gap': this.configObj.contentConfig.val + 'px' },
        { 'grid-template-columns': 'repeat(5, 1fr)' },
      ];
    },
    titleColor() {
      return {
        color: this.configObj.titleColor.color[0].item,
      };
    },
    contentStyle() {
      return {
        'border-radius': this.configObj.contentStyle.val ? this.configObj.contentStyle.val + 'px' : '0',
      };
    },
  },
  watch: {
    num: {
      handler(nVal) {
        let data = this.$store.state.mobildConfig.defaultArray[nVal];
        this.setConfig(data);
      },
      deep: true,
    },
    defaultArray: {
      handler() {
        let data = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(data);
      },
      deep: true,
    },
  },
  data() {
    return {
      // 默认初始化数据（定义组件的配置结构）
      defaultConfig: {
        isHide: false,
        name: 'categoryGrid',
        timestamp: this.num,
        setUp: {
          tabVal: 0,
          cname: '分类宫格',
        },
        categoryConfig: {
          tabTitle: '分类选择',
          title: '选择要展示的二级商品分类',
          categoryIds: [],
        },
        bgStyle: {
          tabTitle: '圆角设置',
          title: '背景圆角',
          name: 'bgStyle',
          val: 0,
          min: 0,
          max: 30,
        },
        contentStyle: {
          title: '图标圆角',
          name: 'contentStyle',
          val: 30,
          min: 0,
          max: 30,
        },
        bgColor: {
          tabTitle: '颜色设置',
          title: '背景颜色',
          name: 'bgColor',
          color: [
            { item: '#fff' },
            { item: '#fff' },
          ],
          default: [
            { item: '#fff' },
            { item: '#fff' },
          ],
        },
        titleColor: {
          title: '文字颜色',
          name: 'titleColor',
          color: [
            { item: '#282828' },
          ],
          default: [
            { item: '#282828' },
          ],
        },
        contentConfig: {
          title: '内容间距',
          val: 10,
          min: 0,
          max: 30,
        },
        upConfig: {
          tabTitle: '边距设置',
          title: '上边距',
          val: 10,
          min: 0,
          max: 100,
        },
        downConfig: {
          title: '下边距',
          val: 10,
          min: 0,
        },
        lrConfig: {
          title: '左右边距',
          val: 12,
          min: 0,
          max: 25,
        },
        mbConfig: {
          title: '页面间距',
          val: 10,
          min: 0,
        },
      },
      configObj: null,
      categoryList: [],
      pageData: {},
    };
  },
  mounted() {
    this.$nextTick(() => {
      if (this.num) {
        this.pageData = this.$store.state.mobildConfig.defaultArray[this.num];
        this.setConfig(this.pageData);
      }
    });
  },
  methods: {
    setConfig(data) {
      if (!data) return;
      this.configObj = data;
      // 根据 categoryIds 获取分类详情用于预览
      let ids = data.categoryConfig ? data.categoryConfig.categoryIds : [];
      if (ids && ids.length) {
        this.fetchCategories(ids);
      } else {
        this.categoryList = [];
      }
    },
    fetchCategories(ids) {
      categroyByIds({ ids: ids.join(',') }).then((res) => {
        // 按 categoryIds 的顺序排列结果
        let idOrder = ids;
        let map = {};
        res.forEach((item) => { map[item.id] = item; });
        this.categoryList = idOrder.map((id) => map[id]).filter(Boolean);
      }).catch(() => {
        this.categoryList = [];
      });
    },
  },
};
</script>

<style scoped lang="scss">
.category-grid {
  padding: 0 12px;
  display: grid;
  grid-template-rows: auto;

  .grid-item {
    font-size: 11px;
    color: #282828;
    text-align: center;

    .img-box {
      width: 50px;
      height: 50px;
      margin: 0 auto 7px auto;

      img {
        width: 100%;
        height: 100%;
      }
    }

    .empty-box {
      width: 100%;
      height: 100%;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .icontupian {
    font-size: 16px;
  }
}
</style>
```

**关键参考：**
- `name`/`cname`/`icon`/`configName`/`type`/`defaultName` 是组件元数据，参考 `home_menu.vue` 第 50-56 行
- `icon` 使用 `t-icon-zujian-shangpinfenlei`（商品分类图标），与系统图标库一致
- `defaultConfig` 定义了组件的初始配置结构，管理端编辑器在添加组件时会使用此数据
- `categroyByIds` 是管理端已有的 API（`admin/src/api/categoryApi.js:127`），用于预览时获取分类详情
- 样式完全参考 `home_menu.vue` 的 `.list_menu` 样式

**Step 2: 验证组件自动注册**

无需手动注册。`mobilePage/index.js` 使用 `require.context('./', false, /\.vue$/)` 自动扫描目录下所有 `.vue` 文件。

**Step 3: 提交**

```bash
git add admin/src/views/design/components/mobilePage/home_category_grid.vue
git commit -m "添加分类宫格管理端预览组件 home_category_grid"
```

---

## Task 4: 管理端 - 创建 mobileConfigRight 分类选择配置组件

**Files:**
- Create: `admin/src/views/design/components/mobileConfigRight/c_category_select.vue`

**Step 1: 创建分类选择器组件**

此组件提供一个多选下拉框，让管理员从二级商品分类中选择要展示的分类，支持拖拽排序。

```vue
<template>
  <!-- 分类宫格选择器 -->
  <div class="c_category_select borderPadding" v-if="configData">
    <div class="title">{{ configData.title }}</div>
    <div class="select-box">
      <el-select
        v-model="configData.categoryIds"
        multiple
        filterable
        placeholder="请选择要展示的二级分类"
        style="width: 100%"
        @change="onCategoryChange"
      >
        <el-option-group
          v-for="group in categoryTree"
          :key="group.id"
          :label="group.name"
        >
          <el-option
            v-for="item in group.child"
            :key="item.id"
            :label="item.name"
            :value="item.id"
            :disabled="!item.status"
          >
          </el-option>
        </el-option-group>
      </el-select>
    </div>
    <div class="tips" v-if="configData.categoryIds && configData.categoryIds.length">
      已选 {{ configData.categoryIds.length }} 个分类
    </div>
    <!-- 已选分类排序列表 -->
    <div class="selected-list" v-if="configData.categoryIds && configData.categoryIds.length">
      <div class="sub-title">拖拽调整显示顺序</div>
      <draggable class="dragArea" :list="configData.categoryIds" handle=".drag-handle">
        <div class="selected-item" v-for="(id, index) in configData.categoryIds" :key="id">
          <span class="drag-handle">
            <span class="iconfont icontuozhuaitubiao" style="font-size: 20px; color: #dddddd; cursor: move;"></span>
          </span>
          <span class="cat-name">{{ getCategoryName(id) }}</span>
          <i class="el-icon-close delete-btn" @click="removeCategory(index)"></i>
        </div>
      </draggable>
    </div>
  </div>
</template>

<script>
import vuedraggable from 'vuedraggable';
import { mapGetters } from 'vuex';
export default {
  name: 'c_category_select',
  props: {
    configObj: {
      type: Object,
    },
    configNme: {
      type: String,
    },
    index: {
      type: null,
    },
  },
  components: {
    draggable: vuedraggable,
  },
  data() {
    return {
      configData: {},
      categoryMap: {},
    };
  },
  computed: {
    ...mapGetters(['adminProductClassify']),
    // 过滤出有子分类的一级分类，构建分组数据
    categoryTree() {
      if (!this.adminProductClassify) return [];
      return this.adminProductClassify.filter((item) => item.child && item.child.length > 0);
    },
  },
  mounted() {
    // 确保分类数据已加载
    if (!localStorage.getItem('adminProductClassify')) {
      this.$store.dispatch('product/getAdminProductClassify');
    }
    this.$nextTick(() => {
      this.configData = this.configObj[this.configNme];
      this.buildCategoryMap();
    });
  },
  watch: {
    configObj: {
      handler(nVal) {
        this.configData = nVal[this.configNme];
      },
      deep: true,
    },
    adminProductClassify: {
      handler() {
        this.buildCategoryMap();
      },
      deep: true,
    },
  },
  methods: {
    // 构建 id -> name 映射，用于显示已选分类名称
    buildCategoryMap() {
      let map = {};
      if (this.adminProductClassify) {
        this.adminProductClassify.forEach((parent) => {
          if (parent.child) {
            parent.child.forEach((child) => {
              map[child.id] = child.name;
            });
          }
        });
      }
      this.categoryMap = map;
    },
    getCategoryName(id) {
      return this.categoryMap[id] || '未知分类';
    },
    onCategoryChange() {
      this.$emit('getConfig', { name: 'categorySelect', values: this.configData.categoryIds });
    },
    removeCategory(index) {
      this.configData.categoryIds.splice(index, 1);
      this.onCategoryChange();
    },
  },
};
</script>

<style scoped lang="scss">
.c_category_select {
  padding-bottom: 20px;

  .title {
    font-size: 12px;
    color: #bbbbbb;
  }

  .select-box {
    margin-top: 12px;
  }

  .tips {
    margin-top: 8px;
    font-size: 12px;
    color: #999;
  }

  .selected-list {
    margin-top: 12px;

    .sub-title {
      font-size: 12px;
      color: #bbbbbb;
      margin-bottom: 8px;
    }

    .selected-item {
      display: flex;
      align-items: center;
      padding: 8px 10px;
      background: #f9f9f9;
      border-radius: 3px;
      margin-bottom: 6px;

      .drag-handle {
        margin-right: 8px;
      }

      .cat-name {
        flex: 1;
        font-size: 13px;
        color: #333;
      }

      .delete-btn {
        color: #999;
        cursor: pointer;
        font-size: 16px;

        &:hover {
          color: #E93323;
        }
      }
    }
  }
}
</style>
```

**关键参考：**
- 分类数据来自 Vuex store 的 `adminProductClassify` getter（参考 `c_classify.vue:111`）
- 使用 `el-option-group` 将二级分类按一级分类分组，方便管理员找到目标分类
- 拖拽排序使用 `vuedraggable`（项目已有依赖，参考 `c_classify.vue:75`）
- 组件自动注册（`mobileConfigRight/index.js` 同样使用 `require.context` 自动扫描）

**Step 2: 提交**

```bash
git add admin/src/views/design/components/mobileConfigRight/c_category_select.vue
git commit -m "添加分类宫格右侧配置面板的分类选择器组件"
```

---

## Task 5: 管理端 - 创建 mobileConfig 配置面板组件

**Files:**
- Create: `admin/src/views/design/components/mobileConfig/c_home_category_grid.vue`

**Step 1: 创建配置面板组件**

此文件将右侧配置组件（分类选择器、颜色、间距等）组合在一起。

```vue
<template>
  <div class="mobile-config pro">
    <div v-for="(item, key) in rCom" :key="key">
      <component
        :is="item.components.name"
        :configObj="configObj"
        ref="childData"
        :configNme="item.configNme"
        :key="key"
        @getConfig="getConfig"
        :index="activeIndex"
        :num="item.num"
      ></component>
    </div>
    <rightBtn :activeIndex="activeIndex" :configObj="configObj"></rightBtn>
  </div>
</template>

<script>
import toolCom from '../mobileConfigRight/index.js';
import rightBtn from '../rightBtn/index.vue';
export default {
  name: 'c_home_category_grid',
  cname: '分类宫格',
  componentsName: 'home_category_grid',
  props: {
    activeIndex: {
      type: null,
    },
    num: {
      type: null,
    },
    index: {
      type: null,
    },
  },
  components: {
    ...toolCom,
    rightBtn,
  },
  data() {
    return {
      configObj: {},
      rCom: [
        {
          components: toolCom.c_checked_tab,
          configNme: 'setUp',
        },
      ],
      // 内容设置（分类选择）
      contentStyle: [
        {
          components: toolCom.c_title,
          configNme: 'categoryConfig',
        },
        {
          components: toolCom.c_category_select,
          configNme: 'categoryConfig',
        },
      ],
      // 样式设置
      configStyle: [
        {
          components: toolCom.c_title,
          configNme: 'bgColor',
        },
        {
          components: toolCom.c_bg_color,
          configNme: 'bgColor',
        },
        {
          components: toolCom.c_bg_color,
          configNme: 'titleColor',
        },
        {
          components: toolCom.c_title,
          configNme: 'upConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'upConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'downConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'lrConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'contentConfig',
        },
        {
          components: toolCom.c_slider,
          configNme: 'mbConfig',
        },
        {
          components: toolCom.c_title,
          configNme: 'bgStyle',
        },
        {
          components: toolCom.c_slider,
          configNme: 'bgStyle',
        },
        {
          components: toolCom.c_slider,
          configNme: 'contentStyle',
        },
      ],
      setUp: 0,
    };
  },
  watch: {
    num(nVal) {
      let value = JSON.parse(JSON.stringify(this.$store.state.mobildConfig.defaultArray[nVal]));
      this.configObj = value;
    },
    configObj: {
      handler(nVal) {
        this.$store.commit('mobildConfig/UPDATEARR', { num: this.num, val: nVal });
      },
      deep: true,
    },
    'configObj.setUp.tabVal': {
      handler(nVal) {
        this.setUp = nVal;
        var arr = [this.rCom[0]];
        if (nVal == 0) {
          this.rCom = arr.concat(this.contentStyle);
        } else {
          this.rCom = arr.concat(this.configStyle);
        }
      },
      deep: true,
    },
  },
  mounted() {
    this.$nextTick(() => {
      let value = JSON.parse(JSON.stringify(this.$store.state.mobildConfig.defaultArray[this.num]));
      this.configObj = value;
    });
  },
  methods: {
    getConfig(data) {},
  },
};
</script>

<style scoped></style>
```

**关键参考：**
- 完全遵循 `c_home_menu.vue` 的模式（第 31-208 行）
- `setUp.tabVal` 用于切换「内容设置」和「样式设置」两个面板（标准模式）
- `c_checked_tab` 是内容/样式切换按钮（现有组件）
- `c_title` 显示分组标题，`c_slider` 显示滑块，`c_bg_color` 显示颜色选择器（均为现有组件）
- 组件自动注册（`mobileConfig/index.js` 使用 `require.context` 自动扫描）

**Step 2: 提交**

```bash
git add admin/src/views/design/components/mobileConfig/c_home_category_grid.vue
git commit -m "添加分类宫格管理端配置面板组件"
```

---

## Task 6: 移动端 - 创建分类宫格渲染组件

**Files:**
- Create: `app/components/homeIndex/categoryGrid.vue`

**Step 1: 创建移动端渲染组件**

```vue
<template>
	<!-- 分类宫格 -->
	<view v-show="categoryList.length" :style="[boxStyle]">
		<view class="category-grid" :style="[gridStyle]">
			<view class="grid-item" v-for="(item, index) in categoryList" :key="index"
				@click="goCategory(item)">
				<view class="pictrue skeleton-radius">
					<easy-loadimage :image-src="item.extra" :radius="dataConfig.contentStyle.val">
					</easy-loadimage>
				</view>
				<view class="grid-txt" :style="[titleColor]">{{ item.name }}</view>
			</view>
		</view>
	</view>
</template>

<script>
	import easyLoadimage from '@/components/base/easy-loadimage.vue';
	import { getCategoryByIds } from '@/api/api.js';
	export default {
		name: 'categoryGrid',
		props: {
			dataConfig: {
				type: Object,
				default: () => {}
			}
		},
		components: {
			easyLoadimage
		},
		data() {
			return {
				categoryList: []
			};
		},
		computed: {
			boxStyle() {
				return {
					borderRadius: this.dataConfig.bgStyle.val * 2 + 'rpx',
					background: `linear-gradient(${this.dataConfig.bgColor.color[0].item}, ${this.dataConfig.bgColor.color[1].item})`,
					margin: this.dataConfig.mbConfig.val * 2 + 'rpx' + ' ' + this.dataConfig.lrConfig.val * 2 + 'rpx' + ' ' + 0,
					padding: this.dataConfig.upConfig.val * 2 + 'rpx' + ' ' + 0 + ' ' + this.dataConfig.downConfig.val * 2 + 'rpx'
				}
			},
			gridStyle() {
				return {
					gridRowGap: this.dataConfig.contentConfig.val * 2 + 'rpx',
					gridTemplateColumns: 'repeat(5, 1fr)'
				}
			},
			titleColor() {
				return {
					'color': this.dataConfig.titleColor.color[0].item
				}
			}
		},
		mounted() {
			this.loadCategories();
		},
		methods: {
			loadCategories() {
				let ids = this.dataConfig.categoryConfig ? this.dataConfig.categoryConfig.categoryIds : [];
				if (!ids || !ids.length) return;
				getCategoryByIds(ids.join(',')).then(res => {
					// 按配置中的 ID 顺序排列
					let map = {};
					res.data.forEach(item => { map[item.id] = item; });
					this.categoryList = ids.map(id => map[id]).filter(Boolean);
				}).catch(() => {
					this.categoryList = [];
				});
			},
			goCategory(item) {
				uni.navigateTo({
					url: '/pages/goods/goods_list/index?cid=' + item.id + '&title=' + item.name
				});
			}
		}
	};
</script>

<style lang="scss" scoped>
	.category-grid {
		display: grid;
		grid-template-rows: auto;
		padding: 0 20rpx;

		.grid-item {
			text-align: center;

			.pictrue {
				width: 90rpx;
				height: 90rpx;
				margin: 0 auto;

				image {
					width: 100%;
					height: 100%;
				}
			}

			.grid-txt {
				font-size: 12px;
				margin-top: 14rpx;
			}
		}
	}
</style>
```

**关键参考：**
- 样式和结构参考 `menus.vue`（`app/components/homeIndex/menus.vue`）
- 使用 `easy-loadimage` 组件加载图标（参考 `menus.vue:15-16`）
- `boxStyle` 计算属性参考 `menus.vue:85-91`（注意移动端使用 `rpx` 单位）
- 跳转路径 `/pages/goods/goods_list/index?cid={id}&title={name}` 是项目现有的分类商品列表跳转模式（参考 `app/pages/index/index.vue:61`）
- API 返回数据可能在 `res.data` 中（取决于请求封装），需根据实际调试确认是 `res` 还是 `res.data`

**Step 2: 提交**

```bash
git add app/components/homeIndex/categoryGrid.vue
git commit -m "添加移动端分类宫格渲染组件 categoryGrid"
```

---

## Task 7: 移动端 - 在首页注册并渲染新组件

**Files:**
- Modify: `app/pages/index/index.vue`

**Step 1: 添加 import 语句**

在 `app/pages/index/index.vue` 的 import 部分（约第 152 行，`bargain` 的 import 之后）添加：

```javascript
import categoryGrid from '@/components/homeIndex/categoryGrid.vue';
```

**Step 2: 在 components 中注册**

在 `components` 对象中（约第 211 行，`bargain` 之后）添加：

```javascript
categoryGrid,
```

**Step 3: 在模板中添加渲染条件**

在 `<view v-for="(item, index) in styleConfig" :key="index">` 循环内（约第 52 行，`homeTab` 的 v-if 之后）添加：

```vue
							<!-- 分类宫格 -->
							<categoryGrid v-if="item.name == 'categoryGrid'&&!item.isHide" :dataConfig="item"></categoryGrid>
```

**关键参考：**
- `item.name` 的值 `'categoryGrid'` 对应 `home_category_grid.vue` 中 `defaultConfig.name` 的值
- 渲染模式完全遵循其他组件的 `v-if="item.name == 'xxx'&&!item.isHide"` 模式

**Step 4: 提交**

```bash
git add app/pages/index/index.vue
git commit -m "在首页注册并渲染分类宫格组件"
```

---

## Task 8: 集成验证

**Step 1: 后端编译验证**

运行：`cd crmeb && mvn clean compile -DskipTests`
预期：BUILD SUCCESS

**Step 2: 管理端前端验证**

运行：`cd admin && npm run dev`
预期：
- 装修编辑器左侧「基础组件」中出现「分类宫格」组件
- 拖拽或点击添加后，中间预览区显示 5 列宫格占位
- 右侧配置面板可选择二级分类，预览区实时更新

**Step 3: 移动端验证**

使用 HBuilderX 运行到浏览器或模拟器：
- 在管理端配置分类宫格并保存到首页装修
- 移动端首页能看到分类宫格组件
- 点击分类项能跳转到对应商品列表页

**Step 4: 最终提交**

如有调试修改，统一提交：
```bash
git add -A
git commit -m "完成分类宫格装修组件集成验证"
```
