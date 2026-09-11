<template>
  <div class="admin-login">
    <el-card class="login-card" shadow="never">
      <h2 class="login-title">管理后台登录</h2>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="管理员手机号">
          <el-input v-model="form.phone" placeholder="请输入管理员手机号" maxlength="11" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })

async function onLogin() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    await userStore.adminLogin({ phone: form.phone, password: form.password })
    ElMessage.success('管理员登录成功')
    router.push('/admin/dashboard')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.admin-login {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface);
}

.login-card {
  width: 360px;
  border-radius: var(--radius-lg);
}

.login-title {
  text-align: center;
  margin: 0 0 20px;
}

.login-btn {
  width: 100%;
}
</style>
