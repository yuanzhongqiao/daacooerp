// 导入所有API模块
import * as userApi from './user';
import * as goodsApi from './goods';
import * as orderApi from './order';
import * as orderGoodsApi from './orderGoods';

// 导出API模块，方便在组件中使用
export {
  userApi,
  goodsApi,
  orderApi,
  orderGoodsApi
};

// 导出默认对象，包含所有API
export default {
  user: userApi,
  goods: goodsApi,
  order: orderApi,
  orderGoods: orderGoodsApi
};

// 以下是API使用示例：
/*
// 在组件中使用方式一：导入整个API模块
import { userApi } from '@/api';

// 调用登录API
userApi.login(username, password).then(response => {
  // 处理响应
});

// 在组件中使用方式二：导入特定API函数
import { login } from '@/api/user';

// 调用登录API
login(username, password).then(response => {
  // 处理响应
});
*/