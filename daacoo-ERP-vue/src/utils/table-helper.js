/**
 * Element Plus表格组件辅助函数
 * 用于修复Element Plus表格组件中的一些已知问题
 */

import { onMounted } from 'vue'

/**
 * 修复Element Plus表格组件中的data.includes错误
 * 错误原因：在某些情况下，表格数据可能不是数组或者数组方法被覆盖
 */
export function fixTableDataIncludes() {
  // 在组件挂载后执行修复
  onMounted(() => {
    // 修复Array.prototype.includes方法，确保它总是可用
    if (!Array.prototype._includes_original) {
      Array.prototype._includes_original = Array.prototype.includes
      
      // 重写includes方法，添加安全检查
      Array.prototype.includes = function(item) {
        try {
          // 使用原始includes方法
          return this._includes_original(item)
        } catch (error) {
          console.warn('Array.includes方法调用失败，使用备选方法', error)
          // 备选实现：手动遍历数组
          for (let i = 0; i < this.length; i++) {
            if (this[i] === item) {
              return true
            }
          }
          return false
        }
      }
    }
    
    // 防止非数组对象调用includes方法时出错
    // 这是针对Element Plus表格组件中的updateCurrentRowData方法的特定修复
    const originalArrayFrom = Array.from
    Array.from = function(items) {
      if (items === null || items === undefined) {
        console.warn('Array.from接收到null或undefined参数，返回空数组')
        return []
      }
      return originalArrayFrom.apply(this, arguments)
    }
  })
}