<template>
  <main class="cover-page" aria-labelledby="cover-title">
    <img class="cover-art" :src="heroImage" alt="" fetchpriority="high" />
    <div class="cover-cloud-layer" aria-hidden="true"></div>
    <div class="cover-readability" aria-hidden="true"></div>
    <div class="cover-sparkles" aria-hidden="true">
      <span class="cover-spark cover-spark--one"></span>
      <span class="cover-spark cover-spark--two"></span>
      <span class="cover-spark cover-spark--three"></span>
      <span class="cover-spark cover-spark--four"></span>
    </div>

    <header class="cover-header">
      <router-link class="cover-brand" to="/" aria-label="星职首页">
        <span class="cover-brand-icon"><AppIcon name="compass" :size="27" /></span>
        <span><strong>星职</strong><small>STAR CAREER</small></span>
      </router-link>
      <span class="cover-header-note">AI 职业导航 · 让每一步都有方向</span>
    </header>

    <section class="cover-copy">
      <p class="cover-kicker">人生如海 · 职业如星</p>
      <h1 id="cover-title">让职业选择，<br />更有方向<span>。</span></h1>
      <h2>从多智能体测评，到 3–5 年成长路径</h2>
      <p class="cover-description">基于你的兴趣、能力与经历，结合职业趋势与岗位要求，<br class="cover-desktop-break" />把职业探索绘成一张可以持续调整的成长航图。</p>
      <button class="cover-login-button" type="button" @click="enterJourney">
        {{ isLoggedIn ? '开始联合测评' : '登录，开启职业旅程' }}
        <span aria-hidden="true">→</span>
      </button>
      <p class="cover-login-hint">{{ isLoggedIn ? '继续探索你的专属成长路径' : '登录后开启联合测评、职业星图与成长计划' }}</p>
    </section>

    <footer class="cover-footer">
      <span>探索更多可能，让每一步都有方向。</span>
      <span>星职 · 与你同行</span>
    </footer>

    <LoginView v-if="loginPanelOpen" embedded @close="loginPanelOpen = false" />
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import LoginView from './LoginView.vue'
import heroImage from '../assets/images/career-dawn-hero-v2.png'
import '../styles/coverLanding.css'

const router = useRouter()
const isLoggedIn = ref(Boolean(localStorage.getItem('token')))
const loginPanelOpen = ref(false)

function enterJourney() {
  if (isLoggedIn.value) router.push('/multi-agent')
  else loginPanelOpen.value = true
}
</script>
