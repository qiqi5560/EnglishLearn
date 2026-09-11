<template>
  <div class="guardian">
    <AppHeader title="家长管控" back />

    <div class="guardian-body">
      <!-- 已绑定 -->
      <template v-if="userStore.userInfo?.guardianId">
        <el-result icon="success" title="已绑定监护人">
          <template #sub-title>
            <p class="text-muted">监护人可查看练习报表并设置使用时长与提醒</p>
          </template>
        </el-result>
        <div class="btn-row">
          <el-button class="wide-btn" @click="unbindVisible = true">解除绑定</el-button>
        </div>
      </template>

      <!-- 未绑定：绑定流程 -->
      <template v-else>
        <el-empty description="该账号尚未绑定监护人" />
        <el-card shadow="never">
          <div class="bind-tip text-muted">
            请输入监护人的手机号并获取验证码完成授权绑定。监护人手机号收到的验证码为演示固定值 123456。
          </div>
          <el-form label-position="top">
            <el-form-item label="监护人手机号">
              <el-input v-model="form.guardianPhone" placeholder="请输入监护人手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="form.code" placeholder="验证码" maxlength="6">
                <template #append>
                  <el-button :disabled="sending || countdown > 0" @click="onSendCode">
                    {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
          <el-button type="primary" class="wide-btn" :loading="binding" @click="onBind">立即绑定</el-button>
        </el-card>
      </template>
    </div>

    <!-- 解除绑定确认 -->
    <el-dialog v-model="unbindVisible" title="解除绑定" width="80%" align-center>
      <p>确定要解除与监护人的绑定吗？解除后监护管理功能将关闭。</p>
      <template #footer>
        <el-button @click="unbindVisible = false">取消</el-button>
        <el-button type="danger" :loading="unbinding" @click="onUnbind">解除绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const sending = ref(false)
const binding = ref(false)
const unbinding = ref(false)
const unbindVisible = ref(false)
const countdown = ref(0)
const form = reactive({ guardianPhone: '', code: '' })

let timer: ReturnType<typeof setInterval> | undefined

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

async function onSendCode() {
  if (sending.value || countdown.value > 0) return
  if (!/^1\d{10}$/.test(form.guardianPhone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  sending.value = true
  try {
    await userStore.sendCode(form.guardianPhone)
    ElMessage.success('验证码已发送（演示固定为 123456）')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = undefined
      }
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function onBind() {
  if (!/^1\d{10}$/.test(form.guardianPhone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!form.code) {
    ElMessage.warning('请输入监护人验证码')
    return
  }
  binding.value = true
  try {
    await userStore.bindGuardian({ guardianPhone: form.guardianPhone, code: form.code })
    ElMessage.success('绑定成功')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    binding.value = false
  }
}

async function onUnbind() {
  unbinding.value = true
  try {
    // 演示环境暂未提供解绑接口，仅提示说明
    ElMessage.info('演示环境暂不支持解除绑定，请与监护人沟通处理')
    unbindVisible.value = false
  } finally {
    unbinding.value = false
  }
}
</script>

<style scoped lang="scss">
.guardian-body {
  padding: 16px;
  min-height: 60vh;
}

.btn-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wide-btn {
  width: 100%;
}

.bind-tip {
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 12px;
}
</style>
