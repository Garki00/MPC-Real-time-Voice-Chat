<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">
        <el-icon :size="40"><ChatDotRound /></el-icon>
        <h1>MPC</h1>
        <p>语音聊天平台</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginRef" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                size="large" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="submit-btn">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerRef" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名（3-50字符）" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱" prefix-icon="Message" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）"
                prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-button type="primary" size="large" :loading="loading" @click="handleRegister" class="submit-btn">
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useWsStore } from '@/stores/ws'
import { authApi } from '@/api'

const router = useRouter()
const auth = useAuthStore()
const ws = useWsStore()

const activeTab = ref('login')
const loading = ref(false)
const loginRef = ref()
const registerRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', email: '', password: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  try {
    await loginRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await auth.login(loginForm)
    ws.connect()
    router.push('/')
  } catch (e) {
    ElMessage.error(e || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await authApi.register(registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
  } catch (e) {
    ElMessage.error(e || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
}

.login-card {
  width: 400px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 40px 36px;
}

.logo {
  text-align: center;
  margin-bottom: 32px;
  color: var(--accent);
}

.logo h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 8px 0 4px;
  letter-spacing: 4px;
}

.logo p {
  color: var(--text-secondary);
  font-size: 13px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--border-color);
}

.login-tabs :deep(.el-tabs__item) {
  color: var(--text-secondary);
  font-size: 15px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: var(--accent);
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--accent);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 42px;
  font-size: 15px;
}

.el-form-item {
  margin-bottom: 16px;
}
</style>
