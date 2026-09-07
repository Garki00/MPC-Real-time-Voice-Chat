<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { groupApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  groupId: { type: Number, required: true },
  currentUserRole: { type: String, required: true }
})

const emit = defineEmits(['back'])

const auth = useAuthStore()
const announcements = ref([])
const showCreateDialog = ref(false)
const newAnnouncement = ref('')
const loading = ref(false)
const createLoading = ref(false)

const isAdmin = computed(() =>
  props.currentUserRole === 'OWNER' || props.currentUserRole === 'ADMIN'
)

async function loadAnnouncements() {
  loading.value = true
  try {
    announcements.value = await groupApi.getAnnouncements(props.groupId)
  } catch {
    announcements.value = []
  } finally {
    loading.value = false
  }
}

async function createAnnouncement() {
  if (!newAnnouncement.value.trim()) return ElMessage.warning('请输入通知内容')
  createLoading.value = true
  try {
    await groupApi.createAnnouncement(props.groupId, { content: newAnnouncement.value.trim() })
    ElMessage.success('通知已发送')
    showCreateDialog.value = false
    newAnnouncement.value = ''
    await loadAnnouncements()
  } catch (e) {
    ElMessage.error(e || '发送失败')
  } finally {
    createLoading.value = false
  }
}

function formatTime(dateStr) {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

onMounted(loadAnnouncements)
watch(() => props.groupId, loadAnnouncements)
</script>

<template>
  <div class="announcements-panel">
    <div class="announcements-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle size="small" @click="emit('back')" />
        <span class="header-title">群组通知</span>
      </div>
      <el-button v-if="isAdmin" type="primary" size="small" :icon="Plus" @click="showCreateDialog = true">
        发送通知
      </el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>

    <div v-else-if="announcements.length" class="announcements-list">
      <div v-for="announcement in announcements" :key="announcement.id" class="announcement-item">
        <div class="announcement-header">
          <span class="author-name">{{ announcement.authorName }}</span>
          <span class="announcement-time">{{ formatTime(announcement.createdAt) }}</span>
        </div>
        <div class="announcement-content">{{ announcement.content }}</div>
      </div>
    </div>

    <div v-else class="empty-state">
      <span>暂无群组通知</span>
    </div>

    <!-- 发送通知弹窗 -->
    <el-dialog v-model="showCreateDialog" title="发送通知" width="460px">
      <el-input
        v-model="newAnnouncement"
        type="textarea"
        :rows="6"
        placeholder="输入通知内容"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="createAnnouncement">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'
export default { components: { Loading } }
</script>

<style scoped>
.announcements-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-primary);
}

.announcements-header {
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

.announcements-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
}

.announcement-item {
  padding: 16px;
  background-color: var(--bg-secondary);
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid var(--border-color);
}

.announcement-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.author-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.announcement-time {
  font-size: 12px;
  color: var(--text-muted);
}

.announcement-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
