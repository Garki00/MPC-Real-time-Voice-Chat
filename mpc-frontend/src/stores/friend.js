import { defineStore } from 'pinia'
import { ref } from 'vue'
import { friendApi } from '@/api'

export const useFriendStore = defineStore('friend', () => {
  const friends = ref([])
  const requests = ref([])

  async function fetchFriends() {
    friends.value = await friendApi.getFriends()
  }

  async function fetchRequests() {
    requests.value = await friendApi.getRequests()
  }

  function addNotifiedRequest(user) {
    if (!requests.value.find(r => r.id === user.id)) {
      requests.value.unshift(user)
    }
  }

  function removeFriend(friendId) {
    friends.value = friends.value.filter(f => f.id !== friendId)
  }

  return { friends, requests, fetchFriends, fetchRequests, addNotifiedRequest, removeFriend }
})
