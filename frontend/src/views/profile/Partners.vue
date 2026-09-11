<template>
  <div class="partners">
    <AppHeader title="搭子组队" back />

    <div class="partners-body">
      <!-- 添加搭子 -->
      <el-card shadow="never" class="add-card">
        <div class="add-tip text-muted">输入对方注册手机号，即可添加为你的学习搭子</div>
        <el-input v-model="form.phone" placeholder="请输入对方手机号" maxlength="11" @keyup.enter="onAdd">
          <template #append>
            <el-button type="primary" :loading="adding" @click="onAdd">添加</el-button>
          </template>
        </el-input>
      </el-card>

      <!-- 搭子列表 -->
      <div class="section-title">我的搭子（{{ partners.length }}）</div>

      <el-empty v-if="!loading && partners.length === 0" description="还没有搭子，快去找一个一起练口语吧" />

      <el-card v-for="p in partners" :key="p.userId" shadow="never" class="partner-card">
        <div class="partner-row">
          <el-avatar :size="44" :src="p.avatarUrl || undefined">
            {{ p.nickname?.[0]?.toUpperCase() || 'P' }}
          </el-avatar>
          <div class="partner-meta">
            <div class="partner-name">
              {{ p.nickname }}
              <LevelTag v-if="p.level" :level="p.level" />
            </div>
            <div class="text-muted partner-phone">{{ maskPhone(p.phone) }}</div>
          </div>
          <el-button size="small" type="danger" plain @click="onDelete(p)">删除</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import LevelTag from '@/components/business/LevelTag.vue'
import * as authApi from '@/api/modules/auth'
import type { PartnerDto } from '@/types/api'

const partners = ref<PartnerDto[]>([])
const loading = ref(false)
const adding = ref(false)
const form = reactive({ phone: '' })

function maskPhone(phone?: string) {
  if (!phone || phone.length < 11) return phone ?? ''
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

async function load() {
  loading.value = true
  try {
    partners.value = await authApi.listPartners()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
}

async function onAdd() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  adding.value = true
  try {
    await authApi.addPartner(form.phone.trim())
    ElMessage.success('添加成功')
    form.phone = ''
    await load()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    adding.value = false
  }
}

async function onDelete(p: PartnerDto) {
  try {
    await ElMessageBox.confirm(`确定删除搭子「${p.nickname}」吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await authApi.deletePartner(p.userId)
    ElMessage.success('已删除')
    await load()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.partners-body {
  padding: 16px;
  min-height: 60vh;
}

.add-card {
  border-radius: var(--radius-md);

  .add-tip {
    font-size: 13px;
    line-height: 1.6;
    margin-bottom: 12px;
  }
}

.section-title {
  font-size: 14px;
  color: var(--text-secondary, #666);
  margin: 16px 0 8px;
}

.partner-card {
  border-radius: var(--radius-md);
  margin-bottom: 12px;
}

.partner-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.partner-meta {
  flex: 1;
  min-width: 0;

  .partner-name {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 700;
    margin-bottom: 4px;
  }

  .partner-phone {
    font-size: 12px;
  }
}
</style>