<template>
  <div class="chat-window">
    <!-- 消息列表 -->
    <div class="message-list" ref="listRef">
      <div v-if="!messages.length" class="empty-tip">暂无消息</div>
      <div
        v-for="(msg, i) in messages"
        :key="i"
        class="message-item"
        :class="{ self: msg.senderId === auth.user?.id }"
      >
        <UserAvatar
          v-if="msg.senderId !== auth.user?.id"
          :src="msg.senderAvatar"
          :name="msg.senderName"
          :size="34"
          class="msg-avatar"
        />
        <div class="msg-body">
          <div class="msg-meta" v-if="type === 'group' && msg.senderId !== auth.user?.id">
            <span class="msg-sender">{{ msg.senderName }}</span>
          </div>
          <div class="msg-bubble">{{ msg.content }}</div>
          <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        <UserAvatar
          v-if="msg.senderId === auth.user?.id"
          :src="auth.user?.avatar"
          :name="auth.user?.username"
          :size="34"
          class="msg-avatar"
        />
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        resize="none"
        @keydown.enter.exact.prevent="send"
      />
      <div class="input-actions">
        <el-button type="primary" size="small" :disabled="!inputText.trim()" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  target: { type: Object, required: true },
  messages: { type: Array, default: () => [] },
  type: { type: String, default: 'private' }
})

const emit = defineEmits(['send'])
const auth = useAuthStore()
const inputText = ref('')
const listRef = ref(null)

watch(() => props.messages.length, () => {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}, { immediate: true })

function send() {
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text)
  inputText.value = ''
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' +
    d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-tip {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  padding: 32px 0;
}

.message-item {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-item.self {
  justify-content: flex-end;
}

.msg-avatar { flex-shrink: 0; }

.msg-body {
  display: flex;
  flex-direction: column;
  max-width: 60%;
}

.message-item.self .msg-body { align-items: flex-end; }

.msg-meta {
  margin-bottom: 2px;
}

.msg-sender {
  font-size: 11px;
  color: var(--text-muted);
}

.msg-bubble {
  background-color: var(--bg-tertiary);
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
  color: var(--text-primary);
}

.message-item.self .msg-bubble {
  background-color: var(--accent);
  color: #fff;
}

.msg-time {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 3px;
}

.input-area {
  border-top: 1px solid var(--border-color);
  padding: 12px;
  background-color: var(--bg-secondary);
  flex-shrink: 0;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.input-area :deep(.el-textarea__inner) {
  background-color: var(--bg-tertiary);
  border-color: var(--border-color);
  color: var(--text-primary);
  resize: none;
}
</style>
