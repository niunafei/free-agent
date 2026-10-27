<template>
  <div class="chat-main">
    <div class="chat-header">
      <span class="title">{{ currentConversation?.title || '新对话' }}</span>
    </div>
    <div class="msg-list" ref="msgListRef">
      <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role.toLowerCase()">
        <div class="msg-bubble">
          <div class="msg-role">{{ roleLabel(msg.role) }}</div>
          <div class="msg-content" :class="{ error: msg.status === 'ERROR' }">{{ msg.content }}</div>
        </div>
      </div>
      <div v-if="thinking" class="msg-row model">
        <div class="msg-bubble thinking-bubble">
          <div class="msg-role">模型</div>
          <div class="msg-content thinking-content">
            <span class="thinking-dots">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </span>
            {{ thinkingText }}
          </div>
        </div>
      </div>
      <el-empty v-if="messages.length === 0 && !thinking" description="开始新的对话" :image-size="80" style="margin-top:80px" />
    </div>
    <div class="input-area">
      <div class="input-card">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          resize="none"
          @keydown.enter.exact.prevent="send"
          class="msg-input"
        />
        <div class="input-toolbar">
          <div class="toolbar-left">
            <el-select v-model="selectedModelId" placeholder="选择模型" size="small" style="width:160px" @change="onModelChange">
              <el-option v-for="m in models" :key="m.id" :label="m.displayName" :value="m.id">
                <span style="float:left">{{ m.displayName }}</span>
                <StatusTag :status="m.status" style="float:right" />
              </el-option>
            </el-select>
            <el-input
              v-model="workingDir"
              placeholder="工作目录"
              size="small"
              style="width:200px"
              :prefix-icon="FolderOpened"
              readonly
              @click="selectWorkingDir"
            >
              <template #append>
                <el-button :icon="FolderOpened" @click="selectWorkingDir" />
              </template>
            </el-input>
          </div>
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="send" :disabled="!inputText.trim() || thinking">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, FolderOpened } from '@element-plus/icons-vue'
import { chatApi } from '@/api/chat'
import { modelsApi } from '@/api/models'
import type { Conversation, ConversationMessage, ModelProfile } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const conversationId = computed(() => route.params.id ? Number(route.params.id) : null)
const currentConversation = ref<Conversation | null>(null)
const messages = ref<ConversationMessage[]>([])
const models = ref<ModelProfile[]>([])
const selectedModelId = ref<number | null>(null)
const workingDir = ref<string>('')
const inputText = ref('')
const sending = ref(false)
const thinking = ref(false)
const thinkingText = ref('思考中...')
const msgListRef = ref<HTMLElement>()

const MODEL_STATUS_PRIORITY: Record<string, number> = {
  READY: 0, CONNECTING: 1, WAITING_USER: 2, NOT_CONFIGURED: 3, ERROR: 4
}

function pickBestModel(): ModelProfile | undefined {
  return [...models.value]
    .sort((a, b) => (MODEL_STATUS_PRIORITY[a.status] ?? 99) - (MODEL_STATUS_PRIORITY[b.status] ?? 99))
    .find(m => m.enabled !== false)
}

async function loadModels() {
  try {
    models.value = await modelsApi.list()
    if (!selectedModelId.value) {
      const best = pickBestModel()
      if (best) selectedModelId.value = best.id
    }
  } catch { /* ignore */ }
}

async function loadConversation() {
  if (!conversationId.value) return
  try {
    messages.value = await chatApi.messages(conversationId.value)
    const convs = await chatApi.conversations()
    currentConversation.value = convs.find(c => c.id === conversationId.value) || null
    const savedModelId = currentConversation.value?.selectedModelId ?? null
    if (savedModelId) {
      selectedModelId.value = savedModelId
    } else if (!selectedModelId.value) {
      const best = pickBestModel()
      if (best) selectedModelId.value = best.id
    }
    workingDir.value = currentConversation.value?.workingDir ?? ''
    scrollToBottom()
  } catch { /* ignore */ }
}

async function send() {
  if (!inputText.value.trim() || thinking.value) return

  if (!selectedModelId.value) {
    ElMessage.warning('请先选择一个模型')
    window.dispatchEvent(new CustomEvent('open-manage-drawer', { detail: 'models' }))
    return
  }
  const selectedModel = models.value.find(m => m.id === selectedModelId.value)
  if (selectedModel && selectedModel.status !== 'READY') {
    ElMessage.warning('模型未连接，请先登录模型')
    window.dispatchEvent(new CustomEvent('open-manage-drawer', { detail: 'models' }))
    return
  }

  let convId = conversationId.value
  if (!convId) {
    try {
      const c = await chatApi.createConversation({
        title: inputText.value.slice(0, 20),
        modelId: selectedModelId.value ?? undefined,
        workingDir: workingDir.value || undefined
      })
      convId = c.id
      await router.push(`/conversation/${c.id}`)
    } catch {
      ElMessage.error('连接已断开，请检查后端服务是否正常运行')
      return
    }
  }
  sending.value = true
  thinking.value = true
  thinkingText.value = '思考中...'
  scrollToBottom()
  try {
    await chatApi.sendMessage(convId!, inputText.value, selectedModelId.value ?? undefined)
    inputText.value = ''
    await loadConversation()
    startPollingMessages(convId!)
  } catch (e: any) {
    thinkingText.value = '连接已断开，请检查后端服务是否正常运行'
    setTimeout(() => { thinking.value = false }, 3000)
  } finally {
    sending.value = false
  }
}

let pollMsgTimer: ReturnType<typeof setInterval> | null = null
let lastMsgCount = 0

function startPollingMessages(convId: number) {
  lastMsgCount = messages.value.length
  if (pollMsgTimer) clearInterval(pollMsgTimer)
  pollMsgTimer = setInterval(async () => {
    try {
      const msgs = await chatApi.messages(convId)
      if (msgs.length > lastMsgCount) {
        messages.value = msgs
        lastMsgCount = msgs.length
        scrollToBottom()
        const lastMsg = msgs[msgs.length - 1]
        if (lastMsg.role === 'MODEL' && (lastMsg.status === 'SUCCESS' || lastMsg.status === 'ERROR')) {
          thinking.value = false
          if (pollMsgTimer) { clearInterval(pollMsgTimer); pollMsgTimer = null }
        }
      }
    } catch {
      thinking.value = false
      thinkingText.value = '连接已断开，请检查后端服务是否正常运行'
      if (pollMsgTimer) { clearInterval(pollMsgTimer); pollMsgTimer = null }
    }
  }, 2000)
}

async function onModelChange() {
  if (conversationId.value) {
    try {
      await chatApi.renameConversation(conversationId.value, {
        modelId: selectedModelId.value ?? undefined,
        workingDir: workingDir.value || undefined
      })
    } catch { /* ignore */ }
  }
}

async function selectWorkingDir() {
  try {
    const w = window as any
    if (w.showDirectoryPicker) {
      const handle = await w.showDirectoryPicker({ mode: 'readwrite' })
      workingDir.value = handle.name
      ElMessage.success(`已选择工作目录: ${handle.name}`)
      await saveWorkingDir()
      return
    }
  } catch (e: any) {
    if (e.name === 'AbortError') return
  }
  const dir = prompt('请输入工作目录绝对路径', workingDir.value || '')
  if (dir !== null) {
    workingDir.value = dir.trim()
    await saveWorkingDir()
  }
}

async function saveWorkingDir() {
  if (conversationId.value) {
    try {
      await chatApi.renameConversation(conversationId.value, { workingDir: workingDir.value || undefined })
    } catch { /* ignore */ }
  }
}

function roleLabel(role: string) {
  return role === 'USER' ? '我' : role === 'MODEL' ? '模型' : role
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  })
}

onMounted(() => {
  loadModels()
  loadConversation()
  window.addEventListener('models-updated', loadModels)
})
onUnmounted(() => {
  window.removeEventListener('models-updated', loadModels)
  if (pollMsgTimer) { clearInterval(pollMsgTimer); pollMsgTimer = null }
})
watch(conversationId, loadConversation)
</script>

<style scoped>
.chat-main { display: flex; flex-direction: column; height: 100%; }
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e6e8eb;
}
.title { font-size: 16px; font-weight: 600; }
.msg-list { flex: 1; overflow-y: auto; padding: 24px; }
.msg-row { display: flex; margin-bottom: 16px; }
.msg-row.user { justify-content: flex-end; }
.msg-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.msg-row.user .msg-bubble { background: #ecf5ff; }
.msg-role { font-size: 12px; color: #909399; margin-bottom: 4px; }
.msg-content { font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.msg-content.error { color: #f56c6c; }
.thinking-bubble { border: 1px dashed #c0c4cc; }
.thinking-content { color: #909399; display: flex; align-items: center; gap: 8px; }
.thinking-dots { display: inline-flex; gap: 4px; }
.thinking-dots .dot {
  width: 6px; height: 6px; border-radius: 50%; background: #409eff;
  animation: dot-bounce 1.4s infinite ease-in-out both;
}
.thinking-dots .dot:nth-child(1) { animation-delay: -0.32s; }
.thinking-dots .dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}
.input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e6e8eb;
}
.input-card {
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.input-card:focus-within { border-color: #409eff; }
.msg-input :deep(.el-textarea__inner) {
  border: none;
  border-radius: 0;
  box-shadow: none;
  padding: 12px 16px;
}
.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>