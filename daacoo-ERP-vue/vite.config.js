import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      'path': 'path-browserify'
    }
  },
  server: {
    host: true,
	port: 9876, // 设置本地默认端口
    proxy: {
      '/api': {
        target: 'http://localhost:8081', // 修改为用户需要的目标地址
        changeOrigin: true, // 是否设置同源
        pathRewrite: {
          '^/api': '' // 路径重写，忽略拦截器里面的内容
        }
      }
    }
  }
})
