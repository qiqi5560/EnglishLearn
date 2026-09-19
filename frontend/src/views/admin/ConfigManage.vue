<template>
  <div v-loading="loading" class="page page-shell page-shell--narrow">
    <header class="page-head">
      <h1 class="page-title">系统配置</h1>
      <p class="page-desc">调整语音语速、内容推荐策略与社区审核规则</p>
    </header>

    <section class="config-section">
      <div class="section-title">语音设置（F008）</div>
      <el-card shadow="never" class="config-card">
        <div class="setting-row">
          <span class="setting-name">慢速语速</span>
          <el-slider v-model="config.speech.slowSpeed" :min="0.5" :max="1.5" :step="0.05" class="setting-control" />
        </div>
        <div class="setting-row">
          <span class="setting-name">常速语速</span>
          <el-slider v-model="config.speech.normalSpeed" :min="0.8" :max="2" :step="0.05" class="setting-control" />
        </div>
      </el-card>
    </section>

    <section class="config-section">
      <div class="section-title">内容推荐策略（F006）</div>
      <el-card shadow="never" class="config-card">
        <div class="setting-row">
          <span class="setting-name">内容推荐</span>
          <el-switch v-model="config.recommend.content" />
        </div>
        <div class="setting-row">
          <span class="setting-name">结伴学习推荐</span>
          <el-switch v-model="config.recommend.collab" />
        </div>
        <div class="setting-row">
          <span class="setting-name">AI 模型推荐</span>
          <el-switch v-model="config.recommend.model" />
        </div>
      </el-card>
    </section>

    <section class="config-section">
      <div class="section-title">社区审核</div>
      <el-card shadow="never" class="config-card">
        <div class="setting-row">
          <span class="setting-name">内容自动审核</span>
          <el-switch v-model="config.audit.content" />
        </div>
        <div class="setting-row">
          <span class="setting-name">人工复核</span>
          <el-switch v-model="config.audit.manual" />
        </div>
      </el-card>
    </section>

    <div class="save-bar">
      <el-button type="primary" class="save-btn" size="large" :loading="saving" @click="onSave">保存配置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfigs, updateSystemConfigs } from '@/api/modules/admin'
import type { SystemConfig } from '@/types/api'

const DEFAULT_CONFIG: SystemConfig = {
  speech: { slowSpeed: 0.75, normalSpeed: 1 },
  recommend: { content: true, collab: true, model: true },
  audit: { content: true, manual: false },
}

const loading = ref(false)
const saving = ref(false)
const config = reactive<SystemConfig>(JSON.parse(JSON.stringify(DEFAULT_CONFIG)))

async function onSave() {
  saving.value = true
  try {
    const res = await updateSystemConfigs(config)
    Object.assign(config, res)
    ElMessage.success('配置已保存')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getSystemConfigs()
    Object.assign(config, res)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.page-head {
  margin-bottom: 8px;
}

.page-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.page-desc {
  margin: 7px 0 0;
  font-size: 13.5px;
  color: var(--muted);
}

.config-section .section-title {
  font-size: 18px;
}

.config-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple,
    border-color 0.35s ease;

  &:hover {
    border-color: rgba(59, 111, 224, 0.22);
    box-shadow: var(--shadow-md);
  }

  :deep(.el-card__body) {
    padding: 8px 24px;
  }
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);

  &:last-child {
    border-bottom: none;
  }
}

.setting-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.setting-control {
  width: 260px;
  flex-shrink: 0;
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  margin: 28px 0 8px;
  padding-top: 22px;
  border-top: 1px solid var(--border);
}

.save-btn.el-button--primary {
  min-width: 168px;
  height: 46px;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0 55%, #2f5bb3);
  box-shadow: 0 10px 24px rgba(59, 111, 224, 0.3);
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover,
  &:focus {
    transform: translateY(-2px);
    background: linear-gradient(135deg, #6b98f3, #4478e4 55%, #33619f);
    box-shadow: 0 16px 34px rgba(59, 111, 224, 0.4);
  }

  &:active {
    transform: translateY(0) scale(0.99);
  }
}

// ---------------- Element Plus 定制 ----------------
:deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, #5b8bf0, #3b6fe0);
}

:deep(.el-slider__bar) {
  background: linear-gradient(90deg, #5b8bf0, #3b6fe0);
}

:deep(.el-slider__button) {
  border-color: var(--primary);
  box-shadow: 0 4px 10px rgba(59, 111, 224, 0.28);
  transition: transform 0.35s $ease-apple;
}

:deep(.el-slider__button:hover) {
  transform: scale(1.14);
}

@media (max-width: 900px) {
  .setting-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .setting-control {
    width: 100%;
  }
}
</style>