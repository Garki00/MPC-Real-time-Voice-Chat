<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi, userApi } from '@/api'
import UserAvatar from './UserAvatar.vue'
import ImageCropDialog from './ImageCropDialog.vue'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const auth = useAuthStore()
const loading = ref({ username: false, email: false, password: false })

const display = ref({ username: '', email: '' })
const editing = ref({ username: false, email: false, password: false })
const draft = ref({ username: '', email: '', password: '', confirmPassword: '' })

const showCrop = ref(false)
const cropSrc = ref('')
const fileInputRef = ref(null)
const avatarUrl = ref('')
const MAX_SIZE = 10 * 1024 * 1024
const USERNAME_MAX = 50

function triggerAvatarPick() { fileInputRef.value?.click() }

function onAvatarFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > MAX_SIZE) { ElMessage.error('图片大小不能超过10MB'); e.target.value = ''; return }
  const reader = new FileReader()
  reader.onload = (ev) => { cropSrc.value = ev.target.result; showCrop.value = true }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function onAvatarCropped(blob) {
  try {
    const res = await userApi.uploadAvatar(blob)
    avatarUrl.value = res.url
    await userApi.updateMe({ avatar: res.url })
    await auth.refreshUser()
    ElMessage.success('头像已更新')
  } catch { ElMessage.error('头像上传失败') }
}

function startEdit(field) {
  if (field === 'username') draft.value.username = display.value.username
  if (field === 'email') draft.value.email = display.value.email
  if (field === 'password') { draft.value.password = ''; draft.value.confirmPassword = '' }
  editing.value = { ...editing.value, [field]: true }
}

function cancelEdit(field) {
  editing.value = { ...editing.value, [field]: false }
}

async function saveField(field) {
  if (field === 'username') {
    const val = draft.value.username.trim()
    if (!val) return ElMessage.warning('用户名不能为空')
    if (val.length < 3) return ElMessage.warning('用户名至少3个字符')
    loading.value = { ...loading.value, username: true }
    try {
      await userApi.updateMe({ username: val })
      await auth.refreshUser()
      display.value.username = auth.user.username
      editing.value = { ...editing.value, username: false }
      ElMessage.success('用户名已更新')
    } catch (e) { ElMessage.error(e || '保存失败') }
    finally { loading.value = { ...loading.value, username: false } }
  }

  if (field === 'email') {
    const val = draft.value.email.trim()
    if (!val) return ElMessage.warning('邮箱不能为空')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return ElMessage.warning('邮箱格式不正确')
    loading.value = { ...loading.value, email: true }
    try {
      await userApi.updateMe({ email: val })
      await auth.refreshUser()
      display.value.email = auth.user.email
      editing.value = { ...editing.value, email: false }
      ElMessage.success('邮箱已更新')
    } catch (e) { ElMessage.error(e || '保存失败') }
    finally { loading.value = { ...loading.value, email: false } }
  }

  if (field === 'password') {
    const pw = draft.value.password
    const cpw = draft.value.confirmPassword
    if (!pw) {
      editing.value = { ...editing.value, password: false }
      return
    }
    if (pw !== cpw) return ElMessage.warning('两次密码不一致')
    if (pw.length < 6) return ElMessage.warning('密码至少6个字符')
    loading.value = { ...loading.value, password: true }
    try {
      await userApi.updateMe({ password: pw })
      editing.value = { ...editing.value, password: false }
      ElMessage.success('密码已更新')
    } catch (e) { ElMessage.error(e || '保存失败') }
    finally { loading.value = { ...loading.value, password: false } }
  }
}

watch(() => props.modelValue, (val) => {
  if (val && auth.user) {
    display.value = {
      username: auth.user.username || '',
      email: auth.user.email || ''
    }
    avatarUrl.value = auth.user.avatar || ''
    editing.value = { username: false, email: false, password: false }
    draft.value = { username: '', email: '', password: '', confirmPassword: '' }
  }
})

function resetForm() {
  editing.value = { username: false, email: false, password: false }
  draft.value = { username: '', email: '', password: '', confirmPassword: '' }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch { return }
  try { await authApi.logout() } catch {}
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
    title="个人设置" width="420px" @close="resetForm">
    <div class="profile-content">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <UserAvatar :src="avatarUrl" :name="display.username" :size="72" />
        <el-button size="small" class="upload-btn" @click="triggerAvatarPick">更换头像</el-button>
        <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="onAvatarFileSelected" />
      </div>

      <!-- 用户名行 -->
      <div class="field-row">
        <span class="field-label">用户名</span>
        <div class="field-body">
          <template v-if="!editing.username">
            <span class="field-value">{{ display.username }}</span>
          </template>
          <template v-else>
            <div class="input-with-limit">
              <el-input
                v-model="draft.username"
                :maxlength="USERNAME_MAX"
                placeholder="请输入用户名"
                size="small"
              />
              <span class="char-count">{{ draft.username.length }}/{{ USERNAME_MAX }}</span>
            </div>
          </template>
        </div>
        <div class="field-actions">
          <el-button
            v-if="!editing.username"
            size="small"
            class="edit-btn"
            @click="startEdit('username')"
          >编辑</el-button>
          <template v-else>
            <el-button size="small" @click="cancelEdit('username')">取消</el-button>
            <el-button size="small" type="primary" :loading="loading.username" @click="saveField('username')">保存</el-button>
          </template>
        </div>
      </div>

      <!-- 邮箱行 -->
      <div class="field-row">
        <span class="field-label">邮箱</span>
        <div class="field-body">
          <template v-if="!editing.email">
            <span class="field-value">{{ display.email }}</span>
          </template>
          <template v-else>
            <el-input
              v-model="draft.email"
              placeholder="请输入邮箱"
              size="small"
            />
          </template>
        </div>
        <div class="field-actions">
          <el-button
            v-if="!editing.email"
            size="small"
            class="edit-btn"
            @click="startEdit('email')"
          >编辑</el-button>
          <template v-else>
            <el-button size="small" @click="cancelEdit('email')">取消</el-button>
            <el-button size="small" type="primary" :loading="loading.email" @click="saveField('email')">保存</el-button>
          </template>
        </div>
      </div>

      <!-- 密码行 -->
      <div class="field-row" :class="{ 'field-row--expanded': editing.password }">
        <span class="field-label">密码</span>
        <div class="field-body">
          <template v-if="!editing.password">
            <span class="field-value field-value--password">*****</span>
          </template>
          <template v-else>
            <div class="password-fields">
              <el-input
                v-model="draft.password"
                type="password"
                placeholder="新密码（留空不修改）"
                show-password
                size="small"
              />
              <el-input
                v-model="draft.confirmPassword"
                type="password"
                placeholder="确认新密码"
                show-password
                size="small"
                :class="{ 'input-mismatch': draft.confirmPassword && draft.password !== draft.confirmPassword }"
              />
              <span v-if="draft.confirmPassword && draft.password !== draft.confirmPassword" class="mismatch-hint">两次密码不一致</span>
            </div>
          </template>
        </div>
        <div class="field-actions">
          <el-button
            v-if="!editing.password"
            size="small"
            class="edit-btn"
            @click="startEdit('password')"
          >编辑</el-button>
          <template v-else>
            <el-button size="small" @click="cancelEdit('password')">取消</el-button>
            <el-button size="small" type="primary" :loading="loading.password" @click="saveField('password')">保存</el-button>
          </template>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      <!-- <div style="flex:1" /> -->
      <!-- <el-button @click="$emit('update:modelValue', false)">关闭</el-button> -->
    </template>
  </el-dialog>

  <ImageCropDialog v-model="showCrop" :image-src="cropSrc" @cropped="onAvatarCropped" />
</template>

<style scoped>
.profile-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.upload-btn {
  font-size: 12px;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--border-color);
  min-height: 52px;
}

.field-row--expanded {
  align-items: flex-start;
  padding-top: 12px;
}

.field-label {
  width: 48px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.field-body {
  flex: 1;
  min-width: 0;
}

.field-value {
  font-size: 14px;
  color: var(--text-primary);
  word-break: break-all;
}

.field-value--password {
  letter-spacing: 2px;
  color: var(--text-muted);
}

.field-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.edit-btn {
  color: var(--text-secondary);
}

.input-with-limit {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-with-limit .el-input {
  flex: 1;
}

.char-count {
  font-size: 11px;
  color: var(--text-secondary);
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 1px 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.password-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mismatch-hint {
  font-size: 12px;
  color: var(--danger);
}
</style>
