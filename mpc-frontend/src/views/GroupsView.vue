<template>
  <div class="groups-view">
    <!-- 群组列表面板 -->
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">群组</span>
        <el-tooltip content="创建群组">
          <el-button circle :icon="Plus" size="small" @click="showCreate = true" />
        </el-tooltip>
        <el-tooltip content="加入群组">
          <el-button circle :icon="Search" size="small" @click="showJoin = true" />
        </el-tooltip>
      </div>

      <div class="search-box">
        <el-input v-model="searchText" placeholder="搜索群组" prefix-icon="Search" size="small" clearable />
      </div>

      <div class="list-scroll">
        <div v-if="!filteredGroups.length" class="empty-tip">暂无群组</div>
        <div
          v-for="group in filteredGroups"
          :key="group.id"
          class="group-item"
          :class="{ active: activeGroupId === group.id }"
          @click="openGroup(group)"
        >
          <div class="group-icon">{{ group.name.charAt(0).toUpperCase() }}</div>
          <div class="group-info">
            <span class="group-name">{{ group.name }}</span>
            <span class="group-meta">{{ group.memberCount }} 成员</span>
          </div>
          <el-icon v-if="group.ownerId === auth.user?.id" class="owner-badge" title="群主">
            <Star />
          </el-icon>
        </div>
      </div>
    </div>

    <!-- 群聊区域 -->
    <template v-if="activeGroup">
      <div class="chat-area">
        <!-- 群聊顶栏 -->
        <div class="chat-header">
          <span class="chat-title">{{ activeGroup.name }}</span>
          <div class="header-actions">
            <el-tooltip content="群组信息">
              <el-button circle :icon="InfoFilled" size="small" @click="showInfo = !showInfo" />
            </el-tooltip>
          </div>
        </div>

        <!-- 公告横幅 -->
        <div class="announcement-bar" v-if="activeGroup.announcement">
          <el-icon><Notification /></el-icon>
          <span>{{ activeGroup.announcement }}</span>
        </div>

        <!-- 消息区 -->
        <ChatWindow
          :target="activeGroup"
          :messages="chatStore.groupMessages[activeGroup.id] || []"
          type="group"
          @send="sendGroupMsg"
        />

        <!-- 语音分组区 -->
        <VoiceChannelPanel :groupId="activeGroup.id" :canManage="canManage" />
      </div>

      <!-- 群组信息侧边栏 -->
      <GroupInfoPanel
        v-if="showInfo"
        :group="activeGroup"
        :members="groupStore.currentMembers"
        :canManage="canManage"
        :isOwner="isOwner"
        @refresh="refreshGroup"
        @close="showInfo = false"
      />
    </template>

    <div class="chat-area empty-chat" v-else>
      <el-icon :size="48" color="var(--text-muted)"><ChatLineRound /></el-icon>
      <p>选择一个群组开始聊天</p>
    </div>

    <!-- 创建群组弹窗 -->
    <el-dialog v-model="showCreate" title="创建群组" width="360px">
      <el-form :model="createForm" @submit.prevent="createGroup">
        <el-form-item label="群组名称">
          <el-input v-model="createForm.name" placeholder="输入群组名称" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="createGroup">创建</el-button>
      </template>
    </el-dialog>

    <!-- 加入群组弹窗 -->
    <el-dialog v-model="showJoin" title="加入群组" width="360px">
      <el-form @submit.prevent="joinGroup">
        <el-form-item label="群组ID">
          <el-input v-model="joinGroupId" placeholder="输入群组ID" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoin = false">取消</el-button>
        <el-button type="primary" :loading="joinLoading" @click="joinGroup">申请加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, InfoFilled, Star, ChatLineRound, Bell as Notification } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useGroupStore } from '@/stores/group'
import { useChatStore } from '@/stores/chat'
import { useWsStore } from '@/stores/ws'
import { groupApi } from '@/api'
import ChatWindow from '@/components/ChatWindow.vue'
import VoiceChannelPanel from '@/components/VoiceChannelPanel.vue'
import GroupInfoPanel from '@/components/GroupInfoPanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const groupStore = useGroupStore()
const chatStore = useChatStore()
const ws = useWsStore()

const searchText = ref('')
const showCreate = ref(false)
const showJoin = ref(false)
const showInfo = ref(false)
const createForm = ref({ name: '' })
const joinGroupId = ref('')
const createLoading = ref(false)
const joinLoading = ref(false)
const activeGroup = ref(null)
const activeGroupId = computed(() => activeGroup.value?.id)

const isOwner = computed(() => activeGroup.value?.ownerId === auth.user?.id)
const canManage = computed(() => {
  if (!activeGroup.value) return false
  const me = groupStore.currentMembers.find(m => m.userId === auth.user?.id)
  return me?.role === 'OWNER' || me?.role === 'ADMIN'
})

const filteredGroups = computed(() =>
  groupStore.groups.filter(g => g.name.toLowerCase().includes(searchText.value.toLowerCase()))
)

onMounted(async () => {
  await groupStore.fetchGroups()
  const id = Number(route.params.groupId)
  if (id) {
    const g = groupStore.groups.find(g => g.id === id)
    if (g) openGroup(g)
  }
})

watch(() => route.params.groupId, async (id) => {
  if (id) {
    const g = groupStore.groups.find(g => g.id === Number(id))
    if (g) openGroup(g)
  }
})

watch(() => groupStore.groups, (groups) => {
  if (activeGroup.value && !groups.find(g => g.id === activeGroup.value.id)) {
    activeGroup.value = null
    showInfo.value = false
    router.replace('/groups')
  }
}, { deep: true })

async function openGroup(group) {
  activeGroup.value = group
  router.replace(`/groups/${group.id}`)
  ws.subscribeGroup(group.id)
  await Promise.all([
    groupStore.fetchMembers(group.id),
    chatStore.fetchGroupHistory(group.id)
  ])
}

async function refreshGroup() {
  if (!activeGroup.value) return
  await groupStore.fetchGroups()
  const updated = groupStore.groups.find(g => g.id === activeGroup.value.id)
  if (updated) activeGroup.value = updated
  await groupStore.fetchMembers(activeGroup.value.id)
}

function sendGroupMsg(content) {
  if (!activeGroup.value) return
  ws.sendGroup(activeGroup.value.id, content)
}

async function createGroup() {
  if (!createForm.value.name.trim()) return ElMessage.warning('请输入群组名称')
  createLoading.value = true
  try {
    const group = await groupApi.create(createForm.value)
    groupStore.addGroup(group)
    showCreate.value = false
    createForm.value.name = ''
    ElMessage.success('群组创建成功')
    openGroup(group)
  } catch (e) {
    ElMessage.error(e || '创建失败')
  } finally {
    createLoading.value = false
  }
}

async function joinGroup() {
  if (!joinGroupId.value) return
  joinLoading.value = true
  try {
    await groupApi.requestJoin(Number(joinGroupId.value))
    ElMessage.success('申请已发送，等待审核')
    showJoin.value = false
    joinGroupId.value = ''
  } catch (e) {
    ElMessage.error(e || '申请失败')
  } finally {
    joinLoading.value = false
  }
}
</script>

<style scoped>
.groups-view {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.panel {
  width: var(--panel-width);
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 16px 12px 8px;
  gap: 6px;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  flex: 1;
}

.search-box { padding: 0 8px 8px; }

.list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 4px 8px;
}

.empty-tip {
  text-align: center;
  color: var(--text-muted);
  padding: 32px 0;
  font-size: 13px;
}

.group-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.1s;
}

.group-item:hover { background-color: var(--bg-hover); }
.group-item.active { background-color: var(--bg-hover); }

.group-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background-color: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.group-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.group-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  font-size: 11px;
  color: var(--text-muted);
}

.owner-badge { color: var(--warning); font-size: 14px; }

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.empty-chat {
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 14px;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 52px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  flex-shrink: 0;
}

.chat-title {
  font-weight: 600;
  font-size: 15px;
  flex: 1;
}

.header-actions { display: flex; gap: 6px; }

.announcement-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background-color: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
}
</style>
