<template>
  <div class="partners page">
    <AppHeader title="搭子组队" back />

    <div class="page-shell partners-body">
      <header class="partners-head">
        <h2 class="partners-title">找到一起练口语的搭子</h2>
        <p class="partners-sub text-muted">互相督促、结伴练习，开口的频次会明显变高。</p>
      </header>

      <div class="partners-grid">
        <!-- ==================== 左栏：添加搭子 ==================== -->
        <section class="add-card glass-card hover-lift sheen">
          <header class="card-head">
            <span class="card-icon"><el-icon><UserFilled /></el-icon></span>
            <div>
              <h3>添加搭子</h3>
              <p class="text-muted">输入对方注册手机号即可添加</p>
            </div>
          </header>

          <el-input v-model="form.phone" placeholder="请输入对方手机号" maxlength="11" @keyup.enter="onAdd">
            <template #append>
              <el-button type="primary" :loading="adding" @click="onAdd">添加</el-button>
            </template>
          </el-input>

          <p class="add-tip text-muted">添加成功后，对方会出现在右侧的搭子列表中。</p>
        </section>

        <!-- ==================== 右栏：搭子列表 ==================== -->
        <section class="list-col">
          <div class="list-head">
            <h3>我的搭子</h3>
            <span class="count-chip">{{ partners.length }}</span>
          </div>

          <el-empty v-if="!loading && partners.length === 0" description="还没有搭子，快去找一个一起练口语吧" />

          <div v-else class="partner-grid stagger">
            <article v-for="p in partners" :key="p.userId" class="partner-card glass-card hover-lift">
              <el-avatar :size="44" class="partner-avatar" :src="p.avatarUrl || undefined">
                {{ p.nickname?.[0]?.toUpperCase() || 'P' }}
              </el-avatar>

              <div class="partner-meta">
                <div class="partner-name">
                  <span class="partner-nickname">{{ p.nickname }}</span>
                  <LevelTag v-if="p.level" :level="p.level" />
                </div>
                <div class="text-muted partner-phone">{{ maskPhone(p.phone) }}</div>
              </div>

              <el-button class="del-btn" size="small" type="danger" plain @click="onDelete(p)">删除</el-button>
            </article>
          </div>
        </section>
      </div>
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
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);
$ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

.partners-body {
  min-height: 60vh;
}

.partners-head {
  margin-bottom: 24px;
}

.partners-title {
  margin: 0;
  font-size: clamp(26px, 2.4vw, 34px);
  font-weight: 700;
  letter-spacing: -0.03em;
}

.partners-sub {
  margin: 8px 0 0;
  font-size: 14px;
}

// ---------------- 桌面两栏：左添加 + 右列表 ----------------
.partners-grid {
  display: grid;
  grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

// ==================== 左栏：添加搭子 ====================
.add-card {
  padding: 24px 26px 26px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  p {
    margin: 5px 0 0;
    font-size: 12.5px;
  }
}

.card-icon {
  flex: 0 0 auto;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  font-size: 20px;
  border-radius: 13px;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.1);
  transition:
    background 0.35s ease,
    color 0.35s ease,
    transform 0.45s $ease-spring;
}

.add-card:hover .card-icon {
  color: #fff;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  transform: rotate(-6deg) scale(1.06);
}

.add-tip {
  margin: 16px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
}

// ==================== 右栏：搭子列表 ====================
.list-col {
  min-width: 0;
}

.list-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
}

.count-chip {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--primary);
  background: rgba(59, 111, 224, 0.12);
}

.partner-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.partner-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  min-width: 0;
}

.partner-avatar {
  flex: 0 0 auto;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(150deg, var(--primary), var(--ink));
  transition: transform 0.5s $ease-spring;
}

.partner-card:hover .partner-avatar {
  transform: scale(1.06);
}

.partner-meta {
  flex: 1;
  min-width: 0;

  .partner-name {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    margin-bottom: 4px;
  }

  .partner-phone {
    font-size: 12.5px;
    letter-spacing: 0.02em;
  }
}

.partner-nickname {
  font-size: 14.5px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.del-btn {
  flex: 0 0 auto;
  border-radius: 999px;
  transition:
    transform 0.35s $ease-apple,
    box-shadow 0.35s $ease-apple;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(229, 72, 77, 0.2);
  }
}

@media (max-width: 1080px) {
  .partner-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .partners-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .add-card {
    padding: 20px 18px 22px;
  }
}
</style>