import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi } from '@/api'
import { useAuthStore } from './auth'

export const useChatStore = defineStore('chat', () => {
  const privateMessages = ref({})  // { friendId: [...] }
  const groupMessages = ref({})    // { groupId: [...] }

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
    privateMessages.value[key].push(msg)
  }

  function pushGroup(msg) {
    const key = msg.groupId
    if (!groupMessages.value[key]) groupMessages.value[key] = []
    groupMessages.value[key].push(msg)
  }

  return { privateMessages, groupMessages, fetchPrivateHistory, fetchGroupHistory, pushPrivate, pushGroup }
})
