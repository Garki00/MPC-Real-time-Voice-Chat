<template>
  <div class="notifications-view">
    <div class="page-header">
      <span class="page-title">通知</span>
      <el-button v-if="notifStore.notifications.length" link size="small" @click="notifStore.markAllRead()">
        全部已读
      </el-button>
    </div>

    <div class="notif-list">
      <div v-if="!notifStore.notifications.length" class="empty-state">
        <el-icon :size="48" color="var(--text-muted)"><Bell /></el-icon>
        <p>暂无通知</p>
      </div>

      <div
        v-for="notif in notifStore.notifications"
        :key="notif.id"
        class="notif-item"
        :class="{ unread: !notif.read }"
        @click="handleNotif(notif)"
      >
        <div class="notif-icon" :class="notif.type.toLowerCase()">
          <el-icon>
            <component :is="notifIcon(notif.type)" />
          </el-icon>
        </div>
        <div class="notif-content">
          <div class="notif-text">{{ notifText(notif) }}</div>
          <div class="notif-time">{{ formatTime(notif.createdAt) }}</div>
        </div>
        <div class="unread-dot" v-if="!notif.read" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, User, ChatLineRound, InfoFilled } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'

const notifStore = useNotificationStore()
const router = useRouter()

onMounted(() => {
  notifStore.markAllRead()
})

function notifIcon(type) {
  const map = {
    FRIEND_REQUEST: User,
    FRIEND_ACCEPTED: User,
    GROUP_JOIN_REQUEST: ChatLineRound,
    GROUP_JOIN_APPROVED: ChatLineRound,
    GROUP_JOIN_REJECTED: ChatLineRound,
    GROUP_KICKED: InfoFilled,
    GROUP_DISSOLVED: InfoFilled
  }
  return map[type] || Bell
}

function notifText(notif) {
  const p = notif.payload || {}
  switch (notif.type) {
    case 'FRIEND_REQUEST': return `${p.username || '用户'} 向你发送了好友申请`
    case 'FRIEND_ACCEPTED': return `${p.username || '用户'} 接受了你的好友申请`
    case 'GROUP_JOIN_REQUEST': return `${p.username || '用户'} 申请加入群组「${p.groupName || ''}」`
    case 'GROUP_JOIN_APPROVED': return `你加入群组「${p.groupName || ''}」的申请已通过`
    case 'GROUP_JOIN_REJECTED': return `你加入群组「${p.groupName || ''}」的申请被拒绝`
    case 'GROUP_KICKED': return `你已被移出群组「${p.groupName || ''}」`
    case 'GROUP_DISSOLVED': return `群组「${p.groupName || ''}」已解散`
    default: return notif.type
  }
}

function handleNotif(notif) {
  const p = notif.payload || {}
  if (notif.type === 'FRIEND_REQUEST' || notif.type === 'FRIEND_ACCEPTED') {
    router.push('/friends')
  } else if (notif.type.startsWith('GROUP') && p.groupId) {
    router.push(`/groups/${p.groupId}`)
  }
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.notifications-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.page-header {
  display: flex;
  align-items: center;
  padding: 20px 24px 12px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  flex-shrink: 0;
}

.page-title {
  font-weight: 600;
  font-size: 16px;
  flex: 1;
}

.notif-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  max-width: 640px;
  width: 100%;
  margin: 0 auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 0;
  color: var(--text-muted);
  font-size: 14px;
}

.notif-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  cursor: pointer;
  transition: background-color 0.1s;
  position: relative;
}

.notif-item:hover { background-color: var(--bg-secondary); }
.notif-item.unread { background-color: var(--bg-tertiary); }

.notif-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background-color: var(--bg-hover);
  color: var(--text-secondary);
}

.notif-icon.friend_request,
.notif-icon.friend_accepted { background-color: rgba(88, 101, 242, 0.15); color: var(--accent); }

.notif-icon.group_join_request,
.notif-icon.group_join_approved { background-color: rgba(59, 165, 92, 0.15); color: var(--success); }

.notif-icon.group_join_rejected,
.notif-icon.group_kicked,
.notif-icon.group_dissolved { background-color: rgba(237, 66, 69, 0.15); color: var(--danger); }

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.4;
}

.notif-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 3px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--accent);
  flex-shrink: 0;
}
</style>
