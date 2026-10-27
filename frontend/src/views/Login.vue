<template>
  <div class="login-page">
    <div class="login-card card-hover">
      <div class="logo">
        <el-icon size="32" color="#409eff"><Cpu /></el-icon>
        <h2>多模型网页 Agent 平台</h2>
      </div>
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-width="0" @submit.prevent="onLogin">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" label-width="0" @submit.prevent="onRegister">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.displayName" placeholder="昵称" prefix-icon="EditPen" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi, type AuthResult } from '@/api/auth'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', displayName: '', password: '' })

function saveAuth(res: AuthResult) {
  localStorage.setItem('token', res.token)
  localStorage.setItem('user', JSON.stringify(res))
}

async function onLogin() {
  if (!loginForm.username || !loginForm.password) { ElMessage.warning('请填写完整'); return }
  loading.value = true
  try {
    const res = await authApi.login(loginForm)
    saveAuth(res)
    ElMessage.success('登录成功')
    router.push('/')
  } finally { loading.value = false }
}

async function onRegister() {
  if (!regForm.username || !regForm.password) { ElMessage.warning('请填写完整'); return }
  loading.value = true
  try {
    const res = await authApi.register(regForm)
    saveAuth(res)
    ElMessage.success('注册成功')
    router.push('/')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}
.logo h2 { font-size: 18px; color: #303133; margin: 0; }
</style>