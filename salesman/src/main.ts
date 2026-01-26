import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'

// Vant 组件
import {
  Button,
  Cell,
  CellGroup,
  Field,
  Form,
  NavBar,
  Tabbar,
  TabbarItem,
  Toast,
  Dialog,
  Loading,
  PullRefresh,
  List,
  Empty,
  Image as VanImage,
  Icon,
  Tag,
  Divider,
  Grid,
  GridItem,
  Skeleton,
  Search,
  ActionSheet,
  Popup
} from 'vant'
import 'vant/lib/index.css'

// 样式
import './style.css'

const app = createApp(App)

// 注册 Vant 组件
const vantComponents = [
  Button,
  Cell,
  CellGroup,
  Field,
  Form,
  NavBar,
  Tabbar,
  TabbarItem,
  Toast,
  Dialog,
  Loading,
  PullRefresh,
  List,
  Empty,
  VanImage,
  Icon,
  Tag,
  Divider,
  Grid,
  GridItem,
  Skeleton,
  Search,
  ActionSheet,
  Popup
]

vantComponents.forEach((component) => {
  app.use(component)
})

app.use(pinia)
app.use(router)
app.mount('#app')
