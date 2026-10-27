<template>
  <div class="panel">
    <el-button type="primary" size="small" @click="dialogVisible = true" style="margin-bottom:12px">添加模型</el-button>
    <el-table :data="models" size="small" border>
      <el-table-column prop="displayName" label="名称" />
      <el-table-column prop="provider" label="平台" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="login(row)">登录</el-button>
          <el-button size="small" @click="disconnect(row.id)">断开</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="添加模型" width="400px" append-to-body>
      <el-form :model="form" label-width="70px">
        <el-form-item label="平台">
          <el-select v-model="form.provider" filterable>
            <el-option label="千问 (通义千问)" value="qianwen" />
            <el-option label="豆包 (字节跳动)" value="doubao" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Kimi (月之暗面)" value="kimi" />
            <el-option label="智谱清言 (GLM)" value="zhipu" />
            <el-option label="腾讯元宝" value="yuanbao" />

            <el-option label="ChatGPT (OpenAI)" value="chatgpt" />
            <el-option label="Claude (Anthropic)" value="claude" />
            <el-option label="Grok (xAI)" value="grok" />
            <el-option label="Gemini (Google)" value="gemini" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.displayName" :placeholder="providerLabel" /></el-form-item>
        <el-form-item label="别名"><el-input v-model="form.accountAlias" :placeholder="form.provider" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="add">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="loginDialog" title="网页登录" width="420px" append-to-body :close-on-click-modal="false">
      <div class="login-guide">
        <p>请在弹出的浏览器窗口中完成 <b>{{ loginModel?.displayName }}</b> 的登录/扫码。</p>
        <p class="hint">系统将自动检测登录状态，请勿关闭弹出的浏览器窗口。</p>
        <div class="login-status">
          <StatusTag :status="loginModelStatus" />
          <el-button v-if="loginModelStatus !== 'READY'" size="small" @click="checkLoginStatus">立即检测</el-button>
        </div>
        <p v-if="loginStatusMessage" class="status-msg">{{ loginStatusMessage }}</p>
        <el-progress v-if="polling" :percentage="100" status="success" :indeterminate="true" :duration="2" />
      </div>
      <template #footer>
        <el-button @click="closeLoginDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { modelsApi } from '@/api/models'
import type { ModelProfile } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const providerLabels: Record<string, string> = {
  qianwen: '千问 (通义千问)',
  doubao: '豆包 (字节跳动)',
  deepseek: 'DeepSeek',
  kimi: 'Kimi (月之暗面)',
  zhipu: '智谱清言 (GLM)',
  yuanbao: '腾讯元宝',
  chatgpt: 'ChatGPT (OpenAI)',
  claude: 'Claude (Anthropic)',
  grok: 'Grok (xAI)',
  gemini: 'Gemini (Google)'
}

const models = ref<ModelProfile[]>([])
const dialogVisible = ref(false)
const form = reactive({ provider: 'qianwen', displayName: '', accountAlias: '' })
const providerLabel = computed(() => providerLabels[form.provider] || form.provider)
const loginDialog = ref(false)
const loginModel = ref<ModelProfile | null>(null)
const loginModelStatus = ref('CONNECTING')
const loginStatusMessage = ref('')
const polling = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

async function load() { models.value = await modelsApi.list() }

async function add() {
  const displayName = form.displayName.trim() || providerLabel.value
  const accountAlias = form.accountAlias.trim() || form.provider
  await modelsApi.create({ provider: form.provider, displayName, accountAlias })
  ElMessage.success('已添加')
  dialogVisible.value = false
  form.displayName = ''
  form.accountAlias = ''
  await load()
}

async function login(row: ModelProfile) {
  if (row.status === 'READY') {
    ElMessage.info('该模型已登录')
    return
  }
  try {
    const result = await modelsApi.loginStatus(row.id)
    if (result.status === 'READY') {
      await load()
      ElMessage.success('模型已登录')
      return
    }
  } catch { /* ignore */ }

  loginModel.value = row
  loginModelStatus.value = 'CONNECTING'
  loginStatusMessage.value = '正在启动浏览器...'
  loginDialog.value = true
  try {
    await modelsApi.connect(row.id)
    startPolling(row.id)
  } catch {
    ElMessage.error('启动浏览器失败，请重试')
    loginModelStatus.value = 'ERROR'
    loginStatusMessage.value = '启动失败'
  }
}

function startPolling(id: number) {
  polling.value = true
  pollTimer = setInterval(async () => {
    try {
      const result = await modelsApi.loginStatus(id)
      loginModelStatus.value = result.status
      loginStatusMessage.value = result.message || ''
      if (result.status === 'READY') {
        stopPolling()
        await load()
        window.dispatchEvent(new CustomEvent('models-updated'))
        setTimeout(() => { loginDialog.value = false }, 1000)
      }
    } catch { /* ignore */ }
  }, 3000)
}

function stopPolling() {
  polling.value = false
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function checkLoginStatus() {
  if (!loginModel.value) return
  const result = await modelsApi.loginStatus(loginModel.value.id)
  loginModelStatus.value = result.status
  loginStatusMessage.value = result.message || ''
  if (result.status === 'READY') { stopPolling(); await load(); setTimeout(() => { loginDialog.value = false }, 1000) }
}

function closeLoginDialog() {
  stopPolling()
  loginDialog.value = false
}

async function disconnect(id: number) {
  await modelsApi.disconnect(id)
  ElMessage.success('已断开')
  await load()
}

onUnmounted(stopPolling)
load()
</script>

<style scoped>
.panel { padding: 0 4px; }
.login-guide p { margin: 8px 0; color: #606266; }
.login-guide .hint { font-size: 12px; color: #909399; }
.login-status { display: flex; align-items: center; gap: 12px; margin: 16px 0; }
.status-msg { font-size: 13px; color: #409eff; margin: 8px 0; }
</style>