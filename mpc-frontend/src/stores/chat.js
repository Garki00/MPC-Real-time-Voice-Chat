import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatApi, messageApi } from '@/api'
import { useAuthStore } from './auth'

export const useChatStore = defineStore('chat', () => {
  const privateMessages = ref({})
  const groupMessages = ref({})
  const unreadCounts = ref({})

  const totalUnreadCount = computed(() => {
    return Object.values(unreadCounts.value).reduce((sum, count) => sum + count, 0)
  })

  const privateTotalUnreadCount = computed(() => {
    return Object.entries(unreadCounts.value)
      .filter(([key]) => key.startsWith('private_'))
      .reduce((sum, [, count]) => sum + count, 0)
  })

  const groupTotalUnreadCount = computed(() => {
    return Object.entries(unreadCounts.value)
      .filter(([key]) => key.startsWith('group_'))
      .reduce((sum, [, count]) => sum + count, 0)
  })

  async function fetchPrivateHistory(friendId) {
    const msgs = await chatApi.getPrivateHistory(friendId)
    privateMessages.value[friendId] = msgs
  }

  async function fetchGroupHistory(groupId) {
    const msgs = await chatApi.getGroupHistory(groupId)
    groupMessages.value[groupId] = msgs
  }

  function pushPrivate(msg) {
    const auth = useAuthStore()
    const key = msg.senderId === auth.user?.id ? msg.receiverId : msg.senderId
    if (!privateMessages.value[key]) privateMessages.value[key] = []

    const existingIndex = privateMessages.value[key].findIndex(m => {
      if (m.id != null && msg.id != null && m.id === msg.id) return true

      const isTemporary = typeof m.id === 'string' && m.id.startsWith('temp-')
      if (!isTemporary) return false

      const sameParticipants = String(m.senderId) === String(msg.senderId) &&
        String(m.receiverId) === String(msg.receiverId)
      const sameContent = m.content === msg.content && m.type === msg.type
      const localTime = Date.parse(m.createdAt)
      const serverTime = Date.parse(msg.createdAt)
      const closeInTime = Number.isNaN(localTime) || Number.isNaN(serverTime)
        ? true
        : Math.abs(localTime - serverTime) < 30_000

      return sameParticipants && sameContent && closeInTime
    })

    if (existingIndex !== -1) {
      // 替换临时消息为真实消息
      privateMessages.value[key][existingIndex] = msg
    } else {
      // 检查是否已存在相同的真实消息（防止重复添加）
      const isDuplicate = msg.id && privateMessages.value[key].some(m => m.id === msg.id)
      if (!isDuplicate) {
        privateMessages.value[key].push(msg)
      }
    }

    // 只有收到别人的新消息才增加未读计数
    if (msg.senderId !== auth.user?.id && existingIndex === -1) {
      const unreadKey = `private_${key}`
      unreadCounts.value[unreadKey] = (unreadCounts.value[unreadKey] || 0) + 1
    }
  }

  function pushGroup(msg) {
    const key = msg.groupId
    if (!groupMessages.value[key]) groupMessages.value[key] = []
    groupMessages.value[key].push(msg)

    const auth = useAuthStore()
    if (msg.senderId !== auth.user?.id) {
      const unreadKey = `group_${key}`
      unreadCounts.value[unreadKey] = (unreadCounts.value[unreadKey] || 0) + 1
    }
  }

  async function markAsRead(conversationType, conversationId) {
    const messages = conversationType === 'private'
      ? privateMessages.value[conversationId]
      : groupMessages.value[conversationId]

    if (!messages || messages.length === 0) return

    const lastMessage = messages[messages.length - 1]
    await messageApi.markAsRead(conversationType, conversationId, lastMessage.id)

    const unreadKey = `${conversationType}_${conversationId}`
    unreadCounts.value[unreadKey] = 0
  }

  async function loadUnreadCounts() {
    const counts = await messageApi.getUnreadCounts()
    unreadCounts.value = counts
  }

  return {
    privateMessages,
    groupMessages,
    unreadCounts,
    totalUnreadCount,
    privateTotalUnreadCount,
    groupTotalUnreadCount,
    fetchPrivateHistory,
    fetchGroupHistory,
    pushPrivate,
    pushGroup,
    markAsRead,
    loadUnreadCounts
  }
})
