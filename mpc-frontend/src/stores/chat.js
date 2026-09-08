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

    // 检查是否是重复消息（去重逻辑）
    // 如果存在相同 senderId、receiverId、content 和相近时间的消息，可能是重复
    const existingIndex = privateMessages.value[key].findIndex(m => {
      if (typeof m.id === 'string' && m.id.startsWith('temp-')) {
        // 临时消息，检查是否匹配服务器返回的消息
        return m.senderId === msg.senderId &&
               m.receiverId === msg.receiverId &&
               m.content === msg.content &&
               Math.abs(new Date(m.createdAt) - new Date(msg.createdAt)) < 5000 // 5秒内
      }
      // 检查是否是完全相同的消息（通过ID）
      return m.id === msg.id
    })

    if (existingIndex !== -1) {
      // 替换临时消息为真实消息
      privateMessages.value[key][existingIndex] = msg
    } else {
      // 新消息
      privateMessages.value[key].push(msg)
    }

    if (msg.senderId !== auth.user?.id) {
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
