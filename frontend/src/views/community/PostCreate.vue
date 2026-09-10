<template>
  <div class="post-create">
    <AppHeader title="发布帖子" back />

    <div class="create-body">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="一句话概括你的内容" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="话题标签">
          <el-select v-model="form.topic" placeholder="选择话题">
            <el-option v-for="t in communityStore.topics" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="分享你的学习经验…" />
        </el-form-item>
        <el-form-item label="添加图片">
          <el-upload action="#" list-type="picture-card" :auto-upload="false">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>

      <el-button type="primary" class="submit-btn" :loading="submitting" @click="onSubmit">发布</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/base/AppHeader.vue'
import { useCommunityStore } from '@/stores/community'

const router = useRouter()
const communityStore = useCommunityStore()

const submitting = ref(false)
const form = reactive({ title: '', topic: '', content: '' })

async function onSubmit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  if (!form.topic) {
    ElMessage.warning('请选择话题标签')
    return
  }
  submitting.value = true
  try {
    await communityStore.publish({
      title: form.title.trim(),
      content: form.content.trim(),
      topic: form.topic,
    })
    ElMessage.success('发布成功')
    router.push('/community')
  } catch {
    /* 错误提示已由请求层统一弹出 */
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!communityStore.topics.length) communityStore.loadTopics()
})
</script>

<style scoped lang="scss">
.create-body {
  padding: 16px;
}

.submit-btn {
  width: 100%;
}
</style>
