<script setup>
import { ref, watch } from 'vue'
import { userApi } from '@/api'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  modelValue: Boolean,
  userId: Number
})

const emit = defineEmits(['update:modelValue'])

const userInfo = ref(null)
const loading = ref(false)

async function load() {
  if (!props.userId) return
  loading.value = true
  try {
    userInfo.value = await userApi.getUser(props.userId)
  } catch {
    userInfo.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val) load()
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="用户信息"
    width="360px"
  >
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    <div v-else-if="userInfo" class="user-info-content">
      <div class="avatar-section">
        <UserAvatar :src="userInfo.avatar" :name="userInfo.username" :size="80" />
      </div>
      <div class="info-row">
        <span class="info-label">用户名</span>
        <span class="info-value">{{ userInfo.username }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">邮箱</span>
        <span class="info-value">{{ userInfo.email }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">ID</span>
        <span class="info-value muted">#{{ userInfo.id }}</span>
      </div>
    </div>
    <div v-else class="error-state">加载失败</div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'
export default { components: { Loading } }
</script>

<style scoped>
.loading-state,
.error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-state .el-icon {
  font-size: 24px;
}

.user-info-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.info-row {
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
}

.info-label {
  width: 56px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-value {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  word-break: break-all;
}

.info-value.muted {
  color: var(--text-muted);
}
</style>
