import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from './auth'
import { useChatStore } from './chat'
import { useNotificationStore } from './notification'
import { useFriendStore } from './friend'
import { useVoiceStore } from './voice'
import { useGroupStore } from './group'

export const useWsStore = defineStore('ws', () => {
  const stompClient = ref(null)
  const signalWs = ref(null)
  const connected = ref(false)
  const subscribedGroups = ref(new Set())

  function connect() {
    const auth = useAuthStore()
    if (!auth.token) return

    // STOMP
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws/chat'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        subscribeAll(client, auth)
        // Re-subscribe on initial connect and every reconnect
        subscribedGroups.value.forEach(id => subscribeGroupInternal(id))
      },
      onDisconnect: () => { connected.value = false }
    })
    client.activate()
    stompClient.value = client

    // Signaling WebSocket
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = import.meta.env.DEV ? 'localhost:8080' : window.location.host
    const ws = new WebSocket(`${protocol}//${host}/ws/signal?token=${auth.token}`)
    ws.onmessage = (e) => handleSignal(JSON.parse(e.data))
    signalWs.value = ws
  }

  function subscribeAll(client, auth) {
    const chat = useChatStore()
    const notif = useNotificationStore()

    // private messages
    client.subscribe('/user/queue/messages', (msg) => {
      const payload = JSON.parse(msg.body)
      chat.pushPrivate(payload)
    })

    // notifications
    client.subscribe('/user/queue/notifications', (msg) => {
      const payload = JSON.parse(msg.body)
      notif.push(payload)
      handleNotification(payload)
    })
  }

  function subscribeGroupInternal(groupId) {
    if (!stompClient.value?.connected) return
    const chat = useChatStore()
    const voice = useVoiceStore()
    stompClient.value.subscribe(`/topic/group/${groupId}`, (msg) => {
      chat.pushGroup(JSON.parse(msg.body))
    })
    stompClient.value.subscribe(`/topic/voice/${groupId}`, (msg) => {
      voice.updateChannelState(JSON.parse(msg.body))
    })
  }

  function subscribeGroup(groupId) {
    if (subscribedGroups.value.has(groupId)) return
    subscribedGroups.value.add(groupId)
    subscribeGroupInternal(groupId)
    // If not yet connected, subscribeGroupInternal is a no-op here;
    // onConnect will subscribe all groups in subscribedGroups when it fires.
  }

  function sendPrivate(receiverId, content, type = 'TEXT') {
    stompClient.value?.publish({
      destination: '/app/chat.private',
      body: JSON.stringify({ receiverId, content, type })
    })
  }

  function sendGroup(groupId, content, type = 'TEXT') {
    stompClient.value?.publish({
      destination: '/app/chat.group',
      body: JSON.stringify({ groupId, content, type })
    })
  }

  function joinVoiceChannel(channelId) {
    stompClient.value?.publish({
      destination: '/app/voice.join',
      body: JSON.stringify({ channelId })
    })
  }

  function leaveVoiceChannel(channelId) {
    stompClient.value?.publish({
      destination: '/app/voice.leave',
      body: JSON.stringify({ channelId })
    })
  }

  function sendSignal(to, type, payload, channelId) {
    if (signalWs.value?.readyState === WebSocket.OPEN) {
      signalWs.value.send(JSON.stringify({ type, to, channelId, payload }))
    }
  }

  async function handleSignal(msg) {
    const voice = useVoiceStore()
    const auth = useAuthStore()
    const fromId = msg.from

    if (msg.type === 'offer') {
      if (!voice.localStream) {
        try {
          voice.localStream = await navigator.mediaDevices.getUserMedia({ audio: true })
        } catch (e) {
          console.warn('Cannot get mic for answering:', e)
        }
      }
      if (msg.channelId && !voice.currentChannelId) {
        voice.setCurrentChannel(msg.channelId)
        joinVoiceChannel(msg.channelId)
      }
      const pc = createPeerConnection(fromId)
      await pc.setRemoteDescription(new RTCSessionDescription(msg.payload))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      sendSignal(fromId, 'answer', answer, msg.channelId)
    } else if (msg.type === 'answer') {
      const pc = voice.peerConnections[fromId]
      if (pc) await pc.setRemoteDescription(new RTCSessionDescription(msg.payload))
    } else if (msg.type === 'ice-candidate') {
      const pc = voice.peerConnections[fromId]
      if (pc && msg.payload) await pc.addIceCandidate(new RTCIceCandidate(msg.payload))
    }
  }

  function createPeerConnection(remoteUserId) {
    const voice = useVoiceStore()
    const auth = useAuthStore()
    const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] })

    if (voice.localStream) {
      voice.localStream.getTracks().forEach(t => pc.addTrack(t, voice.localStream))
    }

    pc.onicecandidate = (e) => {
      if (e.candidate) sendSignal(remoteUserId, 'ice-candidate', e.candidate, voice.currentChannelId)
    }

    pc.ontrack = (e) => {
      const audio = document.createElement('audio')
      audio.srcObject = e.streams[0]
      audio.autoplay = true
      audio.dataset.voice = 'true'
      document.body.appendChild(audio)
      audio.play().catch(() => {})
    }

    voice.peerConnections[remoteUserId] = pc
    return pc
  }

  async function startVoice(channelId, otherUserIds) {
    const voice = useVoiceStore()
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    voice.localStream = stream
    voice.setCurrentChannel(channelId)
    joinVoiceChannel(channelId)

    for (const uid of otherUserIds) {
      const pc = createPeerConnection(uid)
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      sendSignal(uid, 'offer', offer, channelId)
    }
  }

  function stopVoice(channelId) {
    const voice = useVoiceStore()
    leaveVoiceChannel(channelId)
    voice.clearVoice()
    document.querySelectorAll('audio[data-voice]').forEach(a => a.remove())
  }

  async function handleNotification(payload) {
    const friend = useFriendStore()
    const group = useGroupStore()
    switch (payload.type) {
      case 'FRIEND_REQUEST':
        friend.addNotifiedRequest(payload.payload)
        break
      case 'GROUP_JOIN_REQUEST':
        if (payload.payload?.groupId) group.fetchPendingRequests(payload.payload.groupId)
        break
      case 'FRIEND_ACCEPTED':
        await friend.fetchFriends()
        break
      case 'GROUP_JOIN_APPROVED':
        await group.fetchGroups()
        if (payload.payload?.groupId) subscribeGroup(payload.payload.groupId)
        break
      case 'GROUP_KICKED':
      case 'GROUP_DISSOLVED':
        if (payload.payload?.groupId) {
          group.removeGroup(payload.payload.groupId)
          subscribedGroups.value.delete(payload.payload.groupId)
        }
        break
    }
  }

  function disconnect() {
    stompClient.value?.deactivate()
    signalWs.value?.close()
    connected.value = false
    subscribedGroups.value.clear()
  }

  return {
    stompClient, signalWs, connected,
    connect, disconnect, subscribeGroup,
    sendPrivate, sendGroup,
    joinVoiceChannel, leaveVoiceChannel,
    startVoice, stopVoice, sendSignal
  }
})
