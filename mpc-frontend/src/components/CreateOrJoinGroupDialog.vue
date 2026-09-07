<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Loading } from '@element-plus/icons-vue'
import { groupApi } from '@/api'
import ImageCropDialog from './ImageCropDialog.vue'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'created', 'joined'])

const activeTab = ref('create')
const createLoading = ref(false)
const joinLoading = ref(false)
const createName = ref('')
const joinGroupId = ref('')

const avatarPreview = ref('')
const avatarUrl = ref('')
const showCrop = ref(false)
const cropSrc = ref('')
const fileInputRef = ref(null)

const previewGroup = ref(null)
const previewLoading = ref(false)
let previewTimer = null

const MAX_SIZE = 10 * 1024 * 1024

function triggerFilePick() {
  fileInputRef.value?.click()
}

function onFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > MAX_SIZE) {
    ElMessage.error('图片大小不能超过10MB')
    e.target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = (ev) => {
    cropSrc.value = ev.target.result
    showCrop.value = true
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function onCropped(blob) {
  try {
    const res = await groupApi.uploadGroupAvatar(blob)
    avatarUrl.value = res.url
    avatarPreview.value = res.url
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

async function createGroup() {
  if (!createName.value.trim()) return ElMessage.warning('请输入群组名称')
  createLoading.value = true
  try {
    const group = await groupApi.create({ name: createName.value.trim(), avatar: avatarUrl.value || null })
    emit('created', group)
    emit('update:modelValue', false)
    resetCreate()
  } catch (e) {
    ElMessage.error(e || '创建失败')
  } finally {
    createLoading.value = false
  }
}

async function joinGroup() {
  if (!joinGroupId.value || !previewGroup.value) return
  joinLoading.value = true
  try {
    await groupApi.requestJoin(Number(joinGroupId.value))
    ElMessage.success('申请已发送，等待审核')
    emit('joined')
    emit('update:modelValue', false)
    joinGroupId.value = ''
    previewGroup.value = null
  } catch (e) {
    ElMessage.error(e || '申请失败')
  } finally {
    joinLoading.value = false
  }
}

async function previewTargetGroup() {
  const gid = joinGroupId.value.trim()
  if (!gid || isNaN(gid)) {
    previewGroup.value = null
    return
  }

  previewLoading.value = true
  try {
    previewGroup.value = await groupApi.getGroup(Number(gid))
  } catch {
    previewGroup.value = null
  } finally {
    previewLoading.value = false
  }
}

watch(joinGroupId, () => {
  previewGroup.value = null
  if (previewTimer) clearTimeout(previewTimer)
  previewTimer = setTimeout(previewTargetGroup, 1000)
})

watch(() => props.modelValue, (val) => {
  if (!val) {
    if (previewTimer) clearTimeout(previewTimer)
    previewGroup.value = null
  }
})

function resetCreate() {
  createName.value = ''
  avatarPreview.value = ''
  avatarUrl.value = ''
}

function onClose() {
  resetCreate()
  joinGroupId.value = ''
  activeTab.value = 'create'
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="创建或加入群组"
    width="420px"
    @close="onClose"
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="创建群组" name="create">
        <div class="tab-content">
          <div class="avatar-section">
            <div class="avatar-preview" @click="triggerFilePick">
              <img v-if="avatarPreview" :src="avatarPreview" class="preview-img" />
              <div v-else class="avatar-placeholder">
                <el-icon :size="24"><Plus /></el-icon>
                <span>上传头像</span>
              </div>
            </div>
            <span class="avatar-hint">点击上传群组头像（最大10MB）</span>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="onFileSelected"
            />
          </div>

          <el-form @submit.prevent="createGroup">
            <el-form-item label="群组名称">
              <div class="input-with-limit">
                <el-input
                  v-model="createName"
                  placeholder="输入群组名称"
                  :maxlength="100"
                />
                <span class="char-count">{{ createName.length }}/100</span>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="加入群组" name="join">
        <div class="tab-content">
          <el-form @submit.prevent="joinGroup">
            <el-form-item label="群组ID">
              <el-input v-model="joinGroupId" placeholder="输入群组ID" />
            </el-form-item>

            <!-- 群组预览 -->
            <div v-if="previewLoading" class="group-preview loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <div v-else-if="previewGroup" class="group-preview">
              <div class="group-avatar">
                <img v-if="previewGroup.avatar" :src="previewGroup.avatar" />
                <div v-else class="group-fallback">{{ previewGroup.name.charAt(0).toUpperCase() }}</div>
              </div>
              <span class="preview-name">{{ previewGroup.name }}</span>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button
        v-if="activeTab === 'create'"
        type="primary"
        :loading="createLoading"
        @click="createGroup"
      >创建</el-button>
      <el-button
        v-else-if="previewGroup"
        type="primary"
        :loading="joinLoading"
        @click="joinGroup"
      >申请加入</el-button>
    </template>
  </el-dialog>

  <ImageCropDialog v-model="showCrop" :image-src="cropSrc" @cropped="onCropped" />
</template>

<style scoped>
.tab-content {
  padding: 8px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  border: 2px dashed var(--border-color);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s;
}

.avatar-preview:hover {
  border-color: var(--accent);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.avatar-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.group-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: var(--bg-tertiary);
  border-radius: 6px;
  margin-top: 8px;
}

.group-preview.loading {
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
}

.group-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  overflow: hidden;
  background-color: var(--accent);
  flex-shrink: 0;
}

.group-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.group-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.preview-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

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
