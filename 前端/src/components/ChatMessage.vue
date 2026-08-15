<template>
  <div class="chat-area">
    <div class="msg-row msg-ai">
      <div class="avatar avatar-ai">AI</div>
      <div class="msg-bubble">
        你好！我是你的专属 AI 职业规划师。请上传你的简历，或者用一句话描述你的专业和求职目标，我将为你生成全息能力画像。
      </div>
    </div>
    
    <div class="msg-row user msg-user">
      <div class="avatar avatar-user">我</div>
      <div class="msg-bubble">
        我是大一新生，学的是计算机，想知道以后能不能做前端开发？需要学些什么？
      </div>
    </div>
    
    <div class="msg-row msg-ai">
      <div class="avatar avatar-ai" style="opacity: 0.7;">AI</div>
      <div class="msg-bubble loading-bubble">
        正在调取 10,000 条真实岗位数据进行匹配...
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-area { 
  flex: 1; 
  padding: 30px; 
  overflow-y: auto; 
  display: flex; 
  flex-direction: column; 
  gap: 24px;
  background-color: #FAFCFF; 
}

.msg-row { display: flex; gap: 16px; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; }

.avatar {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  font-size: 1.2rem; font-weight: bold; flex-shrink: 0;
}
.avatar-ai { background-color: #E8F0FE; color: var(--primary-color); }
.avatar-user { background-color: var(--primary-color); color: white; }

.msg-bubble {
  padding: 14px 18px; line-height: 1.6; font-size: 0.95rem;
  max-width: 75%;
}

.msg-ai .msg-bubble { 
  background-color: #FFFFFF; 
  border: 1px solid var(--border-color); 
  border-radius: 2px 16px 16px 16px; 
  box-shadow: 0 2px 5px rgba(0,0,0,0.02);
  color: var(--text-main);
}

.msg-user .msg-bubble { 
  background-color: var(--primary-color); 
  border-radius: 16px 2px 16px 16px; 
  color: white;
  box-shadow: 0 4px 10px rgba(74, 144, 226, 0.2);
}

.loading-bubble {
  border: none !important; 
  background: transparent !important; 
  box-shadow: none !important; 
  color: var(--text-sub) !important; 
  padding-left: 0 !important;
}
</style>