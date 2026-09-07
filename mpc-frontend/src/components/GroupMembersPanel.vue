<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { groupApi } from '@/api'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  groupId: { type: Number, required: true }
})

const members = ref([])

const owners = computed(() => members.value.filter(m => m.role === 'OWNER'))
const admins = computed(() => members.value.filter(m => m.role === 'ADMIN'))
const regularMembers = computed(() => members.value.filter(m => m.role === 'MEMBER'))

async function load() {
  try {
    members.value = await groupApi.getMembers(props.groupId)
  } catch {}
}

onMounted(load)
watch(() => props.groupId, load)
</script>

<template>
  <div class="members-panel">
    <div class="members-header">成员</div>
    <div class="members-scroll">
      <template v-if="owners.length">
        <div class="role-label">群主</div>
        <div v-for="m in owners" :key="m.userId" class="member-item">
          <div class="member-avatar">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <span class="status-dot" :class="m.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <span class="member-name" :class="{ offline: m.status !== 'ONLINE' }">{{ m.username }}</span>
        </div>
      </template>
      <template v-if="admins.length">
        <div class="role-label">管理员</div>
        <div v-for="m in admins" :key="m.userId" class="member-item">
          <div class="member-avatar">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <span class="status-dot" :class="m.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <span class="member-name" :class="{ offline: m.status !== 'ONLINE' }">{{ m.username }}</span>
        </div>
      </template>
      <template v-if="regularMembers.length">
        <div class="role-label">成员</div>
        <div v-for="m in regularMembers" :key="m.userId" class="member-item">
          <div class="member-avatar">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <span class="status-dot" :class="m.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <span class="member-name" :class="{ offline: m.status !== 'ONLINE' }">{{ m.username }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.members-panel {
  width: 200px;
  background-color: var(--bg-secondary);
  border-left: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.members-header {
  padding: 16px 12px 8px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.members-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 12px;
}

.role-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  padding: 8px 4px 4px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border-radius: 4px;
}

.member-item:hover {
  background-color: var(--bg-hover);
}

.member-avatar {
  position: relative;
  flex-shrink: 0;
}

.status-dot {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--bg-secondary);
}

.status-dot.online { background-color: var(--online); }
.status-dot.offline { background-color: var(--offline); }

.member-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-name.offline { color: var(--text-muted); }
</style>
