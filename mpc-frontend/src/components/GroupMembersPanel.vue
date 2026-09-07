<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, UserFilled, User, Remove } from '@element-plus/icons-vue'
import { groupApi, friendApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useFriendStore } from '@/stores/friend'
import UserAvatar from './UserAvatar.vue'
import UserInfoDialog from './UserInfoDialog.vue'

const props = defineProps({
  groupId: { type: Number, required: true }
})

const emit = defineEmits(['memberUpdated'])

const auth = useAuthStore()
const friendStore = useFriendStore()

const members = ref([])
const contextMenu = ref({ visible: false, x: 0, y: 0, member: null })
const showUserInfo = ref(false)
const selectedUserId = ref(null)

const owners = computed(() => members.value.filter(m => m.role === 'OWNER'))
const admins = computed(() => members.value.filter(m => m.role === 'ADMIN'))
const regularMembers = computed(() => members.value.filter(m => m.role === 'MEMBER'))

const currentUserRole = computed(() => {
  const me = members.value.find(m => m.userId === auth.user?.id)
  return me?.role || 'MEMBER'
})

const isOwner = computed(() => currentUserRole.value === 'OWNER')
const isAdmin = computed(() => currentUserRole.value === 'ADMIN' || isOwner.value)

function isFriend(userId) {
  return friendStore.friends.some(f => f.id === userId)
}

async function load() {
  try {
    members.value = await groupApi.getMembers(props.groupId)
  } catch {}
}

function showMemberMenu(e, member) {
  const item = e.currentTarget
  const rect = item.getBoundingClientRect()
  const scrollContainer = item.closest('.members-scroll')
  const containerRect = scrollContainer.getBoundingClientRect()

  const relativeTop = rect.top - containerRect.top + scrollContainer.scrollTop
  const avatarWidth = 32
  const menuWidth = 150

  contextMenu.value = {
    visible: true,
    x: avatarWidth + 40,
    y: relativeTop,
    member
  }
}

function viewUserInfo(member) {
  contextMenu.value.visible = false
  selectedUserId.value = member.userId
  showUserInfo.value = true
}

async function addFriend(member) {
  contextMenu.value.visible = false
  try {
    await friendApi.sendRequest(member.userId)
    ElMessage.success('好友申请已发送')
  } catch (e) {
    ElMessage.error(e || '发送失败')
  }
}

async function setAsAdmin(member) {
  contextMenu.value.visible = false
  const isCurrentlyAdmin = member.role === 'ADMIN'
  try {
    await ElMessageBox.confirm(
      isCurrentlyAdmin ? '确定要取消该成员的管理员身份吗？' : '确定要设置该成员为管理员吗？',
      '提示',
      { type: 'warning' }
    )
  } catch { return }

  try {
    await groupApi.setAdmin(props.groupId, member.userId, !isCurrentlyAdmin)
    ElMessage.success(isCurrentlyAdmin ? '已取消管理员' : '已设置为管理员')
    await load()
    emit('memberUpdated')
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

async function kickMember(member) {
  contextMenu.value.visible = false
  try {
    await ElMessageBox.confirm(`确定要将 ${member.username} 踢出群聊吗？`, '提示', { type: 'warning' })
  } catch { return }

  try {
    await groupApi.kick(props.groupId, member.userId)
    ElMessage.success('已踢出群聊')
    await load()
    emit('memberUpdated')
  } catch (e) {
    ElMessage.error(e || '操作失败')
  }
}

onMounted(load)
watch(() => props.groupId, load)

document.addEventListener('click', () => { contextMenu.value.visible = false })
</script>

<template>
  <div class="members-panel">
    <div class="members-header">成员</div>
    <div class="members-scroll">
      <template v-if="owners.length">
        <div class="role-label">群主</div>
        <div v-for="m in owners" :key="m.userId" class="member-item"
          @contextmenu.prevent="showMemberMenu($event, m)">
          <div class="member-avatar">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <span class="status-dot" :class="m.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <span class="member-name" :class="{ offline: m.status !== 'ONLINE' }">{{ m.username }}</span>
        </div>
      </template>
      <template v-if="admins.length">
        <div class="role-label">管理员</div>
        <div v-for="m in admins" :key="m.userId" class="member-item"
          @contextmenu.prevent="showMemberMenu($event, m)">
          <div class="member-avatar">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <span class="status-dot" :class="m.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <span class="member-name" :class="{ offline: m.status !== 'ONLINE' }">{{ m.username }}</span>
        </div>
      </template>
      <template v-if="regularMembers.length">
        <div class="role-label">成员</div>
        <div v-for="m in regularMembers" :key="m.userId" class="member-item"
          @contextmenu.prevent="showMemberMenu($event, m)">
          <div class="member-avatar">
            <UserAvatar :src="m.avatar" :name="m.username" :size="32" />
            <span class="status-dot" :class="m.status === 'ONLINE' ? 'online' : 'offline'" />
          </div>
          <span class="member-name" :class="{ offline: m.status !== 'ONLINE' }">{{ m.username }}</span>
        </div>
      </template>

      <!-- 右键菜单 -->
      <div v-if="contextMenu.visible && contextMenu.member" class="context-menu"
        :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
        @click.stop>
        <div class="menu-item" @click="viewUserInfo(contextMenu.member)">
          <el-icon><InfoFilled /></el-icon> 查看信息
        </div>
        <div v-if="!isFriend(contextMenu.member.userId) && contextMenu.member.userId !== auth.user?.id"
          class="menu-item" @click="addFriend(contextMenu.member)">
          <el-icon><UserFilled /></el-icon> 添加好友
        </div>
        <template v-if="isAdmin && contextMenu.member.userId !== auth.user?.id && contextMenu.member.role !== 'OWNER'">
          <div class="menu-item" @click="setAsAdmin(contextMenu.member)">
            <el-icon><User /></el-icon>
            {{ contextMenu.member.role === 'ADMIN' ? '取消管理员' : '设为管理员' }}
          </div>
          <div class="menu-item danger" @click="kickMember(contextMenu.member)">
            <el-icon><Remove /></el-icon> 踢出群聊
          </div>
        </template>
      </div>
    </div>

    <!-- 用户信息弹窗 -->
    <UserInfoDialog v-model="showUserInfo" :user-id="selectedUserId" />
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
  position: relative;
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
  cursor: pointer;
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

.context-menu {
  position: absolute;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 4px;
  z-index: 100;
  min-width: 150px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
  pointer-events: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
}

.menu-item:hover { background-color: var(--bg-hover); }
.menu-item.danger { color: var(--danger); }
.menu-item.danger:hover { background-color: rgba(237,66,69,0.15); }
</style>
