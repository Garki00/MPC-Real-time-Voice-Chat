<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { groupApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import ImageCropDialog from './ImageCropDialog.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  groupId: { type: Number, required: true }
})
const emit = defineEmits(['update:modelValue', 'updated'])

const auth = useAuthStore()
const group = ref(null)
const editing = ref(false)
const saveLoading = ref(false)
const editName = ref('')
const avatarPreview = ref('')
const avatarUrl = ref('')
const showCrop = ref(false)
const cropSrc = ref('')
const fileInputRef = ref(null)
const isOwner = ref(false)
const MAX_SIZE = 10 * 1024 * 1024

async function load() {
  if (!props.groupId) return
  try {
    group.value = await groupApi.getGroup(props.groupId)
    isOwner.value = group.value.ownerId === auth.user?.id
  } catch {}
}

function startEdit() {
  editName.value = group.value.name
  avatarPreview.value = group.value.avatar || ''
  avatarUrl.value = group.value.avatar || ''
  editing.value = true
}

function cancelEdit() { editing.value = false }

function triggerFilePick() { fileInputRef.value?.click() }

function onFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > MAX_SIZE) { ElMessage.error('图片大小不能超过10MB'); e.target.value = ''; return }
  const reader = new FileReader()
  reader.onload = (ev) => { cropSrc.value = ev.target.result; showCrop.value = true }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function onCropped(blob) {
  try {
    const res = await groupApi.uploadGroupAvatar(blob)
    avatarUrl.value = res.url
    avatarPreview.value = res.url
    ElMessage.success('头像上传成功')
  } catch { ElMessage.error('头像上传失败') }
}

async function save() {
  if (!editName.value.trim()) return ElMessage.warning('请输入群组名称')
  saveLoading.value = true
  try {
    const updated = await groupApi.update(props.groupId, { name: editName.value.trim(), avatar: avatarUrl.value || null })
    group.value = updated
    editing.value = false
    emit('updated', updated)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

watch(() => props.modelValue, (val) => { if (val) load() })
watch(() => props.groupId, () => { if (props.modelValue) load() })
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="群组信息"
    width="400px"
    @close="editing = false"
  >
    <div v-if="group" class="settings-content">
      <div class="group-avatar-wrap">
        <div class="group-avatar-large" :class="{ clickable: editing }" @click="editing && triggerFilePick()">
          <img v-if="editing ? avatarPreview : group.avatar" :src="editing ? avatarPreview : group.avatar" class="avatar-img" />
          <div v-else class="avatar-fallback">{{ group.name.charAt(0).toUpperCase() }}</div>
          <div v-if="editing" class="avatar-edit-overlay">更换</div>
        </div>
        <input v-if="editing" ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="onFileSelected" />
      </div>

      <template v-if="!editing">
        <div class="info-row"><span class="info-label">名称</span><span class="info-value">{{ group.name }}</span></div>
        <div class="info-row"><span class="info-label">ID</span><span class="info-value muted">#{{ group.id }}</span></div>
        <div class="info-row"><span class="info-label">群主</span><span class="info-value">{{ group.ownerName }}</span></div>
      </template>

      <template v-else>
        <el-form style="width:100%">
          <el-form-item label="群组名称">
            <div class="input-with-limit">
              <el-input v-model="editName" :maxlength="100" />
              <span class="char-count">{{ editName.length }}/100</span>
            </div>
          </el-form-item>
        </el-form>
      </template>
    </div>

    <template #footer>
      <template v-if="!editing">
        <el-button v-if="isOwner" type="primary" @click="startEdit">编辑信息</el-button>
        <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
      </template>
      <template v-else>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="save">保存</el-button>
      </template>
    </template>
  </el-dialog>

  <ImageCropDialog v-model="showCrop" :image-src="cropSrc" @cropped="onCropped" />
</template>

<style scoped>
.settings-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}
.group-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  overflow: hidden;
  position: relative;
  background-color: var(--accent);
}
.group-avatar-large.clickable { cursor: pointer; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: #fff;
}
.avatar-edit-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.15s;
}
.group-avatar-large.clickable:hover .avatar-edit-overlay { opacity: 1; }
.info-row { display: flex; gap: 12px; width: 100%; padding: 0 8px; }
.info-label { width: 48px; font-size: 13px; color: var(--text-muted); flex-shrink: 0; }
.info-value { font-size: 14px; color: var(--text-primary); }
.info-value.muted { color: var(--text-secondary); }
.input-with-limit {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.input-with-limit .el-input { flex: 1; }
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
</style>
