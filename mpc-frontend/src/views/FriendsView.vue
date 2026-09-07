<template>
  <div class="friends-view">
    <!-- 好友列表面板 -->
    <div class="panel">
      <div class="panel-header">
        <span class="panel-title">好友</span>
        <div class="panel-actions">
          <el-tooltip content="添加好友">
            <el-button circle :icon="Plus" size="small" @click="showAddFriend = true" />
          </el-tooltip>
        </div>
      </div>

      <div class="search-box">
        <el-input v-model="searchText" placeholder="搜索好友" prefix-icon="Search" size="small" clearable />
      </div>

      <!-- 好友申请入口 -->
      <div class="request-entry" @click="showRequests = !showRequests" v-if="friendStore.requests.length">
        <el-icon><Bell /></el-icon>
        <span>好友申请</span>
        <el-badge :value="friendStore.requests.length" class="ml-auto" />
      </div>

      <!-- 好友申请列表 -->
      <div class="request-list" v-if="showRequests">
        <div class="request-item" v-for="req in friendStore.requests" :key="req.id">
          <UserAvatar :src="req.avatar" :name="req.username" :size="36" />
          <span class="req-name">{{ req.username }}</span>
          <el-button type="primary" size="small" @click="handleRequest(req.id, true)">接受</el-button>
          <el-button size="small" @click="handleRequest(req.id, false)">拒绝</el-button>
        </div>
      </div>

      <div class="divider" />

      <!-- 好友列表 -->
      <div class="list-scroll">
        <div v-if="!filteredFriends.length" class="empty-tip">暂无好友</div>
        <div
          v-for="friend in filteredFriends"
          :key="friend.id"
          class="friend-item"
          :class="{ active: activeFriendId === friend.id }"
          @click="openChat(friend)"
          @contextmenu.prevent="showFriendMenu($event, friend)"
        >
          <div class="avatar-wrap">
            <UserAvatar :src="friend.avatar" :name="friend.username" :size="38" />
            <span class="status-dot" :class="friend.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <div class="friend-info">
            <span class="friend-name">{{ friend.username }}</span>
            <span class="friend-status">{{ friend.status === 'ONLINE' ? '在线' : '离线' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 私聊窗口 -->
    <div class="chat-area" v-if="activeFriend">
      <ChatWindow
        :target="activeFriend"
        :messages="chatStore.privateMessages[activeFriend.id] || []"
        type="private"
        @send="sendMsg"
      />
    </div>
    <div class="chat-area empty-chat" v-else>
      <el-icon :size="48" color="var(--text-muted)"><ChatDotRound /></el-icon>
      <p>选择一位好友开始聊天</p>
    </div>

    <!-- 添加好友弹窗 -->
    <el-dialog v-model="showAddFriend" title="添加好友" width="360px">
      <el-form @submit.prevent="sendFriendRequest">
        <el-form-item label="用户ID">
          <el-input v-model="targetUserId" placeholder="输入对方用户ID" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddFriend = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="sendFriendRequest">发送申请</el-button>
      </template>
    </el-dialog>

    <!-- 右键菜单 -->
    <div v-if="contextMenu.visible" class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      @click.stop>
      <div class="menu-item danger" @click="deleteFriend(contextMenu.friend)">
        <el-icon><Delete /></el-icon> 删除好友
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Bell, ChatDotRound, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useFriendStore } from '@/stores/friend'
import { useChatStore } from '@/stores/chat'
import { useWsStore } from '@/stores/ws'
import { friendApi } from '@/api'
import UserAvatar from '@/components/UserAvatar.vue'
import ChatWindow from '@/components/ChatWindow.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const friendStore = useFriendStore()
const chatStore = useChatStore()
const ws = useWsStore()

const searchText = ref('')
const showAddFriend = ref(false)
const showRequests = ref(false)
const targetUserId = ref('')
const addLoading = ref(false)
const activeFriend = ref(null)
const activeFriendId = computed(() => activeFriend.value?.id)

const contextMenu = ref({ visible: false, x: 0, y: 0, friend: null })

const filteredFriends = computed(() =>
  friendStore.friends.filter(f =>
    f.username.toLowerCase().includes(searchText.value.toLowerCase())
  )
)

onMounted(async () => {
  await friendStore.fetchFriends()
  await friendStore.fetchRequests()
  const id = Number(route.params.friendId)
  if (id) {
    const f = friendStore.friends.find(f => f.id === id)
    if (f) openChat(f)
  }
})

watch(() => route.params.friendId, (id) => {
  if (id) {
    const f = friendStore.friends.find(f => f.id === Number(id))
    if (f) openChat(f)
  }
})

async function openChat(friend) {
  activeFriend.value = friend
  router.replace(`/friends/${friend.id}`)
  if (!chatStore.privateMessages[friend.id]) {
    await chatStore.fetchPrivateHistory(friend.id)
  }
}

function sendMsg(content) {
  if (!activeFriend.value) return
  chatStore.pushPrivate({
    id: Date.now(),
    senderId: auth.user.id,
    receiverId: activeFriend.value.id,
    content,
    type: 'TEXT',
    createdAt: new Date().toISOString(),
    senderName: auth.user?.username,
    senderAvatar: auth.user?.avatar
  })
  ws.sendPrivate(activeFriend.value.id, content)
}

async function sendFriendRequest() {
  if (!targetUserId.value) return
  addLoading.value = true
  try {
    await friendApi.sendRequest(Number(targetUserId.value))
    ElMessage.success('好友申请已发送')
    showAddFriend.value = false
    targetUserId.value = ''
  } catch (e) {
    ElMessage.error(e || '发送失败')
  } finally {
    addLoading.value = false
  }
}

async function handleRequest(requesterId, accept) {
  try {
    await friendApi.handleRequest(requesterId, accept)
    ElMessage.success(accept ? '已接受好友申请' : '已拒绝')
    friendStore.requests = friendStore.requests.filter(r => r.id !== requesterId)
    if (accept) await friendStore.fetchFriends()
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

async function deleteFriend(friend) {
  contextMenu.value.visible = false
  try {
    await friendApi.deleteFriend(friend.id)
    friendStore.removeFriend(friend.id)
    if (activeFriend.value?.id === friend.id) activeFriend.value = null
    ElMessage.success('已删除好友')
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

function showFriendMenu(e, friend) {
  contextMenu.value = { visible: true, x: e.clientX, y: e.clientY, friend }
}

document.addEventListener('click', () => { contextMenu.value.visible = false })
</script>

<style scoped>
.friends-view {
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
  gap: 8px;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  flex: 1;
}

.search-box {
  padding: 0 8px 8px;
}

.request-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  border-radius: 4px;
  margin: 0 4px;
}

.request-entry:hover { background-color: var(--bg-hover); color: var(--text-primary); }

.ml-auto { margin-left: auto; }

.request-list {
  background-color: var(--bg-tertiary);
  margin: 0 4px;
  border-radius: 6px;
  padding: 4px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
}

.req-name { flex: 1; font-size: 13px; }

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 8px 0;
}

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

.friend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.1s;
}

.friend-item:hover { background-color: var(--bg-hover); }
.friend-item.active { background-color: var(--bg-hover); }

.avatar-wrap { position: relative; flex-shrink: 0; }

.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--bg-secondary);
}

.status-dot.online { background-color: var(--online); }
.status-dot.offline { background-color: var(--offline); }

.friend-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.friend-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-status {
  font-size: 11px;
  color: var(--text-muted);
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.empty-chat {
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 14px;
}

.context-menu {
  position: fixed;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 4px;
  z-index: 9999;
  min-width: 140px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.menu-item:hover { background-color: var(--bg-hover); }
.menu-item.danger { color: var(--danger); }
.menu-item.danger:hover { background-color: rgba(237,66,69,0.15); }
</style>
