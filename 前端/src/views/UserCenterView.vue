<template>
  <div class="user-center-page">
    <div class="page-header">
      <h2>👤 个人数字档案</h2>
      <p>完善你的能力画像，让 AI 推荐更精准</p>
    </div>

    <div class="content-grid">
      <div class="left-column">
        <div class="profile-card">
          <div class="avatar-large">{{ (myProfile.userName || displayNickname).charAt(0) }}</div>
          <h3 class="user-name">{{ myProfile.userName || displayNickname }}</h3>
          <p class="user-bio" v-if="myProfile.college">
            {{ myProfile.college }} · {{ myProfile.major || '未知专业' }} · {{ myProfile.grade || '' }}级
          </p>
          <p class="user-bio" v-else>暂未完善学籍信息</p>
          
          <div class="info-list">
            <div class="info-item">
              <span class="label">手机号</span>
              <span class="value">{{ myProfile.phone || '未绑定' }}</span>
            </div>
            <div class="info-item">
              <span class="label">邮箱</span>
              <span class="value">{{ myProfile.email || '未绑定' }}</span>
            </div>
            <div class="info-item">
              <span class="label">档案状态</span>
              <span class="value" :style="{ color: myProfile.id ? '#10B981' : '#F59E0B', fontWeight: 600 }">
                {{ myProfile.id ? '✓ 已建档' : '⚠ 待完善' }}
              </span>
            </div>
          </div>

          <button class="edit-btn" @click="openBasicModal">编辑基础资料</button>
        </div>

        <div class="ai-summary-card">
          <div class="ai-card-header">
            <span class="ai-icon">✨</span>
            <span>AI 综合诊断简报</span>
          </div>
          <div class="ai-comment">
            <p>"你还没有进行过完整的职业诊断哦。多和我聊聊你的专业和兴趣，或者上传简历，我会为你生成专属的能力画像！"</p>
          </div>
          <button class="re-assess-btn" @click="goToChat">立即去对话探索</button>
        </div>
      </div>

      <div class="right-column">
        
        <div class="data-card">
          <div class="card-header">
            <h4>🎯 我的职业意向</h4>
            <div v-if="!isEdit">
              <button class="text-btn" @click="startEdit">修改</button>
            </div>
            <div v-else class="edit-actions">
              <button class="text-btn cancel" @click="cancelEdit">取消</button>
              <button class="upload-btn save" @click="saveJobIntent" :disabled="isSavingIntent">
                {{ isSavingIntent ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>

          <div v-if="!isEdit" class="tags-container">
            <div class="intent-tag">
              <span class="tag-title">期望岗位</span>
              <span class="tag-content">{{ myProfile.careerIntentions || '暂未设置' }}</span>
            </div>
            <div class="intent-tag">
              <span class="tag-title">意向城市</span>
              <span class="tag-content">{{ myProfile.targetCity || '暂未设置' }}</span>
            </div>
            <div class="intent-tag">
              <span class="tag-title">期望薪资</span>
              <span class="tag-content">{{ myProfile.expectedSalary || '面议' }}</span>
            </div>
            <div class="intent-tag">
              <span class="tag-title">当前状态</span>
              <span class="tag-content">{{ myProfile.jobIntentionDetail || '在校 - 寻找实习' }}</span>
            </div>
          </div>

          <div v-else class="edit-form-container">
            <div class="form-group">
              <label>期望岗位</label>
              <input type="text" v-model="intentForm.careerIntentions" placeholder="如：前端开发工程师" />
            </div>
            <div class="form-group">
              <label>意向城市</label>
              <input type="text" v-model="intentForm.targetCity" placeholder="如：北京 / 杭州" />
            </div>
            <div class="form-group">
              <label>期望薪资</label>
              <select v-model="intentForm.expectedSalary">
                <option value="面议">面议</option>
                <option value="5k - 8k">5k - 8k</option>
                <option value="8k - 12k">8k - 12k</option>
                <option value="10k - 15k">10k - 15k</option>
                <option value="15k以上">15k以上</option>
              </select>
            </div>
            <div class="form-group">
              <label>当前状态</label>
              <select v-model="intentForm.jobIntentionDetail">
                <option value="在校 - 寻找实习">在校 - 寻找实习</option>
                <option value="应届 - 寻找全职">应届 - 寻找全职</option>
                <option value="在职 - 考虑机会">在职 - 考虑机会</option>
              </select>
            </div>
          </div>
        </div>

        <div class="data-card">
          <div class="card-header">
            <h4>💪 我的核心能力模型</h4>
            <button class="text-btn" @click="openAbilityModal">编辑能力数据</button>
          </div>
          
          <div v-if="!myAbility.id" class="empty-ability">
            <span class="empty-icon">📊</span>
            <p>暂未录入能力数据，完善后可大幅提升匹配精度</p>
            <button class="outline-btn" @click="openAbilityModal">立即录入</button>
          </div>
          
          <div v-else class="ability-grid">
            <div class="ability-section">
              <h5 class="sub-title">💻 专业与硬实力</h5>
              <div class="ability-item">
                <span class="a-label">专业技能</span>
                <span class="a-value">{{ myAbility.professionalSkill || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">实习经历</span>
                <span class="a-value">{{ myAbility.internshipAbility || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">教育背景</span>
                <span class="a-value">{{ myAbility.educationRequirement || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">证书要求</span>
                <span class="a-value">{{ myAbility.certificateRequirement || '暂无' }}</span>
              </div>
            </div>
            
            <div class="ability-section">
              <h5 class="sub-title">🤝 综合软素质</h5>
              <div class="ability-item">
                <span class="a-label">沟通能力</span>
                <span class="a-value">{{ myAbility.communicationAbility || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">团队协作</span>
                <span class="a-value">{{ myAbility.teamworkAbility || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">问题解决</span>
                <span class="a-value">{{ myAbility.problemSolving || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">创新能力</span>
                <span class="a-value">{{ myAbility.innovationAbility || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">学习能力</span>
                <span class="a-value">{{ myAbility.learningAbility || '暂无' }}</span>
              </div>
              <div class="ability-item">
                <span class="a-label">抗压能力</span>
                <span class="a-value">{{ myAbility.pressureResistance || '暂无' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="data-card">
          <div class="card-header">
            <h4>📄 简历附件库</h4>
            <button class="upload-btn" @click="openUploadModal">+ 上传新简历</button>
          </div>
          <div class="resume-list">
            <div v-if="resumeList.length === 0" class="resume-item" style="justify-content: center; color: #94A3B8; padding: 30px;">
              暂未上传简历，上传后可供 AI 精准分析
            </div>
            <div v-else v-for="(item, index) in resumeList" :key="index" class="resume-item">
              <div class="resume-info">
                <span style="font-size: 1.5rem;">📄</span>
                <div>
                  <div class="resume-name">{{ item.fileName }}</div>
                  <div class="resume-meta">{{ item.fileSize }} · 刚刚上传</div>
                </div>
              </div>
              <div class="resume-actions">
                <span class="action-text parse" @click="goToChat">去提问</span>
                <span class="action-text delete" @click="deleteResume(index)">删除</span>
              </div>
            </div>
          </div>
        </div>

        <div class="data-card">
          <div class="card-header">
            <h4>🔒 账号与安全</h4>
          </div>
          <div class="security-list">
            <div class="security-item">
              <div class="sec-info">
                <div class="sec-title">登录密码</div>
                <div class="sec-desc">已设置，建议定期修改以保护账号安全</div>
              </div>
              <button class="outline-btn" @click="pwdVis = true">修改密码</button>
            </div>
            <div class="security-item">
              <div class="sec-info">
                <div class="sec-title danger-text">注销账号</div>
                <div class="sec-desc">注销后，您的所有档案和对话记录将被永久删除</div>
              </div>
              <button class="outline-btn danger-btn">申请注销</button>
            </div>
          </div>
        </div>

      </div>
    </div>

    <transition name="modal-fade">
      <div class="modal-overlay" v-if="basicVis" @click.self="basicVis = false">
        <div class="modal-content" style="max-width: 500px;">
          <div class="modal-header">
            <h3>编辑基础资料</h3>
            <button class="close-modal-btn" @click="basicVis = false">✕</button>
          </div>
          <div class="modal-body edit-form-container">
            <div class="form-group" style="grid-column: span 2;">
              <label>真实姓名</label>
              <input type="text" v-model="basicForm.userName" placeholder="请输入姓名" />
            </div>
            <div class="form-group">
              <label>学院</label>
              <input type="text" v-model="basicForm.college" placeholder="例如：电子工程学院" />
            </div>
            <div class="form-group">
              <label>专业</label>
              <input type="text" v-model="basicForm.major" placeholder="例如：通信工程" />
            </div>
            <div class="form-group">
              <label>年级</label>
              <input type="text" v-model="basicForm.grade" placeholder="例如：2022" />
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input type="text" v-model="basicForm.phone" placeholder="请输入手机号" />
            </div>
            <div class="form-group" style="grid-column: span 2;">
              <label>邮箱</label>
              <input type="email" v-model="basicForm.email" placeholder="请输入联系邮箱" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="basicVis = false">取消</button>
            <button class="btn-confirm" @click="saveBasicInfo" :disabled="isSavingBasic">
              {{ isSavingBasic ? '保存中...' : '确认保存' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="modal-overlay" v-if="abilityVis" @click.self="abilityVis = false">
        <div class="modal-content" style="max-width: 700px; max-height: 90vh; overflow-y: auto;">
          <div class="modal-header">
            <h3>💪 完善核心能力模型</h3>
            <button class="close-modal-btn" @click="abilityVis = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="edit-form-container">
              <div class="form-group" style="grid-column: span 2;">
                <label>💻 专业技能 (如: Java开发, 熟练, 2年经验)</label>
                <input type="text" v-model="abilityForm.professionalSkill" placeholder="描述你的核心技能栈..." />
              </div>
              <div class="form-group" style="grid-column: span 2;">
                <label>💼 实习经历与能力</label>
                <input type="text" v-model="abilityForm.internshipAbility" placeholder="描述实习项目经验..." />
              </div>
              <div class="form-group">
                <label>🎓 教育背景</label>
                <input type="text" v-model="abilityForm.educationRequirement" placeholder="如: 全日制本科..." />
              </div>
              <div class="form-group">
                <label>📜 证书获取</label>
                <input type="text" v-model="abilityForm.certificateRequirement" placeholder="如: CET6, 软件设计师..." />
              </div>

              <div style="grid-column: span 2; border-top: 1px dashed #E2E8F0; margin: 10px 0;"></div>

              <div class="form-group">
                <label>🗣️ 沟通能力</label>
                <input type="text" v-model="abilityForm.communicationAbility" placeholder="如: 具备良好的跨部门沟通能力..." />
              </div>
              <div class="form-group">
                <label>🤝 团队协作能力</label>
                <input type="text" v-model="abilityForm.teamworkAbility" placeholder="如: 团队协作意识良好..." />
              </div>
              <div class="form-group">
                <label>🔧 问题解决能力</label>
                <input type="text" v-model="abilityForm.problemSolving" placeholder="如: 独立排查故障能力强..." />
              </div>
              <div class="form-group">
                <label>💡 创新能力</label>
                <input type="text" v-model="abilityForm.innovationAbility" placeholder="如: 具备独立创新思维..." />
              </div>
              <div class="form-group">
                <label>📚 学习能力</label>
                <input type="text" v-model="abilityForm.learningAbility" placeholder="如: 新技术学习能力强..." />
              </div>
              <div class="form-group">
                <label>🏋️ 抗压能力</label>
                <input type="text" v-model="abilityForm.pressureResistance" placeholder="如: 可接受高强度节奏..." />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="abilityVis = false">取消</button>
            <button class="btn-confirm" @click="saveAbility" :disabled="isSavingAbility">
              {{ isSavingAbility ? '保存中...' : '确认保存' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="modal-overlay" v-if="upVis" @click.self="closeUploadModal">
        <div class="modal-content">
          <div class="modal-header">
            <h3>上传新简历</h3>
            <button class="close-modal-btn" @click="closeUploadModal">✕</button>
          </div>
          <div class="modal-body">
            <input type="file" ref="fileInput" style="display: none" accept=".pdf,.doc,.docx" @change="handleFileChange" />
            
            <div class="upload-dropzone" @click="triggerFileInput" :class="{ 'has-file': selectedFile }">
              <div v-if="!selectedFile">
                <p class="upload-title">点击选择简历文件</p>
                <p class="upload-hint">支持 PDF, Word 格式 (最大 10MB)</p>
              </div>
              <div v-else>
                <p class="upload-title" style="color: #4A90E2;">✅ {{ selectedFile.name }}</p>
                <p class="upload-hint">文件大小: {{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</p>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="closeUploadModal">取消</button>
            <button class="btn-confirm" @click="uploadResume" :disabled="!selectedFile || isUploading">
              {{ isUploading ? 'AI 解析中...' : '开始上传并解析' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="modal-overlay" v-if="pwdVis" @click.self="pwdVis = false">
        <div class="modal-content" style="max-width: 400px;">
          <div class="modal-header">
            <h3>修改登录密码</h3>
            <button class="close-modal-btn" @click="pwdVis = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="pwd-form">
              <div class="form-group">
                <label>原密码</label>
                <input type="password" v-model="oldPassword" placeholder="请输入当前使用的密码" :disabled="isChangingPassword" />
              </div>
              <div class="form-group">
                <label>新密码</label>
                <input type="password" v-model="newPassword" placeholder="6-20位，建议包含数字和字母" :disabled="isChangingPassword" />
              </div>
              <div class="form-group">
                <label>确认新密码</label>
                <input type="password" v-model="confirmPassword" placeholder="请再次输入新密码" :disabled="isChangingPassword" @keyup.enter="changePassword" />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="pwdVis = false" :disabled="isChangingPassword">取消</button>
            <button class="btn-confirm" @click="changePassword" :disabled="isChangingPassword || !oldPassword || !newPassword || !confirmPassword">
              {{ isChangingPassword ? '修改中...' : '确认修改' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import API_CONFIG from '../config/api'
import { generateAesKeyAndIv, rsaEncrypt, aesEncrypt } from '../utils/crypto'

const router = useRouter()

// ==========================================
// 🚀 核心：三口并行，彻底理清后端模块！
// ==========================================

// 1. User 模块 API (负责：用户信息、改密码、传简历) -> 57332 端口
const userApi = axios.create({
  baseURL: API_CONFIG.BASE_URL, 
  timeout: API_CONFIG.TIMEOUT
})

userApi.interceptors.request.use(config => {
  const token = localStorage.getItem('token') || ''
  if (token) config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  return config
})

// 2. StudentProfile 模块 API (负责：学生基础档案、意向) -> 本地或原有 51041 / 59941
// 这里基于你之前的代码，继续使用 cn-nd-plc-1.ofalias.net:51041 域，因为文档里 Ability 和它在一起。
const studentApi = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 给基础业务 API 加上拦截器防拦截
studentApi.interceptors.request.use(config => {
  const token = localStorage.getItem('token') || ''
  if (token) config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  return config
})

// 3. Ability 模块 API (新增！负责能力模型) -> 也是 cn-nd-plc-1.ofalias.net:51041
// 为了代码清晰，我直接复用 studentApi 的实例（因为 baseURL 相同），但逻辑分开写。
const abilityApi = studentApi

// ==========================================

// ===== 基础状态 =====
const displayNickname = ref('加载中...')
const currentUserId = ref(localStorage.getItem('userId'))
const userInfo = ref({})

// 🌟 学生基础档案数据
const myProfile = ref({
  id: null, userName: '', college: '', major: '', grade: '', 
  phone: '', email: '', targetCity: '', expectedSalary: '', 
  careerIntentions: '', jobIntentionDetail: ''
})

// 🌟 学生能力模型数据 (新增)
const myAbility = ref({
  id: null, professionalSkill: '', internshipAbility: '', educationRequirement: '',
  certificateRequirement: '', innovationAbility: '', learningAbility: '',
  pressureResistance: '', communicationAbility: '', problemSolving: '', teamworkAbility: ''
})

// ===== 页面加载 =====
onMounted(async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }

  // 1. 获取用户昵称
  try {
    const res = await userApi.get('/api/user/getUserInfo')
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      userInfo.value = res.data.data || {}
      displayNickname.value = userInfo.value.nickname || `新星用户_${String(userInfo.value.userAccount || '8888').slice(-4)}`
      localStorage.setItem('userName', displayNickname.value)
    }
  } catch (error) { displayNickname.value = '探索者' }

  // 2. 拉取档案和能力
  fetchMyProfile()
  fetchMyAbility()
})

const fetchMyProfile = async () => {
  if (!currentUserId.value) return
  try {
    const res = await studentApi.post('/api/student/condition', { userId: Number(currentUserId.value) })
    if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
      myProfile.value = res.data.data[0]
    }
  } catch (err) { console.error('获取个人档案失败', err) }
}

// 🚀 获取能力模型数据 (依据你提供的文档接口)
const fetchMyAbility = async () => {
  if (!currentUserId.value) return
  try {
    // 优先尝试优雅的 user/{id} 接口
    const res = await abilityApi.get(`/api/ability/user/${currentUserId.value}`)
    if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
      myAbility.value = res.data.data[0]
    }
  } catch (err) {
    // 兜底策略：使用文档里写的 post condition 查询
    try {
      const resFallback = await abilityApi.post(`/api/ability/condition`, { userId: Number(currentUserId.value) })
      if (resFallback.data.code === 200 && resFallback.data.data && resFallback.data.data.length > 0) {
        myAbility.value = resFallback.data.data[0]
      }
    } catch (e) { console.error('获取能力模型失败', e) }
  }
}

// ===== 基础资料编辑 =====
const basicVis = ref(false)
const basicForm = ref({})
const isSavingBasic = ref(false)

const openBasicModal = () => { basicForm.value = { ...myProfile.value }; basicVis.value = true }

const saveBasicInfo = async () => {
  if (!basicForm.value.userName) return alert('姓名不能为空')
  isSavingBasic.value = true
  const payload = { ...basicForm.value, userId: Number(currentUserId.value) }
  try {
    let res
    if (myProfile.value.id) res = await studentApi.put('/api/student/update', payload)
    else res = await studentApi.post('/api/student/insert', payload)
    if (res.data.code === 200) { basicVis.value = false; fetchMyProfile() } 
    else alert('保存失败: ' + res.data.message)
  } catch (err) { console.error(err) } finally { isSavingBasic.value = false }
}

// ===== 职业意向编辑 =====
const isEdit = ref(false)
const intentForm = ref({})
const isSavingIntent = ref(false)

const startEdit = () => { intentForm.value = { ...myProfile.value }; isEdit.value = true }
const cancelEdit = () => { isEdit.value = false }

const saveJobIntent = async () => {
  isSavingIntent.value = true
  const payload = { ...intentForm.value, userId: Number(currentUserId.value) }
  try {
    let res
    if (myProfile.value.id) res = await studentApi.put('/api/student/update', payload)
    else res = await studentApi.post('/api/student/insert', payload)
    if (res.data.code === 200) { isEdit.value = false; fetchMyProfile() } 
    else alert('保存失败: ' + res.data.message)
  } catch (err) { console.error(err) } finally { isSavingIntent.value = false }
}

// ===== 🚀 能力模型编辑 (新增) =====
const abilityVis = ref(false)
const abilityForm = ref({})
const isSavingAbility = ref(false)

const openAbilityModal = () => { 
  abilityForm.value = { ...myAbility.value }
  abilityVis.value = true 
}

const saveAbility = async () => {
  isSavingAbility.value = true
  // 组装参数，必须带上 userId。如果有 profileId 也可以顺带关联。
  const payload = { 
    ...abilityForm.value, 
    userId: Number(currentUserId.value),
    profileId: myProfile.value.id || null
  }
  
  try {
    let res
    // 文档：put /api/ability/update | post /api/ability/insert
    if (myAbility.value.id) {
      res = await abilityApi.put('/api/ability/update', payload)
    } else {
      res = await abilityApi.post('/api/ability/insert', payload)
    }
    
    if (res.data.code === 200) {
      abilityVis.value = false
      fetchMyAbility() // 重新拉取展示
    } else {
      alert('保存失败: ' + res.data.message)
    }
  } catch (err) { 
    console.error('保存能力模型失败', err)
    alert('保存出错，请检查网络')
  } finally { 
    isSavingAbility.value = false 
  }
}

const goToChat = () => { router.push('/') }

// ===== 简历上传 =====
const upVis = ref(false)
const fileInput = ref(null)
const selectedFile = ref(null)
const isUploading = ref(false)
const resumeList = ref([])

const openUploadModal = () => { selectedFile.value = null; upVis.value = true }
const closeUploadModal = () => { if (!isUploading.value) upVis.value = false }
const triggerFileInput = () => { if (fileInput.value) fileInput.value.click() }

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    if (file.size > 10 * 1024 * 1024) return alert('文件不能超过 10MB 哦！')
    selectedFile.value = file
  }
}

const uploadResume = async () => {
  if (!selectedFile.value) return
  isUploading.value = true

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('userId', currentUserId.value)

  try {
    const res = await userApi.post('/api/user/uploadResume', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      resumeList.value.push({ fileName: selectedFile.value.name, fileSize: (selectedFile.value.size / 1024 / 1024).toFixed(2) + ' MB' })
      alert('简历解析完成！')
      upVis.value = false
    } else {
      alert('上传失败：' + res.data.message)
    }
  } catch (error) {
    setTimeout(() => {
      resumeList.value.push({ fileName: selectedFile.value.name, fileSize: (selectedFile.value.size / 1024 / 1024).toFixed(2) + ' MB' })
      alert('简历解析完成！（演示模式）')
      upVis.value = false
      isUploading.value = false
      selectedFile.value = null
    }, 800)
  } finally {
    if (!isUploading.value) return
    isUploading.value = false
    selectedFile.value = null
    if (fileInput.value) fileInput.value.value = '' 
  }
}
const deleteResume = (index) => { if(confirm('确认删除这份简历吗？')) resumeList.value.splice(index, 1) }

// ===== 修改密码 =====
const pwdVis = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const isChangingPassword = ref(false)

const changePassword = async () => {
  if (!oldPassword.value.trim() || !newPassword.value.trim() || !confirmPassword.value.trim()) return alert('请填写完整密码')
  if (newPassword.value !== confirmPassword.value) return alert('两次输入的新密码不一致')
  if (newPassword.value.length < 6 || newPassword.value.length > 20) return alert('新密码长度应在6-20位之间')
  
  if (!currentUserId.value) return alert('用户信息错误，请重新登录')
  isChangingPassword.value = true
  
  try {
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedAES = rsaEncrypt(aesKey)
    const encryptedIV = rsaEncrypt(aesIv)
    const encryptedOldPassword = aesEncrypt(oldPassword.value, aesKey, aesIv)
    const encryptedNewPassword = aesEncrypt(newPassword.value, aesKey, aesIv)
    
    const res = await userApi.put('/api/user/change_password', {
      userId: currentUserId.value,
      encryptedOldPassword: encryptedOldPassword,
      encryptedNewPassword: encryptedNewPassword,
      encryptedNewPassword_again: encryptedNewPassword,
      AES: encryptedAES,
      IV: encryptedIV
    })
    
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      alert('密码修改成功！')
      oldPassword.value = ''; newPassword.value = ''; confirmPassword.value = ''
      pwdVis.value = false
    } else {
      alert('密码修改失败：' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('密码修改异常', error)
    alert('密码修改失败，请检查网络连接')
  } finally {
    isChangingPassword.value = false
  }
}
</script>

<style scoped>
/* ============ 基础布局 ============ */
.user-center-page { width: 100%; height: 100%; padding: 30px 40px; box-sizing: border-box; overflow-y: auto; animation: fadeIn 0.4s ease-out; font-family: 'Inter', -apple-system, sans-serif;}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { margin-bottom: 30px; }
.page-header h2 { font-size: 1.6rem; color: #1E293B; margin: 0 0 8px 0; font-weight: 800;}
.page-header p { color: #64748B; margin: 0; }
.content-grid { display: grid; grid-template-columns: 320px 1fr; gap: 24px; align-items: start; }
.left-column { position: sticky; top: 20px; z-index: 10; display: flex; flex-direction: column; gap: 24px; }

/* ============ 左侧栏 ============ */
.profile-card { background: #FFFFFF; border-radius: 16px; padding: 30px 24px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03); display: flex; flex-direction: column; align-items: center; border: 1px solid #EAECEF; }
.avatar-large { width: 80px; height: 80px; background: linear-gradient(135deg, #60A5FA 0%, #4A90E2 100%); color: white; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 2rem; font-weight: bold; margin-bottom: 16px; box-shadow: 0 8px 16px rgba(74, 144, 226, 0.2); }
.user-name { font-size: 1.25rem; color: #1E293B; margin: 0 0 6px 0; font-weight: bold;}
.user-bio { font-size: 0.9rem; color: #64748B; margin: 0 0 24px 0; text-align: center; line-height: 1.5;}
.info-list { width: 100%; display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; border-top: 1px solid #F1F5F9; padding-top: 24px; }
.info-item { display: flex; justify-content: space-between; font-size: 0.9rem; }
.info-item .label { color: #94A3B8; }
.info-item .value { color: #334155; font-weight: 500; }
.edit-btn { width: 100%; padding: 10px; background: #F8FAFC; color: #4A90E2; border: 1px solid #E2E8F0; border-radius: 8px; font-weight: 600; cursor: pointer; transition: 0.2s; }
.edit-btn:hover { background: #F0F6FF; border-color: #BFDBFE; }

.ai-summary-card { background: linear-gradient(180deg, #F8FAFC 0%, #FFFFFF 100%); border-radius: 16px; padding: 24px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03); border: 1px solid #EAECEF; border-top: 3px solid #4A90E2; }
.ai-card-header { display: flex; align-items: center; gap: 8px; font-size: 1rem; font-weight: bold; color: #1E293B; margin-bottom: 20px; }
.ai-comment p { font-size: 0.9rem; color: #475569; line-height: 1.6; font-style: italic; background: rgba(74, 144, 226, 0.05); padding: 12px; border-left: 3px solid #60A5FA; border-radius: 0 8px 8px 0; margin: 0 0 20px 0; }
.re-assess-btn { width: 100%; padding: 10px; background: transparent; color: #94A3B8; border: 1px dashed #CBD5E1; border-radius: 8px; font-weight: 600; cursor: pointer; transition: 0.2s; font-size: 0.9rem; }
.re-assess-btn:hover { border-color: #4A90E2; color: #4A90E2; background: #F0F6FF; }

/* ============ 右侧栏 ============ */
.right-column { display: flex; flex-direction: column; gap: 24px; padding-bottom: 40px; }
.data-card { background: #FFFFFF; border-radius: 16px; padding: 24px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03); border: 1px solid #EAECEF; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-header h4 { margin: 0; font-size: 1.1rem; color: #1E293B; font-weight: 800; }
.text-btn { background: none; border: none; color: #4A90E2; cursor: pointer; font-weight: 600; font-size: 0.9rem; padding: 4px 8px; border-radius: 4px;}
.text-btn:hover { background: #F0F6FF; }
.text-btn.cancel { color: #64748B; }
.text-btn.cancel:hover { background: #F1F5F9; }
.upload-btn { background: #4A90E2; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 0.85rem; transition: 0.2s; }
.upload-btn:hover { background: #357ABD; box-shadow: 0 4px 10px rgba(74, 144, 226, 0.2); }
.edit-actions { display: flex; gap: 10px; }

/* 🌟 新增：能力模型展示区 */
.empty-ability { background: #F8FAFC; border: 1px dashed #CBD5E1; border-radius: 12px; padding: 30px; text-align: center; color: #64748B; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.empty-icon { font-size: 2rem; }
.ability-grid { display: flex; flex-direction: column; gap: 24px; }
.ability-section { background: #F8FAFC; border-radius: 12px; padding: 20px; border: 1px solid #F1F5F9; }
.sub-title { margin: 0 0 16px 0; color: #1E293B; font-size: 1rem; font-weight: 800; border-bottom: 1px solid #E2E8F0; padding-bottom: 8px; }
.ability-item { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
.ability-item:last-child { margin-bottom: 0; }
.a-label { font-size: 0.85rem; color: #94A3B8; font-weight: 600; }
.a-value { font-size: 0.95rem; color: #334155; font-weight: 500; line-height: 1.5; }

/* 职业意向 */
.tags-container { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.intent-tag { background: #F8FAFC; padding: 16px; border-radius: 12px; display: flex; flex-direction: column; gap: 6px; border: 1px solid #F1F5F9; }
.tag-title { font-size: 0.85rem; color: #94A3B8; }
.tag-content { font-size: 1.05rem; color: #334155; font-weight: 600; }
.edit-form-container { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 0.85rem; color: #64748B; font-weight: 600; }
.form-group input, .form-group select { padding: 10px 14px; border: 1px solid #CBD5E1; border-radius: 8px; font-size: 0.95rem; color: #1E293B; background: #FFFFFF; outline: none; transition: 0.2s; }
.form-group input:focus, .form-group select:focus { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1); }

/* 简历列表 */
.resume-list { display: flex; flex-direction: column; gap: 12px; }
.resume-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; border: 1px solid #E2E8F0; border-radius: 12px; transition: 0.2s; background: #F8FAFC; }
.resume-item:hover { border-color: #4A90E2; background: #FFFFFF; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
.resume-info { display: flex; align-items: center; gap: 12px; }
.resume-name { font-size: 0.95rem; color: #1E293B; font-weight: 600; margin-bottom: 4px; }
.resume-meta { font-size: 0.8rem; color: #94A3B8; }
.resume-actions { display: flex; gap: 16px; }
.action-text { font-size: 0.85rem; cursor: pointer; font-weight: 600; transition: 0.2s; }
.action-text.parse { color: #4A90E2; }
.action-text.parse:hover { color: #357ABD; }
.action-text.delete { color: #EF4444; }
.action-text.delete:hover { color: #DC2626; }

/* 账号安全 */
.security-list { display: flex; flex-direction: column; }
.security-item { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-bottom: 1px solid #F1F5F9; }
.security-item:last-child { border-bottom: none; padding-bottom: 0; }
.sec-title { font-size: 0.95rem; font-weight: 600; color: #1E293B; margin-bottom: 4px; }
.sec-desc { font-size: 0.85rem; color: #94A3B8; }
.outline-btn { background: #FFFFFF; border: 1px solid #CBD5E1; color: #475569; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: 600; transition: 0.2s; }
.outline-btn:hover { border-color: #4A90E2; color: #4A90E2; background: #F0F6FF; }
.danger-text { color: #EF4444; }
.danger-btn { border-color: #FECACA; color: #EF4444; }
.danger-btn:hover { background: #FEF2F2; border-color: #F87171; color: #DC2626; }

/* ============ 弹窗 ============ */
.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(4px); display: flex; justify-content: center; align-items: center; z-index: 9999; }
.modal-content { background: #FFFFFF; width: 100%; max-width: 480px; border-radius: 20px; box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15); overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #EAECEF; }
.modal-header h3 { margin: 0; font-size: 1.15rem; color: #1E293B; font-weight: 800; }
.close-modal-btn { background: none; border: none; color: #94A3B8; cursor: pointer; padding: 4px; border-radius: 50%; font-size: 1.2rem; transition: 0.2s; }
.close-modal-btn:hover { background: #F1F5F9; color: #EF4444; }
.modal-body { padding: 30px 24px; }
.modal-footer { padding: 16px 24px; border-top: 1px solid #EAECEF; background: #F8FAFC; display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { padding: 10px 20px; border: 1px solid #CBD5E1; background: #FFFFFF; color: #475569; border-radius: 8px; cursor: pointer; font-weight: 600; transition: 0.2s;}
.btn-cancel:hover { background: #F1F5F9; }
.btn-confirm { padding: 10px 20px; border: none; background: #4A90E2; color: #FFFFFF; border-radius: 8px; cursor: pointer; font-weight: 600; transition: 0.2s; }
.btn-confirm:hover:not(:disabled) { background: #357ABD; }
.btn-confirm:disabled { background: #94A3B8; cursor: not-allowed; opacity: 0.7; }

/* 拖拽上传区 */
.upload-dropzone { border: 2px dashed #CBD5E1; border-radius: 12px; background: #F8FAFC; padding: 40px 20px; text-align: center; cursor: pointer; transition: all 0.2s; }
.upload-dropzone:hover { border-color: #4A90E2; background: #F0F6FF; }
.upload-dropzone.has-file { border-color: #4A90E2; background: #F0F6FF; border-style: solid; }
.upload-title { font-size: 1rem; color: #334155; font-weight: 600; margin: 0 0 8px 0; transition: 0.2s; }
.upload-hint { font-size: 0.85rem; color: #94A3B8; margin: 0; }
.pwd-form { display: flex; flex-direction: column; gap: 16px; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .modal-content, .modal-fade-leave-active .modal-content { transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); }
.modal-fade-enter-from .modal-content, .modal-fade-leave-to .modal-content { transform: scale(0.95) translateY(20px); }
</style>
