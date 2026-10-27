<template>
  <div class="timeline">
    <el-timeline>
      <el-timeline-item
        v-for="event in events"
        :key="event.sequence"
        :type="timelineType(event.level)"
        :timestamp="event.timestamp"
        placement="top"
      >
        <div class="event-item">
          <span class="event-type">{{ event.eventType }}</span>
          <span class="event-msg">{{ event.message }}</span>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="events.length === 0" description="暂无事件" />
  </div>
</template>

<script setup lang="ts">
import type { TaskEvent } from '@/types'

defineProps<{ events: TaskEvent[] }>()

function timelineType(level: string): string {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'primary'
}
</script>

<style scoped>
.timeline { max-height: 480px; overflow-y: auto; }
.event-item { display: flex; gap: 8px; align-items: center; }
.event-type { font-weight: 600; color: var(--primary); font-size: 13px; }
.event-msg { color: #606266; font-size: 13px; }
</style>