<template>
  <div class="voice-bar">
    <div class="voice-info">
      <el-icon class="voice-icon" :class="{ muted: voice.muted }"><Microphone /></el-icon>
      <span class="channel-name">{{ currentChannelName }}</span>
      <span class="participant-count">{{ voice.participants.length }} 人</span>
    </div>
    <div class="voice-actions">
      <el-tooltip :content="voice.muted ? '取消静音' : '静音'">
        <el-button
          circle
          size="small"
          :type="voice.muted ? 'warning' : ''"
          :icon="Microphone"
          @click="voice.toggleMute()"
        />
      </el-tooltip>
      <el-tooltip content="离开语音">
        <el-button circle size="small" type="danger" :icon="SwitchButton" @click="leaveVoice" />
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Microphone, SwitchButton } from '@element-plus/icons-vue'
import { useVoiceStore } from '@/stores/voice'
import { useWsStore } from '@/stores/ws'

const voice = useVoiceStore()
const ws = useWsStore()

const currentChannelName = computed(() => {
  const ch = voice.channels.find(c => c.id === voice.currentChannelId)
  return ch?.name || '语音频道'
})

function leaveVoice() {
  if (voice.currentChannelId) {
    ws.stopVoice(voice.currentChannelId)
  }
}
</script>

<style scoped>
.voice-bar {
  position: fixed;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 1000;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  min-width: 260px;
}

.voice-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.voice-icon {
  color: var(--success);
  font-size: 16px;
}

.voice-icon.muted {
  color: var(--warning);
}

.channel-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.participant-count {
  font-size: 12px;
  color: var(--text-muted);
}

.voice-actions {
  display: flex;
  gap: 6px;
}
</style>
