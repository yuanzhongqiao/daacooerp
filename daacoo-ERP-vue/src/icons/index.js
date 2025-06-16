import SvgIcon from '@/components/SvgIcon/index.vue' // svg组件

// 导出注册组件的函数
export function setupSvgIcon(app) {
  app.component('svg-icon', SvgIcon)
}

// 自动导入所有svg图标
const svgRequire = import.meta.glob('./svg/*.svg', { eager: true })
const requireAll = (requireContext) => {
  return Object.keys(requireContext).map((key) => {
    const name = key.replace(/^\.\/(.*)\.\w+$/, '$1')
    return name
  })
}
requireAll(svgRequire)