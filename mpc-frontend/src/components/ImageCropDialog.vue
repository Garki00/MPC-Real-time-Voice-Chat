<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  imageSrc: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'cropped'])

const canvasRef = ref(null)

let img = null
let isDragging = false
let lastX = 0
let lastY = 0
let imgX = 0
let imgY = 0
let scale = 1
const CROP_SIZE = 280

function initCanvas() {
  if (!props.imageSrc || !canvasRef.value) return
  img = new Image()
  img.onload = () => {
    const minScale = Math.max(CROP_SIZE / img.naturalWidth, CROP_SIZE / img.naturalHeight)
    scale = minScale
    imgX = (CROP_SIZE - img.naturalWidth * scale) / 2
    imgY = (CROP_SIZE - img.naturalHeight * scale) / 2
    draw()
  }
  img.src = props.imageSrc
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas || !img) return
  const ctx = canvas.getContext('2d')
  canvas.width = CROP_SIZE
  canvas.height = CROP_SIZE
  ctx.clearRect(0, 0, CROP_SIZE, CROP_SIZE)
  ctx.drawImage(img, imgX, imgY, img.naturalWidth * scale, img.naturalHeight * scale)
}

function clampPosition() {
  const w = img.naturalWidth * scale
  const h = img.naturalHeight * scale
  if (imgX > 0) imgX = 0
  if (imgY > 0) imgY = 0
  if (imgX + w < CROP_SIZE) imgX = CROP_SIZE - w
  if (imgY + h < CROP_SIZE) imgY = CROP_SIZE - h
}

function onMouseDown(e) {
  isDragging = true
  lastX = e.clientX
  lastY = e.clientY
}

function onMouseMove(e) {
  if (!isDragging || !img) return
  imgX += e.clientX - lastX
  imgY += e.clientY - lastY
  lastX = e.clientX
  lastY = e.clientY
  clampPosition()
  draw()
}

function onMouseUp() {
  isDragging = false
}

function onWheel(e) {
  if (!img) return
  const delta = e.deltaY > 0 ? -0.05 : 0.05
  const minScale = Math.max(CROP_SIZE / img.naturalWidth, CROP_SIZE / img.naturalHeight)
  const newScale = Math.max(minScale, scale + delta)
  const ratio = newScale / scale
  imgX = CROP_SIZE / 2 - (CROP_SIZE / 2 - imgX) * ratio
  imgY = CROP_SIZE / 2 - (CROP_SIZE / 2 - imgY) * ratio
  scale = newScale
  clampPosition()
  draw()
}

function onTouchStart(e) {
  if (e.touches.length === 1) {
    isDragging = true
    lastX = e.touches[0].clientX
    lastY = e.touches[0].clientY
  }
}

function onTouchMove(e) {
  if (!isDragging || !img || e.touches.length !== 1) return
  imgX += e.touches[0].clientX - lastX
  imgY += e.touches[0].clientY - lastY
  lastX = e.touches[0].clientX
  lastY = e.touches[0].clientY
  clampPosition()
  draw()
}

function confirm() {
  const canvas = canvasRef.value
  if (!canvas) return
  canvas.toBlob((blob) => {
    emit('cropped', blob)
    emit('update:modelValue', false)
  }, 'image/jpeg', 0.9)
}

function cancel() {
  emit('update:modelValue', false)
}

watch(() => props.modelValue, (val) => {
  if (val) setTimeout(initCanvas, 50)
})

watch(() => props.imageSrc, () => {
  if (props.modelValue) initCanvas()
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="裁剪头像"
    width="360px"
    :close-on-click-modal="false"
  >
    <div class="crop-container">
      <div class="crop-hint">拖动图片调整位置，滚轮缩放</div>
      <div
        class="canvas-wrap"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
        @wheel.prevent="onWheel"
        @touchstart.prevent="onTouchStart"
        @touchmove.prevent="onTouchMove"
        @touchend="onMouseUp"
      >
        <canvas ref="canvasRef" class="crop-canvas" />
        <div class="crop-border" />
      </div>
    </div>
    <template #footer>
      <el-button @click="cancel">取消</el-button>
      <el-button type="primary" @click="confirm">确认裁剪</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.crop-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.crop-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.canvas-wrap {
  position: relative;
  width: 280px;
  height: 280px;
  cursor: grab;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  user-select: none;
}

.canvas-wrap:active {
  cursor: grabbing;
}

.crop-canvas {
  display: block;
  width: 280px;
  height: 280px;
}

.crop-border {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  pointer-events: none;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.45);
}
</style>
