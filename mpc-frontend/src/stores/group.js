import { defineStore } from 'pinia'
import { ref } from 'vue'
import { groupApi } from '@/api'

export const useGroupStore = defineStore('group', () => {
  const groups = ref([])
  const currentMembers = ref([])
  const pendingRequests = ref([])

  async function fetchGroups() {
    groups.value = await groupApi.getMyGroups()
  }

  async function fetchMembers(groupId) {
    currentMembers.value = await groupApi.getMembers(groupId)
  }

  async function fetchPendingRequests(groupId) {
    pendingRequests.value = await groupApi.getPendingRequests(groupId)
  }

  function addGroup(group) {
    groups.value.unshift(group)
  }

  function removeGroup(groupId) {
    groups.value = groups.value.filter(g => g.id !== groupId)
  }

  function updateGroup(updated) {
    const idx = groups.value.findIndex(g => g.id === updated.id)
    if (idx !== -1) groups.value[idx] = updated
  }

  return {
    groups, currentMembers, pendingRequests,
    fetchGroups, fetchMembers, fetchPendingRequests,
    addGroup, removeGroup, updateGroup
  }
})
