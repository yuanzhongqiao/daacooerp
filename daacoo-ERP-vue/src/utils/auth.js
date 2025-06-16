import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'

export function getToken() {
  const token = Cookies.get(TokenKey)
  // 确保返回的token包含Bearer前缀
  if (token && !token.startsWith('Bearer ')) {
    return `Bearer ${token}`
  }
  return token ? token : ''
}

export function setToken(token) {
  // 检查token是否为undefined或null
  if (!token) {
    console.warn('尝试设置空token值')
    return false
  }
  // 确保token格式正确，如果没有Bearer前缀则添加
  const formattedToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  return Cookies.set(TokenKey, formattedToken)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}