<template>
  <div class="post-create page">
    <AppHeader title="发布帖子" back />

    <div class="page-shell create-body">
      <header class="create-hero">
        <h2>写下你的学习片刻</h2>
        <p class="text-muted">一条真实的练习心得，比十条鸡汤更能帮到同学。</p>
      </header>

      <!-- 发帖处罚提示：处罚期内禁止发帖与评论 -->
      <el-alert
        v-if="isPunished"
        class="punish-banner"
        type="error"
        :closable="false"
        show-icon
        :title="punishMessage"
      />

      <el-form label-position="top" class="create-layout">
        <section class="glass-card editor-card">
          <template v-if="isPunished">
            <el-form-item label="标题">
              <el-input :model-value="form.title" disabled placeholder="处罚期间暂时无法发帖" />
            </el-form-item>
            <el-form-item label="正文" class="content-item">
              <el-input :model-value="form.content" type="textarea" :rows="12" disabled placeholder="处罚期间暂时无法发帖" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="标题">
              <el-input
                v-model="form.title"
                size="large"
                placeholder="一句话概括你的内容"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="正文" class="content-item">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="12"
                resize="none"
                placeholder="分享你的学习经验、练习收获，或者想问的问题…"
              />
            </el-form-item>
          </template>
        </section>

        <!-- 右栏：发布设置与提示 -->
        <aside class="side-col stagger">
          <div class="glass-card setting-card">
            <div class="side-title">发布设置</div>

            <el-form-item label="话题标签">
              <el-select v-model="form.topic" size="large" placeholder="选择话题" style="width: 100%">
                <el-option v-for="t in communityStore.topics" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>

            <el-form-item label="添加图片">
              <el-upload action="#" list-type="picture-card" :auto-upload="false">
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </div>

          <div class="glass-card hover-lift peek-host tips-card">
            <PeekMascot :size="48" />
            <div class="side-title">发布小贴士</div>
            <ul class="tips-list">
              <li v-for="t in tips" :key="t">{{ t }}</li>
            </ul>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            :disabled="isPunished"
            @click="onSubmit"
          >
            {{ isPunished ? '处罚期间不可发帖' : '发布' }}
          </el-button>
        </aside>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import PeekMascot from '@/components/base/PeekMascot.vue'
import { useCommunityStore } from '@/stores/community'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const communityStore = useCommunityStore()
const userStore = useUserStore()

const submitting = ref(false)
const form = reactive({ title: '', topic: '', content: '' })

/** 处罚期内禁止发帖：由 users.ban_until / ban_reason 驱动 */
const isPunished = computed(() => !!userStore.userInfo?.punished)
const punishMessage = computed(() => {
  const u = userStore.userInfo
  if (!u?.punished) return ''
  const reason = u.banReason || '违反社区规范'
  return `您因「${reason}」被限制发帖至 ${u.banUntil || '--'}，期间无法发帖或评论`
})

// 纯展示文案
const tips = [
  '标题写清楚练习场景，更容易被同类学习者看到',
  '正文里可以贴出你的原句和更地道的说法',
  '选对话题标签，帖子会出现在对应话题下',
]

async function onSubmit() {
  if (isPunished.value) {
    ElMessage.warning(punishMessage.value)
    return
  }
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  if (!form.topic) {
    ElMessage.warning('请选择话题标签')
    return
  }
  submitting.value = true
  try {
    await communityStore.publish({
      title: form.title.trim(),
      content: form.content.trim(),
      topic: form.topic,
    })
    ElMessage.success('发布成功')
    router.push('/community')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!communityStore.topics.length) communityStore.loadTopics()
  // 拉取最新用户信息，确保处罚状态与后台一致
  userStore.fetchMe().catch(() => {})
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

// ---------------- 页头 ----------------
.create-hero {
  margin-bottom: 26px;

  h2 {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  p {
    margin: 10px 0 0;
    font-size: 14.5px;
  }
}

// ---------------- 发帖处罚提示 ----------------
.punish-banner {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
}

// ---------------- 两栏表单 ----------------
.create-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 26px;
  align-items: start;
}

.editor-card {
  padding: 26px 30px 8px;

  :deep(.el-form-item__label) {
    font-size: 14px;
    font-weight: 700;
    color: var(--ink);
    padding-bottom: 8px;
  }

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    font-size: 14.5px;
    line-height: 1.8;
    padding: 14px 16px;
    background: rgba(255, 255, 255, 0.85);
  }

  :deep(.el-input__wrapper) {
    border-radius: var(--radius-md);
  }

  .content-item {
    margin-bottom: 22px;
  }
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 96px;
}

.side-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: 14px;
}

.setting-card {
  padding: 22px 22px 6px;

  :deep(.el-form-item__label) {
    font-size: 13.5px;
    font-weight: 600;
    color: var(--ink);
    padding-bottom: 6px;
  }

  :deep(.el-upload--picture-card),
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 84px;
    height: 84px;
    border-radius: var(--radius-md);
  }

  :deep(.el-upload--picture-card) {
    transition:
      transform 0.35s $ease-apple,
      border-color 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      border-color: var(--primary);
    }
  }
}

.tips-card {
  position: relative;
  padding: 22px 22px 24px;

  .tips-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;

    li {
      position: relative;
      padding-left: 20px;
      font-size: 13px;
      line-height: 1.65;
      color: var(--muted);

      &::before {
        content: '';
        position: absolute;
        left: 2px;
        top: 7px;
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: linear-gradient(135deg, var(--primary), var(--accent));
      }
    }
  }
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 30px rgba(59, 111, 224, 0.34);
  }
}

// ---------------- 窄屏兜底 ----------------
@media (max-width: 900px) {
  .create-hero h2 {
    font-size: 24px;
  }

  .create-layout {
    grid-template-columns: 1fr;
  }

  .side-col {
    position: static;
  }

  .editor-card {
    padding: 20px 20px 4px;
  }
}
</style>