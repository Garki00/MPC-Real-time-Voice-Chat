import http from './http'

export const authApi = {
  register: (data) => http.post('/auth/register', data),
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout')
}

export const userApi = {
  getMe: () => http.get('/users/me'),
  getUser: (id) => http.get(`/users/${id}`),
  updateMe: (data) => http.put('/users/me', data),
  uploadAvatar: (file) => {
    const form = new FormData()
    form.append('file', file, 'avatar.jpg')
    return http.post('/upload/avatar', form)
  }
}

export const friendApi = {
  getFriends: () => http.get('/friends'),
  getRequests: () => http.get('/friends/requests'),
  sendRequest: (targetId) => http.post(`/friends/request/${targetId}`),
  handleRequest: (requesterId, accept) => http.post(`/friends/request/${requesterId}/handle`, { accept }),
  deleteFriend: (friendId) => http.delete(`/friends/${friendId}`)
}

export const groupApi = {
  create: (data) => http.post('/groups', data),
  dissolve: (groupId) => http.delete(`/groups/${groupId}`),
  update: (groupId, data) => http.put(`/groups/${groupId}`, data),
  getGroup: (groupId) => http.get(`/groups/${groupId}`),
  getMyGroups: () => http.get('/groups/mine'),
  getMembers: (groupId) => http.get(`/groups/${groupId}/members`),
  requestJoin: (groupId) => http.post(`/groups/${groupId}/join`),
  leave: (groupId) => http.post(`/groups/${groupId}/leave`),
  getPendingRequests: (groupId) => http.get(`/groups/${groupId}/requests`),
  handleJoinRequest: (requestId, approve) => http.post(`/groups/requests/${requestId}/handle`, { approve }),
  kick: (groupId, userId) => http.post(`/groups/${groupId}/kick`, { userId }),
  setAdmin: (groupId, userId, grant) => http.post(`/groups/${groupId}/admin?grant=${grant}`, { userId }),
  getAnnouncements: (groupId) => http.get(`/groups/${groupId}/announcements`),
  createAnnouncement: (groupId, data) => http.post(`/groups/${groupId}/announcements`, data),
  uploadGroupAvatar: (blob) => {
    const form = new FormData()
    form.append('file', blob, 'avatar.jpg')
    return http.post('/upload/group-avatar', form)
  }
}

export const chatApi = {
  getPrivateHistory: (friendId) => http.get(`/chat/private/${friendId}`),
  getGroupHistory: (groupId) => http.get(`/chat/group/${groupId}`)
}

export const messageApi = {
  markAsRead: (conversationType, conversationId, messageId) =>
    http.post('/messages/read', null, {
      params: { conversationType, conversationId, messageId }
    }),
  getUnreadCounts: () => http.get('/messages/unread-counts'),
  getUnreadCount: (conversationType, conversationId) =>
    http.get('/messages/unread-count', {
      params: { conversationType, conversationId }
    })
}

export const voiceApi = {
  getChannels: (groupId) => http.get(`/groups/${groupId}/channels`),
  createChannel: (groupId, data) => http.post(`/groups/${groupId}/channels`, data),
  updateChannel: (channelId, data) => http.put(`/channels/${channelId}`, data),
  deleteChannel: (channelId) => http.delete(`/channels/${channelId}`)
}
