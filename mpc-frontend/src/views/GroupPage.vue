<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Setting, Management } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useGroupStore } from '@/stores/group'
import { useChatStore } from '@/stores/chat'
import { useWsStore } from '@/stores/ws'
import { groupApi } from '@/api'
import VoiceChannelPanel from '@/components/VoiceChannelPanel.vue'
import ChatWindow from '@/components/ChatWindow.vue'
import GroupMembersPanel from '@/components/GroupMembersPanel.vue'
import GroupSettingsModal from '@/components/GroupSettingsModal.vue'
import GroupAnnouncementsPanel from '@/components/GroupAnnouncementsPanel.vue'
import GroupRequestsPanel from '@/components/GroupRequestsPanel.vue'

const route = useRoute()
const auth = useAuthStore()
const groupStore = useGroupStore()
const chatStore = useChatStore()
const ws = useWsStore()
const showSettings = ref(false)
const showFunctionsMenu = ref(false)
const activeView = ref('chat')
const requestsRef = ref(null)
const pendingRequestsCount = ref(0)

const groupId = computed(() => Number(route.params.groupId))
const group = computed(() => groupStore.groups.find(g => g.id === groupId.value))
const messages = computed(() => chatStore.groupMessages[groupId.value] || [])

const members = ref([])
const currentUserRole = computed(() => {
  const me = members.value.find(m => m.userId === auth.user?.id)
  return me?.role || 'MEMBER'
})

const isAdmin = computed(() =>
  currentUserRole.value === 'OWNER' || currentUserRole.value === 'ADMIN'
)

async function loadGroup(id) {
  if (!id) return
  ws.subscribeGroup(id)
  await chatStore.fetchGroupHistory(id)
  await loadMembers(id)
  if (isAdmin.value) {
    await loadPendingRequestsCount(id)
  }

  // 检查URL参数，如果有 openRequests=true 则打开申请处理页面
  if (route.query.openRequests === 'true' && isAdmin.value) {
    activeView.value = 'requests'
  }
}

async function loadMembers(id) {
  try {
    members.value = await groupApi.getMembers(id)
  } catch {
    members.value = []
  }
}

async function loadPendingRequestsCount(id) {
  try {
    const requests = await groupApi.getPendingRequests(id)
    pendingRequestsCount.value = requests.filter(r => r.status === 'PENDING').length
  } catch {
    pendingRequestsCount.value = 0
  }
}

function sendGroupMsg(content) {
  ws.sendGroup(groupId.value, content)
}

function onGroupUpdated(updated) {
  groupStore.updateGroup(updated)
}

function toggleFunctionsMenu() {
  if (currentUserRole.value === 'MEMBER') {
    activeView.value = 'announcements'
    showFunctionsMenu.value = false
  } else {
    showFunctionsMenu.value = !showFunctionsMenu.value
  }
}

function selectFunction(func) {
  activeView.value = func
  showFunctionsMenu.value = false
}

function onRequestsUpdated(count) {
  pendingRequestsCount.value = count
}

function backToChat() {
  activeView.value = 'chat'
}

onMounted(() => loadGroup(groupId.value))
watch(groupId, (id) => {
  showSettings.value = false
  showFunctionsMenu.value = false
  activeView.value = 'chat'
  loadGroup(id)
})
</script>

<template>
  <div class="group-page">
    <!-- Left panel: group info + voice channels -->
    <aside class="group-left-panel">
      <div class="group-header">
        <span class="group-name">{{ group?.name ?? '群组' }}</span>
        <div class="header-actions">
          <el-badge v-if="isAdmin && pendingRequestsCount > 0" :value="pendingRequestsCount" type="danger">
            <button class="functions-btn" :class="{ active: showFunctionsMenu }" title="群组功能" @click="toggleFunctionsMenu">
              <el-icon :size="16"><Management /></el-icon>
            </button>
          </el-badge>
          <button v-else class="functions-btn" :class="{ active: showFunctionsMenu }" title="群组功能" @click="toggleFunctionsMenu">
            <el-icon :size="16"><Management /></el-icon>
          </button>
          <button class="settings-btn" title="群组设置" @click="showSettings = true">
            <el-icon :size="16"><Setting /></el-icon>
          </button>
        </div>
      </div>

      <!-- 功能菜单 -->
      <div v-if="showFunctionsMenu && isAdmin" class="functions-menu">
        <div class="menu-item" @click="selectFunction('announcements')">
          <span>群组通知</span>
        </div>
        <div class="menu-item" @click="selectFunction('requests')">
          <span>申请处理</span>
          <el-badge v-if="pendingRequestsCount > 0" :value="pendingRequestsCount" type="danger" />
        </div>
      </div>

      <div class="voice-panel-wrap">
        <VoiceChannelPanel :group-id="groupId" :can-manage="isAdmin" />
      </div>
    </aside>

    <!-- Center: chat / announcements / requests -->
    <div class="group-center">
      <ChatWindow
        v-if="activeView === 'chat' && group"
        :target="group"
        :messages="messages"
        type="group"
        @send="sendGroupMsg"
      />
      <GroupAnnouncementsPanel
        v-else-if="activeView === 'announcements'"
        :group-id="groupId"
        :current-user-role="currentUserRole"
        @back="backToChat"
      />
      <GroupRequestsPanel
        v-else-if="activeView === 'requests' && isAdmin"
        ref="requestsRef"
        :group-id="groupId"
        @requests-updated="onRequestsUpdated"
        @back="backToChat"
      />
    </div>

    <!-- Right panel: members -->
    <GroupMembersPanel :group-id="groupId" @member-updated="loadMembers(groupId)" />

    <!-- Settings modal -->
    <GroupSettingsModal
      v-model="showSettings"
      :group-id="groupId"
      @updated="onGroupUpdated"
    />
  </div>
</template>

<style scoped>
.group-page {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.group-left-panel {
  width: 240px;
  flex-shrink: 0;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 48px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.group-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.functions-btn,
.settings-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: background-color 0.15s, color 0.15s;
}

.functions-btn:hover,
.settings-btn:hover,
.functions-btn.active {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.functions-menu {
  background-color: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);
  padding: 4px;
}

.functions-menu .menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
  transition: background-color 0.15s;
}

.functions-menu .menu-item:hover {
  background-color: var(--bg-hover);
}

.voice-panel-wrap {
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
}

.group-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}
</style>
