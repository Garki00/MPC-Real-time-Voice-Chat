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
    <div class="input-area" :class="{ expanded: isFullscreen }">
      <div class="input-box">
        <textarea
          ref="textareaRef"
          v-model="inputText"
          class="chat-textarea"
          rows="1"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="send"
          @input="autoResize"
        />
        <div class="input-icons">
          <button
            class="icon-btn send-btn"
            :class="{ active: inputText.trim() }"
            :disabled="!inputText.trim()"
            title="发送"
            @click="send"
          >
            <svg viewBox="0 0 24 24"><polyline points="9 10 4 15 9 20" /><path d="M20 4v7a4 4 0 0 1-4 4H4" /></svg>
          </button>

          <div class="icon-wrap">
            <button class="icon-btn" title="表情" @click.stop="toggleEmoji">
              <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" /><path d="M8 14s1.5 2 4 2 4-2 4-2" /><line x1="9" y1="9" x2="9" y2="9" /><line x1="15" y1="9" x2="15" y2="9" /></svg>
            </button>
            <div v-if="showEmoji" class="popover emoji-popover" @click.stop>
              <button
                v-for="e in emojiList"
                :key="e"
                type="button"
                class="emoji-item"
                @click="insertEmoji(e)"
              >{{ e }}</button>
            </div>
          </div>

          <div class="icon-wrap">
            <button class="icon-btn" title="添加附件" @click.stop="toggleAdd">
              <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="16" /><line x1="8" y1="12" x2="16" y2="12" /></svg>
            </button>
            <div v-if="showAdd" class="popover add-popover" @click.stop>
              <div class="popover-item" @click="chooseAttachment">添加附件</div>
            </div>
          </div>

          <button class="icon-btn" title="展开输入框" @click="toggleExpand">
            <svg class="expand-icon" :class="{ rotated: isFullscreen }" viewBox="0 0 24 24"><line x1="7" y1="17" x2="17" y2="7" /><polyline points="7 7 17 7 17 17" /></svg>
          </button>
        </div>
      </div>
      <input ref="fileInputRef" type="file" class="hidden-file-input" @change="onFileSelected" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  target: { type: Object, required: true },
  messages: { type: Array, default: () => [] },
  type: { type: String, default: 'private' }
})

const emit = defineEmits(['send'])
const auth = useAuthStore()
const chat = useChatStore()
const inputText = ref('')
const listRef = ref(null)
const textareaRef = ref(null)
const fileInputRef = ref(null)
const showEmoji = ref(false)
const showAdd = ref(false)
const isFullscreen = ref(false)

const emojiList = ['😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃','😉','😊','😇','🥰','😍','🤩','😘','😗','😚','😙','🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','🤨','😐','😑','😶','😏','😒','🙄','😬','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕','🤢','🤮','🤧','🥵','🥶','🥴','😵','🤯','🤠','🥳','🥸','😎','🤓','🧐','😕','😟','🙁','☹️','😮','😯','😲','😳','🥺','😦','😧','😨','😰','😥','😢','😭','😱','😖','😣','😞','😓','😩','😫','🥱','😤','😡','😠','🤬','👍','👎','👌','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','👇','☝️','👏','🙌','👐','🤲','🤝','🙏','💪','🦾','🦿','🦵','🦶','👂','🦻','👃','🧠','🫀','🫁','🦷','🦴','👀','👁️','👅','👄','💋','🩸']

watch(() => props.messages.length, () => {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
    markCurrentAsRead()
  })
}, { immediate: true })

function markCurrentAsRead() {
  if (props.messages.length === 0) return
  const conversationId = props.type === 'private' ? props.target.id : props.target.id
  chat.markAsRead(props.type, conversationId)
}

onMounted(() => {
  markCurrentAsRead()
  document.addEventListener('click', closePopovers)
})

onUnmounted(() => {
  markCurrentAsRead()
  document.removeEventListener('click', closePopovers)
})

function closePopovers() {
  showEmoji.value = false
  showAdd.value = false
}

function send() {
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text)
  inputText.value = ''
  resetTextareaHeight()
}

function autoResize() {
  if (!textareaRef.value) return
  textareaRef.value.style.height = 'auto'
  const scrollHeight = textareaRef.value.scrollHeight
  const maxHeight = isFullscreen.value ? 400 : 120
  textareaRef.value.style.height = Math.min(scrollHeight, maxHeight) + 'px'
}

function resetTextareaHeight() {
  if (!textareaRef.value) return
  textareaRef.value.style.height = 'auto'
}

function toggleEmoji() {
  showEmoji.value = !showEmoji.value
  showAdd.value = false
}

function toggleAdd() {
  showAdd.value = !showAdd.value
  showEmoji.value = false
}

function toggleExpand() {
  isFullscreen.value = !isFullscreen.value
  nextTick(() => autoResize())
}

function insertEmoji(emoji) {
  const textarea = textareaRef.value
  if (!textarea) return
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const text = inputText.value
  inputText.value = text.substring(0, start) + emoji + text.substring(end)
  showEmoji.value = false
  nextTick(() => {
    textarea.focus()
    const newPos = start + emoji.length
    textarea.setSelectionRange(newPos, newPos)
    autoResize()
  })
}

function chooseAttachment() {
  showAdd.value = false
  fileInputRef.value?.click()
}

function onFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  console.log('File selected:', file.name)
  // TODO: implement file upload
  e.target.value = ''
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
  position: relative;
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
  padding: 8px 12px;
  background-color: var(--bg-secondary);
  flex-shrink: 0;
  transition: all 0.2s;
}

.input-area.expanded {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 50%;
  z-index: 10;
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.input-area.expanded .input-box {
  flex: 1;
  align-items: flex-start;
}

.input-area.expanded .chat-textarea {
  max-height: none;
  height: 100%;
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px 10px;
  transition: border-color 0.2s;
}

.input-box:focus-within {
  border-color: var(--accent);
}

.chat-textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  resize: none;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
  min-height: 22px;
  max-height: 120px;
  overflow-y: auto;
  padding: 0;
}

.expanded .chat-textarea {
  max-height: 400px;
}

.chat-textarea::placeholder {
  color: var(--text-muted);
}

.input-icons {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.icon-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.15s;
  padding: 0;
}

.icon-btn:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.icon-btn:disabled:hover {
  background: transparent;
  color: var(--text-secondary);
}

.icon-btn.send-btn.active {
  color: var(--accent);
}

.icon-btn svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.expand-icon {
  transition: transform 0.2s;
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.icon-wrap {
  position: relative;
}

.popover {
  position: absolute;
  bottom: 100%;
  right: 0;
  margin-bottom: 8px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 100;
}

.emoji-popover {
  width: 300px;
  max-height: 240px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
  display: grid;
  grid-template-columns: repeat(8, 32px);
  gap: 4px;
}

.emoji-item {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 20px;
  padding: 0;
  transition: background-color 0.15s;
}

.emoji-item:hover {
  background-color: var(--bg-hover);
}

.add-popover {
  min-width: 140px;
  padding: 4px;
}

.popover-item {
  padding: 10px 14px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: var(--text-primary);
  transition: background-color 0.15s;
}

.popover-item:hover {
  background-color: var(--bg-hover);
}

.hidden-file-input {
  display: none;
}
</style>
