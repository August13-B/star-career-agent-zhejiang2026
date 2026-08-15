<template>
  <div class="admin-container">
    <div class="tabs">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'job' }" 
        @click="switchTab('job')"
      >
        🏢 岗位基础信息与 AI 枢纽
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'requirement' }" 
        @click="switchTab('requirement')"
      >
        🚧 硬门槛需求
      </button>
    </div>

    <div v-if="activeTab === 'job'">
      <div class="admin-header">
        <div class="header-left">
          <h2>🏢 岗位基础信息管理</h2>
          <p>管理核心岗位数据，并接驳 AI 测算引擎与星图系统</p>
        </div>
        <div class="header-right">
          <div class="search-box">
            <input type="text" v-model="searchParams.jobName" placeholder="搜索岗位名称..." @keyup.enter="fetchData" />
            <button class="search-btn" @click="fetchData">搜索</button>
          </div>
          <button class="add-btn" @click="openModal('add')">+ 新增岗位</button>
        </div>
      </div>

      <div class="table-card">
        <table class="custom-table">
          <thead>
            <tr>
              <th width="10%">岗位 ID</th>
              <th width="20%">岗位名称</th>
              <th width="15%">平均薪资 (元/月)</th>
              <th width="15%">AI 图谱状态</th>
              <th width="40%">操作与 AI 联动</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="5" class="empty-text">数据加载中...</td>
            </tr>
            <tr v-else-if="tableData.length === 0">
              <td colspan="5" class="empty-text">暂无相关岗位数据</td>
            </tr>
            <tr v-else v-for="item in tableData" :key="item.id">
              <td class="id-cell">{{ item.id }}</td>
              <td class="name-cell">{{ item.jobName }}</td>
              <td class="salary-cell">¥ {{ item.salary ? item.salary.toLocaleString() : '面议' }}</td>
              
              <td>
                <span class="status-badge" :class="item.jobId ? 'status-analyzed' : 'status-pending'">
                  {{ item.jobId ? '✅ 已入星图' : '⏳ 待测算' }}
                </span>
              </td>

              <td class="action-cell">
                <div class="action-group">
                  <button class="edit-btn" @click="openModal('edit', item)">编辑</button>
                  <button class="delete-btn" @click="handleDelete(item.id)">删除</button>
                </div>
                <div class="ai-group">
                  <button class="btn-ai-analyze" @click="goToCompare(item.id)">🤖 AI 测算</button>
                  <button 
                    class="btn-graph" 
                    :class="{ 'disabled': !item.jobId }" 
                    @click="goToGraph(item.jobId)"
                    :title="!item.jobId ? '请先点击左侧 AI 测算，生成图谱节点' : '进入职业星图'"
                  >
                    🌌 查星图
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <button :disabled="searchParams.current <= 1" @click="changePage(-1)">上一页</button>
          <span>第 {{ searchParams.current }} 页 / 共 {{ Math.ceil(total / searchParams.size) || 1 }} 页</span>
          <button :disabled="searchParams.current * searchParams.size >= total" @click="changePage(1)">下一页</button>
          
          <div class="jump-box">
            前往 <input type="number" v-model.number="jumpPageInput" @keyup.enter="jumpToPage('job')" min="1" /> 页
            <button class="jump-btn" @click="jumpToPage('job')">跳转</button>
          </div>
        </div>
      </div>

      <transition name="modal-fade">
        <div class="modal-overlay" v-if="modal.visible" @click.self="closeModal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>{{ modal.type === 'add' ? '✨ 新增岗位' : '✏️ 编辑岗位' }}</h3>
              <button class="close-icon" @click="closeModal">×</button>
            </div>
            
            <form @submit.prevent="submitForm" class="modal-form">
              <div class="form-group">
                <label>岗位名称 <span>*</span></label>
                <input type="text" v-model="formData.jobName" placeholder="例如：Java开发" required />
              </div>
              <div class="form-group">
                <label>薪资水平 (元/月) <span>*</span></label>
                <input type="number" v-model="formData.salary" placeholder="例如：15000" required />
              </div>
              
              <div class="modal-footer">
                <button type="button" class="cancel-btn" @click="closeModal">取消</button>
                <button type="submit" class="confirm-btn" :disabled="modal.submitting">
                  {{ modal.submitting ? '提交中...' : '确认保存' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </transition>
    </div>

    <div v-else-if="activeTab === 'requirement'">
      <div class="admin-header">
        <div class="header-left">
          <h2>🚧 硬门槛需求管理</h2>
          <p>管理岗位的硬门槛需求（如学历、经验、技能等）</p>
        </div>
        <div class="header-right">
          <div class="search-box">
            <input type="text" v-model="requirementSearch.jobId" placeholder="岗位ID筛选..." @keyup.enter="fetchRequirements" />
            <button class="search-btn" @click="fetchRequirements">搜索</button>
          </div>
          <button class="add-btn" @click="openRequirementModal('add')">+ 新增需求</button>
        </div>
      </div>

      <div class="table-card">
        <table class="custom-table">
          <thead>
            <tr>
              <th width="15%">需求ID</th>
              <th width="15%">岗位ID</th>
              <th width="25%">学历要求</th>
              <th width="25%">经验要求</th>
              <th width="20%">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="requirementLoading">
              <td colspan="5" class="empty-text">数据加载中...</td>
            </tr>
            <tr v-else-if="requirementData.length === 0">
              <td colspan="5" class="empty-text">暂无硬门槛需求数据</td>
            </tr>
            <tr v-else v-for="item in requirementData" :key="item.id">
              <td class="id-cell">{{ item.id }}</td>
              <td class="id-cell">{{ item.jobId }}</td>
              <td>{{ item.educationRequirement || '未设置' }}</td>
              <td>{{ item.experienceRequirement || '未设置' }}</td>
              <td class="action-cell">
                <button class="edit-btn" @click="openRequirementModal('edit', item)">编辑</button>
                <button class="delete-btn" @click="handleRequirementDelete(item.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <button :disabled="requirementSearch.current <= 1" @click="changeRequirementPage(-1)">上一页</button>
          <span>第 {{ requirementSearch.current }} 页 / 共 {{ Math.ceil(requirementTotal / requirementSearch.size) || 1 }} 页</span>
          <button :disabled="requirementSearch.current * requirementSearch.size >= requirementTotal" @click="changeRequirementPage(1)">下一页</button>
          
          <div class="jump-box">
            前往 <input type="number" v-model.number="reqJumpPageInput" @keyup.enter="jumpToPage('requirement')" min="1" /> 页
            <button class="jump-btn" @click="jumpToPage('requirement')">跳转</button>
          </div>
        </div>
      </div>

      <transition name="modal-fade">
        <div class="modal-overlay" v-if="requirementModal.visible" @click.self="closeRequirementModal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>{{ requirementModal.type === 'add' ? '✨ 新增硬门槛需求' : '✏️ 编辑硬门槛需求' }}</h3>
              <button class="close-icon" @click="closeRequirementModal">×</button>
            </div>
            
            <form @submit.prevent="submitRequirementForm" class="modal-form">
              <div class="form-group">
                <label>岗位ID <span>*</span></label>
                <input type="number" v-model="requirementForm.jobId" placeholder="关联的岗位ID" required />
              </div>
              <div class="form-group">
                <label>学历要求</label>
                <input type="text" v-model="requirementForm.educationRequirement" placeholder="例如：本科及以上" />
              </div>
              <div class="form-group">
                <label>经验要求</label>
                <input type="text" v-model="requirementForm.experienceRequirement" placeholder="例如：3年以上相关经验" />
              </div>
              <div class="form-group">
                <label>技能要求</label>
                <textarea v-model="requirementForm.skillRequirement" placeholder="例如：Java, Spring, MySQL" rows="3"></textarea>
              </div>
              <div class="form-group">
                <label>证书要求</label>
                <input type="text" v-model="requirementForm.certificateRequirement" placeholder="例如：PMP、软考中级" />
              </div>
              <div class="form-group">
                <label>年龄要求</label>
                <input type="text" v-model="requirementForm.ageRequirement" placeholder="例如：18-35岁" />
              </div>
              <div class="form-group">
                <label>其他要求</label>
                <textarea v-model="requirementForm.otherRequirement" placeholder="其他特殊要求" rows="2"></textarea>
              </div>
              
              <div class="modal-footer">
                <button type="button" class="cancel-btn" @click="closeRequirementModal">取消</button>
                <button type="submit" class="confirm-btn" :disabled="requirementModal.submitting">
                  {{ requirementModal.submitting ? '提交中...' : '确认保存' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import API_CONFIG from '../config/api'

const router = useRouter()
const baseURL = API_CONFIG.BASE_URL
const getHeaders = () => {
  const token = localStorage.getItem('token') || ''
  return { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

// ===== 状态管理 =====
const activeTab = ref('job')

// 岗位状态 (size 已改为 5)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const searchParams = reactive({ current: 1, size: 5, jobName: '' })
const modal = reactive({ visible: false, type: 'add', submitting: false })
const formData = reactive({ id: '', jobName: '', salary: '' })

// 硬门槛状态 (size 已改为 5)
const requirementLoading = ref(false)
const requirementData = ref([])
const requirementTotal = ref(0)
const requirementSearch = reactive({ current: 1, size: 5, jobId: '' })
const requirementModal = reactive({ visible: false, type: 'add', submitting: false })
const requirementForm = reactive({ id: '', jobId: '', educationRequirement: '', experienceRequirement: '', skillRequirement: '', certificateRequirement: '', ageRequirement: '', otherRequirement: '' })

// 跳页状态
const jumpPageInput = ref('')
const reqJumpPageInput = ref('')

// ===== 切换选项卡 =====
const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'job') fetchData()
  else if (tab === 'requirement') fetchRequirements()
}

// ===== 跳页逻辑 =====
const jumpToPage = (type) => {
  if (type === 'job') {
    const maxPage = Math.ceil(total.value / searchParams.size) || 1
    let target = Number(jumpPageInput.value)
    if (!target || target < 1) target = 1
    if (target > maxPage) target = maxPage
    
    searchParams.current = target
    fetchData()
    jumpPageInput.value = '' 
  } else if (type === 'requirement') {
    const maxPage = Math.ceil(requirementTotal.value / requirementSearch.size) || 1
    let target = Number(reqJumpPageInput.value)
    if (!target || target < 1) target = 1
    if (target > maxPage) target = maxPage
    
    requirementSearch.current = target
    fetchRequirements()
    reqJumpPageInput.value = '' 
  }
}

// ===== AI 枢纽跳转逻辑 =====
const goToCompare = (jobId) => {
  router.push({ path: '/compare', query: { jobId: jobId } })
}

const goToGraph = (profileId) => {
  if (!profileId) {
    alert('🚫 暂无星图数据！请先点击左侧的 [🤖 AI 测算] 按钮，让 AI 为此岗位建立宇宙星图节点！')
    return
  }
  router.push({ path: '/graph', query: { id: profileId } })
}

// ===== 岗位基础信息 API 调用逻辑 (带前端防爆卡机制) =====
const fetchData = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${baseURL}/api/job-info/page`, { params: searchParams, headers: getHeaders() })
    if (res.data.code === 200 || res.data.code === 0) {
      const rawRecords = res.data.data.records || res.data.data
      
      // 前端防爆卡兜底
      if (Array.isArray(rawRecords) && rawRecords.length > searchParams.size) {
        console.warn('⚠️ 后端分页失效，前端已启动手动切片。')
        const start = (searchParams.current - 1) * searchParams.size
        tableData.value = rawRecords.slice(start, start + searchParams.size)
        total.value = res.data.data.total > 0 ? res.data.data.total : rawRecords.length
      } else {
        tableData.value = rawRecords
        total.value = res.data.data.total || rawRecords.length
      }
    }
  } catch (error) { console.error('获取数据失败', error) } finally { loading.value = false }
}

const changePage = (step) => { searchParams.current += step; fetchData() }

const openModal = (type, row = null) => {
  modal.type = type
  if (type === 'edit' && row) {
    formData.id = row.id; formData.jobName = row.jobName; formData.salary = row.salary
  } else {
    formData.id = ''; formData.jobName = ''; formData.salary = ''
  }
  modal.visible = true
}

const closeModal = () => { modal.visible = false }

const submitForm = async () => {
  modal.submitting = true
  try {
    const payload = { jobName: formData.jobName, salary: Number(formData.salary) }
    if (modal.type === 'add') {
      await axios.post(`${baseURL}/api/job-info`, payload, { headers: getHeaders() })
      alert('新增成功！')
    } else {
      await axios.put(`${baseURL}/api/job-info/${formData.id}`, payload, { headers: getHeaders() })
      alert('修改成功！')
    }
    closeModal()
    fetchData()
  } catch (error) { alert('提交失败，请重试！') } finally { modal.submitting = false }
}

const handleDelete = async (id) => {
  if (!confirm('确定要删除这个岗位吗？此操作不可逆！')) return
  try {
    await axios.delete(`${baseURL}/api/job-info/${id}`, { headers: getHeaders() })
    alert('删除成功！')
    fetchData()
  } catch (error) { alert('删除失败！') }
}

// ===== 硬门槛需求 API 调用逻辑 (带前端防爆卡机制) =====
const fetchRequirements = async () => {
  requirementLoading.value = true
  try {
    const params = { current: requirementSearch.current, size: requirementSearch.size }
    if (requirementSearch.jobId) params.jobId = requirementSearch.jobId
    const res = await axios.get(`${baseURL}/api/job-hard-requirement/page`, { params, headers: getHeaders() })
    
    if (res.data.code === 200 || res.data.code === 0) {
      const rawRecords = res.data.data.records || res.data.data
      
      // 前端防爆卡兜底
      if (Array.isArray(rawRecords) && rawRecords.length > requirementSearch.size) {
        const start = (requirementSearch.current - 1) * requirementSearch.size
        requirementData.value = rawRecords.slice(start, start + requirementSearch.size)
        requirementTotal.value = res.data.data.total > 0 ? res.data.data.total : rawRecords.length
      } else {
        requirementData.value = rawRecords || []
        requirementTotal.value = res.data.data.total || 0
      }
    }
  } catch (error) { console.error('获取硬门槛需求失败', error) } finally { requirementLoading.value = false }
}

const changeRequirementPage = (step) => { requirementSearch.current += step; fetchRequirements() }

const openRequirementModal = (type, row = null) => {
  requirementModal.type = type
  if (type === 'edit' && row) {
    Object.keys(requirementForm).forEach(key => { if (row[key] !== undefined) requirementForm[key] = row[key] })
  } else {
    Object.keys(requirementForm).forEach(key => { requirementForm[key] = '' })
  }
  requirementModal.visible = true
}

const closeRequirementModal = () => { requirementModal.visible = false }

const submitRequirementForm = async () => {
  requirementModal.submitting = true
  try {
    const payload = { ...requirementForm, jobId: Number(requirementForm.jobId) }
    if (requirementModal.type === 'add') {
      await axios.post(`${baseURL}/api/job-hard-requirement`, payload, { headers: getHeaders() })
      alert('新增成功！')
    } else {
      await axios.put(`${baseURL}/api/job-hard-requirement/${requirementForm.id}`, payload, { headers: getHeaders() })
      alert('修改成功！')
    }
    closeRequirementModal()
    fetchRequirements()
  } catch (error) { alert('提交失败，请重试！') } finally { requirementModal.submitting = false }
}

const handleRequirementDelete = async (id) => {
  if (!confirm('确定要删除这个硬门槛需求吗？此操作不可逆！')) return
  try {
    await axios.delete(`${baseURL}/api/job-hard-requirement/${id}`, { headers: getHeaders() })
    alert('删除成功！')
    fetchRequirements()
  } catch (error) { alert('删除失败！') }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.tabs { display: flex; gap: 10px; margin-bottom: 30px; border-bottom: 2px solid #E2E8F0; padding-bottom: 10px; }
.tab-btn { padding: 12px 24px; background: #F1F5F9; border: none; border-radius: 8px; font-weight: bold; color: #64748B; cursor: pointer; transition: all 0.3s ease; display: flex; align-items: center; gap: 8px; }
.tab-btn.active { background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%); color: white; box-shadow: 0 4px 10px rgba(74, 144, 226, 0.3); }
.tab-btn:hover:not(.active) { background: #E2E8F0; color: #334155; }
.admin-container { padding: 30px; max-width: 1200px; margin: 0 auto; background: #F8FAFC; min-height: 100vh; font-family: sans-serif; }
.admin-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 30px; flex-wrap: wrap; gap: 20px; }
.header-left h2 { margin: 0 0 5px 0; color: #1E293B; font-size: 1.8rem; white-space: nowrap; }
.header-left p { margin: 0; color: #64748B; font-size: 0.95rem; }
.header-right { display: flex; gap: 15px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; background: #fff; border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden; }
.search-box input { padding: 10px 15px; border: none; outline: none; width: 200px; }
.search-btn { background: #F1F5F9; border: none; padding: 0 15px; color: #475569; font-weight: bold; cursor: pointer; border-left: 1px solid #E2E8F0; transition: 0.2s; }
.search-btn:hover { background: #E2E8F0; }
.add-btn { background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%); color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; cursor: pointer; transition: 0.3s; box-shadow: 0 4px 10px rgba(74, 144, 226, 0.3); }
.add-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(74, 144, 226, 0.4); }
.table-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); overflow-x: auto; }
@media screen and (max-width: 768px) {
  .admin-container { padding: 15px; }
  .admin-header { flex-direction: column; align-items: flex-start; }
  .header-right { width: 100%; justify-content: space-between; }
  .search-box input { width: 100%; }
}
.custom-table { width: 100%; border-collapse: collapse; text-align: left; }
.custom-table th { background: #F8FAFC; color: #64748B; font-weight: 600; padding: 15px; border-bottom: 2px solid #E2E8F0; }
.custom-table td { padding: 15px; border-bottom: 1px solid #F1F5F9; color: #334155; vertical-align: middle; }
.custom-table tbody tr:hover { background: #F8FAFC; }
.empty-text { text-align: center; color: #94A3B8; padding: 30px !important; }
.id-cell { font-family: monospace; color: #94A3B8; }
.name-cell { font-weight: bold; color: #1E293B; }
.salary-cell { color: #10B981; font-weight: bold; }

.status-badge { font-size: 0.75rem; padding: 4px 8px; border-radius: 6px; font-weight: bold; white-space: nowrap; }
.status-analyzed { background: #ECFDF5; color: #059669; border: 1px solid #A7F3D0; }
.status-pending { background: #FEF2F2; color: #DC2626; border: 1px solid #FECACA; }

.action-cell { display: flex; align-items: center; gap: 20px; }
.action-group { display: flex; gap: 8px; border-right: 1px solid #E2E8F0; padding-right: 20px; }
.ai-group { display: flex; gap: 10px; }

.edit-btn { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: bold; transition: 0.2s;}
.edit-btn:hover { background: #FEF3C7; }
.delete-btn { background: #FEF2F2; color: #EF4444; border: 1px solid #FECACA; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: bold; transition: 0.2s;}
.delete-btn:hover { background: #FEE2E2; }

.btn-ai-analyze { background: linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%); color: white; border: none; padding: 6px 14px; border-radius: 6px; font-size: 0.85rem; font-weight: bold; cursor: pointer; transition: 0.2s; }
.btn-ai-analyze:hover { box-shadow: 0 4px 10px rgba(109, 40, 217, 0.3); transform: translateY(-1px); }
.btn-graph { background: linear-gradient(135deg, #10B981 0%, #059669 100%); color: white; border: none; padding: 6px 14px; border-radius: 6px; font-size: 0.85rem; font-weight: bold; cursor: pointer; transition: 0.2s; }
.btn-graph:hover:not(.disabled) { box-shadow: 0 4px 10px rgba(5, 150, 105, 0.3); transform: translateY(-1px); }
.btn-graph.disabled { background: #E2E8F0; color: #94A3B8; cursor: not-allowed; box-shadow: none; }

.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 15px; margin-top: 20px; color: #64748B; font-size: 0.9rem; }
.pagination button { padding: 6px 12px; border: 1px solid #E2E8F0; background: white; border-radius: 6px; cursor: pointer; transition: 0.2s; }
.pagination button:not(:disabled):hover { background: #F1F5F9; border-color: #CBD5E1; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }

/* 🌟 跳页输入框样式 */
.jump-box { display: flex; align-items: center; gap: 8px; margin-left: 15px; border-left: 1px solid #E2E8F0; padding-left: 15px; color: #64748B; }
.jump-box input { width: 50px; padding: 4px 8px; border: 1px solid #CBD5E1; border-radius: 6px; text-align: center; outline: none; transition: 0.2s; font-size: 0.9rem; }
.jump-box input:focus { border-color: #4A90E2; box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.1); }
.jump-box input::-webkit-outer-spin-button, .jump-box input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
.jump-btn { background: #F8FAFC; border: 1px solid #CBD5E1; color: #475569; padding: 4px 12px; border-radius: 6px; cursor: pointer; transition: 0.2s; font-size: 0.85rem; font-weight: bold; }
.jump-btn:hover { background: #4A90E2; color: white; border-color: #4A90E2; }

.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(4px); display: flex; justify-content: center; align-items: center; z-index: 2000; }
.modal-content { background: white; width: 100%; max-width: 450px; border-radius: 16px; padding: 25px; box-shadow: 0 20px 40px rgba(0,0,0,0.15); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.modal-header h3 { margin: 0; font-size: 1.3rem; color: #1E293B; }
.close-icon { background: none; border: none; font-size: 1.5rem; color: #94A3B8; cursor: pointer; }
.close-icon:hover { color: #334155; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; font-size: 0.9rem; font-weight: bold; color: #475569; margin-bottom: 8px; }
.form-group label span { color: #EF4444; }
.form-group input, .form-group textarea { width: 100%; padding: 12px; border: 1px solid #E2E8F0; border-radius: 8px; box-sizing: border-box; outline: none; transition: 0.2s; font-family: inherit; resize: vertical; }
.form-group input:focus, .form-group textarea:focus { border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1); }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 30px; }
.cancel-btn { background: #F1F5F9; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; color: #475569; cursor: pointer; }
.confirm-btn { background: #4A90E2; color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; cursor: pointer; }
.confirm-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: translateY(-20px) scale(0.95); }
</style>
