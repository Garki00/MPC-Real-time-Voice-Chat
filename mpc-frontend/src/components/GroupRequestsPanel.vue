<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, ArrowLeft } from '@element-plus/icons-vue'
import { groupApi } from '@/api'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  groupId: { type: Number, required: true }
})

const emit = defineEmits(['requestsUpdated', 'back'])

const requests = ref([])
const loading = ref(false)

const pendingCount = computed(() =>
  requests.value.filter(r => r.status === 'PENDING').length
)

async function loadRequests() {
  loading.value = true
  try {
    requests.value = await groupApi.getPendingRequests(props.groupId)
  } catch {
    requests.value = []
  } finally {
    loading.value = false
  }
}

async function handleRequest(request, approve) {
  try {
    await groupApi.handleJoinRequest(request.id, approve)
    ElMessage.success(approve ? '已同意申请' : '已拒绝申请')
    request.status = approve ? 'APPROVED' : 'REJECTED'
    emit('requestsUpdated', pendingCount.value)
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

function getStatusText(status) {
  switch (status) {
    case 'PENDING': return '待处理'
    case 'APPROVED': return '已同意'
    case 'REJECTED': return '已拒绝'
    default: return status
  }
}

function formatTime(dateStr) {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadRequests)
watch(() => props.groupId, loadRequests)

defineExpose({ loadRequests, pendingCount })
</script>

<template>
  <div class="requests-panel">
    <div class="requests-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle size="small" @click="emit('back')" />
        <span class="header-title">申请处理</span>
      </div>
      <el-badge v-if="pendingCount > 0" :value="pendingCount" type="danger" />
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>

    <div v-else-if="requests.length" class="requests-list">
      <div v-for="request in requests" :key="request.id" class="request-item">
        <div class="request-user">
          <UserAvatar :src="request.userAvatar" :name="request.username" :size="40" />
          <div class="user-info">
            <span class="username">{{ request.username }}</span>
            <span class="user-id">#{{ request.userId }}</span>
          </div>
        </div>
        <div class="request-meta">
          <span class="request-time">{{ formatTime(request.createdAt) }}</span>
        </div>
        <div v-if="request.status === 'PENDING'" class="request-actions">
          <el-button size="small" type="success" :icon="Check" @click="handleRequest(request, true)">
            接受
          </el-button>
          <el-button size="small" type="danger" :icon="Close" @click="handleRequest(request, false)">
            拒绝
          </el-button>
        </div>
        <div v-else class="request-status" :class="`status-${request.status.toLowerCase()}`">
          {{ getStatusText(request.status) }}
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <span>暂无加入申请</span>
    </div>
  </div>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'
export default { components: { Loading } }
</script>

<style scoped>
.requests-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-primary);
}

.requests-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.loading-state,
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-state .el-icon {
  font-size: 24px;
}

.requests-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: var(--bg-secondary);
  border-radius: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--border-color);
}

.request-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-id {
  font-size: 12px;
  color: var(--text-muted);
}

.request-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.request-time {
  font-size: 11px;
  color: var(--text-muted);
}

.request-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.request-status {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 12px;
  flex-shrink: 0;
}

.status-approved {
  color: var(--success);
  background-color: rgba(59, 165, 92, 0.15);
}

.status-rejected {
  color: var(--danger);
  background-color: rgba(237, 66, 69, 0.15);
}
</style>
