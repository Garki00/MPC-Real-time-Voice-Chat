import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, userApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('mpc_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('mpc_user') || 'null'))

  async function login(credentials) {
    const res = await authApi.login(credentials)
    token.value = res.token
    user.value = { id: res.userId, username: res.username, avatar: res.avatar }
    localStorage.setItem('mpc_token', res.token)
    localStorage.setItem('mpc_user', JSON.stringify(user.value))
  }

  async function refreshUser() {
    const res = await userApi.getMe()
    user.value = res
    localStorage.setItem('mpc_user', JSON.stringify(res))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('mpc_token')
    localStorage.removeItem('mpc_user')
  }

  return { token, user, login, logout, refreshUser }
})
