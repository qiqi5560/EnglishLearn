<template>
  <div v-loading="loading">
    <div class="section-title">语音设置（F008）</div>
    <el-card shadow="never" class="config-card">
      <div class="setting-row">
        <span>慢速语速</span>
        <el-slider v-model="config.speech.slowSpeed" :min="0.5" :max="1.5" :step="0.05" style="width: 220px" />
      </div>
      <div class="setting-row">
        <span>常速语速</span>
        <el-slider v-model="config.speech.normalSpeed" :min="0.8" :max="2" :step="0.05" style="width: 220px" />
      </div>
    </el-card>

    <div class="section-title">内容推荐策略（F006）</div>
    <el-card shadow="never" class="config-card">
      <div class="setting-row">
        <span>内容推荐</span>
        <el-switch v-model="config.recommend.content" />
      </div>
      <div class="setting-row">
        <span>结伴学习推荐</span>
        <el-switch v-model="config.recommend.collab" />
      </div>
      <div class="setting-row">
        <span>AI 模型推荐</span>
        <el-switch v-model="config.recommend.model" />
      </div>
    </el-card>

    <div class="section-title">社区审核</div>
    <el-card shadow="never" class="config-card">
      <div class="setting-row">
        <span>内容自动审核</span>
        <el-switch v-model="config.audit.content" />
      </div>
      <div class="setting-row">
        <span>人工复核</span>
        <el-switch v-model="config.audit.manual" />
      </div>
    </el-card>

    <el-button type="primary" class="save-btn" :loading="saving" @click="onSave">保存配置</el-button>
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
.section-title {
  margin-bottom: 12px;
}

.config-card {
  margin-bottom: 16px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);

  &:last-child {
    border-bottom: none;
  }
}

.save-btn {
  width: 100%;
}
</style>
