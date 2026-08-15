<template>
  <div class="agent-page">
    <div class="ambient-glow glow-blue"></div>
    <div class="ambient-glow glow-purple"></div>
    
    <aside class="sidebar glass-panel">
      <button class="new-chat-btn" @click="prepareNewChat">
        <span class="icon">+</span> 开启新话题
      </button>
      
      <div class="chat-list">
        <p class="list-title">近期聊天记录</p>
        <div v-if="loadingList" class="loading-text">加载列表中...</div>
        <div v-else-if="conversationList.length === 0" class="empty-text">暂无历史对话</div>
        
        <div
          v-else
          v-for="chat in conversationList"
          :key="String(chat.id)"
          class="chat-item"
          :class="{ active: String(currentChatId) === String(chat.id) }"
          @click="selectChat(String(chat.id))"
        >
          <div class="chat-info">
            <div class="title-editable">
              <span 
                class="chat-title"
                v-if="!editingTitleId || editingTitleId !== String(chat.id)"
                @dblclick.stop="startEditTitle(String(chat.id), chat.title)"
                title="双击编辑标题"
              >
                💬 {{ chat.title === '对话' ? '职业规划咨询' : (chat.title || '职业规划咨询') }}
              </span>
              <input
                v-else
                class="title-input"
                type="text"
                v-model="editingTitleText"
                @keyup.enter="saveEditTitle(String(chat.id))"
                @keyup.esc="cancelEditTitle"
                @blur="saveEditTitle(String(chat.id))"
                ref="titleInput"
                autofocus
              />
            </div>
            <span class="chat-time" v-if="chat.createTime">{{ chat.createTime.substring(5, 16).replace('T', ' ') }}</span>
          </div>
          <div class="chat-actions">
            <button class="edit-icon" @click.stop="startEditTitle(String(chat.id), chat.title)" title="编辑标题">✏️</button>
            <button class="delete-icon" @click.stop="deleteChat(String(chat.id))" title="删除记录">🗑️</button>
          </div>
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <div class="workspace-wrapper">
        <div class="page-header">
          <div class="title-badge">🚀 Agent 引擎已就绪</div>
          <h1 class="gradient-text">准备好规划你的职业未来了吗？</h1>
          <p class="subtitle">基于 <span class="highlight-number">10,000+</span> 真实企业招聘数据，AI 为你量身定制</p>
        </div>
        
        <div class="chat-container">
          <div class="message-container" ref="messageBox">
            
            <div v-if="messages.length === 0" class="welcome-screen">
              <h3>👋 你的专属领航员在此等候</h3>
              <p>请在下方输入你的问题，或者发送包含岗位的图片由我来解析</p>
            </div>
            
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-wrapper"
              :class="msg.role === 'user' ? 'is-user' : 'is-ai'"
            >
              <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
              
              <div class="message-bubble" :class="{ 'markdown-body': msg.role === 'ai' }">
                <div v-if="msg.imageUrl" class="bubble-image-wrapper">
                  <img :src="msg.imageUrl" alt="用户上传的图片" class="bubble-image" />
                </div>
                
                <span v-if="msg.role === 'ai'" v-html="renderMarkdown(msg.content)"></span>
                <span v-else>{{ msg.content }}</span>
                
                <span v-if="msg.isTypingEffect" class="blinking-cursor">▍</span>
              </div>
            </div>
            
            <div v-if="isWaitingResponse" class="message-wrapper is-ai">
              <div class="avatar">AI</div>
              <div class="message-bubble typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
          
          <div class="input-area">
            <div v-if="previewImageUrl" class="image-preview-area">
              <div class="preview-box">
                <img :src="previewImageUrl" alt="预览" />
                <button class="remove-img-btn" @click="removeSelectedImage" title="移除图片">✕</button>
              </div>
            </div>

            <div class="input-box">
              <input 
                type="file" 
                ref="imageInputRef" 
                accept="image/png, image/jpeg, image/jpg, image/gif, image/webp" 
                style="display: none;" 
                @change="onImageSelected"
              />
              
              <button class="tool-btn" @click="triggerImageUpload" title="上传图片" :disabled="isWaitingResponse || isTyping">
                🖼️
              </button>

              <textarea
                v-model="inputContent"
                placeholder="输入你的问题，按 Enter 发送..."
                @keydown.enter.prevent="sendMessage"
                :disabled="isWaitingResponse || isTyping"
              ></textarea>
              <button class="send-btn" @click="sendMessage" :disabled="(!inputContent.trim() && !previewImageUrl) || isWaitingResponse || isTyping">
                发送
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import axios from 'axios'
import { generateAesKeyAndIv, rsaEncrypt } from '../utils/crypto'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

axios.defaults.transformResponse = [
  function (data) {
    if (typeof data === 'string') {
      const regex = /:\s*([0-9]{16,})/g;
      data = data.replace(regex, ':"$1"');
      try { return JSON.parse(data); } catch (e) { return data; }
    }
    return data;
  }
];

axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) { config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}` }
  return config
}, error => Promise.reject(error))

const userId = ref(localStorage.getItem('userId') || '')
const conversationList = ref([])
const currentChatId = ref(null)
const messages = ref([])
const inputContent = ref('')
const isWaitingResponse = ref(false) 
const isTyping = ref(false)
const loadingList = ref(false)
const messageBox = ref(null)
const editingTitleId = ref(null)
const editingTitleText = ref('')
const titleInput = ref(null)
const imageInputRef = ref(null)
const selectedImageFile = ref(null)
const previewImageUrl = ref('')

// ==========================================
// 🌟 核心格式化引擎：把杂乱数据变成极简 Markdown (修复版)
// ==========================================
const formatText = (text) => {
  if (!text) return '';
  let str = String(text);

  str = str.replace(/\\"/g, '"').replace(/\\\\/g, '\\').replace(/\\n/g, '\n');

  if (str.includes('"career_pathway"') || str.includes('career_blueprint') || str.includes('career蓝图')) {
      let formatted = str
          .replace(/\{?"(?:career_pathway|career_blueprint|career蓝图)"\s*:\s*\{?/g, '')
          .replace(/"(?:title|标题)"\s*:\s*"([^"]+)"/g, '### 🎯 $1\n\n')
          .replace(/"(?:phases|阶段目标|阶段)"\s*:\s*\[?/g, '')
          .replace(/\{?"(?:phase|阶段|phase_\d+)"\s*:\s*"([^"]+)"/g, '\n#### 🌟 阶段：$1\n')
          .replace(/"(?:goal|goals|核心任务|目标)"\s*:\s*"([^"]+)"/g, '**📌 核心目标：** $1\n')
          .replace(/"(?:key_actions|关键成果|行动重点)"\s*:\s*\[?/g, '\n**🎯 关键行动：**\n')
          .replace(/\{?"(?:action|action_\d+)"\s*:\s*"([^"]+)"/g, '- $1 ')
          .replace(/"(?:timeline|时间线)"\s*:\s*"([^"]+)"/g, ' ($1)\n')
          .replace(/","/g, '\n- ')
          .replace(/"/g, '')
          .replace(/[\[\]{}]/g, '')
          .trim();
      if (formatted.includes('🎯') || formatted.includes('🌟')) return formatted; 
  }

  try {
      let obj = JSON.parse(str);
      if (obj.response) return typeof obj.response === 'string' ? obj.response : JSON.stringify(obj.response);
      if (obj.data) return typeof obj.data === 'string' ? obj.data : JSON.stringify(obj.data);
  } catch(e) {
      // 🌟 核心修复点 3：残缺 JSON 剥离技术
      // 如果打字机正在输入中，JSON不完整，暴力剥离前缀，保证实时排版美观！
      const match = str.match(/\{?"(?:response|data)"\s*:\s*"?([\s\S]*)/);
      if (match) {
          let extracted = match[1];
          // 去除末尾可能遗留的未闭合引号或括号
          extracted = extracted.replace(/(?:["}\]]|\\")*$/, '');
          return extracted;
      }
  }

  return str;
};

// ==========================================
// 🌟 Markdown 渲染器 (增强版：修复加粗、列表、换行)
// ==========================================
const renderMarkdown = (text) => {
  if (!text) return '';
  let html = text;
  html = html.replace(/</g, '&lt;').replace(/>/g, '&gt;');
  html = html.replace(/```([\s\S]*?)```/g, '<pre class="md-pre"><code class="md-code-block">$1</code></pre>');
  html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>');
  // 🌟 增强加粗正则，支持多行和复杂内容
  html = html.replace(/\*\*([\s\S]*?)\*\*/g, '<strong class="md-bold">$1</strong>');
  html = html.replace(/__([\s\S]*?)__/g, '<strong class="md-bold">$1</strong>');
  html = html.replace(/^### (.*$)/gim, '<h3 class="md-h3">$1</h3>');
  html = html.replace(/^#### (.*$)/gim, '<h4 class="md-h4">$1</h4>');
  html = html.replace(/^## (.*$)/gim, '<h2 class="md-h2">$1</h2>');
  // 支持无序列表
  html = html.replace(/^\s*[-*+]\s+(.*$)/gim, '<div class="md-list-item"><span class="md-bullet">•</span> $1</div>');
  // 支持数字有序列表 (1. 2. 3.)
  html = html.replace(/^\s*(\d+\.)\s+(.*$)/gim, '<div class="md-list-item"><span class="md-num">$1</span> $2</div>');
  html = html.replace(/\n/g, '<br>');
  return html;
};

const getUserInfo = async () => {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedAES = rsaEncrypt(aesKey)
    const encryptedIV = rsaEncrypt(aesIv)
    const res = await axios.get('/api/user/getUserInfo', { params: { IV: encryptedIV, AES: encryptedAES } })
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      userId.value = String(res.data.data.id)
      localStorage.setItem('userId', userId.value)
    }
  } catch (error) { console.error('获取用户信息失败', error) }
}

const fetchChatList = async () => {
  if (!userId.value) return
  loadingList.value = true
  try {
    const res = await axios.get(`/api/ai-conversation/list/${userId.value}`)
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      conversationList.value = res.data.data || []
      if (conversationList.value.length > 0 && !currentChatId.value) {
        selectChat(String(conversationList.value[0].id))
      }
    }
  } catch (error) { console.error('获取列表失败', error) } 
  finally { loadingList.value = false }
}

const selectChat = async (id) => {
  const safeId = String(id)
  currentChatId.value = safeId
  messages.value = [] 
  
  try {
    const res = await axios.get(`/api/ai-conversation/history/${safeId}`)
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      let rawMessages = Array.isArray(res.data.data) ? res.data.data : (res.data.data?.messages || []);
      const parsedMessages = []
      
      rawMessages.forEach((item) => {
        try {
          let role = item.type === 1 ? 'user' : 'ai';
          let contentStr = '';
          if (item.content && item.content.response) {
              contentStr = typeof item.content.response === 'string' ? item.content.response : JSON.stringify(item.content.response);
          } else {
              contentStr = typeof item.content === 'string' ? item.content : JSON.stringify(item.content);
          }
          
          let finalContent = formatText(contentStr);
          let imageUrl = null;
          if (item.contextInfo && item.contextInfo.imageUrl) {
              imageUrl = item.contextInfo.imageUrl;
          }
          parsedMessages.push({ role, content: finalContent, imageUrl: role === 'user' ? imageUrl : null })
        } catch (e) { console.error('解析报错：', e, item) }
      })
      messages.value = parsedMessages
      scrollToBottom()
    }
  } catch (error) { console.error('获取记录失败', error) }
}

const createNewChat = async (customTitle) => {
  if (!userId.value) return null
  try {
    const res = await axios.post('/api/ai-conversation/create', { user_id: userId.value, conversation_type: 1, title: customTitle || "规划咨询" })
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      const newId = String(res.data.data.id || res.data.data.conversationId)
      currentChatId.value = newId
      messages.value = []
      await fetchChatList()
      return newId
    }
  } catch (error) { console.error('创建失败', error) }
  return null
}

const triggerImageUpload = () => { if (imageInputRef.value) imageInputRef.value.click() }
const removeSelectedImage = () => { selectedImageFile.value = null; previewImageUrl.value = ''; }
const onImageSelected = (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) return alert('图片不能超过 5MB 哦！')
  selectedImageFile.value = file
  previewImageUrl.value = URL.createObjectURL(file)
}

const fileToBase64 = (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader(); reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
  });
};

// ==========================================
// 🌟 修复版流式发送引擎
// ==========================================
const sendMessage = async () => {
  if ((!inputContent.value.trim() && !previewImageUrl.value) || isWaitingResponse.value || isTyping.value) return

  const textToSend = inputContent.value.trim() || '请帮我分析这张图片';
  const hasImage = !!selectedImageFile.value;
  const currentImageUrl = previewImageUrl.value;
  
  isWaitingResponse.value = true;
  isTyping.value = true;
  inputContent.value = ''; 

  let isNewConversation = false
  let tempConversationId = currentChatId.value
  let base64ImageToBackend = null;

  try {
    if (hasImage) {
      base64ImageToBackend = await fileToBase64(selectedImageFile.value);
    }
    removeSelectedImage();

    if (!tempConversationId) {
      const newId = await createNewChat(textToSend.substring(0, 12))
      if (!newId) throw new Error("会话创建失败")
      tempConversationId = newId
      currentChatId.value = newId
      isNewConversation = true
    }

    messages.value.push({ role: 'user', content: textToSend, imageUrl: currentImageUrl });
    scrollToBottom();
    
    const token = localStorage.getItem('token') || ''
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {})
    }

    const apiEndpoint = hasImage ? '/api/ai-conversation/send-with-image-stream' : '/api/ai-conversation/send-stream';

    const payload = {
      user_id: userId.value,
      content: textToSend,
      conversation_id: tempConversationId,
      conversation_type: 1
    };
    if (hasImage) payload.image_url = base64ImageToBackend;

    const response = await fetch(apiEndpoint, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

    isWaitingResponse.value = false; 
    
    const newMsgIndex = messages.value.length;
    messages.value.push({ role: 'ai', content: '', rawContent: '', isTypingEffect: true });

    let charQueue = [];
    let backendDone = false;

    const typeTimer = setInterval(() => {
      if (charQueue.length > 0) {
        const takeCount = Math.floor(Math.random() * 3) + 1;
        const chars = charQueue.splice(0, takeCount).join('');
        
        messages.value[newMsgIndex].rawContent += chars;
        messages.value[newMsgIndex].content = formatText(messages.value[newMsgIndex].rawContent);
        scrollToBottom();
      } 
      else if (backendDone) {
        clearInterval(typeTimer);
        messages.value[newMsgIndex].isTypingEffect = false;
        isTyping.value = false; 
        scrollToBottom();
        
        if (isNewConversation) {
          setTimeout(async () => {
            await updateConversationTitle(tempConversationId, textToSend.substring(0, 15) + '...');
            await fetchChatList();
          }, 500);
        }
      }
    }, 25);

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');

    while (true) {
      const { done, value } = await reader.read();
      if (done) break; 
      
      const decoded = decoder.decode(value, { stream: true });
      const lines = decoded.split('\n');

      for (let line of lines) {
         if (!line.trim()) continue;
         let rawStream = line.replace(/^data:\s*/gm, '').trim();
         if (!rawStream) continue;
         
         let chunkText = rawStream;
         try {
            let parsed = JSON.parse(rawStream);
            if (parsed.data) chunkText = parsed.data;
            else if (parsed.response) chunkText = parsed.response;
         } catch(e) {}
         
         // 🌟 核心修复点 1：使用 Array.from() 拆分，完美保留 Emoji 图标不乱码！
         charQueue.push(...Array.from(chunkText));
      }
    }
    backendDone = true;

  } catch (error) {
    console.error('流式请求错误:', error);
    isWaitingResponse.value = false;
    isTyping.value = false; 
    messages.value.push({ role: 'ai', content: '连接异常，请检查后端流式服务。' });
  }
}

const updateConversationTitle = async (conversationId, newTitle) => {
  try { await axios.put('/api/ai-conversation/update-title', { user_id: userId.value, conversation_id: conversationId, newTitle: newTitle }); return true } 
  catch (error) { return false }
}

const startEditTitle = (conversationId, currentTitle) => {
  editingTitleId.value = conversationId
  editingTitleText.value = currentTitle === '对话' ? '职业规划咨询' : (currentTitle || '职业规划咨询')
  nextTick(() => { if (titleInput.value && titleInput.value[0]) { titleInput.value[0].focus(); titleInput.value[0].select() } })
}

const saveEditTitle = async (conversationId) => {
  if (!editingTitleText.value.trim()) { cancelEditTitle(); return }
  if (await updateConversationTitle(conversationId, editingTitleText.value.trim())) await fetchChatList()
  editingTitleId.value = null; editingTitleText.value = ''
}

const cancelEditTitle = () => { editingTitleId.value = null; editingTitleText.value = '' }

const deleteChat = async (id) => {
  if (!confirm('确认删除这条对话吗？')) return
  try {
    await axios.delete(`/api/ai-conversation/end/${id}`)
    if (String(currentChatId.value) === String(id)) { currentChatId.value = null; messages.value = [] }
    fetchChatList()
  } catch (error) { console.error('删除失败', error) }
}

const scrollToBottom = () => { nextTick(() => { if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight }) }
const prepareNewChat = () => { currentChatId.value = null; messages.value = [] }

const handleAutoPrompt = (promptText) => {
  setTimeout(() => {
    prepareNewChat()
    inputContent.value = promptText 
    const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
    window.history.replaceState({}, '', cleanUrl)
  }, 100) 
}

onMounted(async () => {
  await getUserInfo()
  await fetchChatList()
  if (route.query.autoPrompt) handleAutoPrompt(route.query.autoPrompt)
})

watch(() => route.query.autoPrompt, (newVal) => { if (newVal) handleAutoPrompt(newVal) })
</script>

<style scoped>
/* 保持所有原本美丽的样式不变 */
.agent-page { position: relative; width: 100vw; height: 100vh; display: flex; background: #F4F7FA; overflow: hidden; font-family: 'Inter', -apple-system, sans-serif;}
.ambient-glow { position: absolute; width: 500px; height: 500px; border-radius: 50%; filter: blur(100px); opacity: 0.4; z-index: 0; pointer-events: none; }
.glow-blue { top: -100px; left: 200px; background: radial-gradient(circle, rgba(74,144,226,0.3) 0%, rgba(255,255,255,0) 70%); }
.glow-purple { bottom: -150px; right: 50px; background: radial-gradient(circle, rgba(139,92,246,0.2) 0%, rgba(255,255,255,0) 70%); }
.sidebar { width: 280px; position: relative; z-index: 2; border-right: 1px solid rgba(255,255,255,0.5); display: flex; flex-direction: column; padding: 20px; box-sizing: border-box; }
.glass-panel { background-color: rgba(255, 255, 255, 0.4); backdrop-filter: blur(20px); }
.new-chat-btn { width: 100%; padding: 14px; background: rgba(255,255,255,0.8); color: #4A90E2; border: 1px dashed #4A90E2; border-radius: 12px; font-weight: bold; cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.02); }
.new-chat-btn:hover { background: #4A90E2; color: #FFFFFF; }
.chat-list { margin-top: 25px; flex: 1; overflow-y: auto; }
.list-title { font-size: 0.85rem; color: #94A3B8; font-weight: bold; margin-bottom: 10px; padding-left: 5px; }
.empty-text, .loading-text { text-align: center; color: #94A3B8; font-size: 0.9rem; margin-top: 20px; }
.chat-item { padding: 12px 15px; margin-bottom: 8px; border-radius: 10px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: 0.2s; background: rgba(255,255,255,0.5); border: 1px solid transparent; }
.chat-item:hover { background: rgba(255,255,255,0.8); }
.chat-item.active { background: #FFFFFF; border-color: rgba(74, 144, 226, 0.3); box-shadow: 0 4px 10px rgba(0,0,0,0.03); }
.chat-info { display: flex; flex-direction: column; gap: 4px; overflow: hidden; }
.title-editable { position: relative; }
.chat-title { color: #334155; font-size: 0.9rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 150px; cursor: text; padding: 2px 4px; border-radius: 4px; transition: background-color 0.2s; }
.chat-title:hover { background-color: rgba(74, 144, 226, 0.1); }
.title-input { width: 150px; font-size: 0.9rem; font-weight: 600; color: #334155; background: white; border: 1px solid #4A90E2; border-radius: 4px; padding: 2px 6px; outline: none; box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.2); }
.chat-time { font-size: 0.75rem; color: #94A3B8; padding-left: 22px; }
.chat-actions { display: flex; gap: 5px; }
.edit-icon, .delete-icon { background: none; border: none; cursor: pointer; opacity: 0; transition: 0.2s; filter: grayscale(100%); font-size: 0.9rem; }
.edit-icon:hover { filter: none; color: #4A90E2; }
.delete-icon:hover { filter: none; color: #EF4444; }
.chat-item:hover .edit-icon, .chat-item:hover .delete-icon { opacity: 1; }
.chat-main { flex: 1; display: flex; justify-content: center; align-items: center; position: relative; z-index: 1; }
.workspace-wrapper { width: 100%; max-width: 850px; display: flex; flex-direction: column; gap: 20px; }
.page-header { text-align: center; margin-bottom: 20px; animation: fadeInDown 0.8s ease-out; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.title-badge { display: inline-block; padding: 6px 16px; background: rgba(74, 144, 226, 0.1); color: #4A90E2; border-radius: 20px; font-size: 0.85rem; font-weight: 600; border: 1px solid rgba(74, 144, 226, 0.2); letter-spacing: 0.5px; }
.gradient-text { font-size: 1.9rem; margin: 0; font-weight: 800; letter-spacing: 1px; background: linear-gradient(135deg, #2C3E50 0%, #4A90E2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.subtitle { font-size: 1.05rem; color: #64748B; margin: 0; }
.highlight-number { color: #4A90E2; font-weight: 800; font-size: 1.15rem; background: rgba(74, 144, 226, 0.08); padding: 2px 6px; border-radius: 4px; }
.chat-container { width: 100%; height: 75vh; background-color: rgba(255, 255, 255, 0.7); backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 16px; box-shadow: 0 10px 40px rgba(0, 0, 0, 0.05); display: flex; flex-direction: column; overflow: hidden; animation: fadeInUp 0.8s ease-out; }
.message-container { flex: 1; padding: 30px; overflow-y: auto; display: flex; flex-direction: column; gap: 20px; scroll-behavior: smooth; }
.welcome-screen { text-align: center; margin-top: 10vh; color: #64748B; }
.welcome-screen h3 { color: #1E293B; font-size: 1.5rem; margin-bottom: 10px; }
.message-wrapper { display: flex; gap: 15px; max-width: 85%; }
.message-wrapper.is-user { align-self: flex-end; flex-direction: row-reverse; }
.message-wrapper.is-ai { align-self: flex-start; }
.avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #60A5FA, #4A90E2); color: white; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 0.9rem; flex-shrink: 0; box-shadow: 0 4px 10px rgba(74,144,226,0.3); }
.is-user .avatar { background: linear-gradient(135deg, #34D399, #10B981); box-shadow: 0 4px 10px rgba(16,185,129,0.3); }
.message-bubble { padding: 15px 20px; border-radius: 16px; font-size: 1rem; line-height: 1.7; box-shadow: 0 4px 15px rgba(0,0,0,0.03); word-break: break-word; }
.is-user .message-bubble { background: linear-gradient(135deg, #60A5FA, #4A90E2); color: white; border-top-right-radius: 4px; white-space: pre-wrap; }
.is-ai .message-bubble { background: rgba(255,255,255,0.9); border-top-left-radius: 4px; border: 1px solid rgba(255,255,255,1); color: #334155; }
.bubble-image-wrapper { margin-bottom: 10px; max-width: 300px; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.bubble-image { width: 100%; height: auto; display: block; border-radius: 8px; }
:deep(.markdown-body) { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; font-size: 1rem; }
:deep(.md-bold) { color: #1E293B; font-weight: 800; }
:deep(.md-h2), :deep(.md-h3), :deep(.md-h4) { margin: 15px 0 10px 0; color: #1E293B; font-weight: 800; }
:deep(.md-h2) { font-size: 1.25rem; border-bottom: 1px solid #E2E8F0; padding-bottom: 5px; }
:deep(.md-h3) { font-size: 1.15rem; color: #2563EB; }
:deep(.md-h4) { font-size: 1.05rem; color: #4A90E2; }
:deep(.md-list-item) { display: flex; align-items: flex-start; margin-bottom: 6px; line-height: 1.6; }
:deep(.md-bullet) { margin-right: 8px; color: #4A90E2; font-weight: bold; }
:deep(.md-num) { margin-right: 8px; color: #4A90E2; font-weight: bold; min-width: 20px; }
:deep(.md-inline-code) { background: rgba(74, 144, 226, 0.1); color: #2563EB; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 0.9rem; }
:deep(.md-pre) { background: #1E293B; padding: 12px; border-radius: 8px; overflow-x: auto; margin: 10px 0; }
:deep(.md-code-block) { color: #E2E8F0; font-family: monospace; font-size: 0.9rem; white-space: pre-wrap;}
.blinking-cursor { font-weight: bold; color: #4A90E2; animation: blink 1s step-end infinite; margin-left: 2px; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
.typing-indicator { display: flex; gap: 5px; align-items: center; padding: 15px 25px; }
.typing-indicator span { width: 8px; height: 8px; background: #94A3B8; border-radius: 50%; animation: bounce 1.4s infinite ease-in-out; }
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
.input-area { padding: 15px 30px; background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(10px); border-top: 1px solid rgba(255, 255, 255, 0.6); display: flex; flex-direction: column; gap: 10px; }
.image-preview-area { padding-bottom: 5px; }
.preview-box { position: relative; display: inline-block; width: 60px; height: 60px; border-radius: 8px; border: 2px solid #4A90E2; padding: 2px; background: white; }
.preview-box img { width: 100%; height: 100%; object-fit: cover; border-radius: 4px; }
.remove-img-btn { position: absolute; top: -8px; right: -8px; width: 20px; height: 20px; border-radius: 50%; background: #EF4444; color: white; border: none; font-size: 0.7rem; font-weight: bold; cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0; box-shadow: 0 2px 5px rgba(0,0,0,0.2); }
.remove-img-btn:hover { background: #DC2626; }
.input-box { display: flex; gap: 12px; background: rgba(255,255,255,0.8); border: 1px solid #E2E8F0; border-radius: 12px; padding: 6px 8px; transition: 0.3s; box-shadow: inset 0 2px 4px rgba(0,0,0,0.02); align-items: center; }
.input-box:focus-within { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.15); }
.tool-btn { background: none; border: none; font-size: 1.4rem; cursor: pointer; padding: 5px; opacity: 0.7; transition: 0.2s; border-radius: 8px; }
.tool-btn:hover:not(:disabled) { opacity: 1; background: #F1F5F9; }
.tool-btn:disabled { opacity: 0.3; cursor: not-allowed; }
textarea { flex: 1; height: 40px; min-height: 40px; max-height: 120px; border: none; background: transparent; outline: none; resize: none; font-size: 1rem; color: #1E293B; font-family: inherit; padding: 8px 5px; line-height: 24px; }
textarea::placeholder { color: #94A3B8; }
.send-btn { background: linear-gradient(135deg, #60A5FA, #4A90E2); color: white; border: none; border-radius: 10px; padding: 0 25px; height: 40px; font-weight: bold; cursor: pointer; transition: 0.2s; box-shadow: 0 4px 10px rgba(74,144,226,0.3); }
.send-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(74,144,226,0.4); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
@keyframes fadeInDown { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>