<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Setting } from '@element-plus/icons-vue'
import { useGroupStore } from '@/stores/group'
import { useChatStore } from '@/stores/chat'
import { useWsStore } from '@/stores/ws'
import VoiceChannelPanel from '@/components/VoiceChannelPanel.vue'
import ChatWindow from '@/components/ChatWindow.vue'
import GroupMembersPanel from '@/components/GroupMembersPanel.vue'
import GroupSettingsModal from '@/components/GroupSettingsModal.vue'

const route = useRoute()
const groupStore = useGroupStore()
const chatStore = useChatStore()
const ws = useWsStore()
const showSettings = ref(false)

const groupId = computed(() => Number(route.params.groupId))
const group = computed(() => groupStore.groups.find(g => g.id === groupId.value))
const messages = computed(() => chatStore.groupMessages[groupId.value] || [])

async function loadGroup(id) {
  if (!id) return
  ws.subscribeGroup(id)
  await chatStore.fetchGroupHistory(id)
}

function sendGroupMsg(content) {
  ws.sendGroup(groupId.value, content)
}

function onGroupUpdated(updated) {
  groupStore.updateGroup(updated)
}

onMounted(() => loadGroup(groupId.value))
watch(groupId, (id) => { showSettings.value = false; loadGroup(id) })
</script>

<template>
  <div class="group-page">
    <!-- Left panel: group info + voice channels -->
    <aside class="group-left-panel">
      <div class="group-header">
        <span class="group-name">{{ group?.name ?? '群组' }}</span>
        <button class="settings-btn" title="群组设置" @click="showSettings = true">
          <el-icon :size="16"><Setting /></el-icon>
        </button>
      </div>
      <div class="voice-panel-wrap">
        <VoiceChannelPanel :group-id="groupId" />
      </div>
    </aside>

    <!-- Center: chat -->
    <div class="group-center">
      <ChatWindow
        v-if="group"
        :target="group"
        :messages="messages"
        type="group"
        @send="sendGroupMsg"
      />
    </div>

    <!-- Right panel: members -->
    <GroupMembersPanel :group-id="groupId" />

    <!-- Settings modal -->
    <GroupSettingsModal
      v-model="showSettings"
      :group-id="groupId"
      @updated="onGroupUpdated"
    />
  </div>
</template>

<style scoped>
.group-page {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.group-left-panel {
  width: 240px;
  flex-shrink: 0;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 48px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.group-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.settings-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: background-color 0.15s, color 0.15s;
}

.settings-btn:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.voice-panel-wrap {
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
}

.group-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}
</style>
