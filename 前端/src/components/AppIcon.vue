<template>
  <svg
    class="app-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    :stroke="color"
    stroke-width="1.75"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-for="(d, i) in paths" :key="i">
      <circle v-if="d.type === 'circle'" :cx="d.cx" :cy="d.cy" :r="d.r" />
      <line v-else-if="d.type === 'line'" :x1="d.x1" :y1="d.y1" :x2="d.x2" :y2="d.y2" />
      <rect v-else-if="d.type === 'rect'" :x="d.x" :y="d.y" :width="d.w" :height="d.h" :rx="d.rx || 0" />
      <path v-else :d="d.d" />
    </template>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
  color: { type: String, default: 'currentColor' }
})

// 线性图标集（24x24 viewBox，细描边，风格统一）
const ICONS = {
  // 品牌 / 导航
  compass: [
    { type: 'circle', cx: 12, cy: 12, r: 9 },
    { d: 'M15.5 8.5l-2 5-5 2 2-5 5-2z' }
  ],
  plus: [{ d: 'M12 5v14M5 12h14' }],
  chat: [{ d: 'M21 11.5a8.4 8.4 0 0 1-9 8.4 9 9 0 0 1-4-.9L3 21l1.9-4.6A8.4 8.4 0 0 1 12 3.1a8.4 8.4 0 0 1 9 8.4z' }],
  graph: [
    { type: 'circle', cx: 6, cy: 7, r: 2.4 },
    { type: 'circle', cx: 18, cy: 7, r: 2.4 },
    { type: 'circle', cx: 12, cy: 17.5, r: 2.4 },
    { d: 'M7.9 8.6l3 6.6M16.1 8.6l-3 6.6M8.4 7h7.2' }
  ],
  radar: [
    { d: 'M12 3a9 9 0 1 0 9 9' },
    { d: 'M12 7.2a4.8 4.8 0 1 0 4.8 4.8' },
    { type: 'circle', cx: 12, cy: 12, r: 1 },
    { d: 'M12 12l6-4.5' }
  ],
  cpu: [
    { type: 'rect', x: 7, y: 7, w: 10, h: 10, rx: 2 },
    { type: 'rect', x: 10, y: 10, w: 4, h: 4, rx: 1 },
    { d: 'M10 3v4M14 3v4M10 17v4M14 17v4M3 10h4M3 14h4M17 10h4M17 14h4' }
  ],
  dashboard: [
    { d: 'M3 17.5l5-5 3.5 3.5L21 7' },
    { d: 'M15.5 7H21v5.5' }
  ],
  user: [
    { type: 'circle', cx: 12, cy: 8, r: 3.6 },
    { d: 'M4.5 20.5a7.5 7.5 0 0 1 15 0' }
  ],
  settings: [
    { type: 'circle', cx: 12, cy: 12, r: 3 },
    { d: 'M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1v.2a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-2.8-1.1l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.6 1.6 0 0 0 3 15a2 2 0 1 1 0-4 1.6 1.6 0 0 0 1.5-2.7l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.6 1.6 0 0 0 10 4.6V4a2 2 0 1 1 4 0v.2a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1A1.6 1.6 0 0 0 21 11a2 2 0 1 1 0 4 1.6 1.6 0 0 0-1.6 1z' }
  ],
  logout: [
    { d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' },
    { d: 'M16 17l5-5-5-5M21 12H9' }
  ],
  edit: [{ d: 'M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z' }],
  trash: [
    { d: 'M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2' },
    { d: 'M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6' }
  ],
  close: [{ d: 'M18 6L6 18M6 6l12 12' }],
  image: [
    { type: 'rect', x: 3, y: 4, w: 18, h: 16, rx: 2 },
    { type: 'circle', cx: 8.5, cy: 9.5, r: 1.5 },
    { d: 'M21 16l-5-5-9.5 9' }
  ],
  send: [{ d: 'M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z' }],
  sparkle: [
    { d: 'M12 3l1.8 4.9L18.7 9.7l-4.9 1.8L12 16.4l-1.8-4.9L5.3 9.7l4.9-1.8L12 3z' },
    { d: 'M18.5 15.5l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8.8-2.2z' }
  ],
  briefcase: [
    { type: 'rect', x: 3, y: 7, w: 18, h: 13, rx: 2 },
    { d: 'M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M3 12h18' }
  ],
  scale: [
    { d: 'M12 3v18M7 21h10M5 7h14M5 7l-2.5 5.5a3 3 0 0 0 5 0L5 7zM19 7l-2.5 5.5a3 3 0 0 0 5 0L19 7z' },
    { type: 'circle', cx: 12, cy: 4.5, r: 1.2 }
  ],
  trendUp: [
    { d: 'M3 17l6-6 4 4 8-8' },
    { d: 'M15 7h6v6' }
  ],
  activity: [{ d: 'M3 12h4l2.5-7 4 14L16 12h5' }],
  file: [
    { d: 'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5z' },
    { d: 'M14 3v5h5M9 13h6M9 17h4' }
  ],
  mic: [
    { type: 'rect', x: 9, y: 3, w: 6, h: 11, rx: 3 },
    { d: 'M5 11a7 7 0 0 0 14 0M12 18v3M8 21h8' }
  ],
  route: [
    { type: 'circle', cx: 6, cy: 6, r: 2.4 },
    { type: 'circle', cx: 18, cy: 18, r: 2.4 },
    { d: 'M8.4 6H14a3 3 0 0 1 0 6h-4a3 3 0 0 0 0 6h3.6' }
  ],
  bolt: [{ d: 'M13 2L4 14h7l-1 8 9-12h-7l1-8z' }],
  graduation: [
    { d: 'M22 9L12 4 2 9l10 5 10-5z' },
    { d: 'M6 11.5V16c0 1 2.7 2.5 6 2.5s6-1.5 6-2.5v-4.5' }
  ],
  trophy: [
    { d: 'M8 4h8v5a4 4 0 0 1-8 0V4z' },
    { d: 'M8 5H5v2a3 3 0 0 0 3 3M16 5h3v2a3 3 0 0 1-3 3M10 15v3M14 15v3M8 21h8' }
  ],
  check: [{ d: 'M20 6L9 17l-5-5' }],
  alert: [
    { type: 'circle', cx: 12, cy: 12, r: 9 },
    { d: 'M12 8v5M12 16.5v.01' }
  ],
  info: [
    { type: 'circle', cx: 12, cy: 12, r: 9 },
    { d: 'M12 11v5M12 7.5v.01' }
  ]
}

const paths = computed(() => ICONS[props.name] || ICONS.info)
</script>

<style scoped>
.app-icon {
  display: inline-block;
  vertical-align: -0.15em;
  flex-shrink: 0;
}
</style>
