import { defineStore } from 'pinia'
import { ref } from 'vue'
import { voiceApi } from '@/api'

export const useVoiceStore = defineStore('voice', () => {
  const channels = ref([])           // current group's channels
  const currentChannelId = ref(null)
  const participants = ref([])       // users in current channel
  const peerConnections = ref({})    // { userId: RTCPeerConnection }
  const localStream = ref(null)
  const muted = ref(false)

  async function fetchChannels(groupId) {
    channels.value = await voiceApi.getChannels(groupId)
  }

  function updateChannelState(channelInfo) {
    const idx = channels.value.findIndex(c => c.id === channelInfo.id)
    if (idx !== -1) channels.value[idx] = channelInfo
    else channels.value.push(channelInfo)
    if (channelInfo.id === currentChannelId.value) {
      participants.value = channelInfo.participants
    }
  }

  function setCurrentChannel(channelId) {
    currentChannelId.value = channelId
  }

  function clearVoice() {
    Object.values(peerConnections.value).forEach(pc => pc.close())
    peerConnections.value = {}
    if (localStream.value) {
      localStream.value.getTracks().forEach(t => t.stop())
      localStream.value = null
    }
    currentChannelId.value = null
    participants.value = []
    muted.value = false
  }

  function toggleMute() {
    muted.value = !muted.value
    if (localStream.value) {
      localStream.value.getAudioTracks().forEach(t => { t.enabled = !muted.value })
    }
  }

  return {
    channels, currentChannelId, participants, peerConnections,
    localStream, muted,
    fetchChannels, updateChannelState, setCurrentChannel, clearVoice, toggleMute
  }
})
