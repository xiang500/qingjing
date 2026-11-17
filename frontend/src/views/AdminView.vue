<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  createKnowledge,
  deleteKnowledge,
  fetchAdminOverview,
  fetchAdminUsers,
  fetchKnowledge,
  updateKnowledge,
  type AdminOverview,
  type AdminUser,
  type KnowledgeChunk,
} from '@/api/admin'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const theme = useThemeStore()
const auth = useAuthStore()

const overview = ref<AdminOverview | null>(null)
const users = ref<AdminUser[]>([])
const knowledge = ref<KnowledgeChunk[]>([])
const loading = ref(true)
const error = ref('')

const dialogOpen = ref(false)
const editing = ref<KnowledgeChunk | null>(null)
const saving = ref(false)
const form = ref({ title: '', category: '通用', tags: '', content: '', enabled: true })

const CATEGORIES = ['通用', '场合', '配色', '版型', '材质', '季节', '身材']

const overviewCards = [
  { key: 'users', label: '注册用户', en: 'Users' },
  { key: 'clothing', label: '衣橱单品', en: 'Pieces' },
  { key: 'outfits', label: '穿搭套装', en: 'Outfits' },
  { key: 'posts', label: '社区帖子', en: 'Posts' },
  { key: 'chatSessions', label: 'AI 会话', en: 'Sessions' },
  { key: 'recommendations', label: 'AI 推荐', en: 'Recs' },
  { key: 'knowledgeChunks', label: '知识片段', en: 'Chunks' },
] as const

async function loadAll() {
  // 未登录不拉管理数据（后端会用缺省演示账号兜底）
  if (!auth.isLoggedIn) {
    overview.value = null
    users.value = []
    knowledge.value = []
    error.value = ''
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [adminOverview, adminUsers, chunks] = await Promise.all([
      fetchAdminOverview(),
      fetchAdminUsers(),
      fetchKnowledge(),
    ])
    overview.value = adminOverview
    users.value = adminUsers
    knowledge.value = chunks
  } catch (err) {
    error.value = err instanceof Error ? err.message : '管理数据加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { title: '', category: '通用', tags: '', content: '', enabled: true }
  dialogOpen.value = true
}

function openEdit(row: unknown) {
  const chunk = row as KnowledgeChunk
  editing.value = chunk
  form.value = {
    title: chunk.title,
    category: chunk.category ?? '通用',
    tags: chunk.tags.join('，'),
    content: chunk.content,
    enabled: chunk.enabled,
  }
  dialogOpen.value = true
}

async function save() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.value.title.trim(),
      category: form.value.category,
      tags: form.value.tags
        .split(/[,，]/)
        .map((tag) => tag.trim())
        .filter(Boolean),
      content: form.value.content.trim(),
      enabled: form.value.enabled,
    }
    if (editing.value) {
      await updateKnowledge(editing.value.id, payload)
    } else {
      await createKnowledge(payload)
    }
    ElMessage.success(editing.value ? '知识片段已更新，AI 检索已刷新' : '知识片段已新增，AI 检索已刷新')
    dialogOpen.value = false
    await loadAll()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function remove(row: unknown) {
  const chunk = row as KnowledgeChunk
  try {
    await ElMessageBox.confirm(
      `确定删除「${chunk.title}」吗？删除后 AI 将不再使用这条知识。`,
      '删除知识片段',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
    await deleteKnowledge(chunk.id)
    ElMessage.success('已删除')
    await loadAll()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err instanceof Error ? err.message : '删除失败')
    }
  }
}

function genderText(gender?: number | null) {
  if (gender === 1) {
    return '男'
  }
  if (gender === 2) {
    return '女'
  }
  return '未设置'
}

watch(
  () => auth.isLoggedIn,
  async () => {
    await loadAll()
  },
)

onMounted(loadAll)
</script>

<template>
  <div class="page-stack">
    <!-- ============ 页头 ============ -->
    <header class="flex flex-wrap items-end justify-between gap-[var(--space-4)] sm:gap-[var(--space-6)]">
      <div class="max-w-3xl flex-1">
        <p class="eyebrow">{{ theme.meta.en }} · {{ theme.meta.light }} · Admin Console</p>
        <h1 class="display mt-[var(--space-4)] text-balance text-[clamp(2rem,3.4vw,2.75rem)] leading-[1.2] text-mist-100">
          管理后台，
          <span class="accent-text">知识库与运营数据一处看清</span>
        </h1>
        <p class="mt-[var(--space-3)] max-w-xl text-sm leading-[1.95] text-mist-400">
          维护 AI 穿搭知识库（RAG 数据源）、查看注册用户与全局数据量。换季只换光线与玻璃材质，数据与检索逻辑保持不变。
        </p>
      </div>

      <div class="action-row justify-end">
        <RouterLink to="/stats" class="btn-ghost">
          数据看板
          <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </RouterLink>
      </div>
    </header>

    <!-- ============ 错误提示 ============ -->
    <div
      v-if="error"
      class="glass-soft flex flex-wrap items-center gap-3 border border-rose-400/30 px-4 py-3 text-sm text-mist-100"
    >
      <svg class="size-4 shrink-0 text-gold-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 8v5M12 16h.01" stroke-linecap="round" />
      </svg>
      <span class="flex-1">
        {{ error }}
        <span class="text-mist-500">（请确认 Spring Boot 已在 8080 端口启动）</span>
      </span>
      <button type="button" class="btn-ghost !px-3 !py-1.5 !text-[11px]" @click="loadAll">重试</button>
    </div>

    <!-- ============ 平台概览：7 张小卡，网格排开不挤成一条 ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div>
          <h2>平台概览</h2>
          <p>7 项全局数据量，来自注册用户、内容与 AI 使用记录。</p>
        </div>
        <div class="action-row justify-end">
          <span class="chip cursor-default">7 项指标</span>
          <button type="button" class="btn-ghost !py-2 !text-xs" :disabled="loading" @click="loadAll">
            <svg
              class="size-3.5"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
            >
              <path d="M20 12a8 8 0 1 1-2.3-5.6" stroke-linecap="round" />
              <path d="M20 4v4h-4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            {{ loading ? '刷新中…' : '刷新数据' }}
          </button>
        </div>
      </header>

      <div v-if="overview" class="grid grid-cols-2 gap-[var(--space-3)] sm:grid-cols-4 sm:gap-[var(--space-4)]">
        <article
          v-for="card in overviewCards"
          :key="card.key"
          class="glass-soft px-[var(--space-4)] py-[var(--space-3)]"
        >
          <div class="flex items-baseline justify-between gap-2">
            <p class="truncate text-[10px] uppercase tracking-[0.22em] text-mist-500">{{ card.label }}</p>
            <span class="hidden shrink-0 text-[9px] uppercase tracking-[0.16em] text-mist-500/70 2xl:inline">
              {{ card.en }}
            </span>
          </div>
          <p class="display mt-[var(--space-2)] text-[1.5rem] leading-none text-mist-100 tabular-nums">
            {{ overview[card.key] }}
          </p>
        </article>
      </div>

      <!-- 首次加载：用骨架板占住同样的网格，避免区块高度跳一下 -->
      <div v-else-if="loading" class="grid grid-cols-2 gap-[var(--space-3)] sm:grid-cols-4 sm:gap-[var(--space-4)]">
        <div v-for="index in 7" :key="index" class="glass-soft h-[5.5rem] px-[var(--space-4)] py-[var(--space-3)]">
          <div class="skeleton h-2.5 w-1/2 rounded-full" />
          <div class="skeleton mt-[var(--space-3)] h-6 w-2/3 rounded-md" />
        </div>
      </div>

      <!-- 未登录：不展示平台数据 -->
      <div v-else class="glass flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
        <p class="text-sm font-medium text-mist-200">登录后查看平台数据</p>
        <p class="max-w-sm text-[11px] leading-relaxed text-mist-500">
          知识库、注册用户与全局数据量只对已登录账号开放，登录后这里会自动加载。
        </p>
      </div>
    </section>

    <!-- ============ 知识库：表格 + 新增主操作 ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div>
          <h2>知识库</h2>
          <p>
            共 {{ knowledge.length }} 条 · 停用的片段不会进入 AI 检索，新增后 AI 立即可用。
          </p>
        </div>
        <button type="button" class="btn-primary !py-2 !text-xs" @click="openCreate">
          <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14" stroke-linecap="round" />
          </svg>
          新增知识片段
        </button>
      </header>

      <div class="glass overflow-hidden">
        <div class="relative z-10 px-[var(--space-2)] pb-[var(--space-3)] pt-[var(--space-4)]">
        <el-table :data="knowledge" v-loading="loading" style="width: 100%" max-height="440">
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="90" />
          <el-table-column label="标签" min-width="150">
            <template #default="{ row }">
              <span
                v-for="tag in row.tags"
                :key="tag"
                class="chip mr-1 mt-1 cursor-default !px-2 !py-0.5 !text-[10px]"
              >
                {{ tag }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="86">
            <template #default="{ row }">
              <span
                class="chip cursor-default !gap-1 !px-2 !py-0.5 !text-[10px]"
                :class="row.enabled ? '!border-gold-300/40 !text-gold-300' : '!text-mist-500'"
              >
                <span class="size-1 rounded-full" :class="row.enabled ? 'bg-gold-300' : 'bg-mist-500'" />
                {{ row.enabled ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="132"
            fixed="right"
            class-name="op-cell"
            label-class-name="op-cell"
          >
            <template #default="{ row }">
              <button type="button" class="chip mr-1 !px-2.5 !py-1 !text-[10px]" @click="openEdit(row)">
                编辑
              </button>
              <button
                type="button"
                class="chip !px-2.5 !py-1 !text-[10px] hover:!border-rose-400/50 hover:!text-rose-400"
                @click="remove(row)"
              >
                删除
              </button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="py-10 text-center">
              <p class="display text-base text-mist-200">知识库还是空的</p>
              <p class="mt-1 text-[11px] text-mist-500">新增一条搭配知识，AI 的建议就有据可依。</p>
            </div>
          </template>
        </el-table>
        </div>
      </div>
    </section>

    <!-- ============ 注册用户：表格只读，计数即操作区 ============ -->
    <section v-reveal class="section-block">
      <header class="section-head">
        <div>
          <h2>注册用户</h2>
          <p>最近 {{ users.length }} 位 · 身高体重与风格偏好会参与 AI 推荐。</p>
        </div>
        <div class="action-row justify-end">
          <span class="chip cursor-default">{{ users.length }} 位用户</span>
        </div>
      </header>

      <div class="glass overflow-hidden">
        <div class="relative z-10 px-[var(--space-2)] pb-[var(--space-3)] pt-[var(--space-4)]">
        <el-table :data="users" v-loading="loading" style="width: 100%" max-height="360">
          <el-table-column prop="username" label="账号" width="140" />
          <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip />
          <el-table-column label="性别" width="80">
            <template #default="{ row }">{{ genderText(row.gender) }}</template>
          </el-table-column>
          <el-table-column label="身高 / 体重" width="130">
            <template #default="{ row }">{{ row.heightCm ?? '—' }} / {{ row.weightKg ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="风格偏好" min-width="200">
            <template #default="{ row }">
              <span
                v-for="tag in row.stylePreference"
                :key="tag"
                class="chip mr-1 mt-1 cursor-default !px-2 !py-0.5 !text-[10px]"
              >
                {{ tag }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="bodyType" label="体型" width="80" />

          <template #empty>
            <div class="py-10 text-center">
              <p class="display text-base text-mist-200">还没有注册用户</p>
              <p class="mt-1 text-[11px] text-mist-500">用户注册后会自动出现在这里。</p>
            </div>
          </template>
        </el-table>
        </div>
      </div>
    </section>

    <!-- ============ 知识片段 CRUD 弹窗（已全局玻璃化） ============ -->
    <el-dialog
      v-model="dialogOpen"
      :title="editing ? '编辑知识片段' : '新增知识片段'"
      width="min(640px, 92vw)"
      append-to-body
    >
      <el-form label-position="top" class="pt-1">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="128" placeholder="例如：同色系搭配更容易出效果" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="item in CATEGORIES" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签（逗号分隔，用于命中检索）">
          <el-input v-model="form.tags" placeholder="通勤，配色，显高" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <button type="button" class="btn-ghost !py-2 !text-xs" @click="dialogOpen = false">取消</button>
          <button type="button" class="btn-primary !py-2 !text-xs" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存片段' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 全局表格化玻璃把单元格底色设成透明；固定操作列需要一层近似面板的底色，
   否则窄屏横向滚动时被滚过的内容会从按钮背后透出来 */
:deep(.el-table .op-cell) {
  background: var(--c-bg-2);
  box-shadow: inset 1px 0 0 var(--c-line-soft);
}

:deep(.el-table th.op-cell) {
  background: var(--c-bg-3);
}

/* 弹窗里的开关跟随季节强调色，而不是 Element 默认蓝 */
:deep(.el-switch.is-checked .el-switch__core) {
  background-color: var(--c-accent);
  border-color: var(--c-accent);
}

:deep(.el-switch .el-switch__core) {
  border-color: color-mix(in oklab, var(--c-line) 100%, transparent);
}
</style>
