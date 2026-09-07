<template>
  <div class="group-info-panel">
    <div class="panel-header">
      <span class="panel-title">群组信息</span>
      <el-button circle :icon="Close" size="small" @click="$emit('close')" />
    </div>

    <div class="panel-body">
      <!-- 基本信息 -->
      <div class="info-section">
        <div class="group-icon-large">{{ group.name.charAt(0).toUpperCase() }}</div>
        <div class="group-name-text">{{ group.name }}</div>
        <div class="group-id-text">ID: {{ group.id }}</div>
      </div>

      <!-- 公告 -->
      <div class="section">
        <div class="section-header">
          <span class="section-title">群公告</span>
          <el-button v-if="canManage" link size="small" @click="editAnnouncement = true">编辑</el-button>
        </div>
        <div class="announcement-text" v-if="group.announcement">{{ group.announcement }}</div>
        <div class="empty-tip" v-else>暂无公告</div>
      </div>

      <!-- 成员列表 -->
      <div class="section">
        <div class="section-header">
          <span class="section-title">成员 ({{ members.length }})</span>
        </div>
        <div class="member-list">
          <div class="member-item" v-for="m in members" :key="m.userId">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <div class="member-info">
              <span class="member-name">{{ m.username }}</span>
              <span class="member-role" :class="m.role.toLowerCase()">{{ roleLabel(m.role) }}</span>
            </div>
            <div class="member-actions" v-if="canManage && m.userId !== auth.user?.id">
              <el-tooltip v-if="isOwner && m.role === 'MEMBER'" content="设为管理员">
                <el-button link size="small" @click="setAdmin(m.userId, true)">
                  <el-icon><UserFilled /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip v-if="isOwner && m.role === 'ADMIN'" content="撤销管理员">
                <el-button link size="small" @click="setAdmin(m.userId, false)">
                  <el-icon><User /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="踢出群组">
                <el-button link size="small" type="danger" @click="kickMember(m.userId)">
                  <el-icon><Remove /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>

      <!-- 加入申请 (仅管理员可见) -->
      <div class="section" v-if="canManage && groupStore.pendingRequests.length">
        <div class="section-header">
          <span class="section-title">加入申请</span>
          <el-badge :value="groupStore.pendingRequests.length" />
        </div>
        <div class="request-list">
          <div class="request-item" v-for="req in groupStore.pendingRequests" :key="req.id">
            <UserAvatar :src="req.avatar" :name="req.username" :size="28" />
            <span class="req-name">{{ req.username }}</span>
            <el-button type="primary" size="small" @click="handleJoin(req.id, true)">通过</el-button>
            <el-button size="small" @click="handleJoin(req.id, false)">拒绝</el-button>
          </div>
        </div>
      </div>

      <!-- 危险操作 -->
      <div class="section danger-section">
        <el-button v-if="isOwner" type="danger" plain size="small" @click="dissolveGroup">解散群组</el-button>
        <el-button v-else type="danger" plain size="small" @click="leaveGroup">退出群组</el-button>
      </div>
    </div>

    <!-- 编辑公告弹窗 -->
    <el-dialog v-model="editAnnouncement" title="编辑群公告" width="360px" append-to-body>
      <el-input v-model="announcementText" type="textarea" :rows="4" placeholder="输入群公告内容" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="editAnnouncement = false">取消</el-button>
        <el-button type="primary" :loading="announcementLoading" @click="saveAnnouncement">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, UserFilled, User, Remove } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useGroupStore } from '@/stores/group'
import { groupApi } from '@/api'
import UserAvatar from './UserAvatar.vue'

const props = defineProps({
  group: { type: Object, required: true },
  members: { type: Array, default: () => [] },
  canManage: { type: Boolean, default: false },
  isOwner: { type: Boolean, default: false }
})

const emit = defineEmits(['refresh', 'close'])

const auth = useAuthStore()
const groupStore = useGroupStore()

const editAnnouncement = ref(false)
const announcementText = ref('')
const announcementLoading = ref(false)

watch(editAnnouncement, (val) => {
  if (val) announcementText.value = props.group.announcement || ''
})

watch(() => props.group.id, () => {
  if (props.canManage) groupStore.fetchPendingRequests(props.group.id)
}, { immediate: true })

function roleLabel(role) {
  return { OWNER: '群主', ADMIN: '管理员', MEMBER: '成员' }[role] || role
}

async function setAdmin(userId, isAdmin) {
  try {
    await groupApi.setAdmin(props.group.id, userId, isAdmin)
    emit('refresh')
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

async function kickMember(userId) {
  try {
    await ElMessageBox.confirm('确定踢出该成员？', '踢出确认', { type: 'warning' })
    await groupApi.kick(props.group.id, userId)
    emit('refresh')
    ElMessage.success('已踢出')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e || '操作失败')
  }
}

async function handleJoin(reqId, approve) {
  try {
    await groupApi.handleJoinRequest(reqId, approve)
    groupStore.pendingRequests = groupStore.pendingRequests.filter(r => r.id !== reqId)
    if (approve) emit('refresh')
    ElMessage.success(approve ? '已通过' : '已拒绝')
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

async function saveAnnouncement() {
  announcementLoading.value = true
  try {
    await groupApi.update(props.group.id, { announcement: announcementText.value })
    editAnnouncement.value = false
    emit('refresh')
    ElMessage.success('公告已更新')
  } catch (e) {
    ElMessage.error(e || '更新失败')
  } finally {
    announcementLoading.value = false
  }
}

async function dissolveGroup() {
  try {
    await ElMessageBox.confirm('解散后群组将永久删除，确定继续？', '解散群组', { type: 'warning' })
    await groupApi.dissolve(props.group.id)
    emit('close')
    groupStore.removeGroup(props.group.id)
    ElMessage.success('群组已解散')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e || '操作失败')
  }
}

async function leaveGroup() {
  try {
    await ElMessageBox.confirm('确定退出该群组？', '退出群组', { type: 'warning' })
    await groupApi.leave(props.group.id)
    emit('close')
    groupStore.removeGroup(props.group.id)
    ElMessage.success('已退出群组')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e || '操作失败')
  }
}
</script>

<style scoped>
.group-info-panel {
  width: 240px;
  background-color: var(--bg-secondary);
  border-left: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
  flex: 1;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.info-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 12px 16px;
  gap: 6px;
}

.group-icon-large {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background-color: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 24px;
}

.group-name-text {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.group-id-text {
  font-size: 11px;
  color: var(--text-muted);
}

.section {
  padding: 8px 12px;
  border-top: 1px solid var(--border-color);
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.5px;
  flex: 1;
}

.announcement-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  border-radius: 4px;
}

.member-item:hover { background-color: var(--bg-hover); }

.member-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-role {
  font-size: 10px;
  color: var(--text-muted);
}

.member-role.owner { color: var(--warning); }
.member-role.admin { color: var(--accent); }

.member-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.1s;
}

.member-item:hover .member-actions { opacity: 1; }

.request-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.req-name {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.danger-section {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}
</style>
