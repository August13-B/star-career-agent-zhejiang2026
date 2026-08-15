<template>
  <div class="login-page">
    
    <transition name="toast-fade">
      <div v-if="toast.show" class="custom-toast" :class="toast.type">
        <span class="toast-icon">{{ toast.type === 'success' ? '✅' : '⚠️' }}</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="login-card">
      
      <button class="close-btn" @click="goBack" title="返回主页">
        <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>

      <div class="form-header">
        <h2 class="gradient-text">{{ isLogin ? '欢迎登录' : '创建新账号' }}</h2>
        <p>{{ isLogin ? 'Agent 职业领航智能体' : (regStep === 1 ? 'Step 1: 验证联系方式' : 'Step 2: 完善个人与身份信息') }}</p>
      </div>

      <transition name="form-fade" mode="out-in">
        
        <form v-if="isLogin" @submit.prevent="handleLogin" class="login-form">
          <div class="input-group">
            <label>账号 / 邮箱 / 手机号 <span>*</span></label>
            <input type="text" v-model="loginForm.account" placeholder="请输入登录凭证" required />
          </div>
          <div class="input-group">
            <label>密码 <span>*</span></label>
            <input type="password" v-model="loginForm.password" placeholder="请输入密码" required />
          </div>
          <div class="form-actions">
            <label class="checkbox-container">
              <input type="checkbox" v-model="loginForm.remember" />
              <span class="checkmark"></span>自动登录
            </label>
            <a href="#" class="forgot-link" @click.prevent="openForgotPassword">忘记密码？</a>
          </div>
          <button type="submit" class="submit-btn" :disabled="isLoading">
            {{ isLoading ? '加密登录中...' : '立即登录' }}
          </button>
          
          <div class="divider"><span>其他方式登录</span></div>
          <div class="social-login">
            <button type="button" class="social-btn wechat"><img src="https://api.iconify.design/ri:wechat-fill.svg?color=%2307c160" alt="微信" /></button>
            <button type="button" class="social-btn qq"><img src="https://api.iconify.design/ri:qq-fill.svg?color=%2312b7f5" alt="QQ" /></button>
            <button type="button" class="social-btn dingtalk"><img src="https://api.iconify.design/ri:dingding-fill.svg?color=%23007fff" alt="钉钉" /></button>
          </div>

          <p class="register-hint">
            还没有账号？ <a href="#" class="register-link" @click.prevent="switchToRegister">免费注册</a>
          </p>
        </form>

        <div v-else class="register-container">
          <transition name="step-fade" mode="out-in">
            
            <form v-if="regStep === 1" @submit.prevent="nextStep" class="login-form">
              <div class="input-group">
                <label>邮箱 <span>*</span></label>
                <input type="email" v-model="regForm.account" placeholder="请输入有效的邮箱" required />
              </div>

              <div class="input-group">
                <label>验证码 <span>*</span></label>
                <div class="code-input-wrap">
                  <input type="text" v-model="regForm.code" placeholder="请输入验证码" required />
                  <button type="button" class="send-code-btn" @click="sendCode" :disabled="isCounting">
                    {{ isCounting ? `${countdown}秒后重发` : '获取验证码' }}
                  </button>
                </div>
              </div>

              <button type="submit" class="submit-btn register-btn" :disabled="isLoading">
                {{ isLoading ? '验证中...' : '下一步' }}
              </button>

              <p class="register-hint" style="margin-top: 25px;">
                已有账号？ <a href="#" class="register-link" @click.prevent="isLogin = true">立即登录</a>
              </p>
            </form>

            <form v-else @submit.prevent="handleRegister" class="login-form">
              
              <div class="input-group">
                <label>注册身份 <span>*</span></label>
                <div class="role-selector">
                  <label class="radio-label">
                    <input type="radio" v-model="regForm.userRole" :value="1" /> 🎓 学生
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="regForm.userRole" :value="4" /> 👨‍🏫 导师
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="regForm.userRole" :value="3" /> 🏢 企业
                  </label>
                  <label class="radio-label">
                    <input type="radio" v-model="regForm.userRole" :value="2" /> ⚙️ 管理员
                  </label>
                </div>
              </div>

              <transition name="fade">
                <div class="input-group" v-if="regForm.userRole !== 1">
                  <label>专属邀请码 <span>*</span></label>
                  <input type="text" v-model="regForm.invitationCode" placeholder="非学生必须填写平台分配的邀请码" :required="regForm.userRole !== 1" />
                </div>
              </transition>

              <div class="input-group">
                <label>专属昵称 <span>*</span></label>
                <input type="text" v-model="regForm.nickname" placeholder="起一个好听的昵称(2-32位)" required minlength="2" maxlength="32" />
              </div>

              <div class="input-group">
                <label>设置密码 <span>*</span></label>
                <input type="password" v-model="regForm.password" placeholder="6-20位密码" required minlength="6" maxlength="20" />
              </div>

              <div class="form-actions" style="margin-bottom: 20px;">
                <label class="checkbox-container">
                  <input type="checkbox" v-model="regForm.agree" required />
                  <span class="checkmark"></span>
                  我已阅读并同意 <a href="#" class="forgot-link">《用户协议》</a>
                </label>
              </div>

              <div class="step-actions">
                <button type="button" class="back-btn" @click="regStep = 1">返回</button>
                <button type="submit" class="submit-btn register-btn" :disabled="isLoading">
                  {{ isLoading ? '注册中...' : '完成注册' }}
                </button>
              </div>
            </form>

          </transition>
        </div>

      </transition>
    </div>

    <transition name="modal-fade">
      <div class="modal-overlay" v-if="forgotPasswordVisible" @click.self="closeForgotPassword">
        <div class="modal-content" style="max-width: 450px;">
          <div class="modal-header">
            <h3>🔐 忘记密码</h3>
            <button class="close-modal-btn" @click="closeForgotPassword">✕</button>
          </div>
          <div class="modal-body">
            <transition name="forgot-toast-fade">
              <div v-if="forgotToast.show" class="forgot-toast" :class="forgotToast.type">
                <span class="forgot-toast-icon">{{ forgotToast.type === 'success' ? '✅' : '⚠️' }}</span>
                <span class="forgot-toast-text">{{ forgotToast.message }}</span>
              </div>
            </transition>
            
            <div v-if="forgotStep === 1" class="forgot-form">
              <div class="input-group">
                <label>注册邮箱 <span>*</span></label>
                <input 
                  type="email" 
                  v-model="forgotForm.email" 
                  placeholder="请输入注册时使用的邮箱" 
                  :disabled="isForgotLoading"
                />
              </div>
              <div class="input-group">
                <label>验证码 <span>*</span></label>
                <div class="code-input-wrap">
                  <input 
                    type="text" 
                    v-model="forgotForm.code" 
                    placeholder="请输入邮箱收到的验证码" 
                    :disabled="isForgotLoading"
                  />
                  <button 
                    type="button" 
                    class="send-code-btn" 
                    @click="sendForgetPasswordCode" 
                    :disabled="isForgotLoading || isForgotCounting"
                  >
                    {{ isForgotCounting ? `${forgotCountdown}秒后重发` : '获取验证码' }}
                  </button>
                </div>
              </div>
              <button 
                class="submit-btn" 
                @click="verifyForgetCode" 
                :disabled="isForgotLoading || !forgotForm.email || !forgotForm.code"
                style="margin-top: 10px;"
              >
                {{ isForgotLoading ? '验证中...' : '验证并重置密码' }}
              </button>
            </div>

            <div v-if="forgotStep === 2" class="forgot-form">
              <div class="input-group">
                <label>新密码 <span>*</span></label>
                <input 
                  type="password" 
                  v-model="forgotForm.newPassword" 
                  placeholder="请输入新密码（6-20位）" 
                  :disabled="isForgotLoading"
                />
              </div>
              <div class="input-group">
                <label>确认新密码 <span>*</span></label>
                <input 
                  type="password" 
                  v-model="forgotForm.confirmPassword" 
                  placeholder="请再次输入新密码" 
                  :disabled="isForgotLoading"
                  @keyup.enter="resetPassword"
                />
              </div>
              <button 
                class="submit-btn" 
                @click="resetPassword" 
                :disabled="isForgotLoading || !forgotForm.newPassword || !forgotForm.confirmPassword"
                style="margin-top: 10px;"
              >
                {{ isForgotLoading ? '重置中...' : '确认重置密码' }}
              </button>
            </div>
          </div>
          <div class="modal-footer" v-if="forgotStep === 1">
            <button class="btn-cancel" @click="closeForgotPassword" :disabled="isForgotLoading">取消</button>
            <p class="modal-hint">验证通过后将进入密码重置页面</p>
          </div>
          <div class="modal-footer" v-else>
            <button class="btn-cancel" @click="forgotStep = 1" :disabled="isForgotLoading">返回</button>
            <p class="modal-hint">重置成功后请使用新密码登录</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { generateAesKeyAndIv, rsaEncrypt, aesEncrypt } from '../utils/crypto'

const router = useRouter()

// 🌟 取消了 baseURL，让请求继续走 vite.config.js 的 Proxy 代理
axios.defaults.withCredentials = true

// ===== 🌟 自定义 Toast 弹窗逻辑 🌟 =====
const toast = reactive({ show: false, message: '', type: 'success' })
let toastTimer = null

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.message = message
  toast.type = type
  toast.show = true
  toastTimer = setTimeout(() => {
    toast.show = false
  }, 2500)
}

// ===== 页面与按钮状态 =====
const isLogin = ref(true)
const regStep = ref(1) 
const isLoading = ref(false)
const countdown = ref(0)
const isCounting = ref(false)

// ===== 表单数据 =====
const loginForm = reactive({ account: '', password: '', remember: false })
const regForm = reactive({ 
  account: '', 
  code: '', 
  nickname: '', 
  password: '', 
  userRole: 1, 
  invitationCode: '', 
  agree: false 
})

const goBack = () => router.push('/')

const switchToRegister = () => {
  isLogin.value = false
  regStep.value = 1
}

// ==========================================
// 1. 登录逻辑
// ==========================================
const handleLogin = async () => {
  if (!loginForm.account || !loginForm.password) return showToast('请输入账号和密码！', 'error')

  try {
    isLoading.value = true
    let loginWay = 'userAccount'
    if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(loginForm.account)) loginWay = 'email'
    else if (/^\d{11}$/.test(loginForm.account)) loginWay = 'phone'

    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedLoginValue = aesEncrypt(loginForm.account, aesKey, aesIv)
    const encryptedPassword = aesEncrypt(loginForm.password, aesKey, aesIv)

    const loginPayload = {
      login_way: loginWay, 
      encryptedLoginValue: encryptedLoginValue,
      encryptedPassword: encryptedPassword,
      AES: rsaEncrypt(aesKey),
      IV: rsaEncrypt(aesIv)
    }

    const res = await axios.post('/api/user/login', loginPayload)

    if (res.data.code === 200 || res.data.code === 0 || res.data.success || res.data.data?.token) {
      const { token, userAccount } = res.data.data
      localStorage.setItem('token', token)
      localStorage.setItem('userAccount', userAccount)
      
      // 登录成功后拉取用户信息
      try {
        const { aesKey: k, aesIv: i } = generateAesKeyAndIv()
        const infoRes = await axios.get('/api/user/getUserInfo', {
          headers: { 'Authorization': `Bearer ${token}` },
          params: { IV: rsaEncrypt(i), AES: rsaEncrypt(k) }
        })
        if (infoRes.data.data) {
          localStorage.setItem('userRole', infoRes.data.data.userRole) 
          localStorage.setItem('userId', infoRes.data.data.id)
          localStorage.setItem('userName', infoRes.data.data.nickname)
        }
      } catch(e) { 
        console.error('获取用户角色信息失败', e) 
      }
      
      showToast('登录成功！欢迎回来！', 'success')
      setTimeout(() => router.push('/'), 1000)
    } else {
      throw new Error(res.data.msg || res.data.message || '登录失败')
    }
  } catch (error) {
    console.error('登录报错：', error)
    const errorMsg = error.response?.status === 401 ? '无权限(401)：请联系后端放行白名单' : (error.response?.data?.msg || error.message || '网络或服务器异常！')
    showToast(errorMsg, 'error')
  } finally {
    isLoading.value = false
  }
}

// ==========================================
// 2. 获取邮件验证码
// ==========================================
const sendCode = async () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(regForm.account)) return showToast('请先输入正确的邮箱地址！', 'error')

  try {
    isCounting.value = true 
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedEmail = aesEncrypt(regForm.account, aesKey, aesIv)
    
    const codeParams = {
      encryptedEmail: encryptedEmail,
      AES: rsaEncrypt(aesKey),
      IV: rsaEncrypt(aesIv)
    }

    const res = await axios.get('/api/user/sendmail', { params: codeParams })

    if (res.data.code === 200 || res.data.code === 0 || res.data.data?.includes('成功') || res.data.data?.includes('查收')) {
      showToast('验证码已发送，请前往邮箱查收！', 'success')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
          isCounting.value = false 
        }
      }, 1000)
    } else {
      throw new Error(res.data.msg || '发送验证码失败')
    }
  } catch (error) {
    isCounting.value = false 
    showToast(error.response?.data?.msg || error.message || '网络错误！', 'error')
  }
}

// ==========================================
// 3. 校验验证码
// ==========================================
const nextStep = async () => {
  if (!regForm.code) return showToast('请输入验证码！', 'error')

  try {
    isLoading.value = true
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedEmail = aesEncrypt(regForm.account, aesKey, aesIv)

    const verifyPayload = {
      input_code: regForm.code,
      encryptedEmail: encryptedEmail,
      AES: rsaEncrypt(aesKey),
      IV: rsaEncrypt(aesIv)
    }

    const res = await axios.post('/api/user/verify_code', verifyPayload)

    if (res.data.code === 200 || res.data.code === 0 || res.data.data === '邮箱验证成功') {
      if (!regForm.nickname) regForm.nickname = "新星_" + regForm.account.split('@')[0].substring(0, 5)
      showToast('邮箱验证成功！', 'success')
      regStep.value = 2
    } else {
      throw new Error(res.data.msg || '验证码错误')
    }
  } catch (error) {
    showToast(error.response?.data?.msg || error.message || '验证失败！', 'error')
  } finally {
    isLoading.value = false
  }
}

// ==========================================
// 4. 最终注册提交
// ==========================================
const handleRegister = async () => {
  if (!regForm.agree) return showToast('请先勾选用户协议！', 'error')
  if (regForm.userRole !== 1 && !regForm.invitationCode.trim()) {
    return showToast('非学生角色必须填写邀请码！', 'error')
  }

  try {
    isLoading.value = true
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedEmail = aesEncrypt(regForm.account, aesKey, aesIv)

    const registerPayload = {
      nickname: regForm.nickname,
      userPassword: regForm.password,
      encryptedEmail: encryptedEmail,
      AES: rsaEncrypt(aesKey),
      IV: rsaEncrypt(aesIv),
      userRole: regForm.userRole,
      invitationCode: regForm.userRole !== 1 ? regForm.invitationCode : '' 
    }

    const res = await axios.post('/api/user/register', registerPayload)

    if (res.data.code === 200 || res.data.code === 0 || res.data.data?.message === '注册成功') {
      showToast('🎉 注册成功！已为您自动填入账号', 'success')
      loginForm.account = res.data.data?.userAccount || regForm.account
      loginForm.password = ''
      
      setTimeout(() => {
        isLogin.value = true 
        regStep.value = 1    
        Object.assign(regForm, { account: '', code: '', nickname: '', password: '', userRole: 1, invitationCode: '', agree: false })
      }, 1000)
    } else {
      throw new Error(res.data.msg || '注册失败')
    }
  } catch (error) {
    showToast(error.response?.data?.msg || error.message || '服务器异常！', 'error')
  } finally {
    isLoading.value = false
  }
}

// ==========================================
// 5. 忘记密码功能
// ==========================================

const forgotPasswordVisible = ref(false)
const forgotStep = ref(1)
const isForgotLoading = ref(false)
const isForgotCounting = ref(false)
const forgotCountdown = ref(0)
const forgotToast = reactive({ show: false, message: '', type: 'success' })
let forgotToastTimer = null

const showForgotToast = (message, type = 'success') => {
  if (forgotToastTimer) clearTimeout(forgotToastTimer)
  forgotToast.message = message
  forgotToast.type = type
  forgotToast.show = true
  forgotToastTimer = setTimeout(() => {
    forgotToast.show = false
  }, 3000)
}

const forgotForm = reactive({ email: '', code: '', newPassword: '', confirmPassword: '' })

const openForgotPassword = () => {
  forgotPasswordVisible.value = true
  forgotStep.value = 1
  Object.assign(forgotForm, { email: '', code: '', newPassword: '', confirmPassword: '' })
}

const closeForgotPassword = () => {
  if (!isForgotLoading.value) {
    forgotPasswordVisible.value = false
  }
}

const sendForgetPasswordCode = async () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(forgotForm.email)) {
    return showForgotToast('请输入正确的邮箱地址！', 'error')
  }

  try {
    isForgotCounting.value = true
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedEmail = aesEncrypt(forgotForm.email, aesKey, aesIv)
    
    const codeParams = {
      encryptedEmail: encryptedEmail,
      AES: rsaEncrypt(aesKey),
      IV: rsaEncrypt(aesIv)
    }

    const res = await axios.get('/api/user/forget_password_sendmail', { params: codeParams })

    if (res.data.code === 200 || res.data.code === 0 || res.data.data?.includes('成功') || res.data.data?.includes('查收')) {
      showForgotToast('验证码已发送，请前往邮箱查收！', 'success')
      forgotCountdown.value = 60
      const timer = setInterval(() => {
        forgotCountdown.value--
        if (forgotCountdown.value <= 0) {
          clearInterval(timer)
          isForgotCounting.value = false
        }
      }, 1000)
    } else {
      throw new Error(res.data.msg || res.data.message || '发送验证码失败')
    }
  } catch (error) {
    isForgotCounting.value = false
    showForgotToast(error.response?.data?.msg || error.message || '网络错误！', 'error')
  }
}

const verifyForgetCode = async () => {
  if (!forgotForm.code) return showForgotToast('请输入验证码！', 'error')

  try {
    isForgotLoading.value = true
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedEmail = aesEncrypt(forgotForm.email, aesKey, aesIv)

    const verifyPayload = {
      input_code: forgotForm.code,
      encryptedEmail: encryptedEmail,
      AES: rsaEncrypt(aesKey),
      IV: rsaEncrypt(aesIv)
    }

    const res = await axios.post('/api/user/verify_code', verifyPayload)

    const successConditions = [
      res.data.code === 200, res.data.code === 0, res.data.code === 10001,
      res.data.data === '邮箱验证成功', res.data.data?.includes('成功'), res.data.msg?.includes('成功'),
      res.data.message?.includes('成功'), res.data.success === true, res.data.success === 'true',
      res.status === 200 && !res.data.msg?.includes('失败') && !res.data.message?.includes('失败')
    ]

    if (successConditions.some(condition => condition === true)) {
      showForgotToast('验证成功！请设置新密码', 'success')
      forgotStep.value = 2
    } else {
      let errorMsg = '验证码验证失败'
      if (res.data.msg) errorMsg = `验证失败: ${res.data.msg}`
      else if (res.data.message) errorMsg = `验证失败: ${res.data.message}`
      else if (res.data.data) errorMsg = `验证失败: ${String(res.data.data)}`
      else if (res.data.code !== undefined) errorMsg = `验证失败，错误码: ${res.data.code}`
      
      if (errorMsg.length > 100) errorMsg = errorMsg.substring(0, 100) + '...'
      showForgotToast(errorMsg + ' (请检查验证码是否正确)', 'error')
    }
  } catch (error) {
    let errorMsg = '验证失败'
    if (error.response) {
      const { status, statusText } = error.response
      errorMsg = `服务器错误: ${status} ${statusText}`
      if (error.response.data) {
        const serverError = error.response.data.msg || error.response.data.message || error.response.data
        errorMsg += ` - ${String(serverError).substring(0, 80)}`
      }
    } else if (error.request) {
      errorMsg = '网络错误: 无法连接到服务器，请检查网络连接'
    } else {
      errorMsg = error.message || '未知错误'
    }
    showForgotToast(errorMsg + ' (请检查网络连接)', 'error')
  } finally {
    isForgotLoading.value = false
  }
}

const resetPassword = async () => {
  if (!forgotForm.newPassword || !forgotForm.confirmPassword) {
    return showForgotToast('请输入新密码和确认密码！', 'error')
  }
  
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    return showForgotToast('两次输入的新密码不一致！', 'error')
  }
  
  if (forgotForm.newPassword.length < 6 || forgotForm.newPassword.length > 20) {
    return showForgotToast('新密码长度应在6-20位之间！', 'error')
  }

  try {
    isForgotLoading.value = true
    const { aesKey, aesIv } = generateAesKeyAndIv()
    const encryptedEmail = aesEncrypt(forgotForm.email, aesKey, aesIv)
    const encryptedNewPassword = aesEncrypt(forgotForm.newPassword, aesKey, aesIv)
    const encryptedAES = rsaEncrypt(aesKey)
    const encryptedIV = rsaEncrypt(aesIv)

    const params = new URLSearchParams()
    params.append('encryptedEmail', encryptedEmail)
    params.append('encryptedNewPassword', encryptedNewPassword)
    params.append('encryptedNewPassword_again', encryptedNewPassword) 
    params.append('AES', encryptedAES)
    params.append('IV', encryptedIV)

    const res = await axios.post('/api/user/forget_password', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    })

    if (res.data.code === 200 || res.data.code === 0 || res.data.data?.includes('成功')) {
      showForgotToast('密码重置成功！请使用新密码登录', 'success')
      loginForm.password = ''
      setTimeout(() => {
        forgotPasswordVisible.value = false
        isForgotLoading.value = false
      }, 1500)
    } else {
      throw new Error(res.data.msg || res.data.message || '密码重置失败')
    }
  } catch (error) {
    showForgotToast(error.response?.data?.msg || error.message || '重置密码失败！', 'error')
  } finally {
    isForgotLoading.value = false
  }
}
</script>

<style scoped>
/* 🌟 高级 Toast 弹窗样式 */
.custom-toast {
  position: absolute;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: bold;
  color: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}
.custom-toast.success {
  background: rgba(16, 185, 129, 0.9);
  border: 1px solid rgba(16, 185, 129, 0.5);
}
.custom-toast.error {
  background: rgba(239, 68, 68, 0.9);
  border: 1px solid rgba(239, 68, 68, 0.5);
}
.toast-icon { font-size: 1.1rem; }

/* Toast 弹窗平滑动画 */
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55); }
.toast-fade-enter-from { opacity: 0; transform: translate(-50%, -30px); }
.toast-fade-leave-to { opacity: 0; transform: translate(-50%, -20px); }

/* 主体样式 */
.login-page { width: 100vw; height: 100vh; background-image: url('/your-image-name.png'); background-size: cover; background-position: center; background-repeat: no-repeat; display: flex; justify-content: center; align-items: center; position: absolute; top: 0; left: 0; z-index: 1000; overflow: hidden; }
.login-page::before { content: ""; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(240, 246, 255, 0.3); backdrop-filter: blur(5px); z-index: 1; }
.login-card { position: relative; z-index: 2; width: 100%; max-width: 440px; min-height: 580px; background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 20px; padding: 40px; box-shadow: 0 25px 50px rgba(0, 0, 0, 0.1); animation: fadeInUp 0.5s cubic-bezier(0.25, 0.8, 0.25, 1); display: flex; flex-direction: column; overflow: hidden; }
.close-btn { position: absolute; top: 15px; right: 15px; background: transparent; border: none; color: #94A3B8; cursor: pointer; padding: 8px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all 0.3s ease; z-index: 10; }
.close-btn:hover { background: #F1F5F9; color: #334155; transform: rotate(90deg); }
@keyframes fadeInUp { from { transform: translateY(30px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.form-header { text-align: center; margin-bottom: 30px; margin-top: 10px; }
.gradient-text { font-size: 2rem; font-weight: 800; margin: 0 0 8px 0; background: linear-gradient(135deg, #60A5FA 0%, #4A90E2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; letter-spacing: 2px; }
.form-header p { color: #64748B; font-size: 0.95rem; margin: 0; font-weight: 600; letter-spacing: 1px; }

.input-group { margin-bottom: 20px; }
.input-group label { display: block; font-size: 0.9rem; font-weight: bold; color: #334155; margin-bottom: 8px; letter-spacing: 0.5px; }
.input-group label span { color: #4A90E2; }
.input-group input { width: 100%; padding: 12px 16px; border: 1px solid #E2E8F0; border-radius: 12px; background-color: #F8FAFC; color: #1E293B; font-size: 0.95rem; box-sizing: border-box; transition: all 0.3s ease; }
.input-group input:focus { background-color: #FFFFFF; border-color: #4A90E2; box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.15); outline: none; }
.input-group input::placeholder { color: #94A3B8; font-size: 0.9rem; }

.role-selector { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px; }
.radio-label { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px 14px; border: 1px solid #E2E8F0; border-radius: 10px; background: #F8FAFC; cursor: pointer; font-size: 0.85rem; font-weight: 600; color: #475569; transition: all 0.2s; flex: 1 1 calc(50% - 5px); box-sizing: border-box; }
.radio-label:hover { border-color: #4A90E2; background: #F0F6FF; }
.radio-label input[type="radio"] { width: 16px; height: 16px; margin: 0; accent-color: #4A90E2; cursor: pointer; }

.code-input-wrap { display: flex; gap: 12px; }
.code-input-wrap input { flex: 1; }
.send-code-btn { flex-shrink: 0; padding: 0 16px; background: #F0F6FF; color: #4A90E2; border: 1px solid #BFDBFE; border-radius: 12px; font-weight: 600; font-size: 0.9rem; cursor: pointer; transition: 0.2s; }
.send-code-btn:hover:not(:disabled) { background: #E1EDFF; border-color: #93C5FD; }
.send-code-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.form-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; font-size: 0.9rem; }
.checkbox-container { display: flex; align-items: center; gap: 8px; color: #64748B; cursor: pointer; }
.forgot-link { color: #4A90E2; text-decoration: none; font-weight: 600; transition: 0.2s; }
.forgot-link:hover { color: #357ABD; }

.submit-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #60A5FA 0%, #4A90E2 100%); color: #FFFFFF; border: none; border-radius: 12px; font-size: 1.05rem; font-weight: bold; letter-spacing: 2px; cursor: pointer; transition: all 0.3s; box-shadow: 0 6px 15px rgba(74, 144, 226, 0.25); }
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(74, 144, 226, 0.4); background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.register-btn { background: linear-gradient(135deg, #34D399 0%, #10B981 100%); box-shadow: 0 6px 15px rgba(16, 185, 129, 0.25); }
.register-btn:hover:not(:disabled) { background: linear-gradient(135deg, #10B981 0%, #059669 100%); box-shadow: 0 8px 20px rgba(16, 185, 129, 0.4); }

.step-actions { display: flex; gap: 12px; }
.back-btn { padding: 14px 20px; background: #F1F5F9; color: #64748B; border: none; border-radius: 12px; font-weight: bold; cursor: pointer; transition: 0.2s; white-space: nowrap; }
.back-btn:hover { background: #E2E8F0; color: #334155; }

.divider { text-align: center; margin: 25px 0 20px; position: relative; }
.divider::before { content: ""; position: absolute; top: 50%; left: 0; right: 0; height: 1px; background: #E2E8F0; z-index: 1; }
.divider span { background: transparent; padding: 0 15px; color: #94A3B8; font-size: 0.85rem; position: relative; z-index: 2; }
.social-login { display: flex; justify-content: center; gap: 24px; margin-bottom: 25px; }
.social-btn { width: 46px; height: 46px; border-radius: 50%; background: #F8FAFC; border: 1px solid #E2E8F0; display: flex; justify-content: center; align-items: center; cursor: pointer; transition: all 0.2s; }
.social-btn:hover { background: #FFFFFF; transform: translateY(-3px); box-shadow: 0 6px 15px rgba(0,0,0,0.08); }
.social-btn img { width: 24px; height: 24px; }

.register-hint { text-align: center; color: #64748B; font-size: 0.9rem; margin: 0; }
.register-link { color: #4A90E2; text-decoration: none; font-weight: bold; }

.form-fade-enter-active, .form-fade-leave-active { transition: all 0.3s ease; }
.form-fade-enter-from { opacity: 0; transform: translateX(20px); }
.form-fade-leave-to { opacity: 0; transform: translateX(-20px); }

.step-fade-enter-active, .step-fade-leave-active { transition: all 0.25s ease; }
.step-fade-enter-from { opacity: 0; transform: translateX(30px); }
.step-fade-leave-to { opacity: 0; transform: translateX(-30px); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-10px); }

/* 忘记密码弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-content {
  background: #FFFFFF;
  width: 100%;
  max-width: 450px;
  border-radius: 20px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #EAECEF;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.15rem;
  color: #1E293B;
}

.close-modal-btn {
  background: none;
  border: none;
  color: #94A3B8;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  font-size: 1.2rem;
  transition: 0.2s;
}

.close-modal-btn:hover {
  background: #F1F5F9;
  color: #EF4444;
}

.modal-body {
  padding: 30px 24px;
}

.forgot-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #EAECEF;
  background: #F8FAFC;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid #CBD5E1;
  background: #FFFFFF;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: 0.2s;
}

.btn-cancel:hover:not(:disabled) {
  background: #F1F5F9;
}

.btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-hint {
  font-size: 0.85rem;
  color: #94A3B8;
  margin: 0;
}

/* 忘记密码弹窗内的提示样式 */
.forgot-toast {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  animation: slideDown 0.3s ease-out;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.forgot-toast.success {
  background: rgba(16, 185, 129, 0.1);
  color: #065F46;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.forgot-toast.error {
  background: rgba(239, 68, 68, 0.1);
  color: #991B1B;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.forgot-toast-icon {
  font-size: 1.1rem;
}

.forgot-toast-text {
  flex: 1;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.forgot-toast-fade-enter-active,
.forgot-toast-fade-leave-active {
  transition: all 0.3s ease;
}

.forgot-toast-fade-enter-from,
.forgot-toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 弹窗动画 */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-content,
.modal-fade-leave-active .modal-content {
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.modal-fade-enter-from .modal-content,
.modal-fade-leave-to .modal-content {
  transform: scale(0.95) translateY(20px);
}
</style>