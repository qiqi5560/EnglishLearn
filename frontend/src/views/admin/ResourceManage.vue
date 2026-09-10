<template>
  <div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索资源标题" clearable style="width: 240px" @keyup.enter="loadRows(1)">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="toolbar-right">
        <el-button @click="loadRows(1)">查询</el-button>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Upload /></el-icon>&nbsp;上传资源
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="level" label="难度" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已上架' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="success" size="small" @click="toggleStatus(row, 1)">
              上架
            </el-button>
            <el-button v-else link type="warning" size="small" @click="toggleStatus(row, 0)">
              下架
            </el-button>
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无资源数据" />
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="loadRows"
      />
    </el-card>

    <!-- 新建 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑资源' : '上传资源'" width="90%" align-center>
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入资源标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="t in categoryOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度等级">
          <el-select v-model="form.level" style="width: 100%">
            <el-option v-for="l in ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="媒体地址（可选）">
          <el-input v-model="form.mediaUrl" placeholder="音频/视频 URL" />
        </el-form-item>
        <el-form-item label="时长（秒）">
          <el-input-number v-model="form.durationSec" :min="0" style="width: 100%" />
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
  adminCreateResource,
  adminDeleteResource,
  adminListResources,
  adminUpdateResource,
  type AdminResourcePayload,
} from '@/api/modules/admin'
import type { ResourceDto } from '@/types/api'

const typeOptions = ['精听', '跟读', '口语', '动画']
const categoryOptions = ['生活', '商务', '考试', '学习']

const rows = ref<ResourceDto[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const keyword = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<AdminResourcePayload>({
  title: '',
  type: '精听',
  category: '生活',
  level: 'A2',
  mediaUrl: '',
  durationSec: 180,
})

async function loadRows(target = 1) {
  page.value = target
  loading.value = true
  try {
    const res = await adminListResources({
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      pageSize,
    })
    rows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openDialog(row?: ResourceDto) {
  editingId.value = row?.id ?? null
  form.title = row?.title ?? ''
  form.type = row?.type ?? '精听'
  form.category = row?.category ?? '生活'
  form.level = row?.level ?? 'A2'
  form.mediaUrl = row?.mediaUrl ?? ''
  form.durationSec = row?.durationSec ?? 180
  dialogVisible.value = true
}

async function onSave() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入资源标题')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, title: form.title.trim() }
    if (editingId.value) {
      await adminUpdateResource(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      // 后端资源审核规则：管理端新增的资源默认直接上架（status=1）
      await adminCreateResource({ ...payload, status: 1 })
      ElMessage.success('上传成功')
    }
    dialogVisible.value = false
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: ResourceDto, status: number) {
  try {
    await adminUpdateResource(row.id, { status })
    ElMessage.success(status === 1 ? '已上架' : '已下架')
    loadRows(page.value)
  } catch {
    /* 错误提示已由请求层统一弹出 */
  }
}

function onDelete(row: ResourceDto) {
  ElMessageBox.confirm(`确定删除资源「${row.title}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await adminDeleteResource(row.id)
        ElMessage.success('删除成功')
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
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;

  .toolbar-right {
    display: flex;
    gap: 8px;
  }
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
