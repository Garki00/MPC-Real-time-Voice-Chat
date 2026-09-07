import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)

  function push(notif) {
    notifications.value.unshift({ ...notif, id: Date.now(), read: false })
    unreadCount.value++
  }

  function markAllRead() {
    notifications.value.forEach(n => n.read = true)
    unreadCount.value = 0
  }

  function clear() {
    notifications.value = []
    unreadCount.value = 0
  }

  return { notifications, unreadCount, push, markAllRead, clear }
})
