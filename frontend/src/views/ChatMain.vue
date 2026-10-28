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
          @keydown="handleKeydown"
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
              v-model="workingDirDisplay"
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

    <el-dialog v-model="authDialogVisible" title="路径授权确认" width="500px" :close-on-click-modal="false">
      <div style="line-height: 1.8">
        <p style="color: #e6a23c; font-weight: 600">⚠️ 工具请求访问工作目录外的路径</p>
        <p>请求路径：<code style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px">{{ authPendingPath }}</code></p>
        <p>工作目录：<code style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px">{{ workingDir }}</code></p>
        <p style="color: #909399; font-size: 13px">授权后，Agent 可以读写该路径下的文件。请确认信任此路径。</p>
      </div>
      <template #footer>
        <el-button @click="authDialogVisible = false">拒绝</el-button>
        <el-button type="primary" @click="confirmAuthorize">授权并继续</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, FolderOpened } from '@element-plus/icons-vue'
import { chatApi } from '@/api/chat'
import { modelsApi } from '@/api/models'
import { createTaskEventSource } from '@/api/sse'
import type { Conversation, ConversationMessage, ModelProfile, TaskEvent } from '@/types'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const conversationId = computed(() => route.params.id ? Number(route.params.id) : null)
const currentConversation = ref<Conversation | null>(null)
const messages = ref<ConversationMessage[]>([])
const models = ref<ModelProfile[]>([])
const selectedModelId = ref<number | null>(null)
const workingDir = ref<string>('')
const workingDirDisplay = computed(() => {
  if (!workingDir.value) return ''
  const parts = workingDir.value.replace(/\\/g, '/').split('/').filter(Boolean)
  return parts[parts.length - 1] || workingDir.value
})
const inputText = ref('')
const sending = ref(false)
const thinking = ref(false)
const thinkingText = ref('思考中...')
const msgListRef = ref<HTMLElement>()
const authDialogVisible = ref(false)
const authPendingPath = ref('')

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

const knownMessageIds = ref<Set<number>>(new Set())

async function loadConversation() {
  if (!conversationId.value) return
  try {
    const msgs = await chatApi.messages(conversationId.value)
    messages.value = msgs
    knownMessageIds.value = new Set(msgs.map(m => m.id))
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

let eventSource: EventSource | null = null
const messageQueue: ConversationMessage[] = []
let isProcessingQueue = false

function startSSE(convId: number) {
  stopSSE()
  eventSource = createTaskEventSource(convId, null, (event: TaskEvent) => {
    handleSSEEvent(convId, event)
  }, () => {
    thinking.value = false
    stopSSE()
  })
}

function stopSSE() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

async function handleSSEEvent(convId: number, event: TaskEvent) {
  if (event.eventType === 'NEW_MESSAGE') {
    await fetchNewMessages(convId)
  } else if (event.eventType === 'AGENT_DONE') {
    await fetchNewMessages(convId)
    thinking.value = false
    stopSSE()
  }
}

async function fetchNewMessages(convId: number) {
  try {
    const msgs = await chatApi.messages(convId)
    const newMsgs = msgs.filter(m => !knownMessageIds.value.has(m.id))
    for (const msg of newMsgs) {
      knownMessageIds.value.add(msg.id)
      messageQueue.push(msg)
    }
    processQueue()
  } catch { /* ignore */ }
}

async function processQueue() {
  if (isProcessingQueue) return
  isProcessingQueue = true
  while (messageQueue.length > 0) {
    const msg = messageQueue.shift()!
    if (msg.role === 'MODEL' && msg.status === 'SUCCESS' && msg.content) {
      await typewriterMessage(msg)
    } else {
      messages.value.push(msg)
      scrollToBottom()
      checkAuthRequests()
      if (msg.status === 'ERROR' && msg.content && (
        msg.content.includes('浏览器会话已断开') ||
        msg.content.includes('请重新点击登录') ||
        msg.content.includes('模型未就绪')
      )) {
        thinking.value = false
        stopSSE()
        await loadModels()
        ElMessage.warning('模型连接已断开，请重新登录')
        window.dispatchEvent(new CustomEvent('open-manage-drawer', { detail: 'models' }))
      }
    }
  }
  isProcessingQueue = false
}

async function typewriterMessage(msg: ConversationMessage) {
  const fullText = msg.content || ''
  if (!fullText) {
    messages.value.push(msg)
    scrollToBottom()
    return
  }

  const tempMsg: any = { ...msg, content: '' }
  messages.value.push(tempMsg)
  scrollToBottom()

  const totalChars = fullText.length
  let delay = 20
  let chunkSize = 1
  if (totalChars > 1000) { delay = 8; chunkSize = 5 }
  else if (totalChars > 500) { delay = 10; chunkSize = 3 }
  else if (totalChars > 200) { delay = 15; chunkSize = 2 }

  let i = 0
  while (i < totalChars) {
    i = Math.min(i + chunkSize, totalChars)
    tempMsg.content = fullText.substring(0, i)
    scrollToBottom()
    await new Promise(resolve => setTimeout(resolve, delay))
  }
  checkAuthRequests()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    send()
  }
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

  const sentText = inputText.value
  const tempUserMsg: any = {
    id: -Date.now(),
    conversationId: convId,
    role: 'USER',
    content: sentText,
    charCount: sentText.length,
    status: 'SUCCESS',
    createdAt: new Date().toISOString()
  }
  messages.value.push(tempUserMsg)
  knownMessageIds.value.add(tempUserMsg.id)
  inputText.value = ''
  thinking.value = true
  thinkingText.value = '思考中...'
  scrollToBottom()

  sending.value = true
  try {
    await chatApi.sendMessage(convId, sentText, selectedModelId.value ?? undefined)
    startSSE(convId)
  } catch {
    thinking.value = false
    ElMessage.error('发送失败，请检查后端服务是否正常运行')
  } finally {
    sending.value = false
  }
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
  const dir = prompt('请输入工作目录绝对路径', workingDir.value || '')
  if (dir !== null && dir.trim()) {
    const trimmed = dir.trim()
    if (!trimmed.startsWith('/')) {
      ElMessage.warning('请输入绝对路径（以 / 开头）')
      return
    }
    workingDir.value = trimmed
    ElMessage.success(`工作目录: ${trimmed}`)
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

function checkAuthRequests() {
  for (const msg of messages.value) {
    if (msg.role === 'TOOL' && msg.content && msg.content.includes('[需要授权]')) {
      const match = msg.content.match(/路径\s+(\S+)\s+超出工作目录/)
      if (match && match[1]) {
        authPendingPath.value = match[1]
        authDialogVisible.value = true
        return
      }
    }
  }
}

async function confirmAuthorize() {
  if (!conversationId.value || !authPendingPath.value) return
  try {
    await chatApi.authorizePath(conversationId.value, authPendingPath.value)
    ElMessage.success(`已授权路径: ${authPendingPath.value}`)
    authDialogVisible.value = false
    ElMessage.info('请重新发送消息让 Agent 继续执行')
  } catch {
    ElMessage.error('授权失败')
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
  stopSSE()
})
watch(conversationId, () => {
  stopSSE()
  thinking.value = false
  loadConversation()
})
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