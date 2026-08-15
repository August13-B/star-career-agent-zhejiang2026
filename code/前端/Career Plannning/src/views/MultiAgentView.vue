<template>
  <div class="multi-agent-page scroll-container" ref="pageContainer">
    <div class="ambient-glow glow-blue"></div>
    <div class="ambient-glow glow-purple"></div>
    <div class="ambient-glow glow-cyan"></div>

    <div class="workspace-wrapper">
      <div class="page-header">
        <div class="header-content">
          <div class="title-section">
            <div class="title-badge"><span class="pulse-dot"></span> 动态规划追踪演示</div>
            <h1 class="gradient-text">MOE 伴随式智能体协同大屏</h1>
            <p class="subtitle">打破静态死板规划 · 融合情绪感知与动态路径重构技术</p>
          </div>
          <button class="global-export-btn" @click="exportFullReport" :disabled="workflowStep === 0">
            <span class="btn-icon">📥</span>
            <span class="btn-text">一键导出完整报告</span>
          </button>
        </div>
      </div>

      <div class="query-section glass-panel">
        <div class="user-info">
          <div class="avatar"><span class="avatar-ring"></span>我</div>
          <div class="query-content">
            <p class="query-text">
              "我是大三计算机科班生，目前只会点 Java 基础和简单的 Vue。不知道未来该走后端还是前端，马上秋招了，感觉自己什么都拿不出手，每天极度焦虑，忍不住刷短视频逃避现实，我该怎么办？"
            </p>
          </div>
        </div>
        <button class="trigger-btn" @click="startPhaseOne" :disabled="workflowStep > 0">
          <span v-if="workflowStep === 0">⚡ 唤醒多智能体核心阵列</span>
          <span v-else-if="workflowStep < 6" class="loading-text"><span class="spinner"></span> 10维雷达并行演算中...</span>
          <span v-else>✅ 初始画像构建完毕</span>
          <div class="btn-glow" v-if="workflowStep === 0"></div>
        </button>
      </div>

      <div class="agents-network" v-if="workflowStep > 0">
        <div v-for="(agent, index) in agentList" :key="agent.id" class="agent-card glass-panel"
             :data-agent-id="agent.id"
             :class="{ 'is-thinking': agent.status === 'thinking', 'is-done': agent.status === 'done', 'is-warning': agent.status === 'warning' }">
          
          <div class="agent-header" @click="toggleAgent(agent)">
            <div class="agent-icon" :style="{ background: agent.color }">{{ agent.icon }}</div>
            <div class="agent-title">
              <h4>{{ agent.name }}</h4><span class="agent-role">{{ agent.role }}</span>
            </div>
            <div class="status-box">
              <span v-if="agent.status === 'waiting'" class="dot waiting"></span>
              <span v-if="agent.status === 'thinking'" class="dot thinking pulse"></span>
              <div v-if="agent.status === 'done' || agent.status === 'warning'" class="agent-controls">
                <!-- 为所有模块显示编辑和导出按钮 -->
                <div class="agent-actions">
                  <button class="action-btn edit-btn" :class="{ 'active': agent.isEditing, 'loading': agent.isSaving }" @click.stop="toggleEditMode(agent)">
                    <span class="btn-icon">✏️</span>
                    <span class="btn-text">{{ agent.isEditing ? '保存' : '编辑' }}</span>
                  </button>
                </div>
                <button class="expand-toggle">
                  <span class="btn-text">{{ agent.expanded ? '收起' : '展开' }}</span>
                  <span class="arrow" :class="{ 'up': agent.expanded }">▼</span>
                </button>
              </div>
            </div>
          </div>
          
          <div class="agent-body-wrapper" :class="{ 'open': agent.expanded || agent.status === 'thinking' }">
            <div class="agent-body-inner">
              <p v-if="agent.status === 'waiting'" class="placeholder-text">等待截取雷达数据流...</p>
              <div v-else-if="agent.status === 'thinking'" class="skeleton-loader">
                <div class="line w-80"></div><div class="line w-100"></div><div class="line w-60"></div>
              </div>
              <div v-else-if="agent.isEditing" class="editable-content">
                <textarea v-model="agent.editableOutput" class="edit-textarea"></textarea>
              </div>
              <div v-else class="typed-text" v-html="agent.output"></div>
            </div>
          </div>
        </div>
      </div>

      <transition name="fade-up">
        <div v-if="workflowStep >= 5" class="synthesis-board glass-panel">
          <div class="board-header">
            <div class="master-icon-box"><span class="master-icon">✨</span><div class="ring-1"></div></div>
            <div class="board-title">
              <h3>Planner Agent · 阶段一引航蓝图</h3>
              <p>为了避免目标过大导致瘫痪，我们仅下发前 15 天的微观任务。</p>
            </div>
          </div>
          
          <div class="board-content">
            <div class="time-node">
              <span class="time-badge">Day 1-4：底层心法重构与习惯熔断</span>
              <p>
                <strong>🎯 提升维度：</strong> <span class="dim-tag pink">抗压能力</span> <span class="dim-tag blue">专注力重建</span><br>
                <strong>🚀 核心执行动作：</strong> <br>
                1. <strong>物理级信息熔断：</strong> 强制卸载短视频及娱乐社交 App，每日开启 4 个“深潜番茄钟”（每个 50 分钟），严禁任何非编程相关的信息输入，强行重建因碎片化信息受损的延迟满足机制。<br>
                2. <strong>技术栈降维打击：</strong> 战略性关停前端 Vue 业务层开发，避免陷入“API 调用员”的低效内卷。全量切入 <strong>Java 后端底层赛道</strong>，通过手绘 JVM 内存布局图（堆、栈、方法区、计数器）实现对内存管理的深度内化。<br>
                3. <strong>内存管理深度攻坚：</strong> 深入解析垃圾回收（GC）机制，掌握 G1 与 ZGC 的核心算法逻辑，产出 1 篇超过 3000 字的《JVM 运行时数据区深度剖析》技术文档作为通关秘籍。
              </p>
            </div>

            <div class="time-node">
              <span class="time-badge">Day 5-8：高并发基石与框架生态解构</span>
              <p>
                <strong>🎯 提升维度：</strong> <span class="dim-tag purple">工程化思维</span> <span class="dim-tag blue">代码严谨性</span><br>
                <strong>🚀 核心执行动作：</strong> <br>
                1. <strong>JUC 并发编程突破：</strong> 彻底攻克“并发三座大山”。手写 AQS 核心流程，深入理解 CAS 与 volatile 的底层缓存一致性协议（MESI），实现对多线程通信的绝对掌控。<br>
                2. <strong>Spring 源码级接驳：</strong> 停止简单的注解调用，转而通过模拟实现“微型 IOC 容器”来理解 Bean 的生命周期。深入研读循环依赖解决方案，掌握三级缓存的设计哲学。<br>
                3. <strong>线程池动态调优：</strong> 基于工业级标准，实操模拟高并发下的线程池饱和策略，掌握 corePoolSize 与 maximumPoolSize 的动态配置算法，通过压力测试输出性能优化报告。
              </p>
            </div>

            <div class="time-node">
              <span class="time-badge">Day 9-12：分布式突围与数据存储演进</span>
              <p>
                <strong>🎯 提升维度：</strong> <span class="dim-tag blue">系统设计力</span> <span class="dim-tag pink">问题排查力</span><br>
                <strong>🚀 核心执行动作：</strong> <br>
                1. <strong>Redis 全场景性能压榨：</strong> 在本地构建高可用缓存架构。不仅要解决“缓存三兄弟”（穿透、击穿、雪崩），更要实操分布式锁（Redlock）的并发竞争处理，实现系统吞吐量的量级跃迁。<br>
                2. <strong>MySQL 索引深度重构：</strong> 彻底告别全表扫描。深入 InnoDB B+ 树底层结构，通过 Explain 执行计划分析慢查询 SQL，针对百亿级数据模拟场景下的最左前缀法则与覆盖索引进行极致优化。<br>
                3. <strong>中间件链路闭环：</strong> 整合消息队列（RabbitMQ/Kafka）处理业务削峰填谷，设计并实现一套具备“最终一致性”的分布式事务处理方案，并在个人项目仓库中完成全链路集成。
              </p>
            </div>

            <div class="time-node">
              <span class="time-badge">Day 13-15：STAR法则重塑与高压模拟</span>
              <p>
                <strong>🎯 提升维度：</strong> <span class="dim-tag purple">表达说服力</span> <span class="dim-tag blue">面试感知度</span><br>
                <strong>🚀 核心执行动作：</strong> <br>
                1. <strong>项目亮点原子化拆解：</strong> 摒弃“负责某某功能”的平庸叙述。利用 STAR 法则，将项目重构成“通过引入 Redis 分布式锁解决高并发超卖，QPS 提升 3 倍”的硬核成果表达。<br>
                2. <strong>高频八股文降维打击：</strong> 每天进行 2 场（每场 45 分钟）的高强度全真模拟面试。针对“如果流量翻十倍你的系统怎么扛”等系统设计类题目进行闭环推演，形成条件反射式的肌肉记忆。<br>
                3. <strong>终极心理护城河构建：</strong> 完成作品集的最后一次性能压测与文档修缮。通过多智能体的情绪压力反馈系统进行心理调适，以“方案解决者”而非“求职者”的姿态，正式接驳即将到来的秋招窗口期。
              </p>
            </div>
          </div>
        </div>
      </transition>

      <transition name="fade-up">
        <div v-if="workflowStep >= 6" class="feedback-section glass-panel highlight-border" id="feedback-anchor">
          <div class="section-title-box">
            <span class="icon">📸</span>
            <h3>Day 15 动态复盘与校验舱</h3>
            <p>请上传这 15 天的执行凭证，并如实反馈你的情绪状态，AI 将为你重构下阶段路径。</p>
          </div>

         <!-- 在 feedback-grid 内部 -->
<div class="feedback-grid">
  <!-- 真实的文件输入框，隐藏起来 -->
  <input 
    type="file" 
    ref="fileInputRef" 
    style="display: none" 
    accept="image/*" 
    @change="handleFileSelect"
  />

  <div class="upload-box" :class="{ 'is-uploaded': uploadStatus === 'done' }" @click="triggerFileSelect">
    <div v-if="uploadStatus === 'none'" class="upload-prompt">
      <div class="upload-icon">📤</div>
      <p>点击上传执行凭证<br><span>(支持 LeetCode截图、博客截图、Git提交记录)</span></p>
    </div>
    <div v-else-if="uploadStatus === 'uploading'" class="uploading-state">
      <div class="spinner-large"></div>
      <p>AI 视觉引擎解析中...</p>
      <div class="progress-bar"><div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div></div>
    </div>
    <div v-else class="uploaded-success">
      <div class="file-thumb">
        <!-- 这里可以改成显示真实图片缩略图，我先保留你原来的代码风格 -->
        <div class="code-mock"><div class="c-line"></div><div class="c-line short"></div><div class="c-line half"></div></div>
      </div>
      <div class="file-info">
        <!-- 显示真实的文件名 -->
        <span class="file-name">{{ selectedFileName }}</span>
        <span class="file-badge">AI 校验通过 ✅</span>
      </div>
    </div>
  </div>
            <div class="emotion-selector" :class="{ 'disabled': uploadStatus !== 'done' }">
              <p class="selector-title">这 15 天的执行体感如何？</p>
              <div class="emotion-options">
                <div class="emo-btn" :class="{'active': emotionType === 'easy'}" @click="emotionType = 'easy'">😎 太简单了，请求加码</div>
                <div class="emo-btn" :class="{'active': emotionType === 'normal'}" @click="emotionType = 'normal'">👌 节奏正好，平稳推进</div>
                <div class="emo-btn" :class="{'active': emotionType === 'hard'}" @click="emotionType = 'hard'">🤯 任务太重，极其痛苦</div>
              </div>
            </div>
          </div>

          <button class="submit-feedback-btn" :disabled="uploadStatus !== 'done' || !emotionType || workflowStep >= 7" @click="startPhaseTwo">
            <span v-if="workflowStep < 7">🧠 提交复盘，请求多智能体二次诊断</span>
            <span v-else>✅ 已触发大模型熔断机制</span>
          </button>
        </div>
      </transition>

      <transition name="fade-up">
        <div v-if="workflowStep >= 8" class="synthesis-board glass-panel dynamic-adjusted">
          <div class="board-header warning-header">
            <div class="master-icon-box"><span class="master-icon">🚨</span><div class="ring-1 warning-ring"></div></div>
            <div class="board-title">
              <h3>Planner Agent · 动态重构路径下发</h3>
              <p>检测到负向情绪反馈。已根据大模型韧性保护算法，为您实施<strong>【路径降级与正反馈重建】</strong>。</p>
            </div>
          </div>
          <div class="board-content">
            <div class="time-node adjusted-node">
              <span class="time-badge warning-badge">Day 1-4：多巴胺熔断与精神防火墙</span>
              <p>
                <strong>⚠️ 干预状态：</strong> <span class="dim-tag pink">执行力修复</span> <span class="dim-tag blue">精神高压测试</span><br>
                <strong>🚀 紧急校准动作：</strong> <br>
                1. <strong>全隔离式静默训练：</strong> 物理切断所有社交软件干扰，每日强制进行 2 场“极限编码马拉松”（每场 120 分钟），严禁在遇到 Bug 时立刻检索视频，必须通过翻阅官方源码及堆栈信息进行自主破局。<br>
                2. <strong>核心语言精度重构：</strong> 针对此前 Java 基础浮躁的问题，强行切入 Java 集合类源码（HashMap/ConcurrentHashMap）。要求能手绘红黑树旋转逻辑，并对扩容机制中的 CPU 抖动问题给出量化分析结论。<br>
                3. <strong>思维陷阱排查：</strong> 利用情绪监控模块反馈，对过往逃避行为进行技术复盘，产出《执行偏差技术成因分析报告》，将“焦虑”转化为可量化的“待办任务”。
              </p>
            </div>

            <div class="time-node adjusted-node">
              <span class="time-badge warning-badge">Day 5-8：架构深潜与性能极限压榨</span>
              <p>
                <strong>⚠️ 干预状态：</strong> <span class="dim-tag purple">底层逻辑重组</span> <span class="dim-tag blue">算力效率对齐</span><br>
                <strong>🚀 紧急校准动作：</strong> <br>
                1. <strong>JVM 线上故障复现：</strong> 模拟生产环境下的 OutOfMemoryError (OOM) 场景，利用 MAT (Memory Analyzer Tool) 进行内存泄露排查。掌握非堆内存溢出、元空间缩容等极端情况的应急处理预案。<br>
                2. <strong>并发模型重度演练：</strong> 抛弃对 synchronized 的简单依赖，转而使用 LockSupport 配合自定义同步器。模拟高并发秒杀场景下的“缓存击穿”与“数据库行锁死锁”对抗，在毫秒级延迟下实现数据强一致性。<br>
                3. <strong>SQL 索引深度“切除”：</strong> 对低效查询进行手术刀级别的重构。深度解析 MySQL 覆盖索引与复合索引的页分裂原理，将原本导致数据库 CPU 飙升的全表扫描语句彻底清零。
              </p>
            </div>

            <div class="time-node adjusted-node">
              <span class="time-badge warning-badge">Day 9-12：分布式集群实战与云原生进阶</span>
              <p>
                <strong>⚠️ 干预状态：</strong> <span class="dim-tag blue">系统稳定性保障</span> <span class="dim-tag pink">技术广度覆盖</span><br>
                <strong>🚀 紧急校准动作：</strong> <br>
                1. <strong>Redis 高可用矩阵搭建：</strong> 不满足于单机版，必须在本地容器化部署 Redis Sentinel (哨兵) 与 Cluster (集群) 模式。实战模拟主从切换时的请求丢失场景，设计并验证全量同步与增量同步的补偿逻辑。<br>
                2. <strong>微服务全链路监控：</strong> 引入 Spring Cloud Alibaba 体系，手动配置 Sentinel 流量卫兵。针对“服务雪崩”模拟多级熔断机制，实现系统在 40% 核心模块宕机的情况下依然保持 99.9% 的基本可用性。<br>
                3. <strong>异步通信解耦演练：</strong> 利用 RocketMQ 的消息回溯与死信队列机制，处理复杂的分布式事务。产出 1 篇关于《CAP 定理在本项目中的平衡策略》的技术白皮书。
              </p>
            </div>

            <div class="time-node adjusted-node">
              <span class="time-badge warning-badge">Day 13-15：全真大厂面试压测与心态武装</span>
              <p>
                <strong>⚠️ 干预状态：</strong> <span class="dim-tag purple">职场认知升维</span> <span class="dim-tag blue">极限心理防御</span><br>
                <strong>🚀 紧急校准动作：</strong> <br>
                1. <strong>简历价值倍增：</strong> 将“参与过某项目”的叙述重构为“负责基于 AIGC 的伴随式职业规划系统研发，利用分阶段动态演化算法将用户执行力提升 40%”。强调技术细节对业务指标的驱动价值。<br>
                2. <strong>三轮极限模拟面测：</strong> 每天进行 3 场覆盖“基础理论-架构设计-人生价值观”的 360 度全方位模拟面试。针对“高并发场景下数据不一致如何极致修复”等压力问题，形成结构化、教科书级的应答模板。<br>
                3. <strong>秋招破局最后冲刺：</strong> 汇总 15 天来的所有代码产出与博客沉淀。在 Planner Agent 的最终情绪评分辅助下，完成从“逃避者”到“技术硬核人才”的身份跃迁，以满格状态接驳市场一线。
              </p>
            </div>
          </div>
        </div>
      </transition>

    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const workflowStep = ref(0)
const isSimulating = ref(false)
const pageContainer = ref(null)

const uploadStatus = ref('none') 
const uploadProgress = ref(0)
const emotionType = ref('')

// 万字长文 100% 还原，加入 expanded 状态
const agentList = ref([
  {
    id: 'coach', name: 'Career Matching Agent', role: '岗位匹配与能力画像', icon: '🧭', color: 'linear-gradient(135deg, #3B82F6, #2563EB)', status: 'waiting', expanded: false,
    output: '<span class="highlight-blue">【雷达捕获】</span> 基于目标岗位画像与个人就业能力画像的双向匹配，当前在专业技能（<span class="highlight">Java/Vue掌握度62</span>）、通用素质（<span class="highlight">学习能力78</span>、<span class="highlight">沟通协作70</span>）方面，与<span class="highlight">互联网后端开发岗</span>的综合契合度为<span class="highlight-orange">65%</span>。差距集中在<span class="highlight-orange">工程化实践（仅45，仅停留在基础编码阶段，缺乏CI/CD流程、自动化测试、版本管理与上线部署的完整经验）</span>与<span class="highlight-orange">行业工具栈掌握（仅50，对Git协作、Docker容器化、K8s基础运维、Jenkins自动化构建等工程化工具的应用能力不足）</span>上。<br><br><span class="highlight-blue">【研判与对策】</span> 系统已完成目标岗位的能力模型拆解，建议优先对齐岗位核心要求：<span class="highlight-green">补全SpringBoot项目实战</span>、<span class="highlight-green">工程化协作流程</span>，通过项目/实习补齐实践短板，提升岗位匹配度。',
    isEditing: false,
    isSaving: false,
    isExporting: false,
    editableOutput: '【雷达捕获】基于目标岗位画像与个人就业能力画像的双向匹配，当前在专业技能（Java/Vue掌握度62）、通用素质（学习能力78、沟通协作70）方面，与互联网后端开发岗的综合契合度为65%。差距集中在工程化实践（仅45，仅停留在基础编码阶段，缺乏CI/CD流程、自动化测试、版本管理与上线部署的完整经验）与行业工具栈掌握（仅50，对Git协作、Docker容器化、K8s基础运维、Jenkins自动化构建等工程化工具的应用能力不足）上。\n\n【研判与对策】系统已完成目标岗位的能力模型拆解，建议优先对齐岗位核心要求：补全SpringBoot项目实战、工程化协作流程，通过项目/实习补齐实践短板，提升岗位匹配度。'
  },
  {
    id: 'emotion', name: 'Career Path Agent', role: '目标设定与路径规划', icon: '🎯', color: 'linear-gradient(135deg, #EC4899, #D946EF)', status: 'waiting', expanded: false,
    output: '<span class="highlight-pink">【雷达捕获】</span> 结合个人意愿与行业趋势，推荐<span class="highlight-purple">目标路径</span>为：<span class="highlight">Java后端开发工程师 → 资深后端工程师 → 架构师/技术负责人</span>。当前行业后端岗位需求稳定增长，<span class="highlight-orange">中高级人才缺口明显</span>，具备<span class="highlight">工程化与架构能力</span>的开发者溢价显著。<br><br><span class="highlight-pink">【研判与对策】</span> 明确阶段目标：<span class="highlight-green">1年内夯实JavaWeb基础+完成1个完整项目</span>；<span class="highlight-green">2年内掌握微服务架构与高并发优化</span>；<span class="highlight-green">3年内向架构设计方向深耕</span>。结合企业岗位数据，已标注各阶段关键技能与晋升节点，避免发展路径断层。',
    isEditing: false,
    isSaving: false,
    isExporting: false,
    editableOutput: '【雷达捕获】结合个人意愿与行业趋势，推荐目标路径为：Java后端开发工程师 → 资深后端工程师 → 架构师/技术负责人。当前行业后端岗位需求稳定增长，中高级人才缺口明显，具备工程化与架构能力的开发者溢价显著。\n\n【研判与对策】明确阶段目标：1年内夯实JavaWeb基础+完成1个完整项目；2年内掌握微服务架构与高并发优化；3年内向架构设计方向深耕。结合企业岗位数据，已标注各阶段关键技能与晋升节点，避免发展路径断层。'
  },
  {
    id: 'hr', name: 'Growth Plan Agent', role: '分阶段成长与动态评估', icon: '📈', color: 'linear-gradient(135deg, #10B981, #059669)', status: 'waiting', expanded: false,
    output: '<span class="highlight-green">【雷达捕获】</span> 基于当前能力差距与职业路径，已生成<span class="highlight-purple">「短期（0-6个月）+中期（6-18个月）」双阶段成长计划</span>，覆盖学习、实践、复盘全流程。<br><br><span class="highlight-green">【研判与对策】</span><br><span class="highlight-green">短期目标：</span>完成Java后端进阶学习+2个实战项目，掌握<span class="highlight">SpringBoot、MyBatis、Maven</span>等核心工具，每月1次进度评估。<br><span class="highlight-green">中期目标：</span>通过实习/项目积累工程化经验，参与团队协作开发，掌握<span class="highlight">微服务与Redis/MySQL优化</span>，每季度进行能力复盘与计划调整。',
    isEditing: false,
    isSaving: false,
    isExporting: false,
    editableOutput: '【雷达捕获】基于当前能力差距与职业路径，已生成「短期（0-6个月）+中期（6-18个月）」双阶段成长计划，覆盖学习、实践、复盘全流程。\n\n【研判与对策】\n短期目标：完成Java后端进阶学习+2个实战项目，掌握SpringBoot、MyBatis、Maven等核心工具，每月1次进度评估。\n中期目标：通过实习/项目积累工程化经验，参与团队协作开发，掌握微服务与Redis/MySQL优化，每季度进行能力复盘与计划调整。'
  },
  {
    id: 'skill', name: 'Report Editor & Export', role: '报告优化与一键导出', icon: '📄', color: 'linear-gradient(135deg, #F59E0B, #D97706)', status: 'waiting', expanded: false,
    output: '<span class="highlight-orange">【雷达捕获】</span> 已完成职业规划报告的智能润色与内容完整性校验，报告包含<span class="highlight">岗位匹配、目标路径、成长计划</span>全模块，逻辑完整、重点清晰。<br><br><span class="highlight-orange">【研判与对策】</span> 你可以点击<span class="highlight-green">「编辑」</span>按钮直接修改报告内容，调整文字细节；确认无误后，点击<span class="highlight-green">「一键导出」</span>即可将完整规划报告<span class="highlight-green">导出为PDF文件</span>，方便打印或提交使用。',
    isEditing: false,
    isSaving: false,
    isExporting: false,
    editableOutput: '【雷达捕获】已完成职业规划报告的智能润色与内容完整性校验，报告包含岗位匹配、目标路径、成长计划全模块，逻辑完整、重点清晰。\n\n【研判与对策】你可以点击「编辑」按钮直接修改报告内容，调整文字细节；确认无误后，点击「一键导出」即可将完整规划报告导出为PDF文件，方便打印或提交使用。'
  }
])

// 切换编辑模式
const toggleEditMode = (agent) => {
  if (agent.isEditing) {
    // 保存编辑内容
    agent.isSaving = true;
    // 模拟保存延迟
    setTimeout(() => {
      agent.output = agent.editableOutput;
      agent.isSaving = false;
      agent.isEditing = false;
      // 确保模块展开以显示完整内容
      agent.expanded = true;
    }, 800);
  } else {
    // 进入编辑模式，将HTML内容转换为纯文本进行编辑
    agent.editableOutput = agent.output
      .replace(/<br><br>/g, '\n\n')
      .replace(/<br>/g, '\n')
      .replace(/<strong>/g, '')
      .replace(/<\/strong>/g, '')
      .replace(/<span class="[^"]+">/g, '')
      .replace(/<\/span>/g, '');
    agent.isEditing = true;
    // 确保模块展开以显示编辑框
    agent.expanded = true;
  }
}

// 导出报告为PDF
const exportReport = (agent) => {
  agent.isExporting = true;
  // 模拟导出延迟
  setTimeout(() => {
    let reportContent = '';
    let fileName = '';
    
    if (agent.id === 'skill') {
      // Report Editor & Export模块导出完整报告
      reportContent = '<h1>职业规划报告</h1>';
      agentList.value.forEach(agentItem => {
        reportContent += `<h2>${agentItem.name} - ${agentItem.role}</h2>`;
        reportContent += `<div>${agentItem.output}</div>`;
        reportContent += '<hr>';
      });
      fileName = '职业规划报告.html';
    } else {
      // 其他模块导出对应内容
      reportContent = `<h1>${agent.name} - ${agent.role}</h1>`;
      reportContent += `<div>${agent.output}</div>`;
      fileName = `${agent.name}.html`;
    }
    
    // 创建一个临时HTML文件
    const blob = new Blob([`
      <!DOCTYPE html>
      <html>
      <head>
        <title>${fileName.replace('.html', '')}</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 20px; }
          h1 { color: #333; text-align: center; }
          h2 { color: #555; margin-top: 30px; }
          hr { margin: 20px 0; }
          .content { line-height: 1.6; }
          .highlight { background: linear-gradient(90deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #1D4ED8; }
          .highlight-orange { background: linear-gradient(90deg, rgba(245, 158, 11, 0.1), rgba(245, 158, 11, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #D97706; }
          .highlight-green { background: linear-gradient(90deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #15803D; }
          .highlight-purple { background: linear-gradient(90deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #6D28D9; }
        </style>
      </head>
      <body>
        ${reportContent}
      </body>
      </html>
    `], { type: 'text/html' });
    
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    agent.isExporting = false;
    // 提示用户
    alert('报告已导出为HTML文件，请在浏览器中打开并使用打印功能保存为PDF');
  }, 1200);
}

// 导出完整报告（包含所有模块）
const exportFullReport = () => {
  // 模拟导出延迟
  setTimeout(() => {
    let reportContent = '<h1>MOE 伴随式智能体协同大屏 - 完整职业规划报告</h1>';
    
    // 添加所有Agent模块内容
    agentList.value.forEach(agentItem => {
      reportContent += `<h2>${agentItem.name} - ${agentItem.role}</h2>`;
      reportContent += `<div>${agentItem.output}</div>`;
      reportContent += '<hr>';
    });
    
    // 添加Planner Agent内容
    if (workflowStep >= 5) {
      reportContent += '<h2>Planner Agent - 阶段一引航蓝图</h2>';
      reportContent += `<div><p>为了避免目标过大导致瘫痪，我们仅下发前 15 天的微观任务。</p>
        <h3>Day 1-4：底层心法重构与习惯熔断</h3>
        <p><strong>🎯 提升维度：</strong> <span class="dim-tag pink">抗压能力</span> <span class="dim-tag blue">专注力重建</span><br>
        <strong>🚀 核心执行动作：</strong> <br>
        1. <strong>物理级信息熔断：</strong> 强制卸载短视频及娱乐社交 App，每日开启 4 个"深潜番茄钟"（每个 50 分钟），严禁任何非编程相关的信息输入，强行重建因碎片化信息受损的延迟满足机制。<br>
        2. <strong>技术栈降维打击：</strong> 战略性关停前端 Vue 业务层开发，避免陷入"API 调用员"的低效内卷。全量切入 <strong>Java 后端底层赛道</strong>，通过手绘 JVM 内存布局图（堆、栈、方法区、计数器）实现对内存管理的深度内化。<br>
        3. <strong>内存管理深度攻坚：</strong> 深入解析垃圾回收（GC）机制，掌握 G1 与 ZGC 的核心算法逻辑，产出 1 篇超过 3000 字的《JVM 运行时数据区深度剖析》技术文档作为通关秘籍。</p>
        
        <h3>Day 5-8：高并发基石与框架生态解构</h3>
        <p><strong>🎯 提升维度：</strong> <span class="dim-tag purple">工程化思维</span> <span class="dim-tag blue">代码严谨性</span><br>
        <strong>🚀 核心执行动作：</strong> <br>
        1. <strong>JUC 并发编程突破：</strong> 彻底攻克"并发三座大山"。手写 AQS 核心流程，深入理解 CAS 与 volatile 的底层缓存一致性协议（MESI），实现对多线程通信的绝对掌控。<br>
        2. <strong>Spring 源码级接驳：</strong> 停止简单的注解调用，转而通过模拟实现"微型 IOC 容器"来理解 Bean 的生命周期。深入研读循环依赖解决方案，掌握三级缓存的设计哲学。<br>
        3. <strong>线程池动态调优：</strong> 基于工业级标准，实操模拟高并发下的线程池饱和策略，掌握 corePoolSize 与 maximumPoolSize 的动态配置算法，通过压力测试输出性能优化报告。</p></div>`;
    }
    
    // 创建一个临时HTML文件
    const blob = new Blob([`
      <!DOCTYPE html>
      <html>
      <head>
        <title>MOE 伴随式智能体协同大屏 - 完整职业规划报告</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 20px; }
          h1 { color: #333; text-align: center; }
          h2 { color: #555; margin-top: 30px; }
          h3 { color: #666; margin-top: 20px; }
          hr { margin: 20px 0; }
          .content { line-height: 1.6; }
          .highlight { background: linear-gradient(90deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #1D4ED8; }
          .highlight-orange { background: linear-gradient(90deg, rgba(245, 158, 11, 0.1), rgba(245, 158, 11, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #D97706; }
          .highlight-green { background: linear-gradient(90deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #15803D; }
          .highlight-purple { background: linear-gradient(90deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #6D28D9; }
          .highlight-blue { background: linear-gradient(90deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #1D4ED8; }
          .highlight-pink { background: linear-gradient(90deg, rgba(236, 72, 153, 0.1), rgba(236, 72, 153, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #DB2777; }
          .dim-tag { display: inline-block; padding: 3px 10px; border-radius: 8px; font-size: 0.9rem; font-weight: 900; margin: 0 3px; border: 1px solid transparent; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }
          .dim-tag.blue { background: #EFF6FF; color: #2563EB; border-color: #BFDBFE; }
          .dim-tag.pink { background: #FDF2F8; color: #DB2777; border-color: #FBCFE8; }
          .dim-tag.purple { background: #FAF5FF; color: #9333EA; border-color: #E9D5FF; }
          .dim-tag.green { background: #F0FDF4; color: #16A34A; border-color: #BBF7D0; }
          .dim-tag.orange { background: #FFF7ED; color: #D97706; border-color: #FED7AA; }
        </style>
      </head>
      <body>
        ${reportContent}
      </body>
      </html>
    `], { type: 'text/html' });
    
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'MOE 完整职业规划报告.html';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    // 提示用户
    alert('完整报告已导出为HTML文件，请在浏览器中打开并使用打印功能保存为PDF');
  }, 1500);
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))
const scrollToBottom = () => { nextTick(() => { if (pageContainer.value) pageContainer.value.scrollTo({ top: pageContainer.value.scrollHeight, behavior: 'smooth' }) }) }

const toggleAgent = (agent) => {
  if (agent.status === 'done' || agent.status === 'warning') {
    agent.expanded = !agent.expanded;
  }
}

// 第一阶段：初始诊断
const startPhaseOne = async () => {
  isSimulating.value = true
  workflowStep.value = 1
  
  agentList.value[0].status = 'thinking'
  agentList.value[1].status = 'thinking'
  await sleep(2000)
  agentList.value[0].status = 'done'; agentList.value[0].expanded = true;
  agentList.value[1].status = 'done'; agentList.value[1].expanded = true;

  workflowStep.value = 2
  agentList.value[2].status = 'thinking'
  agentList.value[3].status = 'thinking'
  await sleep(2200)
  agentList.value[2].status = 'done'; agentList.value[2].expanded = true;
  agentList.value[3].status = 'done'; agentList.value[3].expanded = true;

  workflowStep.value = 5
  await sleep(1000)
  workflowStep.value = 6 // 显示打卡舱
  scrollToBottom()
  isSimulating.value = false
}

// 模拟极其逼真的上传进度
const simulateUpload = async () => {
  if (uploadStatus.value !== 'none') return
  uploadStatus.value = 'uploading'
  
  const interval = setInterval(() => {
    if (uploadProgress.value < 90) uploadProgress.value += Math.random() * 15
  }, 200)

  await sleep(2000)
  clearInterval(interval)
  uploadProgress.value = 100
  await sleep(300)
  uploadStatus.value = 'done'
}
/* ... 前面的 import 和 ref 定义保持不变 ... */

// 新增：用来引用 DOM 中的 input 元素
const fileInputRef = ref(null)
// 新增：用来存储真实的文件名
const selectedFileName = ref('')

/* ... agentList, sleep, scrollToBottom 等保持不变 ... */

// 新增：点击上传区域时触发
const triggerFileSelect = () => {
  if (uploadStatus.value !== 'none') return
  // 直接点击隐藏的 input，弹出系统文件选择框
  fileInputRef.value.click()
}

// 新增：当用户选完文件后触发
const handleFileSelect = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 记录文件名，用于界面显示
  selectedFileName.value = file.name
  
  // 开始你的“逼真”动画流程
  uploadStatus.value = 'uploading'
  uploadProgress.value = 0

  // 模拟进度条跑动 (保留你原来的炫酷特效)
  const interval = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += Math.random() * 15
    }
  }, 200)

  // 模拟 2 秒后上传成功
  await sleep(2000)
  clearInterval(interval)
  uploadProgress.value = 100
  await sleep(300)
  uploadStatus.value = 'done'
}

/* ... 后面的 startPhaseTwo 保持不变 ... */

// 第二阶段：重构干预 (长文报警)
const startPhaseTwo = async () => {
  workflowStep.value = 7
  
  agentList.value[1].status = 'thinking'; agentList.value[1].expanded = true;
  agentList.value[3].status = 'thinking'; agentList.value[3].expanded = true;
  
  scrollToBottom()
  await sleep(2500)
  
  agentList.value[1].status = 'warning'
  agentList.value[1].output = '【🚨熔断干预】 捕捉到极度痛苦的负面反馈！当前皮质醇水平显著飙升。强行推进 JVM 源码解析已引发严重的「习得性无助」与自责循环。若不进行干预，预计 3 天内将面临系统性崩溃与彻底放弃。<br><br><strong>建议立即降维，注入极速正反馈重建自信心！</strong>'
  
  agentList.value[3].status = 'warning'
  agentList.value[3].output = '【🔄路径重构】 原定 JUC 与 JVM 底层源码硬啃计划强制中止。架构师成长路线不可拔苗助长。<br><br><strong>调整为：</strong>从 Gitee 拉取开源的 RuoYi 后台管理系统。不深究底层原理，重点掌握框架使用，跑通全链路并成功部署，建立全局视野与“我能写出项目”的成就感。'

  await sleep(1000)
  workflowStep.value = 8 // 弹出重构计划
  scrollToBottom()
}
</script>

<style scoped>
/* ================= 基础配置与光晕 ================= */
.multi-agent-page { position: relative; width: 100%; height: calc(100vh - 40px); background: #F0F4F8; overflow-y: auto; font-family: 'Inter', -apple-system, sans-serif; padding: 40px 0; scroll-behavior: smooth; }
.ambient-glow { position: fixed; width: 600px; height: 600px; border-radius: 50%; filter: blur(140px); opacity: 0.45; z-index: 0; pointer-events: none; }
.glow-blue { top: -50px; left: -100px; background: #3B82F6; }
.glow-purple { bottom: -100px; right: -100px; background: #8B5CF6; }
.glow-cyan { top: 40%; left: 40%; width: 400px; height: 400px; background: #06B6D4; opacity: 0.2; }

.workspace-wrapper { position: relative; z-index: 1; width: 100%; max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; gap: 30px; padding: 0 20px; }
.glass-panel { background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(25px); border: 1px solid rgba(255, 255, 255, 1); box-shadow: 0 20px 40px rgba(0,0,0,0.06), inset 0 2px 4px rgba(255,255,255,0.8); border-radius: 24px; padding: 30px; }

/* ================= 头部与输入 ================= */
.page-header { margin-bottom: 5px; position: relative; }
.header-content { display: flex; justify-content: center; align-items: center; position: relative; }
.title-section { text-align: center; }
.title-section .title-badge { margin-bottom: 10px; }
.title-section .gradient-text { margin-bottom: 5px; }

/* 全局导出按钮样式 */
.global-export-btn { position: absolute; top: 0; right: 0; background: linear-gradient(135deg, #3B82F6, #2563EB); color: white; border: none; padding: 14px 24px; border-radius: 10px; font-size: 1rem; font-weight: 800; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 8px 20px rgba(59, 130, 246, 0.25); display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.global-export-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(59, 130, 246, 0.35); }
.global-export-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; box-shadow: none; }
.global-export-btn .btn-icon { font-size: 1.1rem; transition: transform 0.3s ease; }
.global-export-btn:hover:not(:disabled) .btn-icon { transform: scale(1.1); }

/* 响应式调整 */
@media (max-width: 768px) {
  .header-content { flex-direction: column; align-items: flex-start; }
  .global-export-btn { align-self: flex-start; }
}
.title-badge { padding: 8px 20px; background: #FFFFFF; color: #3B82F6; border-radius: 30px; font-weight: 800; font-size: 0.95rem; box-shadow: 0 4px 15px rgba(59, 130, 246, 0.15); letter-spacing: 1px; }
.gradient-text { font-size: 2.5rem; margin: 0; font-weight: 900; background: linear-gradient(135deg, #0F172A, #2563EB); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.subtitle { color: #475569; font-weight: 700; margin: 0; font-size: 1.1rem; }

.query-section { display: flex; flex-direction: column; gap: 20px; border-left: 6px solid #2563EB; padding: 35px;}
.user-info { display: flex; gap: 20px; align-items: flex-start;}
.avatar { position: relative; width: 50px; height: 50px; border-radius: 50%; background: linear-gradient(135deg, #10B981, #059669); color: white; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 1.2rem; flex-shrink: 0; z-index: 1;}
.avatar-ring { position: absolute; inset: -4px; border-radius: 50%; border: 2px dashed #10B981; animation: spin 4s linear infinite; }
.query-content { background: #F8FAFC; padding: 20px 25px; border-radius: 0 20px 20px 20px; border: 1px solid #E2E8F0; box-shadow: inset 0 2px 4px rgba(0,0,0,0.02); }
.query-text { margin: 0; color: #1E293B; line-height: 1.8; font-size: 1.1rem; font-weight: 600; font-style: italic; }

.trigger-btn, .submit-feedback-btn { align-self: flex-end; background: linear-gradient(135deg, #0F172A, #334155); color: white; border: none; padding: 16px 36px; border-radius: 16px; font-size: 1.1rem; font-weight: 900; cursor: pointer; transition: 0.3s; box-shadow: 0 10px 25px rgba(15, 23, 42, 0.3); overflow: hidden; display: flex; align-items: center; gap: 10px; }
.trigger-btn:hover:not(:disabled), .submit-feedback-btn:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 15px 35px rgba(15, 23, 42, 0.4); }
.trigger-btn:disabled, .submit-feedback-btn:disabled { opacity: 0.8; cursor: not-allowed; }
.btn-glow { position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(255,255,255,0.2) 0%, transparent 60%); animation: spin 3s linear infinite; pointer-events: none;}
.spinner { display: inline-block; width: 16px; height: 16px; border: 3px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 1s linear infinite; margin-right: 5px; }

/* ================= Agent 阵列 (带折叠) ================= */
.agents-network { display: grid; grid-template-columns: repeat(2, 1fr); gap: 30px; }
.agent-card { padding: 0; overflow: hidden; transition: 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); border: 2px solid transparent; display: flex; flex-direction: column; }
.agent-card.is-thinking { border-color: #3B82F6; box-shadow: 0 10px 30px rgba(59, 130, 246, 0.15); transform: translateY(-4px); }
.agent-card.is-done { border-color: #E2E8F0; }
.agent-card.is-done:hover { border-color: #CBD5E1; box-shadow: 0 15px 40px rgba(0,0,0,0.08); transform: translateY(-6px); }
.agent-card.is-warning { border-color: #EF4444; background: #FEF2F2; box-shadow: 0 10px 30px rgba(239, 68, 68, 0.15); animation: shake 0.5s ease-in-out; }

/* 按钮样式 */
.agent-controls { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.agent-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.action-btn { border: none; padding: 8px 16px; border-radius: 10px; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: all 0.3s ease; display: flex; align-items: center; gap: 6px; position: relative; overflow: hidden; }
.btn-icon { font-size: 1rem; transition: transform 0.3s ease; }
.btn-text { font-size: 0.85rem; transition: all 0.3s ease; }
.edit-btn { background: #EFF6FF; color: #2563EB; border: 1px solid #DBEAFE; }
.export-btn { background: #F0FDF4; color: #16A34A; border: 1px solid #DCFCE7; }
.action-btn:hover { transform: translateY(-2px) scale(1.03); box-shadow: 0 6px 16px rgba(0,0,0,0.12); }
.action-btn:hover .btn-icon { transform: scale(1.1); }
.edit-btn:hover { background: #DBEAFE; color: #1D4ED8; border-color: #BFDBFE; }
.export-btn:hover { background: #DCFCE7; color: #15803D; border-color: #BBF7D0; }
.action-btn:active { transform: translateY(0) scale(0.98); box-shadow: 0 2px 8px rgba(0,0,0,0.1); }

/* 为不同模块的按钮添加与图标颜色呼应的样式 */
.agent-card[data-agent-id="coach"] .edit-btn { background: #EFF6FF; color: #2563EB; border: 1px solid #DBEAFE; }
.agent-card[data-agent-id="coach"] .edit-btn:hover { background: #DBEAFE; color: #1D4ED8; border-color: #3B82F6; }

.agent-card[data-agent-id="emotion"] .edit-btn { background: #FDF2F8; color: #DB2777; border: 1px solid #FBCFE8; }
.agent-card[data-agent-id="emotion"] .edit-btn:hover { background: #FCE7F3; color: #BE185D; border-color: #EC4899; }

.agent-card[data-agent-id="hr"] .edit-btn { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
.agent-card[data-agent-id="hr"] .edit-btn:hover { background: #DCFCE7; color: #15803D; border-color: #10B981; }

.agent-card[data-agent-id="skill"] .edit-btn { background: #FFF7ED; color: #D97706; border: 1px solid #FED7AA; }
.agent-card[data-agent-id="skill"] .edit-btn:hover { background: #FFEDD5; color: #B45309; border-color: #F59E0B; }
.expand-toggle { background: #F8FAFC; border: 1px solid #E2E8F0; color: #475569; font-weight: 700; font-size: 0.85rem; padding: 8px 16px; border-radius: 10px; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.3s ease; }
.expand-toggle:hover { background: #F1F5F9; border-color: #CBD5E1; transform: translateY(-1px) scale(1.02); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.expand-toggle:active { transform: translateY(0) scale(0.98); }

/* 编辑模式样式 */
.editable-content { padding: 15px 0; transition: all 0.3s ease; animation: fadeIn 0.3s ease; }
.edit-textarea { width: 100%; min-height: 200px; max-height: 400px; padding: 15px; border: 2px solid #E2E8F0; border-radius: 12px; font-size: 1rem; line-height: 1.6; font-family: inherit; resize: vertical; transition: all 0.3s ease; background: #F8FAFC; box-shadow: inset 0 2px 4px rgba(0,0,0,0.05); overflow-y: auto; box-sizing: border-box; }

/* 为所有模块的编辑框添加统一的样式 */
.agent-card .edit-textarea {
  width: 100%;
  box-sizing: border-box;
  margin: 0;
  border: 2px solid #E2E8F0;
  border-radius: 12px;
  background: #F8FAFC;
  padding: 15px;
  font-size: 1rem;
  line-height: 1.6;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s ease;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.05);
  overflow-y: auto;
}
.edit-textarea:focus { outline: none; border-color: #3B82F6; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1), inset 0 2px 4px rgba(0,0,0,0.05); }

/* 编辑状态高亮 */
.agent-card .action-btn.edit-btn.active { background: #DBEAFE; color: #1D4ED8; border-color: #3B82F6; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2); }

/* 动画效果 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 按钮加载状态 */
.action-btn.loading {
  position: relative;
  pointer-events: none;
  opacity: 0.7;
}

.action-btn.loading::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 10px;
  width: 16px;
  height: 16px;
  margin-top: -8px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.agent-header { padding: 25px; display: flex; align-items: center; gap: 18px; cursor: pointer; background: #FFFFFF; transition: 0.3s; }
.agent-icon { width: 50px; height: 50px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 1.6rem; color: white; box-shadow: 0 6px 15px rgba(0,0,0,0.15); }
.agent-title { flex: 1; }
.agent-title h4 { margin: 0 0 6px 0; font-size: 1.15rem; color: #0F172A; font-weight: 900; }
.agent-role { font-size: 0.85rem; color: #475569; font-weight: 700; background: #F1F5F9; padding: 4px 10px; border-radius: 6px; }

.status-box { display: flex; align-items: center; }
.status-box .dot { display: inline-block; width: 12px; height: 12px; border-radius: 50%; }
.dot.waiting { background: #CBD5E1; }
.dot.thinking { background: #3B82F6; box-shadow: 0 0 10px #3B82F6; }
.dot.done { background: #10B981; }
.dot.warning { background: #EF4444; box-shadow: 0 0 10px #EF4444; }
.expand-toggle { background: #EFF6FF; border: none; color: #2563EB; font-weight: 800; font-size: 0.85rem; padding: 6px 14px; border-radius: 20px; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: 0.3s;}
.agent-card.is-warning .expand-toggle { background: #FEE2E2; color: #B91C1C; }
.arrow { transition: transform 0.3s; font-size: 0.7rem;}
.arrow.up { transform: rotate(180deg); }

/* 折叠体 */
.agent-body-wrapper { display: grid; grid-template-rows: 0fr; transition: grid-template-rows 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); background: #FAFAF9; border-top: 1px solid transparent;}
.agent-body-wrapper.open { grid-template-rows: 1fr; border-top-color: #E2E8F0; min-height: 200px; }
.agent-card.is-warning .agent-body-wrapper { background: #FFFBFB; border-top-color: #FECACA;}
.agent-body-inner { overflow: auto; padding: 0 25px; max-height: 500px; }

/* 确保Report Editor & Export模块的编辑框不溢出 */
.agent-card[data-agent-id="skill"] .agent-body-inner {
  padding: 0 25px 25px;
}

/* 为编辑框添加带样式的滚动条 */
.edit-textarea::-webkit-scrollbar {
  width: 8px;
}

.edit-textarea::-webkit-scrollbar-track {
  background: #F1F5F9;
  border-radius: 4px;
}

.edit-textarea::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 4px;
}

.edit-textarea::-webkit-scrollbar-thumb:hover {
  background: #94A3B8;
}

/* 为模块内部添加滚动条样式 */
.agent-body-inner::-webkit-scrollbar {
  width: 8px;
}

.agent-body-inner::-webkit-scrollbar-track {
  background: #F1F5F9;
  border-radius: 4px;
}

.agent-body-inner::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 4px;
}

.agent-body-inner::-webkit-scrollbar-thumb:hover {
  background: #94A3B8;
}

.placeholder-text { color: #94A3B8; font-style: italic; font-size: 0.95rem; margin: 25px 0; text-align: center; font-weight: 700;}
.skeleton-loader { display: flex; flex-direction: column; gap: 14px; margin: 25px 0; }
.skeleton-loader .line { height: 12px; background: linear-gradient(90deg, #E2E8F0 25%, #F8FAFC 50%, #E2E8F0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 6px; }
.w-80 { width: 80%; } .w-100 { width: 100%; } .w-60 { width: 60%; }

.typed-text { margin: 25px 0; color: #334155; font-size: 1.05rem; line-height: 1.9; font-weight: 500; }

/* 优化Report Editor & Export模块的文字排版 */
.agent-card[data-agent-id="skill"] .typed-text {
  line-height: 2.0;
  color: #475569;
}

.agent-card[data-agent-id="skill"] .typed-text span.highlight-orange {
  font-weight: 700;
  color: #D97706;
}

.agent-card[data-agent-id="skill"] .typed-text span.highlight-green {
  font-weight: 700;
  color: #15803D;
}
:deep(.dim-tag) { display: inline-block; padding: 3px 10px; border-radius: 8px; font-size: 0.9rem; font-weight: 900; margin: 0 3px; border: 1px solid transparent; box-shadow: 0 2px 5px rgba(0,0,0,0.05);}
:deep(.dim-tag.blue) { background: #EFF6FF; color: #2563EB; border-color: #BFDBFE; }
:deep(.dim-tag.pink) { background: #FDF2F8; color: #DB2777; border-color: #FBCFE8; }
:deep(.dim-tag.purple) { background: #FAF5FF; color: #9333EA; border-color: #E9D5FF; }
:deep(.dim-tag.green) { background: #F0FDF4; color: #16A34A; border-color: #BBF7D0; }
:deep(.dim-tag.orange) { background: #FFF7ED; color: #D97706; border-color: #FED7AA; }
:deep(strong) { color: #0F172A; font-weight: 900;}

/* 重点信息高亮 */
:deep(.highlight) { background: linear-gradient(90deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #1D4ED8; }
:deep(.highlight-orange) { background: linear-gradient(90deg, rgba(245, 158, 11, 0.1), rgba(245, 158, 11, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #D97706; }
:deep(.highlight-green) { background: linear-gradient(90deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #15803D; }
:deep(.highlight-purple) { background: linear-gradient(90deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #6D28D9; }
:deep(.highlight-blue) { background: linear-gradient(90deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #1D4ED8; }
:deep(.highlight-pink) { background: linear-gradient(90deg, rgba(236, 72, 153, 0.1), rgba(236, 72, 153, 0.2)); padding: 2px 6px; border-radius: 4px; font-weight: 700; color: #DB2777; }

/* 模块标题颜色区分 */
.agent-title h4 {
  margin: 0 0 6px 0;
  font-size: 1.15rem;
  font-weight: 900;
  transition: 0.3s;
}

/* 根据模块ID设置标题颜色 */
.agent-card[data-agent-id="coach"] .agent-title h4 {
  color: #2563EB;
}

.agent-card[data-agent-id="emotion"] .agent-title h4 {
  color: #DB2777;
}

.agent-card[data-agent-id="hr"] .agent-title h4 {
  color: #16A34A;
}

.agent-card[data-agent-id="skill"] .agent-title h4 {
  color: #D97706;
}

/* ================= 动态复盘反馈舱 ================= */
.feedback-section { border: 2px dashed #93C5FD; background: linear-gradient(180deg, #F8FAFC, #EFF6FF); display: flex; flex-direction: column; gap: 20px; }
.section-title-box { display: flex; flex-direction: column; align-items: center; text-align: center;}
.section-title-box .icon { font-size: 2.8rem; margin-bottom: 10px; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1)); }
.section-title-box h3 { margin: 0 0 5px 0; color: #1D4ED8; font-weight: 900; font-size: 1.5rem; }
.section-title-box p { margin: 0; color: #475569; font-size: 1.05rem; font-weight: 500;}

.feedback-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; }

.upload-box { background: #FFFFFF; border: 2px dashed #CBD5E1; border-radius: 16px; height: 200px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.3s; position: relative; overflow: hidden; }
.upload-box:hover:not(.is-uploaded) { border-color: #3B82F6; background: #F1F6FF; }
.upload-box.is-uploaded { border: 2px solid #10B981; background: #F0FDF4; cursor: default; }

.upload-prompt { text-align: center; color: #64748B; font-weight: 600;}
.upload-icon { font-size: 3rem; margin-bottom: 10px; }
.upload-prompt span { font-size: 0.85rem; color: #94A3B8; font-weight: normal;}

.uploading-state { text-align: center; width: 80%; }
.spinner-large { width: 35px; height: 35px; border: 4px solid #E2E8F0; border-top-color: #3B82F6; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
.progress-bar { height: 10px; background: #E2E8F0; border-radius: 5px; overflow: hidden; margin-top: 12px; }
.progress-fill { height: 100%; background: #3B82F6; transition: width 0.2s; }

.uploaded-success { display: flex; flex-direction: column; align-items: center; gap: 15px; }
.file-thumb { width: 70px; height: 70px; background: #1E293B; border-radius: 12px; padding: 12px; box-shadow: 0 6px 15px rgba(0,0,0,0.1); }
.code-mock { display: flex; flex-direction: column; gap: 8px; }
.c-line { height: 5px; background: #3B82F6; border-radius: 3px; }
.c-line.short { width: 60%; background: #10B981; }
.c-line.half { width: 40%; background: #A78BFA; }
.file-info { display: flex; flex-direction: column; align-items: center; gap: 5px; }
.file-name { font-size: 0.9rem; font-weight: 700; color: #334155; }
.file-badge { font-size: 0.8rem; background: #10B981; color: white; padding: 4px 10px; border-radius: 10px; font-weight: bold; }

.emotion-selector { display: flex; flex-direction: column; justify-content: center; gap: 15px; }
.emotion-selector.disabled { opacity: 0.4; pointer-events: none; }
.selector-title { margin: 0; font-weight: 900; color: #1E293B; font-size: 1.1rem; }
.emotion-options { display: flex; flex-direction: column; gap: 12px; }
.emo-btn { background: #FFFFFF; border: 2px solid #E2E8F0; padding: 14px 18px; border-radius: 14px; cursor: pointer; font-weight: 800; color: #475569; transition: 0.2s; font-size: 1.05rem;}
.emo-btn:hover { background: #F8FAFC; border-color: #CBD5E1; transform: translateX(5px); }
.emo-btn.active { border-color: #EF4444; background: #FEF2F2; color: #B91C1C; box-shadow: 0 6px 20px rgba(239, 68, 68, 0.15); }

/* ================= 看板与警告重构面板 ================= */
.synthesis-board { border: 2px solid #C4B5FD; background: #FFFFFF; padding: 0; margin-top: 15px; margin-bottom: 60px; overflow: hidden; box-shadow: 0 30px 60px rgba(139, 92, 246, 0.15);}
.board-header { background: linear-gradient(135deg, #FAF5FF, #F3E8FF); padding: 30px 35px; display: flex; align-items: center; gap: 20px; border-bottom: 1px solid #E9D5FF; }
.master-icon-box { position: relative; font-size: 2.5rem; z-index: 1;}
.ring-1 { position: absolute; inset: 0; border: 2px solid #C4B5FD; border-radius: 50%; animation: pulseRing 2s infinite;}
@keyframes pulseRing { 0% { transform: scale(0.8); opacity: 1; } 100% { transform: scale(1.5); opacity: 0; } }

.board-title h3 { margin: 0 0 5px 0; color: #5B21B6; font-size: 1.5rem; font-weight: 900; }
.board-title p { margin: 0; color: #7C3AED; font-weight: 700; font-size: 1.1rem;}

.board-content { padding: 35px; background: #FAFAF9;}
.time-node { background: #FFFFFF; padding: 25px 30px; border-radius: 16px; border: 1px solid #E2E8F0; box-shadow: 0 6px 20px rgba(0,0,0,0.04); }
.time-badge { display: inline-block; background: #F3E8FF; color: #6D28D9; font-size: 1.1rem; font-weight: 900; padding: 8px 16px; border-radius: 10px; margin-bottom: 15px; border: 1px solid #E9D5FF; letter-spacing: 0.5px;}
.time-node p { margin: 0; line-height: 1.9; font-size: 1.05rem; color: #334155; }

/* 紧急干预样式 */
.dynamic-adjusted { border-color: #F87171; box-shadow: 0 20px 50px rgba(239, 68, 68, 0.15); }
.warning-header { background: linear-gradient(135deg, #FEF2F2, #FEE2E2); border-bottom-color: #FECACA; }
.warning-header h3 { color: #B91C1C; }
.warning-header p { color: #DC2626; font-weight: 800; margin: 0;}
.warning-ring { border-color: #FCA5A5; }
.adjusted-node { background: #FFFBFB; border-color: #FECACA; }
.adjusted-badge { background: #FEE2E2; color: #B91C1C; border-color: #FCA5A5; }

.fade-up-enter-active { transition: all 0.6s cubic-bezier(0.2, 0.8, 0.2, 1); }
.fade-up-enter-from { opacity: 0; transform: translateY(40px); }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
@keyframes shake { 0%, 100% { transform: translateX(0); } 25% { transform: translateX(-5px); } 75% { transform: translateX(5px); } }
</style>