<template>
  <el-container class="chat-layout">
    <el-aside width="260px" class="sidebar">
      <div class="logo">
        <el-icon size="22" color="#409eff"><Cpu /></el-icon>
        <span>Agent 平台</span>
      </div>
      <el-button class="new-chat" type="primary" @click="newChat">
        <el-icon><Plus /></el-icon> 新建对话
      </el-button>
      <div class="conv-list">
        <div
          v-for="c in conversations"
          :key="c.id"
          class="conv-item"
          :class="{ active: c.id === currentId }"
          @click="selectConv(c.id)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="conv-title">{{ c.title || '新对话' }}</span>
          <span class="conv-actions" @click.stop>
            <el-icon class="action-btn" @click="startRename(c)"><Edit /></el-icon>
            <el-icon class="action-btn" @click="confirmDelete(c)"><Delete /></el-icon>
          </span>
        </div>
        <el-empty v-if="conversations.length === 0" description="暂无对话" :image-size="60" />
      </div>
      <div class="user-area">
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user-info">
            <el-avatar :size="32" icon="UserFilled" />
            <span class="user-name">{{ user?.displayName || '用户' }}</span>
            <el-icon><ArrowUp /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="models" icon="Connection">模型账户管理</el-dropdown-item>
              <el-dropdown-item command="tools" icon="Tools">工具管理</el-dropdown-item>
              <el-dropdown-item command="mcp" icon="Share">MCP 管理</el-dropdown-item>
              <el-dropdown-item command="skills" icon="Document">Skills 管理</el-dropdown-item>
              <el-dropdown-item command="settings" icon="Setting">系统设置</el-dropdown-item>
              <el-dropdown-item divided command="logout" icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>
    <el-main class="main-area">
      <router-view />
    </el-main>
    <ManageDrawer v-model:visible="drawerVisible" :type="drawerType" />

    <el-dialog v-model="renameDialog" title="重命名对话" width="360px" append-to-body>
      <el-input v-model="renameTitle" placeholder="请输入对话名称" maxlength="50" show-word-limit />
      <template #footer>
        <el-button @click="renameDialog = false">取消</el-button>
        <el-button type="primary" @click="doRename">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi } from '@/api/chat'
import { modelsApi } from '@/api/models'
import type { Conversation, ModelProfile } from '@/types'
import ManageDrawer from '@/components/ManageDrawer.vue'

const router = useRouter()
const route = useRoute()
const conversations = ref<Conversation[]>([])
const models = ref<ModelProfile[]>([])
const drawerVisible = ref(false)
const drawerType = ref('models')
const renameDialog = ref(false)
const renameTitle = ref('')
const renameTarget = ref<Conversation | null>(null)

const user = computed(() => {
  try { return JSON.parse(localStorage.getItem('user') || '{}') } catch { return null }
})
const currentId = computed(() => route.params.id ? Number(route.params.id) : null)

async function loadConversations() {
  try { conversations.value = await chatApi.conversations() } catch { /* ignore */ }
}
async function loadModels() {
  try { models.value = await modelsApi.list() } catch { /* ignore */ }
}

async function newChat() {
  const c = await chatApi.createConversation({})
  conversations.value.unshift(c)
  router.push(`/conversation/${c.id}`)
}

function selectConv(id: number) {
  router.push(`/conversation/${id}`)
}

function startRename(c: Conversation) {
  renameTarget.value = c
  renameTitle.value = c.title || '新对话'
  renameDialog.value = true
}

async function doRename() {
  if (!renameTarget.value || !renameTitle.value.trim()) return
  try {
    const updated = await chatApi.renameConversation(renameTarget.value.id, { title: renameTitle.value.trim() })
    const idx = conversations.value.findIndex(c => c.id === updated.id)
    if (idx >= 0) conversations.value[idx] = updated
    renameDialog.value = false
    ElMessage.success('已重命名')
  } catch { /* ignore */ }
}

async function confirmDelete(c: Conversation) {
  try {
    await ElMessageBox.confirm('确定删除该对话？删除后不可恢复。', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await chatApi.deleteConversation(c.id)
    conversations.value = conversations.value.filter(item => item.id !== c.id)
    if (currentId.value === c.id) {
      router.push('/')
    }
    ElMessage.success('已删除')
  } catch { /* cancel */ }
}

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
    return
  }
  drawerType.value = cmd
  drawerVisible.value = true
  if (cmd === 'models') loadModels()
}

onMounted(() => {
  loadConversations()
  window.addEventListener('open-manage-drawer', ((e: CustomEvent) => {
    drawerType.value = e.detail || 'models'
    drawerVisible.value = true
    if (drawerType.value === 'models') loadModels()
  }) as EventListener)
})
onUnmounted(() => {
  window.removeEventListener('open-manage-drawer', (() => {}) as EventListener)
})
watch(() => route.params.id, loadConversations)
</script>

<style scoped>
.chat-layout { height: 100vh; }
.sidebar {
  background: #fff;
  border-right: 1px solid #e6e8eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.new-chat {
  margin: 0 16px 12px;
  border-radius: 8px;
}
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}
.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: var(--transition);
  color: #606266;
}
.conv-item:hover { background: #f5f7fa; }
.conv-item.active { background: #ecf5ff; color: var(--primary); }
.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.conv-actions {
  display: none;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.conv-item:hover .conv-actions { display: flex; }
.action-btn {
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  transition: var(--transition);
}
.action-btn:hover { color: var(--primary); }
.user-area {
  border-top: 1px solid #e6e8eb;
  padding: 12px 16px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: var(--transition);
}
.user-info:hover { background: #f5f7fa; }
.user-name { flex: 1; font-size: 14px; font-weight: 500; }
.main-area { padding: 0; background: var(--bg); }
</style>