<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">用户管理</h1>
      <p class="page-desc">查看注册与使用明细、调整角色状态，并对恶意发帖用户发起处罚</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          placeholder="搜索手机号 / 昵称"
          clearable
          @keyup.enter="loadRows(1)"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="roleFilter" class="toolbar-select" clearable placeholder="角色" @change="loadRows(1)">
          <el-option label="学习者" value="learner" />
          <el-option label="管理员" value="admin" />
          <el-option label="监护人" value="guardian" />
        </el-select>
        <el-select v-model="banFilter" class="toolbar-select" @change="loadRows(1)">
          <el-option label="全部用户" value="all" />
          <el-option label="处罚中" value="1" />
          <el-option label="正常" value="0" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button @click="loadRows(1)">查询</el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="userId" label="ID" width="60" />
        <el-table-column prop="phone" label="手机号" width="116" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column label="年龄段" width="78">
          <template #default="{ row }">
            {{ ageGroupMeta[row.ageGroup] ?? row.ageGroup }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="76">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ roleMeta[row.role] ?? row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="56" align="center">
          <template #default="{ row }">{{ row.level || '--' }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="146">
          <template #default="{ row }">
            <span :title="row.registerTime || ''">{{ minuteTime(row.registerTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="146">
          <template #default="{ row }">
            <span :title="row.lastLoginTime || ''">{{ minuteTime(row.lastLoginTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="76">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发帖权限" width="144" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.punished"
              :content="`原因：${row.banReason || '违反社区规范'}，解禁时间：${row.banUntil || '--'}`"
              placement="top"
            >
              <el-tag type="danger" size="small" effect="dark">禁言至 {{ shortTime(row.banUntil) }}</el-tag>
            </el-tooltip>
            <el-tag v-else type="info" size="small" effect="plain">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div v-if="row.userId !== userStore.userInfo?.userId" class="op-cell">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <span class="op-divider" aria-hidden="true"></span>
              <el-button v-if="!row.punished" link type="warning" size="small" @click="openPunish(row)">
                处罚
              </el-button>
              <el-button v-else link type="success" size="small" @click="onUnban(row)">解除处罚</el-button>
              <span class="op-divider" aria-hidden="true"></span>
              <el-button link size="small" @click="onResetPassword(row)">重置密码</el-button>
              <span class="op-divider" aria-hidden="true"></span>
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'success'"
                size="small"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </div>
            <span v-else class="text-muted">当前账号</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无用户数据" />
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="loadRows"
      />
    </el-card>

    <!-- 编辑用户 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="520px" align-center>
      <el-form label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.userRole" style="width: 100%">
            <el-option label="学习者" value="learner" />
            <el-option label="监护人" value="guardian" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发帖处罚对话框 -->
    <el-dialog v-model="punishVisible" title="发起发帖处罚" width="440px" :close-on-click-modal="false">
      <p class="punish-tip text-muted">
        处罚期间，该用户无法发帖与评论，登录与学习功能不受影响。
      </p>
      <el-form label-position="top">
        <el-form-item label="处罚天数">
          <div class="punish-days">
            <el-radio-group v-model="punishDays" size="small">
              <el-radio-button :value="1">1 天</el-radio-button>
              <el-radio-button :value="3">3 天</el-radio-button>
              <el-radio-button :value="7">7 天</el-radio-button>
              <el-radio-button :value="30">30 天</el-radio-button>
            </el-radio-group>
            <el-input-number v-model="punishDays" :min="1" :max="365" size="small" class="punish-num" />
          </div>
        </el-form-item>
        <el-form-item label="处罚原因">
          <el-input v-model="punishReason" placeholder="如：恶意发帖 / 广告刷屏" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="punishVisible = false">取消</el-button>
        <el-button type="danger" :loading="punishing" @click="submitPunish">确认处罚</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminBanUser,
  adminListUsers,
  adminResetPassword,
  adminUnbanUser,
  adminUpdateUser,
} from '@/api/modules/admin'
import type { UserDto } from '@/types/api'
import { useUserStore } from '@/stores/user'

const ageGroupMeta: Record<string, string> = { child: '儿童', k12: 'K12 学生', adult: '成人', senior: '中老年' }
const roleMeta: Record<string, string> = { learner: '学习者', admin: '管理员', guardian: '监护人' }

const userStore = useUserStore()
const rows = ref<UserDto[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref('')
const banFilter = ref('all')
const editVisible = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const editForm = reactive({ nickname: '', userRole: 'learner' })

// 发帖处罚
const punishVisible = ref(false)
const punishing = ref(false)
const punishTarget = ref<UserDto | null>(null)
const punishDays = ref(3)
const punishReason = ref('恶意发帖')

/** 发帖处罚筛选交由后端处理，避免只过滤当前页导致漏查 */
const punishedParam = computed(() => (banFilter.value === 'all' ? undefined : banFilter.value === '1'))

/** 只保留「月-日 时:分」，便于在表格窄列中展示解禁时间 */
function shortTime(value?: string | null) {
  if (!value) return '--'
  return value.slice(5, 16)
}

/** 表格中注册/登录时间精确到分钟（完整到秒的值放在 title 里） */
function minuteTime(value?: string | null) {
  if (!value) return '--'
  return value.slice(0, 16)
}

async function loadRows(target = 1) {
  page.value = target
  loading.value = true
  try {
    const res = await adminListUsers({
      keyword: keyword.value.trim() || undefined,
      role: roleFilter.value || undefined,
      punished: punishedParam.value,
      page: page.value,
      pageSize,
    })
    rows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openEdit(row: UserDto) {
  editId.value = row.userId
  editForm.nickname = row.nickname
  editForm.userRole = row.role
  editVisible.value = true
}

async function onSave() {
  if (!editForm.nickname.trim() || !editId.value) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await adminUpdateUser(editId.value, { nickname: editForm.nickname.trim(), userRole: editForm.userRole })
    editVisible.value = false
    ElMessage.success('保存成功')
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    saving.value = false
  }
}

function toggleStatus(row: UserDto) {
  const toDisable = row.status === 1
  ElMessageBox.confirm(
    toDisable ? `确定禁用用户「${row.nickname}」吗？` : `确定恢复用户「${row.nickname}」吗？`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      try {
        await adminUpdateUser(row.userId, { status: toDisable ? 0 : 1 })
        ElMessage.success(toDisable ? '已禁用' : '已启用')
        loadRows(page.value)
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

function onResetPassword(row: UserDto) {
  ElMessageBox.confirm(
    `确定将用户「${row.nickname}」的密码重置为默认密码 123456 吗？`,
    '重置密码',
    { confirmButtonText: '重置', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      try {
        await adminResetPassword(row.userId)
        ElMessage.success('密码已重置为 123456')
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

function openPunish(row: UserDto) {
  if (row.role === 'admin') {
    ElMessage.warning('不能处罚管理员账号')
    return
  }
  punishTarget.value = row
  punishDays.value = 3
  punishReason.value = '恶意发帖'
  punishVisible.value = true
}

async function submitPunish() {
  const target = punishTarget.value
  if (!target) return
  punishing.value = true
  try {
    await adminBanUser(target.userId, { days: punishDays.value, reason: punishReason.value })
    ElMessage.success(`已处罚「${target.nickname}」${punishDays.value} 天`)
    punishVisible.value = false
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    punishing.value = false
  }
}

function onUnban(row: UserDto) {
  ElMessageBox.confirm(`确定解除用户「${row.nickname}」的发帖处罚吗？`, '解除处罚', {
    confirmButtonText: '解除',
    cancelButtonText: '取消',
    type: 'info',
  })
    .then(async () => {
      try {
        await adminUnbanUser(row.userId)
        ElMessage.success('已解除处罚')
        loadRows(page.value)
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

onMounted(() => loadRows(1))
</script>

<style scoped lang="scss">
$ease-apple: cubic-bezier(0.22, 1, 0.36, 1);

.page-head {
  margin-bottom: 24px;
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

// ---------------- 工具条 ----------------
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(14px) saturate(150%);
  -webkit-backdrop-filter: blur(14px) saturate(150%);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.toolbar-search {
  width: 260px;
}

.toolbar-select {
  width: 150px;
}

// ---------------- 表格卡片 ----------------
.table-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 16px 20px 18px;
  }
}

:deep(.el-table) {
  --el-table-border-color: var(--border);
  --el-table-header-bg-color: var(--surface);
  --el-table-row-hover-bg-color: rgba(59, 111, 224, 0.06);
  --el-table-text-color: var(--ink);
  font-size: 14px;
  border-radius: var(--radius-md);
}

:deep(.el-table th.el-table__cell) {
  background: var(--surface);
  color: #55617e;
  font-size: 13px;
  font-weight: 600;
}

:deep(.el-table .el-table__cell) {
  padding: 12px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(246, 247, 251, 0.6);
}

:deep(.el-table__body tr.el-table__row td.el-table__cell) {
  transition: background 0.28s $ease-apple;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

.pager {
  margin-top: 18px;
  justify-content: flex-end;
}

// ---------------- 操作列：按钮分组拉开间距，避免与处罚状态挤在一起 ----------------
.op-cell {
  display: flex;
  align-items: center;
  gap: 4px;

  :deep(.el-button.is-link) {
    margin-left: 0;
    padding: 0 6px;
    font-size: 13px;
  }
}

.op-divider {
  flex: 0 0 auto;
  width: 1px;
  height: 12px;
  border-radius: 1px;
  background: rgba(31, 42, 68, 0.14);
}

// ---------------- 弹窗 ----------------
:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  box-shadow: 0 30px 70px rgba(31, 42, 68, 0.2);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 24px 14px;
  border-bottom: 1px solid var(--border);
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

:deep(.el-dialog__body) {
  padding: 20px 24px 6px;
}

:deep(.el-dialog__footer) {
  padding: 12px 24px 20px;
}

:deep(.el-form-item__label) {
  padding-bottom: 5px;
  font-size: 13px;
  font-weight: 600;
  color: #5c6579;
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
}

// ---------------- 发帖处罚 ----------------
.punish-tip {
  margin: 0 0 14px;
  font-size: 12.5px;
  line-height: 1.6;
}

.punish-days {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.punish-num {
  width: 116px;
}

@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search,
  .toolbar-select {
    width: 100%;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}
</style>