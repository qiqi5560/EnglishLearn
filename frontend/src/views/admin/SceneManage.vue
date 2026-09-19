<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">场景管理</h1>
      <p class="page-desc">维护「场景对话」的场景库，新增 / 上架后所有学员进入练习即可看到</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          placeholder="搜索场景名称"
          clearable
          @keyup.enter="load"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" class="toolbar-select">
          <el-option label="全部场景" value="all" />
          <el-option label="已上架" value="1" />
          <el-option label="已下架" value="0" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>&nbsp;新增场景
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>场景列表</span>
        <em class="text-muted">共 {{ filtered.length }} 个场景</em>
      </div>
      <el-table v-loading="loading" :data="filtered" stripe>
        <el-table-column prop="id" label="ID" width="66" />
        <el-table-column prop="name" label="场景名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="96">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category || '--' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="76" align="center" />
        <el-table-column prop="role" label="对话角色" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.role || '--' }}</template>
        </el-table-column>
        <el-table-column prop="desc" label="场景描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.desc || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !filtered.length" description="暂无场景数据" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑场景' : '新增场景'" width="680px" align-center>
      <el-form label-position="top">
        <div class="form-row">
          <el-form-item label="场景名称">
            <el-input v-model="form.sceneName" maxlength="50" placeholder="如：咖啡店点单" />
          </el-form-item>
          <el-form-item label="场景分类">
            <el-select v-model="form.sceneCategory" style="width: 100%">
              <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="适用等级">
            <el-select v-model="form.levelScope" style="width: 100%">
              <el-option v-for="lv in LEVELS" :key="lv" :label="lv" :value="lv" />
            </el-select>
          </el-form-item>
          <el-form-item label="AI 对话角色">
            <el-input v-model="form.role" maxlength="30" placeholder="如：咖啡店店员" />
          </el-form-item>
        </div>
        <el-form-item label="场景描述">
          <el-input v-model="form.sceneDesc" maxlength="120" placeholder="一句话说明练习目标，学员在场景卡片上看到" />
        </el-form-item>
        <el-form-item label="对话脚本 / 情境设定">
          <el-input
            v-model="form.script"
            type="textarea"
            :rows="4"
            placeholder="描述对话情境与 AI 的扮演要求，留空则使用默认设定"
          />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="form.coverUrl" placeholder="可选，留空使用默认封面" />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch v-model="form.online" active-text="立即上架" inactive-text="暂不上架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminCreateScene,
  adminDeleteScene,
  adminListScenes,
  adminUpdateScene,
} from '@/api/modules/admin'
import type { SceneDto } from '@/types/api'

const CATEGORIES = ['生活', '工作', '学习', '出行']
const LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

const loading = ref(false)
const saving = ref(false)
const rows = ref<SceneDto[]>([])
const keyword = ref('')
const statusFilter = ref('all')
const dialogVisible = ref(false)
const editId = ref<number | null>(null)

const form = reactive({
  sceneName: '',
  sceneCategory: '生活',
  levelScope: 'A1',
  role: '',
  sceneDesc: '',
  script: '',
  coverUrl: '',
  online: true,
})

/** 状态在前端过滤：后端 /admin/scenes 返回全量（含已下架） */
const filtered = computed(() =>
  statusFilter.value === 'all'
    ? rows.value
    : rows.value.filter((r) => String(r.status) === statusFilter.value),
)

async function load() {
  loading.value = true
  try {
    rows.value = await adminListScenes(keyword.value.trim() || undefined)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.sceneName = ''
  form.sceneCategory = '生活'
  form.levelScope = 'A1'
  form.role = ''
  form.sceneDesc = ''
  form.script = ''
  form.coverUrl = ''
  form.online = true
}

function openCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: SceneDto) {
  editId.value = row.id
  form.sceneName = row.name
  form.sceneCategory = row.category || '生活'
  form.levelScope = row.level || 'A1'
  form.role = row.roleSetting?.role ?? row.role ?? ''
  form.sceneDesc = row.desc ?? ''
  form.script = row.roleSetting?.script ?? ''
  form.coverUrl = row.coverUrl ?? ''
  form.online = row.status === 1
  dialogVisible.value = true
}

async function onSave() {
  if (!form.sceneName.trim()) {
    ElMessage.warning('场景名称不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      sceneName: form.sceneName.trim(),
      sceneCategory: form.sceneCategory,
      sceneDesc: form.sceneDesc.trim(),
      levelScope: form.levelScope,
      role: form.role.trim(),
      script: form.script.trim(),
      coverUrl: form.coverUrl.trim(),
      status: form.online ? 1 : 0,
    }
    if (editId.value) {
      await adminUpdateScene(editId.value, {
        sceneName: payload.sceneName,
        sceneCategory: payload.sceneCategory,
        sceneDesc: payload.sceneDesc,
        levelScope: payload.levelScope,
        role: payload.role,
        script: payload.script,
        coverUrl: payload.coverUrl,
        status: payload.status,
      })
      ElMessage.success('场景已更新')
    } else {
      await adminCreateScene(payload)
      ElMessage.success('场景已添加，所有学员立即可见')
    }
    dialogVisible.value = false
    load()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: SceneDto) {
  const next = row.status === 1 ? 0 : 1
  try {
    await adminUpdateScene(row.id, { status: next })
    ElMessage.success(next === 1 ? '场景已上架' : '场景已下架')
    load()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

function onDelete(row: SceneDto) {
  ElMessageBox.confirm(
    `确定删除场景「${row.name}」吗？已产生练习记录的场景建议改为「下架」。`,
    '删除场景',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      try {
        await adminDeleteScene(row.id)
        ElMessage.success('场景已删除')
        load()
      } catch {
        /* 错误提示已由请求层统一弹出 */
      }
    })
    .catch(() => {})
}

onMounted(load)
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
  width: 140px;
}

.table-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 42, 68, 0.07);
  background: var(--card);
  box-shadow: var(--shadow-sm);

  :deep(.el-card__body) {
    padding: 18px 20px 20px;
  }
}

.card-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;

  em {
    font-size: 12px;
    font-style: normal;
    font-weight: 500;
  }
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

:deep(.el-table) {
  --el-table-border-color: var(--border);
  --el-table-header-bg-color: var(--surface);
  --el-table-row-hover-bg-color: rgba(59, 111, 224, 0.06);
  --el-table-striped-bg-color: rgba(246, 247, 251, 0.6);
  --el-table-text-color: var(--ink);
  font-size: 13.5px;
  border-radius: var(--radius-md);
}

:deep(.el-table th.el-table__cell) {
  background: var(--surface);
  color: #55617e;
  font-size: 12.5px;
  font-weight: 600;
}

:deep(.el-table .el-table__cell) {
  padding: 10px 0;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-table__body tr.el-table__row td.el-table__cell) {
  transition: background 0.28s $ease-apple;
}

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

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
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

  .form-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
