<template>
  <div class="page page-shell page-shell--wide">
    <header class="page-head">
      <h1 class="page-title">名句素材</h1>
      <p class="page-desc">维护「名句跟读」的内置素材库，新增或修订后学员端立即生效</p>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          placeholder="搜索标题 / 出处 / 正文"
          clearable
          @keyup.enter="load"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="builtinFilter" class="toolbar-select" @change="load">
          <el-option label="全部素材" value="all" />
          <el-option label="内置素材" value="1" />
          <el-option label="用户自建" value="0" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>&nbsp;新增名句
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="card-title">
        <span>素材列表</span>
        <em class="text-muted">共 {{ rows.length }} 条</em>
      </div>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="quoteId" label="ID" width="66" />
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="source" label="出处" width="170" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category || '--' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="76" align="center" />
        <el-table-column prop="wordCount" label="词数" width="76" align="center" />
        <el-table-column label="来源" width="96">
          <template #default="{ row }">
            <el-tag :type="row.builtin ? 'primary' : 'info'" size="small" effect="plain">
              {{ row.builtin ? '内置' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无名句素材" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑名句' : '新增名句'" width="640px" align-center>
      <el-form label-position="top">
        <div class="form-row">
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="60" placeholder="留空则自动截取正文" />
          </el-form-item>
          <el-form-item label="出处">
            <el-input v-model="form.source" maxlength="40" placeholder="如《阿甘正传》" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="form.category" style="width: 100%">
              <el-option label="电影台词" value="电影台词" />
              <el-option label="英语美句" value="英语美句" />
              <el-option label="励志名言" value="励志名言" />
            </el-select>
          </el-form-item>
          <el-form-item label="等级">
            <el-select v-model="form.level" style="width: 100%">
              <el-option v-for="lv in ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']" :key="lv" :label="lv" :value="lv" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="英文正文">
          <el-input v-model="form.textEn" type="textarea" :rows="5" placeholder="约 50 词的英文名句" />
        </el-form-item>
        <el-form-item label="中文译文">
          <el-input v-model="form.textZh" type="textarea" :rows="4" placeholder="可留空，学员端可一键翻译" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminCreateQuote,
  adminDeleteQuote,
  adminListQuotes,
  adminUpdateQuote,
} from '@/api/modules/admin'
import type { AdminQuoteDto } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<AdminQuoteDto[]>([])
const keyword = ref('')
const builtinFilter = ref('all')
const dialogVisible = ref(false)
const editId = ref<number | null>(null)

const form = reactive({
  title: '',
  source: '',
  category: '英语美句',
  level: 'B1',
  textEn: '',
  textZh: '',
})

async function load() {
  loading.value = true
  try {
    rows.value = await adminListQuotes({
      keyword: keyword.value.trim() || undefined,
      builtin: builtinFilter.value === 'all' ? undefined : Number(builtinFilter.value),
    })
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.source = ''
  form.category = '英语美句'
  form.level = 'B1'
  form.textEn = ''
  form.textZh = ''
}

function openCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AdminQuoteDto) {
  editId.value = row.quoteId
  form.title = row.title
  form.source = row.source ?? ''
  form.category = row.category || '英语美句'
  form.level = row.level || 'B1'
  form.textEn = row.textEn
  form.textZh = row.textZh ?? ''
  dialogVisible.value = true
}

async function onSave() {
  if (!form.textEn.trim()) {
    ElMessage.warning('英文正文不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim() || undefined,
      source: form.source.trim(),
      category: form.category,
      level: form.level,
      textEn: form.textEn.trim(),
      textZh: form.textZh.trim(),
    }
    if (editId.value) {
      await adminUpdateQuote(editId.value, payload)
      ElMessage.success('名句已更新')
    } else {
      await adminCreateQuote(payload)
      ElMessage.success('名句已添加')
    }
    dialogVisible.value = false
    load()
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    saving.value = false
  }
}

function onDelete(row: AdminQuoteDto) {
  ElMessageBox.confirm(`确定删除名句「${row.title}」吗？`, '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await adminDeleteQuote(row.quoteId)
        ElMessage.success('已删除')
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
