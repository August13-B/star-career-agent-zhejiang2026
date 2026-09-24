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
            <p>"你还没有进行过完整的职业诊断哦。请先完善基本信息并完成能力测评，再生成职业报告。"</p>
          </div>
          <button class="re-assess-btn" @click="goToChat">立即去对话探索</button>
        </div>
      </div>

      <div class="right-column">
        
        <div class="data-card">
          <div class="card-header">
            <h4>📑 我的职业规划报告</h4>
            <div class="report-header-actions">
              <button v-if="reportList.length > 0" class="text-btn" @click="toggleManage">
                {{ manageMode ? '完成' : '管理' }}
              </button>
              <router-link class="upload-btn" to="/multi-agent">+ 生成新报告</router-link>
            </div>
          </div>

          <div v-if="manageMode && reportList.length > 0" class="report-manage-bar">
            <label class="report-select-all">
              <input type="checkbox" :checked="allSelected" @change="toggleSelectAll($event.target.checked)" /> 全选
            </label>
            <button class="outline-btn danger-btn" :disabled="selectedReportIds.length === 0" @click="deleteSelectedReports">
              删除选中（{{ selectedReportIds.length }}）
            </button>
          </div>

          <div class="report-list">
            <div v-if="reportLoading" class="report-empty">加载中…</div>
            <div v-else-if="reportList.length === 0" class="report-empty">
              还没有职业规划报告，点右上角「生成新报告」开始吧
            </div>
            <div v-else v-for="r in reportList" :key="r.id" class="report-item">
              <input v-if="manageMode" type="checkbox" class="report-check" :value="String(r.id)" v-model="selectedReportIds" />
              <div class="report-info">
                <div class="report-name">{{ r.reportName || '职业规划报告' }}</div>
                <div class="report-meta">
                  <span>{{ formatTime(r.createTime) }}</span>
                  <span class="report-status">{{ statusText(r.status) }}</span>
                </div>
              </div>
              <div class="report-actions">
                <button class="outline-btn" @click="openReport(r)">查看报告</button>
                <div class="export-wrap">
                  <button class="text-btn" @click.stop="toggleExport(r.id)">导出 PDF ▾</button>
                  <div v-if="String(exportOpenId) === String(r.id)" class="export-menu">
                    <button class="export-item" @click="exportReportPdf(r, 'report')">报告导出（简介）</button>
                    <button class="export-item" @click="exportReportPdf(r, 'full')">全量导出（含过程）</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

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
          
          <div v-if="!myAbility.id && !hasScore" class="empty-ability">
            <span class="empty-icon">📊</span>
            <p>暂未录入能力数据，完善后可大幅提升匹配精度</p>
            <button class="outline-btn" @click="openAbilityModal">立即测评</button>
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
              <h5 class="sub-title">🤝 综合软素质（六维测评）</h5>
              <template v-if="hasScore">
                <div class="ability-item" v-for="d in softDims" :key="d.key">
                  <span class="a-label">
                    {{ d.name }}
                    <i v-if="d.key === topKey" class="dim-tag top">优势</i>
                    <i v-else-if="d.key === lowKey" class="dim-tag low">待提升</i>
                  </span>
                  <span class="a-value score-value">
                    <b>{{ d.score }}</b>
                    <i class="lv">{{ levelText(d.score) }}</i>
                    <span class="score-track"><i :style="{ width: d.score + '%' }"></i></span>
                  </span>
                </div>
                <div class="ability-item total-row">
                  <span class="a-label">综合得分</span>
                  <span class="a-value score-value">
                    <b>{{ myScore.totalScore }}</b><i class="lv">{{ levelText(myScore.totalScore) }}</i>
                  </span>
                </div>
              </template>
              <div v-else class="empty-inline">
                还没有六维测评结果，点右上角「编辑能力数据」开始测评（约 10 题）
              </div>
            </div>
          </div>
        </div>

        <div class="data-card">
          <div class="card-header">
            <h4>📄 简历附件库</h4>
            <button class="upload-btn" disabled title="简历上传和解析服务尚未接入">上传解析待接入</button>
          </div>
          <p class="empty-inline">当前版本暂不支持上传解析简历。请在基本信息和能力测评中填写个人经历。</p>
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
      <div class="modal-overlay" v-if="reportVis" @click.self="reportVis = false">
        <div class="modal-content report-modal">
          <div class="modal-header">
            <h3>{{ currentReportName }}</h3>
            <button class="close-modal-btn" @click="reportVis = false">✕</button>
          </div>
          <div class="modal-body report-view">
            <section v-for="(a, i) in currentReportAgents" :key="i" class="report-section">
              <h4 class="report-section-title">{{ a.name || a.key }}</h4>
              <div class="report-section-body" v-html="renderReportHtml(a.content)"></div>
            </section>
            <div v-if="currentReportAgents.length === 0" class="report-empty">报告内容为空</div>
          </div>
          <div class="modal-footer">
            <button class="outline-btn" @click="exportReportPdf(currentReport)">导出 PDF</button>
            <button class="upload-btn" @click="reportVis = false">关闭</button>
          </div>
        </div>
      </div>
    </transition>

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

    <!-- 核心能力模型：两步测评（基本情况 → 六维情境题 → 自动评分） -->
    <AbilityQuizModal v-model:visible="quizVis" :user-id="currentUserId" @saved="onQuizSaved" />




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
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AbilityQuizModal from '../components/AbilityQuizModal.vue'
import axios from 'axios'
import API_CONFIG from '../config/api'
import { generateAesKeyAndIv, rsaEncrypt, aesEncrypt } from '../utils/crypto'

const router = useRouter()
const route = useRoute()

// ==========================================
// 🚀 核心：三口并行，彻底理清后端模块！
// ==========================================

// 1. User 模块 API（用户信息、修改密码，统一 /api 代理）
const userApi = axios.create({
  baseURL: API_CONFIG.BASE_URL, 
  timeout: API_CONFIG.TIMEOUT
})

userApi.interceptors.request.use(config => {
  const token = localStorage.getItem('token') || ''
  if (token) config.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  return config
})

// 2. StudentProfile 模块 API（学生基础档案、意向）—— 统一走同源 /api（Vite/Nginx 代理）
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

// 3. Ability 模块 API（能力模型）—— 与 studentApi 同源，复用同一实例
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
    // getUserInfo 的 IV/AES 用于加密返回的手机号/邮箱；这里只需要 id/nickname/userRole，
    // 但仍然传上，避免依赖后端对可选参数的处理
    const { aesKey: k, aesIv: i } = generateAesKeyAndIv()
    const res = await userApi.get('/api/user/getUserInfo', {
      params: { IV: rsaEncrypt(i), AES: rsaEncrypt(k) }
    })
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      userInfo.value = res.data.data || {}
      displayNickname.value = userInfo.value.nickname || `新星用户_${String(userInfo.value.userAccount || '8888').slice(-4)}`
      localStorage.setItem('userName', displayNickname.value)
      // 关键：用最新用户信息刷新 userId，避免 localStorage 残留旧 id 导致保存时外键失败
      // 注意：ID 是 64 位雪花值，必须保持字符串，切勿 Number()
      if (userInfo.value.id) {
        currentUserId.value = String(userInfo.value.id)
        localStorage.setItem('userId', currentUserId.value)
      }
    } else {
      // 服务端明确告知 token 失效 / 用户不存在（token 仍在但账号已被重建/清理）
      // 此时继续用 localStorage 旧 userId 会必然触发外键失败，必须清掉并回登录页
      handleAuthInvalid(res.data && res.data.message)
      return
    }
  } catch (error) {
    displayNickname.value = '探索者'
    handleAuthInvalid('获取用户信息失败')
    return
  }

  // 2. 拉取档案和能力
  fetchMyProfile()
  fetchMyAbility()
  fetchReports()
  fetchMyScore()
  // 注册后跳转过来：自动弹出六维能力初步测评（初步分，后续 AI 测评会覆盖）
  if (route.query.quiz === '1') {
    quizVis.value = true
  }
})

// 登录态失效统一处理：清本地身份 + 回登录页
const handleAuthInvalid = (msg) => {
  localStorage.removeItem('userId')
  localStorage.removeItem('userRole')
  localStorage.removeItem('userName')
  alert(`登录状态已失效（${msg || '账号信息与服务器不一致'}）。\n请重新登录后再操作。`)
  router.push('/login')
}

// 64 位雪花 ID 校验：必须是纯数字且 > 0（保持字符串，不做 Number 转换）
const isValidId = (v) => {
  const s = String(v ?? '').trim()
  return /^\d+$/.test(s) && s !== '0'
}

const fetchMyProfile = async () => {
  if (!currentUserId.value) return
  try {
    // userId 保持字符串，避免 64 位 ID 在 JS 中丢精度
    const res = await studentApi.post('/api/student/condition', { userId: String(currentUserId.value) })
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
      const resFallback = await abilityApi.post(`/api/ability/condition`, { userId: String(currentUserId.value) })
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
  if (!isValidId(currentUserId.value)) {
    isSavingBasic.value = false
    return alert('登录状态已失效，请重新登录后再保存')
  }
  const payload = { ...basicForm.value, userId: String(currentUserId.value) }
  try {
    let res
    if (myProfile.value.id) {
      res = await studentApi.put('/api/student/update', { ...payload, id: myProfile.value.id })
    } else {
      res = await studentApi.post('/api/student/insert', payload)
      // 该用户已存在档案（user_id 唯一）→ 重新拉取后改用 update
      if (res.data && res.data.code !== 200) {
        await fetchMyProfile()
        if (myProfile.value.id) {
          res = await studentApi.put('/api/student/update', { ...payload, id: myProfile.value.id })
        }
      }
    }
    if (res.data.code === 200) { basicVis.value = false; fetchMyProfile() }
    else handleSaveError(res.data.message)
  } catch (err) { console.error(err); handleSaveError(err?.response?.data?.message || err.message) } finally { isSavingBasic.value = false }
}

// ===== 职业意向编辑 =====
const isEdit = ref(false)
const intentForm = ref({})
const isSavingIntent = ref(false)

const startEdit = () => { intentForm.value = { ...myProfile.value }; isEdit.value = true }
const cancelEdit = () => { isEdit.value = false }

const saveJobIntent = async () => {
  isSavingIntent.value = true
  if (!isValidId(currentUserId.value)) {
    isSavingIntent.value = false
    return alert('登录状态已失效，请重新登录后再保存')
  }
  const payload = { ...intentForm.value, userId: String(currentUserId.value) }
  try {
    let res
    if (myProfile.value.id) {
      res = await studentApi.put('/api/student/update', { ...payload, id: myProfile.value.id })
    } else {
      res = await studentApi.post('/api/student/insert', payload)
      if (res.data && res.data.code !== 200) {
        await fetchMyProfile()
        if (myProfile.value.id) {
          res = await studentApi.put('/api/student/update', { ...payload, id: myProfile.value.id })
        }
      }
    }
    if (res.data.code === 200) { isEdit.value = false; fetchMyProfile() }
    else handleSaveError(res.data.message)
  } catch (err) { console.error(err); handleSaveError(err?.response?.data?.message || err.message) } finally { isSavingIntent.value = false }
}

// ===== 🚀 核心能力模型：六维问卷测评（注册后初步评价；AI 测评会参考并覆盖） =====
const quizVis = ref(false)
const openAbilityModal = () => { quizVis.value = true }
const onQuizSaved = async () => { await Promise.all([fetchMyAbility(), fetchMyScore()]) }

// 六维评分（来自 student_ability_score）
const myScore = ref(null)
const hasScore = computed(() => !!myScore.value)
const SOFT_DEFS = [
  { key: 'communicationScore', name: '沟通能力' },
  { key: 'teamworkScore', name: '团队协作' },
  { key: 'problemSolvingScore', name: '问题解决' },
  { key: 'innovationScore', name: '创新能力' },
  { key: 'learningScore', name: '学习能力' },
  { key: 'pressureScore', name: '抗压能力' }
]
const softDims = computed(() => {
  const s = myScore.value
  if (!s) return []
  return SOFT_DEFS.map(d => ({ key: d.key, name: d.name, score: Number(s[d.key] ?? 0) }))
})
const topKey = computed(() => softDims.value.length
  ? [...softDims.value].sort((a, b) => b.score - a.score)[0].key : '')
const lowKey = computed(() => softDims.value.length
  ? [...softDims.value].sort((a, b) => a.score - b.score)[0].key : '')
const levelText = (v) => {
  const n = Number(v || 0)
  if (n >= 90) return '优秀'
  if (n >= 80) return '良好'
  if (n >= 70) return '中等'
  if (n >= 60) return '及格'
  return '待提升'
}

const fetchMyScore = async () => {
  if (!currentUserId.value) return
  try {
    const res = await studentApi.get(`/api/ability/score/user/${currentUserId.value}`)
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      const list = Array.isArray(res.data.data) ? res.data.data : []
      myScore.value = list.length > 0 ? list[0] : null
    }
  } catch (e) { /* 无评分时忽略 */ }
}

// 统一的保存失败处理：识别「用户不存在/外键」类错误，清掉旧身份并引导重新登录
const handleSaveError = (msg) => {
  const m = String(msg || '')
  if (m.includes('foreign key') || m.includes('user_id') || m.includes('网络错误') || m.includes('用户不存在')) {
    localStorage.removeItem('userId')
    localStorage.removeItem('userRole')
    alert('登录状态已过期（账号信息与服务器不一致），已清除本地登录信息。\n请重新登录后再保存。')
    router.push('/login')
    return
  }
  alert('保存失败: ' + (m || '未知错误'))
}

const goToChat = () => { router.push('/') }

// ===== 📑 职业规划报告 =====
const reportList = ref([])
const reportLoading = ref(false)
const reportVis = ref(false)
const currentReport = ref(null)
const currentReportName = ref('')
const currentReportAgents = ref([])

const fetchReports = async () => {
  if (!currentUserId.value) return
  reportLoading.value = true
  try {
    const res = await studentApi.get(`/api/career-report/user/${currentUserId.value}`)
    if (res.data.code === 10001 || res.data.code === 200 || res.data.code === 0) {
      reportList.value = Array.isArray(res.data.data) ? res.data.data : []
    }
  } catch (e) { console.error('获取报告列表失败', e) }
  finally { reportLoading.value = false }
}

const parseReportContent = (r) => {
  currentReport.value = r
  currentReportName.value = r.reportName || '职业规划报告'
  currentReportAgents.value = []
  try {
    const content = typeof r.reportContent === 'string' ? JSON.parse(r.reportContent) : r.reportContent
    if (content && content.final) {
      // 最终报告 = 第 6 段整合后的「简介」（不再叠加 6 段过程）
      currentReportAgents.value = [{ name: r.reportName || '职业规划报告', content: content.final }]
    } else if (content && Array.isArray(content.agents)) {
      currentReportAgents.value = content.agents
    } else if (content && content.fullText) {
      currentReportAgents.value = [{ name: '报告正文', content: content.fullText }]
    }
  } catch (e) { console.error('解析报告内容失败', e) }
}

const openReport = (r) => { parseReportContent(r); reportVis.value = true }

// ===== 报告管理（批量删除：我们侧逻辑删 + 平台侧物理删） =====
const manageMode = ref(false)
const selectedReportIds = ref([])
const allSelected = computed(() =>
  reportList.value.length > 0 && selectedReportIds.value.length === reportList.value.length)

const toggleManage = () => {
  manageMode.value = !manageMode.value
  selectedReportIds.value = []
}
const toggleSelectAll = (checked) => {
  selectedReportIds.value = checked ? reportList.value.map(r => String(r.id)) : []
}
const deleteSelectedReports = async () => {
  if (selectedReportIds.value.length === 0) return
  if (!confirm(`确认删除选中的 ${selectedReportIds.value.length} 份报告？删除后不可恢复。`)) return
  try {
    const res = await studentApi.post('/api/career-report/batch-delete', { ids: selectedReportIds.value })
    const body = res.data || {}
    if (body.code !== 10001 && body.code !== 200 && body.code !== 0) {
      alert('删除失败：' + (body.message || '未知错误'))
      return
    }
    const d = body.data || {}
    // 仅提示本地删除结果；平台侧删除情况不弹窗（后台静默处理，失败不影响本地）
    alert(`已删除 ${d.deleted || 0} 份报告`)
    manageMode.value = false
    selectedReportIds.value = []
    fetchReports()
  } catch (e) {
    console.error('批量删除失败', e)
    alert('删除出错，请稍后重试')
  }
}

const formatTime = (t) => (!t ? '' : String(t).replace('T', ' ').slice(0, 16))
const statusText = (s) => ({ 1: '草稿', 2: '已生成', 3: '已修改', 4: '已确认' }[s] || '已生成')

const renderReportHtml = (text) => {
  if (!text) return ''
  let html = String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  html = html.replace(/^#### (.*)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.*)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.*)$/gm, '<h2>$1</h2>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/^- (.*)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
  html = html.replace(/\n{2,}/g, '</p><p>')
  html = html.replace(/\n/g, '<br/>')
  return `<p>${html}</p>`
}

const exportOpenId = ref(null)
const toggleExport = (id) => {
  exportOpenId.value = (String(exportOpenId.value) === String(id)) ? null : id
}

const exportReportPdf = async (r, mode = 'report') => {
  exportOpenId.value = null
  if (!r || !r.id) return
  try {
    const res = await studentApi.get(`/api/career-report/${r.id}/export/pdf`, {
      params: { mode },
      responseType: 'blob',
      timeout: 60000
    })
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `${r.reportName || '职业规划报告'}${mode === 'full' ? '-全量过程' : ''}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 2000)
  } catch (e) {
    console.error('导出 PDF 失败', e)
    alert('导出失败，请确认已登录后重试')
  }
}

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

/* 六维评分展示（差异性：数字 + 等级 + 进度条 + 优势/待提升） */
.score-value { display: inline-flex; align-items: center; gap: 8px; }
.score-value b { color: #1D4ED8; font-size: 1.02rem; font-weight: 800; min-width: 26px; }
.score-value .lv { font-style: normal; font-size: 0.72rem; color: #64748B; background: #F1F5F9; padding: 1px 7px; border-radius: 999px; }
.score-track { display: inline-block; width: 72px; height: 6px; background: #EEF2F7; border-radius: 999px; overflow: hidden; }
.score-track i { display: block; height: 100%; background: linear-gradient(90deg, #60A5FA, #4A90E2); transition: width 0.3s ease; }
.dim-tag { font-style: normal; font-size: 0.66rem; font-weight: 700; padding: 1px 6px; border-radius: 4px; margin-left: 6px; }
.dim-tag.top { color: #059669; background: #ECFDF5; }
.dim-tag.low { color: #D97706; background: #FFFBEB; }
.total-row { border-top: 1px dashed #E2E8F0; padding-top: 10px; }
.empty-inline { font-size: 0.82rem; color: #94A3B8; padding: 6px 0; }

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

/* ===== 职业规划报告卡片 ===== */
.card-header a.upload-btn { text-decoration: none; display: inline-block; }
.report-list { display: flex; flex-direction: column; gap: 12px; }
.report-empty { padding: 28px; text-align: center; color: #94A3B8; font-size: 0.9rem; background: #F8FAFC; border: 1px dashed #E2E8F0; border-radius: 12px; }
.report-item { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 14px 16px; border: 1px solid #E2E8F0; border-radius: 12px; transition: 0.2s; background: #F8FAFC; }
.report-item:hover { border-color: #4A90E2; background: #FFFFFF; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
.report-info { min-width: 0; flex: 1; }
.report-name { font-size: 0.95rem; color: #1E293B; font-weight: 600; margin-bottom: 4px; word-break: break-all; }
.report-meta { display: flex; align-items: center; gap: 10px; font-size: 0.8rem; color: #94A3B8; }
.report-status { color: #10B981; font-weight: 600; }
.report-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

/* 导出下拉：报告（简介）/ 全量（含过程） */
.export-wrap { position: relative; }
.export-menu { position: absolute; right: 0; top: calc(100% + 6px); z-index: 30; min-width: 168px; background: #FFFFFF;
  border: 1px solid #E4EAF2; border-radius: 10px; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12); padding: 6px; display: flex; flex-direction: column; }
.export-item { text-align: left; background: none; border: none; padding: 9px 12px; border-radius: 8px; font-size: 0.85rem;
  color: #334155; cursor: pointer; font-family: inherit; white-space: nowrap; }
.export-item:hover { background: #EFF6FF; color: #1D4ED8; }
.report-header-actions { display: flex; align-items: center; gap: 10px; }
.report-manage-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; padding: 8px 12px; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 10px; }
.report-select-all { display: inline-flex; align-items: center; gap: 6px; font-size: 0.85rem; color: #475569; cursor: pointer; }
.report-check { width: 16px; height: 16px; margin-right: 10px; flex-shrink: 0; cursor: pointer; }
.modal-content.report-modal { display: flex; flex-direction: column; width: 92%; max-width: 860px; max-height: 88vh; overflow: hidden; }
.modal-content.report-modal .modal-body { flex: 1; overflow-y: auto; }
.report-view { background: #FFFFFF; }
.report-section { border-bottom: 1px solid #F1F5F9; padding-bottom: 16px; margin-bottom: 16px; }
.report-section:last-child { border-bottom: none; margin-bottom: 0; }
.report-section-title { margin: 0 0 10px; color: #1D4ED8; font-size: 1.05rem; font-weight: 800; border-left: 4px solid #4A90E2; padding-left: 10px; }
.report-section-body { color: #334155; font-size: 0.92rem; line-height: 1.8; }
.report-section-body h2, .report-section-body h3, .report-section-body h4 { color: #1E293B; margin: 12px 0 8px; }
.report-section-body ul { margin: 6px 0 6px 18px; padding: 0; }
.report-section-body strong { color: #111827; }

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
