<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
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
  if (!joinGroupId.value) return ElMessage.warning('请输入群组ID')
  joinLoading.value = true
  try {
    await groupApi.requestJoin(Number(joinGroupId.value))
    ElMessage.success('申请已发送，等待审核')
    emit('joined')
    emit('update:modelValue', false)
    joinGroupId.value = ''
  } catch (e) {
    ElMessage.error(e || '申请失败')
  } finally {
    joinLoading.value = false
  }
}

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
              <el-input
                v-model="createName"
                placeholder="输入群组名称"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="加入群组" name="join">
        <div class="tab-content">
          <el-form @submit.prevent="joinGroup">
            <el-form-item label="群组ID">
              <el-input v-model="joinGroupId" placeholder="输入群组ID" type="number" />
            </el-form-item>
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
        v-else
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
</style>
