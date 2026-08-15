<template>
  <div class="input-wrapper">
    <div class="quick-prompts">
      <span class="prompt-tag">📄 解析我的简历</span>
      <span class="prompt-tag">🎯 测测我适合什么岗位</span>
      <span class="prompt-tag">📈 前端开发晋升路线</span>
    </div>
    
    <div class="input-area">
      <button class="attach-btn" title="上传简历附件">
        <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path></svg>
      </button>
      
      <input type="text" placeholder="输入你的问题，或点击左侧回形针上传简历..." />
      
      <button class="send-btn">
        <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
        <span>发送</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.input-wrapper {
  background-color: #FFFFFF;
  border-top: 1px solid var(--border-color);
  padding: 15px 24px 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 快捷指令样式 */
.quick-prompts {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.prompt-tag {
  font-size: 0.85rem;
  color: var(--primary-color);
  background-color: #F0F6FF;
  padding: 6px 12px;
  border-radius: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.prompt-tag:hover {
  background-color: #E1EDFF;
  border-color: #BFDBFE;
  transform: translateY(-1px);
}

/* 输入框区域样式 */
.input-area {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 8px 8px 8px 16px;
  transition: all 0.3s ease;
}

.input-area:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.15);
  background-color: #FFFFFF;
}

.attach-btn {
  background: none;
  border: none;
  color: #94A3B8;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.attach-btn:hover {
  color: var(--primary-color);
}

input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.95rem;
  color: var(--text-main);
  padding: 8px 0;
}

input::placeholder {
  color: #94A3B8;
}

/* 替换 ChatInput.vue 里的 .send-btn 相关样式 */

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  /* 换成高级的渐变蓝背景 */
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  color: #FFFFFF !important; /* 加 !important 强制白字，防止被其他样式覆盖 */
  border: none;
  padding: 10px 20px;
  border-radius: 10px;
  font-weight: bold;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.25); /* 淡淡的发光阴影 */
}

/* 确保 SVG 图标也是纯白色的 */
.send-btn svg {
  stroke: #FFFFFF !important;
}

.send-btn:hover {
  transform: translateY(-2px); /* 鼠标放上去微微上浮 */
  box-shadow: 0 6px 16px rgba(74, 144, 226, 0.45); /* 光晕变大 */
  background: linear-gradient(135deg, #357ABD 0%, #285A9B 100%);
}

</style>