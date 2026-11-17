<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const auth = useAuthStore()
const theme = useThemeStore()

const nickname = ref('')
const avatarUrl = ref('')
const gender = ref(0)
const heightCm = ref<number | undefined>(undefined)
const weightKg = ref<number | undefined>(undefined)
const bodyType = ref('')
const styles = ref<string[]>([])
const saving = ref(false)

const STYLE_OPTIONS = ['通勤', '法式', '简约', '休闲', '温柔', '复古', '运动', '度假', '精致', '气场', '居家', '英伦']
const BODY_TYPES = [
  { value: 'A', label: 'A 型（梨形）' },
  { value: 'H', label: 'H 型（直筒）' },
  { value: 'O', label: 'O 型（苹果形）' },
  { value: 'Y', label: 'Y 型（倒三角）' },
  { value: 'X', label: 'X 型（沙漏）' },
]

const completeness = computed(() => {
  const fields = [nickname.value.trim(), avatarUrl.value.trim(), heightCm.value, weightKg.value, bodyType.value]
  const filled = fields.filter((value) => value !== '' && value !== undefined && value !== null).length
  const total = fields.length + 1
  return Math.round(((filled + (styles.value.length ? 1 : 0)) / total) * 100)
})

/** 头像环形描边用的首字 */
const initial = computed(() => (nickname.value.trim() || auth.displayName).slice(0, 1))

watch(
  () => auth.user,
  (user) => {
    if (!user) {
      return
    }
    nickname.value = user.nickname ?? ''
    avatarUrl.value = user.avatarUrl ?? ''
    gender.value = user.gender ?? 0
    heightCm.value = user.heightCm ?? undefined
    weightKg.value = user.weightKg ?? undefined
    bodyType.value = user.bodyType ?? ''
    styles.value = [...(user.stylePreference ?? [])]
  },
  { immediate: true },
)

function toggleStyle(tag: string) {
  if (styles.value.includes(tag)) {
    styles.value = styles.value.filter((item) => item !== tag)
  } else if (styles.value.length < 6) {
    styles.value = [...styles.value, tag]
  } else {
    ElMessage.warning('最多选择 6 个风格偏好')
  }
}

async function save() {
  if (!auth.isLoggedIn) {
    return
  }
  if (heightCm.value != null && (heightCm.value < 100 || heightCm.value > 250)) {
    ElMessage.warning('身高需在 100-250cm 之间')
    return
  }
  if (weightKg.value != null && (weightKg.value < 30 || weightKg.value > 200)) {
    ElMessage.warning('体重需在 30-200kg 之间')
    return
  }
  saving.value = true
  try {
    await auth.saveProfile({
      nickname: nickname.value.trim() || undefined,
      avatarUrl: avatarUrl.value.trim() || undefined,
      gender: gender.value,
      heightCm: heightCm.value,
      weightKg: weightKg.value,
      bodyType: bodyType.value || undefined,
      stylePreference: styles.value,
    })
    ElMessage.success('资料已保存，AI 会参考这些信息给出更贴合的搭配')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-stack">
    <!-- ============ 页头 ============ -->
    <header class="max-w-3xl">
      <p class="eyebrow">{{ theme.meta.en }} · {{ theme.meta.light }} · Profile</p>
      <h1 class="display mt-[var(--space-4)] text-balance text-[clamp(2rem,3.4vw,2.75rem)] leading-[1.2] text-mist-100">
        个人中心，
        <span class="accent-text">让 AI 更懂你的身材与风格</span>
      </h1>
      <p class="mt-[var(--space-3)] max-w-xl text-sm leading-[1.95] text-mist-400">
        身高、体重、体型与风格偏好会作为 AI 穿搭顾问的上下文，直接影响推荐结果。填得越完整，建议越贴身。
      </p>
    </header>

    <!-- ============ 未登录 ============ -->
    <div v-if="!auth.isLoggedIn" class="glass flex flex-col items-center gap-[var(--space-4)] px-[var(--space-5)] py-[var(--space-8)] text-center">
      <span class="profile-seal">
        <span class="display text-xl">轻</span>
      </span>
      <h2 class="display mt-1 text-2xl text-mist-100">登录后才能编辑个人资料</h2>
      <p class="max-w-md text-sm leading-[1.9] text-mist-400">
        右上角「登录」使用演示账号 demo / 123456，或先注册一个新账号。登录后这里会记住你的尺码与偏好。
      </p>
      <RouterLink to="/" class="btn-primary mt-2">回到首页登录</RouterLink>
    </div>

    <!-- ============ 资料编辑：左概览 / 右表单，两栏之间 24px ============ -->
    <div v-else class="grid gap-[var(--space-5)] lg:grid-cols-[19.5rem_minmax(0,1fr)] lg:items-start">
      <!-- 左栏：资料概览 + 隐私说明，桌面端依次下沉 16px -->
      <div class="staggered min-w-0">
        <section v-reveal class="section-block">
          <header class="section-head">
            <div>
              <h2>资料概览</h2>
              <p>头像、完整度与基础尺码。</p>
            </div>
          </header>

          <div class="glass p-[var(--space-5)] text-center">
            <span class="avatar-ring">
              <span class="avatar-core">
                <img v-if="avatarUrl" :src="avatarUrl" :alt="nickname || auth.displayName" />
                <span v-else class="display text-2xl">{{ initial }}</span>
              </span>
            </span>

          <h3 class="display mt-[var(--space-4)] text-xl text-mist-100">{{ nickname || auth.displayName }}</h3>
          <p class="mt-1 text-[11px] tracking-[0.12em] text-mist-500">@{{ auth.user?.username }}</p>

          <div class="mt-[var(--space-5)] text-left">
            <div class="flex items-baseline justify-between text-[10px] uppercase tracking-[0.24em] text-mist-500">
              <span>资料完整度</span>
              <span class="text-[13px] tracking-normal text-gold-300 tabular-nums">{{ completeness }}%</span>
            </div>
            <div class="mt-[var(--space-2)] h-2 overflow-hidden rounded-full bg-ink-800">
              <span class="profile-progress" :style="{ width: `${completeness}%` }" />
            </div>
            <p class="mt-[var(--space-2)] text-[11px] leading-[1.8] text-mist-500">
              {{
                completeness >= 100
                  ? '资料已完整，AI 做比例与配色建议时会更准确。'
                  : '补全身高、体重与体型后，推荐的比例建议会更准。'
              }}
            </p>
          </div>

          <dl class="mt-[var(--space-5)] grid grid-cols-2 gap-[var(--space-2)] border-t border-ink-700/40 pt-[var(--space-4)] text-left">
            <div class="rounded-[var(--radius-sm)] border border-ink-700/40 px-[var(--space-3)] py-[var(--space-2)]">
              <dt class="text-[9px] uppercase tracking-[0.18em] text-mist-500">身高</dt>
              <dd class="mt-0.5 text-sm text-mist-100 tabular-nums">{{ heightCm ?? '—' }}<span class="text-[10px] text-mist-500"> cm</span></dd>
            </div>
            <div class="rounded-[var(--radius-sm)] border border-ink-700/40 px-[var(--space-3)] py-[var(--space-2)]">
              <dt class="text-[9px] uppercase tracking-[0.18em] text-mist-500">体重</dt>
              <dd class="mt-0.5 text-sm text-mist-100 tabular-nums">{{ weightKg ?? '—' }}<span class="text-[10px] text-mist-500"> kg</span></dd>
            </div>
          </dl>
          </div>
        </section>

        <section v-reveal class="section-block">
          <header class="section-head">
            <div>
              <h2>隐私说明</h2>
              <p>资料用在哪、谁能看到。</p>
            </div>
          </header>

          <!-- 次要信息刻意下沉，不与「保存资料」抢视觉 -->
          <div class="surface-sunken p-[var(--space-4)]">
            <p class="text-[11px] leading-[1.9] text-mist-400">
              资料只用于生成穿搭建议，不会用于其他用途。身高体重可选填，但填了 AI 在做比例建议时会更准确。
            </p>
          </div>
        </section>
      </div>

      <!-- 右栏：基础资料表单 -->
      <section v-reveal class="section-block min-w-0">
        <header class="section-head">
          <div>
            <h2>基础资料</h2>
            <p>填得越完整，AI 在比例、配色与场合上的建议越贴合。</p>
          </div>
          <div class="action-row justify-end">
            <span class="chip cursor-default">已选 {{ styles.length }} / 6 个风格</span>
          </div>
        </header>

        <div class="glass p-[var(--space-5)] sm:p-[var(--space-6)]">
          <div class="grid gap-[var(--space-4)] sm:grid-cols-2">
          <div>
            <label class="field-label" for="profile-nickname">昵称</label>
            <input id="profile-nickname" v-model="nickname" class="field-input" maxlength="32" placeholder="怎么称呼你" />
          </div>
          <div>
            <label class="field-label" for="profile-avatar">头像地址（可选）</label>
            <input id="profile-avatar" v-model="avatarUrl" class="field-input" maxlength="512" placeholder="https://…" />
          </div>
          <div>
            <label class="field-label" for="profile-gender">性别</label>
            <select id="profile-gender" v-model="gender" class="field-input">
              <option :value="0">未设置</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
          <div>
            <label class="field-label" for="profile-body">体型</label>
            <select id="profile-body" v-model="bodyType" class="field-input">
              <option value="">未设置</option>
              <option v-for="item in BODY_TYPES" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </div>
          <div>
            <label class="field-label" for="profile-height">身高（cm）</label>
            <input
              id="profile-height"
              v-model.number="heightCm"
              type="number"
              min="100"
              max="250"
              step="0.5"
              class="field-input"
              placeholder="例如 165"
            />
          </div>
          <div>
            <label class="field-label" for="profile-weight">体重（kg）</label>
            <input
              id="profile-weight"
              v-model.number="weightKg"
              type="number"
              min="30"
              max="200"
              step="0.5"
              class="field-input"
              placeholder="例如 52"
            />
          </div>
        </div>

        <div class="mt-[var(--space-5)]">
          <div class="flex flex-wrap items-baseline justify-between gap-2">
            <span class="field-label !mb-0">风格偏好</span>
            <span class="text-[10px] text-mist-500">最多 6 个 · 选中的标签会进入 AI 上下文</span>
          </div>
          <div class="mt-[var(--space-2)] flex flex-wrap gap-[var(--space-2)]">
            <button
              v-for="tag in STYLE_OPTIONS"
              :key="tag"
              type="button"
              class="chip !text-[11px]"
              :class="{ 'chip-active': styles.includes(tag) }"
              :aria-pressed="styles.includes(tag)"
              @click="toggleStyle(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <!-- 底部保存：一条操作行，ghost 压暗在左、primary 在右 -->
        <div class="action-row mt-[var(--space-5)] justify-between border-t border-ink-700/40 pt-[var(--space-4)]">
          <p class="flex-1 basis-[14rem] text-[11px] leading-[1.8] text-mist-500">
            保存后会立即作为 AI 顾问的上下文，下一次推荐就能用上。
          </p>
          <div class="action-row w-full justify-end sm:w-auto">
            <button type="button" class="btn-ghost flex-1 !py-2 !text-xs sm:flex-none" @click="auth.restore()">
              重新读取
            </button>
            <button type="button" class="btn-primary flex-1 !py-2 !text-xs sm:flex-none" :disabled="saving" @click="save">
              {{ saving ? '保存中…' : '保存资料' }}
            </button>
          </div>
        </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* 头像：accent 环形描边（双环 + 柔光），换季自动换色 */
.avatar-ring {
  display: grid;
  place-items: center;
  width: 6rem;
  height: 6rem;
  margin: 0 auto;
  padding: 3px;
  border-radius: 9999px;
  background: conic-gradient(
    from 150deg,
    color-mix(in oklab, var(--c-accent) 85%, transparent),
    color-mix(in oklab, var(--c-accent) 16%, transparent) 52%,
    var(--c-accent)
  );
  box-shadow:
    0 0 0 6px color-mix(in oklab, var(--c-accent) 10%, transparent),
    0 18px 38px -28px var(--c-glass-shadow);
}

.avatar-core {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 9999px;
  color: var(--c-accent-ink);
  background: radial-gradient(
    120% 120% at 30% 18%,
    color-mix(in oklab, var(--c-accent) 58%, white),
    var(--c-accent)
  );
  box-shadow: inset 0 1px 0 color-mix(in oklab, white 55%, transparent);
}

.avatar-core :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 完整度：accent 渐变进度 */
.profile-progress {
  display: block;
  height: 100%;
  border-radius: 9999px;
  background: linear-gradient(
    90deg,
    color-mix(in oklab, var(--c-accent) 58%, transparent),
    var(--c-accent)
  );
  transition: width 0.5s ease;
}

/* 未登录时的印鉴式图标 */
.profile-seal {
  display: grid;
  place-items: center;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 9999px;
  color: var(--c-accent-ink);
  background: radial-gradient(
    120% 120% at 30% 20%,
    color-mix(in oklab, var(--c-accent) 72%, white),
    var(--c-accent)
  );
  box-shadow:
    inset 0 1px 0 color-mix(in oklab, white 55%, transparent),
    0 10px 26px -16px color-mix(in oklab, var(--c-accent) 80%, transparent);
}
</style>
