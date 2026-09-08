<template>
  <div class="main-layout">
    <!-- 左侧图标导航栏 -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <!-- 个人头像/设置 -->
        <div class="nav-item" @click="showSettings = true" title="个人设置">
          <UserAvatar :src="auth.user?.avatar" :name="auth.user?.username" :size="36" />
        </div>

        <!-- 好友 -->
        <el-badge :value="chatStore.privateTotalUnreadCount || ''" :hidden="!chatStore.privateTotalUnreadCount" :max="99">
          <div class="nav-item" :class="{ active: route.path.startsWith('/friends') }"
            @click="router.push('/friends')" title="好友">
            <el-icon :size="22"><User /></el-icon>
          </div>
        </el-badge>

        <!-- 通知 -->
        <el-badge :value="notifStore.unreadCount || ''" :hidden="!notifStore.unreadCount">
          <div class="nav-item" :class="{ active: route.path === '/notifications' }"
            @click="router.push('/notifications')" title="通知">
            <el-icon :size="22"><Bell /></el-icon>
          </div>
        </el-badge>
      </div>

      <!-- 群组头像列表 -->

     <!-- 新增包装器 -->
      <div class="group-list-wrapper">
        <div class="group-list" @wheel.stop>
          <div
            v-for="group in visibleGroups"
            :key="group.id"
            class="group-avatar-wrapper"
            :title="group.name"
            @click="router.push(`/groups/${group.id}`)"
          >
            <div
              class="group-avatar-item"
              :class="{ active: route.params.groupId == group.id }"
            >
              <div v-if="group.avatar" class="group-avatar-img">
                <img :src="group.avatar" :alt="group.name" />
              </div>
              <div v-else class="group-avatar-fallback">{{ group.name.charAt(0).toUpperCase() }}</div>

              <!-- 未读徽章 -->
              <div v-if="getGroupUnreadCount(group.id)" class="group-unread-badge">
                {{ getGroupUnreadCount(group.id) > 99 ? '99+' : getGroupUnreadCount(group.id) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-bottom">
        <!-- 创建/加入群组 -->
        <div class="nav-item add-btn" @click="showCreateJoin = true" title="创建或加入群组">
          <el-icon :size="22"><Plus /></el-icon>
        </div>
      </div>
    </aside>

    <!-- 中间 + 右侧内容区 -->
    <div class="content-area">
      <router-view />
    </div>

    <!-- 语音通话悬浮控制栏 -->
    <VoiceBar v-if="voice.currentChannelId" />

    <!-- 个人设置弹窗 -->
    <ProfileDialog v-model="showSettings" />

    <!-- 创建或加入群组弹窗 -->
    <CreateOrJoinGroupDialog v-model="showCreateJoin" @created="onGroupCreated" @joined="onGroupJoined" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Bell, Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useVoiceStore } from '@/stores/voice'
import { useWsStore } from '@/stores/ws'
import { useFriendStore } from '@/stores/friend'
import { useGroupStore } from '@/stores/group'
import { useChatStore } from '@/stores/chat'
import UserAvatar from '@/components/UserAvatar.vue'
import VoiceBar from '@/components/VoiceBar.vue'
import ProfileDialog from '@/components/ProfileDialog.vue'
import CreateOrJoinGroupDialog from '@/components/CreateOrJoinGroupDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifStore = useNotificationStore()
const voice = useVoiceStore()
const ws = useWsStore()
const friendStore = useFriendStore()
const groupStore = useGroupStore()
const chatStore = useChatStore()

const showSettings = ref(false)
const showCreateJoin = ref(false)

const MAX_GROUPS = 8
const visibleGroups = computed(() => groupStore.groups.slice(0, MAX_GROUPS))

function getGroupUnreadCount(groupId) {
  const key = `group_${groupId}`
  return chatStore.unreadCounts[key] || 0
}

function onGroupCreated(group) {
  groupStore.addGroup(group)
  router.push(`/groups/${group.id}`)
}

function onGroupJoined() {
  groupStore.fetchGroups()
}

onMounted(async () => {
  if (!ws.connected) ws.connect()
  await Promise.all([
    auth.refreshUser(),
    friendStore.fetchFriends(),
    groupStore.fetchGroups(),
    chatStore.loadUnreadCounts()
  ])
})

onUnmounted(() => {
  ws.disconnect()
})
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.sidebar {
  width: var(--sidebar-width);
  background-color: var(--bg-primary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  flex-shrink: 0;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.group-list-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end; /* 内容从底部开始 */
  min-height: 0; /* 防止flex溢出 */
  width: 100%;
  padding: 4px 0;
}

/* .group-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  overflow-y: auto;
  padding: 8px 0;
  scrollbar-width: none;
  width: 100%;
} */

/* 修改后的群组列表 */
.group-list {
  display: flex;
  flex-direction: column-reverse; /* 从下往上排列 */
  align-items: center;
  gap: 6px;
  max-height: 50%; /* 不超过父容器的一半 */
  overflow-y: auto;
  padding: 4px 0;
  scrollbar-width: none;
  width: 100%;
  
  /* 当内容不足时，从底部开始排列 */
  &:not(:has(.group-avatar-item)) {
    justify-content: flex-end;
  }
}

.group-list::-webkit-scrollbar {
  display: none;
}

.group-avatar-wrapper {
  position: relative;
  cursor: pointer;
}

.group-avatar-item {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  transition: border-radius 0.15s;
  position: relative;
}

.group-unread-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background-color: var(--accent);
  color: #fff;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 5px;
  min-width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-shadow: 0 1px 3px rgba(0,0,0,0.3);
  z-index: 1;
}

.group-avatar-item:hover {
  border-radius: 16px;
}

.group-avatar-wrapper:hover .group-avatar-item {
  border-radius: 16px;
}

.group-avatar-item.active {
  border-radius: 16px;
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.group-avatar-img {
  width: 100%;
  height: 100%;
}

.group-avatar-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.group-avatar-fallback {
  width: 100%;
  height: 100%;
  background-color: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

/* .sidebar-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
} */

.sidebar-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  flex-shrink: 0; /* 防止底部被压缩 */
  border-top: 1px solid var(--border-color);
  width: 100%;
}

.nav-item {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
  transition: background-color 0.15s, color 0.15s, border-radius 0.15s;
}

.nav-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
  border-radius: 16px;
}

.nav-item.active {
  background-color: var(--accent);
  color: #fff;
  border-radius: 16px;
}

.add-btn {
  border: 2px dashed var(--border-color);
  color: var(--text-muted);
}

.add-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background-color: transparent;
  border-radius: 12px;
}

.content-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}
</style>
