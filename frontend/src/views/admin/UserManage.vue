<template>
  <div>
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索手机号 / 昵称"
        clearable
        style="width: 220px"
        @keyup.enter="loadRows(1)"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="roleFilter" clearable placeholder="角色" style="width: 140px" @change="loadRows(1)">
        <el-option label="学习者" value="learner" />
        <el-option label="管理员" value="admin" />
        <el-option label="监护人" value="guardian" />
      </el-select>
      <el-button @click="loadRows(1)">查询</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="年龄段" width="100">
          <template #default="{ row }">
            {{ ageGroupMeta[row.ageGroup] ?? row.ageGroup }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ roleMeta[row.role] ?? row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="70" align="center">
          <template #default="{ row }">{{ row.level || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.userId !== userStore.userInfo?.userId" link type="primary" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.userId !== userStore.userInfo?.userId"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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
    <el-dialog v-model="editVisible" title="编辑用户" width="80%" align-center>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminListUsers, adminUpdateUser } from '@/api/modules/admin'
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
const editVisible = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const editForm = reactive({ nickname: '', userRole: 'learner' })

async function loadRows(target = 1) {
  page.value = target
  loading.value = true
  try {
    const res = await adminListUsers({
      keyword: keyword.value.trim() || undefined,
      role: roleFilter.value || undefined,
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

onMounted(() => loadRows(1))
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
