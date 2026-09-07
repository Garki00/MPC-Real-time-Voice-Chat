<template>
  <div class="user-avatar" :style="{ width: size + 'px', height: size + 'px', fontSize: fontSize + 'px' }">
    <img v-if="src" :src="src" :alt="name" @error="imgError = true" v-show="!imgError" />
    <span v-if="!src || imgError">{{ initials }}</span>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: 36 }
})

const imgError = ref(false)
const initials = computed(() => {
  if (!props.name) return '?'
  return props.name.charAt(0).toUpperCase()
})
const fontSize = computed(() => Math.floor(props.size * 0.42))
</script>

<style scoped>
.user-avatar {
  border-radius: 50%;
  background-color: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
  user-select: none;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
