<template>
  <div class="voice-panel">
    <div class="voice-panel-header">
      <span class="section-title">语音频道</span>
      <el-tooltip v-if="canManage" content="创建频道">
        <el-button circle :icon="Plus" size="small" @click="showCreate = true" />
      </el-tooltip>
    </div>

    <div class="channel-list">
      <div v-if="!voiceStore.channels.length" class="empty-tip">暂无语音频道</div>
      <div
        v-for="ch in voiceStore.channels"
        :key="ch.id"
        class="channel-item"
        :class="{ active: voiceStore.currentChannelId === ch.id }"
      >
        <div class="channel-row" @click="toggleChannel(ch)">
          <el-icon class="ch-icon"><Headset /></el-icon>
          <span class="ch-name">{{ ch.name }}</span>
          <span class="ch-count">{{ ch.participants?.length || 0 }}/{{ ch.maxCapacity }}</span>
          <el-tooltip v-if="canManage" content="删除频道">
            <el-icon class="ch-delete" @click.stop="deleteChannel(ch)"><Delete /></el-icon>
          </el-tooltip>
        </div>

        <!-- 频道内成员 -->
        <div class="participant-list" v-if="ch.participants?.length">
          <div class="participant-item" v-for="p in ch.participants" :key="p.userId">
            <UserAvatar :src="p.avatar" :name="p.username" :size="22" />
            <span class="p-name">{{ p.username }}</span>
            <el-icon v-if="p.muted" class="muted-icon"><Microphone /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建频道弹窗 -->
    <el-dialog v-model="showCreate" title="创建语音频道" width="320px">
      <el-form :model="createForm" @submit.prevent="createChannel">
        <el-form-item label="频道名称">
          <el-input v-model="createForm.name" placeholder="输入频道名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="createForm.maxCapacity" :min="2" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="createChannel">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Headset, Microphone } from '@element-plus/icons-vue'
import { useVoiceStore } from '@/stores/voice'
import { useWsStore } from '@/stores/ws'
import { voiceApi } from '@/api'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  groupId: { type: Number, required: true },
  canManage: { type: Boolean, default: false }
})

const voiceStore = useVoiceStore()
const ws = useWsStore()

const showCreate = ref(false)
const createLoading = ref(false)
const createForm = ref({ name: '', maxCapacity: 10 })

onMounted(() => voiceStore.fetchChannels(props.groupId))
watch(() => props.groupId, (id) => voiceStore.fetchChannels(id))

async function toggleChannel(ch) {
  if (voiceStore.currentChannelId === ch.id) {
    ws.stopVoice(ch.id)
    return
  }
  if (voiceStore.currentChannelId) {
    ws.stopVoice(voiceStore.currentChannelId)
  }
  const participants = ch.participants?.map(p => p.userId).filter(id => id !== undefined) || []
  await ws.startVoice(ch.id, participants)
}

async function createChannel() {
  if (!createForm.value.name.trim()) return ElMessage.warning('请输入频道名称')
  createLoading.value = true
  try {
    await voiceApi.createChannel(props.groupId, createForm.value)
    await voiceStore.fetchChannels(props.groupId)
    showCreate.value = false
    createForm.value = { name: '', maxCapacity: 10 }
    ElMessage.success('频道创建成功')
  } catch (e) {
    ElMessage.error(e || '创建失败')
  } finally {
    createLoading.value = false
  }
}

async function deleteChannel(ch) {
  try {
    await ElMessageBox.confirm(`确定删除频道「${ch.name}」？`, '删除确认', { type: 'warning' })
    await voiceApi.deleteChannel(ch.id)
    await voiceStore.fetchChannels(props.groupId)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e || '删除失败')
  }
}
</script>

<style scoped>
.voice-panel {
  border-top: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  flex-shrink: 0;
  max-height: 280px;
  overflow-y: auto;
}

.voice-panel-header {
  display: flex;
  align-items: center;
  padding: 8px 12px 4px;
  gap: 6px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.5px;
  flex: 1;
}

.channel-list {
  padding: 0 4px 8px;
}

.empty-tip {
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  padding: 12px 0;
}

.channel-item {
  border-radius: 4px;
  margin-bottom: 2px;
}

.channel-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: background-color 0.1s;
}

.channel-row:hover { background-color: var(--bg-hover); color: var(--text-primary); }
.channel-item.active .channel-row { background-color: var(--bg-hover); color: var(--success); }

.ch-icon { font-size: 14px; flex-shrink: 0; }
.ch-name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ch-count { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }

.ch-delete {
  font-size: 13px;
  color: var(--text-muted);
  opacity: 0;
  transition: opacity 0.1s;
}

.channel-row:hover .ch-delete { opacity: 1; }
.ch-delete:hover { color: var(--danger); }

.participant-list {
  padding: 2px 8px 4px 28px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.p-name { flex: 1; }

.muted-icon {
  font-size: 12px;
  color: var(--warning);
}
</style>
